package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.learning.SpacedRepetitionScheduler.Companion.DAY
import com.turkcekolay.app.domain.learning.SpacedRepetitionScheduler.Companion.MINUTE
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearningState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    private val scheduler = SpacedRepetitionScheduler()
    private val t0 = 1_700_000_000_000L

    @Test
    fun `first correct answer schedules review for tomorrow`() {
        val p = scheduler.onAnswer(ItemProgress("w_elma"), correct = true, now = t0)
        assertEquals(LearningState.LEARNING, p.state)
        assertEquals(1, p.repetitions)
        assertEquals(t0 + DAY, p.nextReviewAt)
        assertEquals(1, p.correctCount)
    }

    @Test
    fun `wrong answer returns item to learning with immediate repetition`() {
        val reviewed = ItemProgress("w_elma", state = LearningState.REVIEW, repetitions = 3, confidence = 0.7f, nextReviewAt = t0)
        val p = scheduler.onAnswer(reviewed, correct = false, now = t0, exerciseType = ExerciseType.WORD_SELECTION)
        assertEquals(LearningState.LEARNING, p.state)
        assertEquals(0, p.repetitions)
        assertEquals(t0 + 10 * MINUTE, p.nextReviewAt)
        assertEquals(1, p.lapses)
        assertEquals(0.35f, p.confidence, 0.0001f)
        assertEquals(ExerciseType.WORD_SELECTION, p.lastExerciseType)
    }

    @Test
    fun `intervals follow 1, 3, 7, 14, 30 days when answered on time`() {
        var p = ItemProgress("w_su")
        var now = t0
        val expected = listOf(1L, 3L, 7L, 14L, 30L)
        for (days in expected) {
            p = scheduler.onAnswer(p, correct = true, now = now)
            assertEquals(now + days * DAY, p.nextReviewAt)
            now = p.nextReviewAt!!
        }
    }

    @Test
    fun `item becomes mastered only after the full ladder with high confidence`() {
        var p = ItemProgress("w_ev")
        var now = t0
        repeat(4) {
            p = scheduler.onAnswer(p, correct = true, now = now)
            assertNotEquals(LearningState.MASTERED, p.state)
            now = p.nextReviewAt!!
        }
        p = scheduler.onAnswer(p, correct = true, now = now)
        assertEquals(LearningState.MASTERED, p.state)
    }

    @Test
    fun `answering early does not climb the ladder`() {
        val first = scheduler.onAnswer(ItemProgress("w_ekmek"), correct = true, now = t0)
        val early = scheduler.onAnswer(first, correct = true, now = t0 + MINUTE)
        assertEquals(first.repetitions, early.repetitions)
        assertEquals(first.nextReviewAt, early.nextReviewAt)
        assertEquals(2, early.correctCount)
        assertTrue(early.confidence > first.confidence)
    }

    @Test
    fun `cramming in one session can never produce a mastered word`() {
        var p = ItemProgress("w_kitap")
        repeat(50) { i -> p = scheduler.onAnswer(p, correct = true, now = t0 + i * MINUTE) }
        assertNotEquals(LearningState.MASTERED, p.state)
        assertEquals(1, p.repetitions)
    }

    @Test
    fun `repeated forgetting flags the item for reinforcement`() {
        var p = ItemProgress("w_kapi", confidence = 0.6f)
        p = scheduler.onAnswer(p, correct = false, now = t0)
        p = scheduler.onAnswer(p, correct = false, now = t0 + 20 * MINUTE)
        assertTrue(p.needsReinforcement)
    }

    @Test
    fun `scheduling is deterministic`() {
        val a = scheduler.onAnswer(ItemProgress("x"), true, t0)
        val b = scheduler.onAnswer(ItemProgress("x"), true, t0)
        assertEquals(a, b)
    }
}
