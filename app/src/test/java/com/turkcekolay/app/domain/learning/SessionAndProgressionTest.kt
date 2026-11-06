package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.ChoiceExercise
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.model.DailyStats
import com.turkcekolay.app.domain.model.Exercise
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LessonType
import com.turkcekolay.app.domain.model.LevelInfo
import com.turkcekolay.app.domain.model.ModuleInfo
import com.turkcekolay.app.domain.model.PresentationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionAndProgressionTest {

    private fun choice(id: String, item: String) = ChoiceExercise(
        id = id, type = ExerciseType.WORD_SELECTION, instruction = "", targetItemId = item,
        options = listOf(ChoiceOption("a"), ChoiceOption("b")), correctIndex = 0, explanation = "",
    )

    @Test
    fun `wrong item is requeued once and only first attempt is scored`() {
        var requeues = 0
        val engine = ExerciseSessionEngine(listOf(choice("e1", "x"), choice("e2", "y"))) { _: Exercise, item: String ->
            requeues++
            choice("retry_$item", item)
        }
        assertEquals(mapOf("x" to false), engine.submit(mapOf("x" to false)))
        engine.advance()
        engine.submit(mapOf("y" to true)); engine.advance()
        // retry of x: correct now, but it is not a first attempt
        assertEquals("retry_x", engine.current?.id)
        assertEquals(emptyMap<String, Boolean>(), engine.submit(mapOf("x" to true)))
        engine.advance()
        assertTrue(engine.isFinished)
        assertEquals(1, requeues)
        assertEquals(50, engine.scorePercent)
    }

    @Test
    fun `item is not requeued twice`() {
        var requeues = 0
        val engine = ExerciseSessionEngine(listOf(choice("e1", "x"))) { _: Exercise, item: String ->
            requeues++
            choice("r$requeues", item)
        }
        engine.submit(mapOf("x" to false)); engine.advance()
        engine.submit(mapOf("x" to false)); engine.advance()
        assertTrue(engine.isFinished)
        assertEquals(1, requeues)
    }

    @Test
    fun `streak counts consecutive days and survives until tonight`() {
        val today = LocalDate.of(2026, 11, 6)
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, StreakCalculator.currentStreak(dates, today))
        assertEquals(3, StreakCalculator.currentStreak(dates + today, today))
        assertEquals(0, StreakCalculator.currentStreak(setOf(today.minusDays(3)), today))
    }

    @Test
    fun `inactive days are ignored for the streak`() {
        val stats = listOf(DailyStats("2026-11-05", 0, 0, 0, 0), DailyStats("2026-11-06", 10, 60, 1, 3))
        assertEquals(setOf(LocalDate.of(2026, 11, 6)), StreakCalculator.activeDates(stats))
    }

    private fun lesson(id: String) = LessonInfo(id, "m", id, "", LessonType.VOCABULARY, 5, listOf("w"), null)
    private fun level(id: String, order: Int, open: Boolean, lessons: List<String>) =
        LevelInfo(id, id, id, "", order, open, if (lessons.isEmpty()) emptyList() else listOf(ModuleInfo("m_$id", id, "", "", "", 5, lessons.map(::lesson))))

    @Test
    fun `next level opens after half of the previous one is completed`() {
        val levels = listOf(level("l0", 0, true, listOf("a", "b", "c", "d")), level("a1", 1, false, listOf("e")), level("b1", 2, false, emptyList()))
        val done = { ids: List<String> -> ids.associateWith { LessonProgressInfo(it, LessonStatus.COMPLETED, 100, 1L) } }
        assertEquals(setOf("l0"), CurriculumUnlockPolicy.unlockedLevelIds(levels, done(listOf("a")), false))
        assertEquals(setOf("l0", "a1"), CurriculumUnlockPolicy.unlockedLevelIds(levels, done(listOf("a", "b")), false))
        // "coming soon" levels without content never open
        assertFalse("b1" in CurriculumUnlockPolicy.unlockedLevelIds(levels, emptyMap(), true))
        assertTrue("a1" in CurriculumUnlockPolicy.unlockedLevelIds(levels, emptyMap(), true))
    }

    @Test
    fun `presentation support is removed gradually`() {
        assertEquals(PresentationMode.A, PresentationModePolicy.modeFor(null, false))
        assertEquals(PresentationMode.B, PresentationModePolicy.modeFor(ItemProgress("x", state = LearningState.LEARNING), false))
        assertEquals(PresentationMode.A, PresentationModePolicy.modeFor(ItemProgress("x", state = LearningState.LEARNING, lapses = 3, confidence = 0.1f), false))
        assertEquals(PresentationMode.C, PresentationModePolicy.modeFor(ItemProgress("x", state = LearningState.REVIEW), false))
        assertEquals(PresentationMode.D, PresentationModePolicy.modeFor(ItemProgress("x", state = LearningState.MASTERED), false))
        assertEquals(PresentationMode.C, PresentationModePolicy.modeFor(ItemProgress("x", state = LearningState.MASTERED), true))
    }

    @Test
    fun `viewing a card is not learning`() {
        assertEquals(0, Achievements.learnedCount(listOf(ItemProgress("x"))))
        assertEquals(1, Achievements.learnedCount(listOf(ItemProgress("x", state = LearningState.LEARNING, correctCount = 1))))
    }
}
