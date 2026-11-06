package com.turkcekolay.app.features.exercises

import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.domain.learning.ExerciseGenerator
import com.turkcekolay.app.domain.learning.ExerciseSessionEngine
import com.turkcekolay.app.domain.model.Exercise
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.repository.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PracticeUi(
    val exercise: Exercise?,
    val progress: Float,
    val finished: Boolean,
    val scorePercent: Int,
    val correctFirstTry: Int,
    val scoredItems: Int,
)

/** Connects the pure [ExerciseSessionEngine] to persistence. Shared by lessons and smart review. */
class PracticeSession(
    exercises: List<Exercise>,
    private val itemsById: Map<String, LearnItem>,
    private val pool: List<LearnItem>,
    private val generator: ExerciseGenerator,
    private val quietMode: Boolean,
    private val lessonId: String?,
    private val progress: ProgressRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val engine = ExerciseSessionEngine(exercises) { exercise, itemId ->
        itemsById[itemId]?.let { generator.alternativeFor(it, exercise.type, pool, quietMode) }
    }
    private var exerciseStartedAt = clock.now()

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<PracticeUi> = _state.asStateFlow()

    private fun snapshot() = PracticeUi(
        exercise = engine.current,
        progress = engine.progress,
        finished = engine.isFinished,
        scorePercent = engine.scorePercent,
        correctFirstTry = engine.firstTryCorrect,
        scoredItems = engine.firstTryTotal,
    )

    fun answer(results: Map<String, Boolean>, selected: String) {
        val exercise = engine.current ?: return
        val seconds = ((clock.now() - exerciseStartedAt) / 1000).toInt().coerceIn(0, 600)
        val fresh = engine.submit(results)
        scope.launch {
            if (results.isEmpty()) {
                progress.recordAnswer(
                    itemId = exercise.targetItemIds.firstOrNull(), exerciseId = exercise.id, exerciseType = exercise.type,
                    lessonId = lessonId, selectedAnswer = selected, correct = true, timeSpentSeconds = seconds, updateMemory = false,
                )
            } else {
                results.forEach { (itemId, correct) ->
                    progress.recordAnswer(
                        itemId = itemId, exerciseId = exercise.id, exerciseType = exercise.type, lessonId = lessonId,
                        selectedAnswer = selected, correct = correct, timeSpentSeconds = seconds,
                        // Only first attempts move the memory schedule; retries are logged as attempts.
                        updateMemory = exercise.type.updatesMemory && itemId in fresh,
                    )
                }
            }
        }
        _state.value = snapshot()
    }

    fun next() {
        engine.advance()
        exerciseStartedAt = clock.now()
        _state.value = snapshot()
    }
}

/** Measures active learning time in chunks, capping idle gaps. */
class SessionTimer(private val clock: Clock) {
    private var start = clock.now()

    fun takeSeconds(): Int {
        val now = clock.now()
        val seconds = ((now - start) / 1000).toInt().coerceIn(0, MAX_CHUNK_SECONDS)
        start = now
        return seconds
    }

    companion object { const val MAX_CHUNK_SECONDS = 30 * 60 }
}
