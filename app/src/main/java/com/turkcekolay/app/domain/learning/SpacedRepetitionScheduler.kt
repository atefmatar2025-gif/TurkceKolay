package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearningState

/**
 * Deterministic, side-effect free spaced repetition.
 *
 * The item climbs a ladder of intervals with each correct, *due* answer.
 * A wrong answer sends it back to the first rung (immediate repetition) and back to LEARNING.
 * A correct answer given before the item is due does not climb the ladder: cramming in one
 * session must not make a word look mastered.
 */
class SpacedRepetitionScheduler(
    private val intervals: List<Long> = DEFAULT_INTERVALS,
) {
    init {
        require(intervals.size >= 3) { "Need at least three intervals" }
    }

    val lastStep: Int get() = intervals.lastIndex

    fun onAnswer(
        progress: ItemProgress,
        correct: Boolean,
        now: Long,
        exerciseType: ExerciseType? = null,
    ): ItemProgress {
        if (!correct) {
            return progress.copy(
                state = LearningState.LEARNING,
                repetitions = 0,
                incorrectCount = progress.incorrectCount + 1,
                lapses = progress.lapses + 1,
                confidence = (progress.confidence * 0.5f).coerceIn(0f, 1f),
                lastExerciseType = exerciseType ?: progress.lastExerciseType,
                lastReviewedAt = now,
                nextReviewAt = now + intervals[0],
            )
        }

        val due = progress.nextReviewAt == null || now >= progress.nextReviewAt
        if (!due) {
            // Early practice: reward a little confidence but keep the schedule.
            return progress.copy(
                correctCount = progress.correctCount + 1,
                confidence = (progress.confidence + (1f - progress.confidence) * 0.1f).coerceIn(0f, 1f),
                lastExerciseType = exerciseType ?: progress.lastExerciseType,
                lastReviewedAt = now,
            )
        }

        val step = (progress.repetitions + 1).coerceAtMost(lastStep)
        val confidence = (progress.confidence + (1f - progress.confidence) * 0.3f).coerceIn(0f, 1f)
        val state = when {
            step >= lastStep && confidence >= MASTERED_CONFIDENCE -> LearningState.MASTERED
            step >= 2 -> LearningState.REVIEW
            else -> LearningState.LEARNING
        }
        return progress.copy(
            state = state,
            repetitions = step,
            correctCount = progress.correctCount + 1,
            confidence = confidence,
            lastExerciseType = exerciseType ?: progress.lastExerciseType,
            lastReviewedAt = now,
            nextReviewAt = now + intervals[step],
        )
    }

    companion object {
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE
        const val MASTERED_CONFIDENCE = 0.8f

        /** Immediate (10 min), 1, 3, 7, 14, 30 days. */
        val DEFAULT_INTERVALS = listOf(10 * MINUTE, DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)
    }
}
