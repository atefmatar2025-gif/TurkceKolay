package com.turkcekolay.app.features.lessons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.ApplicationScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.domain.learning.ExerciseGenerator
import com.turkcekolay.app.domain.learning.Xp
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.UserSettings
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.features.exercises.PracticeSession
import com.turkcekolay.app.features.exercises.PracticeUi
import com.turkcekolay.app.features.exercises.SessionTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed interface LessonPhase {
    data object Loading : LessonPhase
    data class Learn(val index: Int) : LessonPhase
    data object Practice : LessonPhase
    data class Result(val scorePercent: Int, val correct: Int, val total: Int, val xp: Int) : LessonPhase
    data class Error(val message: String) : LessonPhase
}

data class LessonUiState(
    val phase: LessonPhase = LessonPhase.Loading,
    val lesson: LessonInfo? = null,
    val items: List<LearnItem> = emptyList(),
    val settings: UserSettings = UserSettings(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val lessonId: String = checkNotNull(savedStateHandle["lessonId"])
    private val timer = SessionTimer(clock)
    private val generator = ExerciseGenerator(Random(lessonId.hashCode() xor clock.now().toInt()))
    private var pool: List<LearnItem> = emptyList()

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    val favorites: StateFlow<Set<String>> = progress.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val session = MutableStateFlow<PracticeSession?>(null)
    val practice: StateFlow<PracticeUi?> = session
        .flatMapLatest { it?.state ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val settings = settingsRepository.settings.first()
        val lesson = content.getLesson(lessonId)
        if (lesson == null) {
            _state.value = LessonUiState(phase = LessonPhase.Error("لم نجد هذا الدرس."))
            return@launch
        }
        val items = content.getItems(lesson.itemIds)
        if (items.isEmpty()) {
            _state.value = LessonUiState(phase = LessonPhase.Error("هذا الدرس فارغ حاليًا."), lesson = lesson)
            return@launch
        }
        pool = content.getPool()
        progress.markLessonStarted(lessonId)
        _state.value = LessonUiState(LessonPhase.Learn(0), lesson, items, settings)
    }

    fun toggleFavorite(itemId: String) = viewModelScope.launch { progress.toggleFavorite(itemId) }

    fun nextCard() {
        val phase = _state.value.phase as? LessonPhase.Learn ?: return
        if (phase.index < _state.value.items.lastIndex) _state.update { it.copy(phase = LessonPhase.Learn(phase.index + 1)) }
        else startPractice()
    }

    fun previousCard() {
        val phase = _state.value.phase as? LessonPhase.Learn ?: return
        if (phase.index > 0) _state.update { it.copy(phase = LessonPhase.Learn(phase.index - 1)) }
    }

    fun startPractice() {
        val s = _state.value
        val exercises = generator.lessonExercises(s.items, pool, s.settings.quietMode)
        session.value = PracticeSession(
            exercises = exercises,
            itemsById = s.items.associateBy { it.id },
            pool = pool,
            generator = generator,
            quietMode = s.settings.quietMode,
            lessonId = lessonId,
            progress = progress,
            clock = clock,
            scope = appScope,
        )
        _state.update { it.copy(phase = LessonPhase.Practice) }
    }

    fun answer(results: Map<String, Boolean>, selected: String) = session.value?.answer(results, selected)

    fun next() {
        val current = session.value ?: return
        current.next()
        val ui = current.state.value
        if (ui.finished) finish(ui)
    }

    private fun finish(ui: PracticeUi) {
        val seconds = timer.takeSeconds()
        _state.update { it.copy(phase = LessonPhase.Result(ui.scorePercent, ui.correctFirstTry, ui.scoredItems, Xp.LESSON_COMPLETED)) }
        appScope.launch {
            progress.completeLesson(lessonId, ui.scorePercent, seconds, Xp.LESSON_COMPLETED)
            progress.addLearningTime(seconds)
        }
    }

    fun restart() {
        session.value = null
        _state.update { it.copy(phase = LessonPhase.Learn(0)) }
    }

    override fun onCleared() {
        val seconds = timer.takeSeconds()
        if (seconds >= 3) appScope.launch { progress.addLearningTime(seconds) }
        super.onCleared()
    }
}
