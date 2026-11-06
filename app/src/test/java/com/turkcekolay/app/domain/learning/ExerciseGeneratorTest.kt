package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.AudioRef
import com.turkcekolay.app.domain.model.ChoiceExercise
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemKind
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LetterArrangementExercise
import com.turkcekolay.app.domain.model.ListenAndRepeatExercise
import com.turkcekolay.app.domain.model.MatchingExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExerciseGeneratorTest {

    private fun word(id: String, tr: String, ar: String, emoji: String, ex: String? = null, exAr: String? = null, cat: String = "c") =
        LearnItem(id, ItemKind.WORD, tr, ar, cat, "emoji:$emoji", AudioRef(id, "vocabulary", tr), ex, exAr)

    private val pool = listOf(
        word("w_elma", "Elma", "تفاحة", "🍎", "Bu bir elma.", "هذه تفاحة."),
        word("w_su", "Su", "ماء", "💧", "Su içiyorum.", "أنا أشرب الماء."),
        word("w_ev", "Ev", "منزل", "🏠", "Bu benim evim.", "هذا منزلي."),
        word("w_kitap", "Kitap", "كتاب", "📖", "Bu bir kitap.", "هذا كتاب."),
        word("w_kedi", "Kedi", "قطة", "🐱", "Kedi küçük.", "القطة صغيرة."),
        word("w_cay", "Çay", "شاي", "🍵", "Çay sıcak.", "الشاي ساخن."),
    )

    @Test
    fun `lesson exercises cover every item and include matching`() {
        val ex = ExerciseGenerator(Random(1)).lessonExercises(pool.take(4), pool, quietMode = false)
        val covered = ex.flatMap { it.targetItemIds }.toSet()
        assertTrue(pool.take(4).all { it.id in covered })
        assertTrue(ex.any { it is MatchingExercise })
        assertTrue(ex.any { it is ListenAndRepeatExercise })
    }

    @Test
    fun `quiet mode never produces audio-dependent exercises`() {
        repeat(20) { seed ->
            val ex = ExerciseGenerator(Random(seed)).lessonExercises(pool, pool, quietMode = true)
            assertFalse(ex.any { it.type.requiresAudio })
        }
    }

    @Test
    fun `choice exercises have one correct answer and distinct options`() {
        repeat(30) { seed ->
            val ex = ExerciseGenerator(Random(seed)).lessonExercises(pool, pool, quietMode = false)
            ex.filterIsInstance<ChoiceExercise>().forEach { c ->
                assertTrue(c.correctIndex in c.options.indices)
                assertEquals(c.options.size, c.options.distinct().size)
                assertTrue(c.options.size >= 3)
            }
        }
    }

    @Test
    fun `picture selection never offers two identical pictures`() {
        val g = ExerciseGenerator(Random(3))
        val item = pool.first()
        val ex = g.build(item, ExerciseType.PICTURE_SELECTION, pool) as ChoiceExercise
        assertEquals(ex.options.size, ex.options.map { it.image }.distinct().size)
        assertEquals("emoji:🍎", ex.options[ex.correctIndex].image)
    }

    @Test
    fun `sentence completion blanks the whole word only`() {
        val g = ExerciseGenerator(Random(5))
        val span = g.blankSpan(pool.first())
        assertNotNull(span)
        assertEquals("elma", span!!.answer)
        assertEquals("Bu bir ${ExerciseGenerator.BLANK}.", span.blanked)
        // "Anne" must not match inside "Annem"
        val anne = word("w_anne", "Anne", "أم", "👩", "Annem evde.", "أمي في المنزل.")
        assertEquals(null, g.blankSpan(anne))
    }

    @Test
    fun `letter arrangement uses Turkish lowercase rules`() {
        val g = ExerciseGenerator(Random(7))
        val irmak = word("w_irmak", "Irmak", "نهر", "🏞️")
        val ex = g.build(irmak, ExerciseType.LETTER_ARRANGEMENT, pool) as LetterArrangementExercise
        assertEquals("ırmak", ex.answer) // dotless ı, not "irmak"
        assertEquals(ex.answer.toList().sorted(), ex.tiles.map { it.single() }.sorted())
        assertNotEquals(ex.answer, ex.tiles.joinToString(""))
    }

    @Test
    fun `review avoids repeating the last exercise type`() {
        val g = ExerciseGenerator(Random(11))
        val entries = pool.take(3).map { it to ItemProgress(it.id, lastExerciseType = ExerciseType.PICTURE_SELECTION) }
        val ex = g.reviewExercises(entries, pool, quietMode = false)
        ex.filter { it !is MatchingExercise }.forEach { assertNotEquals(ExerciseType.PICTURE_SELECTION, it.type) }
    }

    @Test
    fun `alternative exercise uses a different type`() {
        val g = ExerciseGenerator(Random(2))
        repeat(10) {
            val alt = g.alternativeFor(pool[1], ExerciseType.WORD_SELECTION, pool, quietMode = false)
            assertNotNull(alt)
            assertNotEquals(ExerciseType.WORD_SELECTION, alt!!.type)
        }
    }

    @Test
    fun `same seed gives the same lesson`() {
        val a = ExerciseGenerator(Random(42)).lessonExercises(pool, pool, false)
        val b = ExerciseGenerator(Random(42)).lessonExercises(pool, pool, false)
        assertEquals(a, b)
    }
}
