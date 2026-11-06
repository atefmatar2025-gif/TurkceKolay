package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.Exercise

/**
 * Runs a queue of exercises one at a time.
 *
 * - Only the first answer for an item counts towards memory and score.
 * - An item answered wrongly is re-queued once, with a different exercise type (via [requeue]),
 *   so the learner gets another attempt without being punished.
 */
class ExerciseSessionEngine(
    initial: List<Exercise>,
    private val requeue: (exercise: Exercise, itemId: String) -> Exercise?,
) {
    private val queue = ArrayDeque(initial)
    private val firstResults = LinkedHashMap<String, Boolean>()
    private val requeued = mutableSetOf<String>()
    private var completed = 0

    val current: Exercise? get() = queue.firstOrNull()
    val isFinished: Boolean get() = queue.isEmpty()
    val completedCount: Int get() = completed
    val remainingCount: Int get() = queue.size

    val progress: Float
        get() {
            val total = completed + queue.size
            return if (total == 0) 1f else completed.toFloat() / total
        }

    /**
     * Records the outcome of the current exercise (itemId → correct).
     * Returns only the results that are first attempts, which callers persist.
     */
    fun submit(results: Map<String, Boolean>): Map<String, Boolean> {
        val exercise = current ?: return emptyMap()
        val fresh = results.filterKeys { it !in firstResults }
        firstResults.putAll(fresh)
        results.filterValues { !it }.keys.forEach { itemId ->
            if (requeued.add(itemId)) requeue(exercise, itemId)?.let { queue.addLast(it) }
        }
        return fresh
    }

    fun advance() {
        if (queue.isNotEmpty()) {
            queue.removeFirst()
            completed++
        }
    }

    val firstTryCorrect: Int get() = firstResults.values.count { it }
    val firstTryTotal: Int get() = firstResults.size

    /** Percentage of items answered correctly on the first try (100 when nothing was scored). */
    val scorePercent: Int
        get() = if (firstResults.isEmpty()) 100 else (firstTryCorrect * 100) / firstTryTotal
}
