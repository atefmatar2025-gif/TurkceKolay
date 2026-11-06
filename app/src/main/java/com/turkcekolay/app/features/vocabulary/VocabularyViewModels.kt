package com.turkcekolay.app.features.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.ApplicationScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.domain.learning.ExerciseGenerator
import com.turkcekolay.app.domain.learning.PresentationModePolicy
import com.turkcekolay.app.domain.learning.Xp
import com.turkcekolay.app.domain.model.Category
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.PresentationMode
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.features.exercises.PracticeSession
import com.turkcekolay.app.features.exercises.PracticeUi
import com.turkcekolay.app.features.exercises.SessionTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CategoriesViewModel @Inject constructor(content: ContentRepository) : ViewModel() {
    val categories: StateFlow<List<Category>?> = content.observeBrowsableCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

sealed interface WordsMode {
    data object Browse : WordsMode
    data object Practice : WordsMode
    data class Result(val score: Int, val correct: Int, val total: Int, val xp: Int) : WordsMode
}

data class WordsUi(
    val loading: Boolean = true,
    val title: String = "",
    val isFavorites: Boolean = false,
    val items: List<LearnItem> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val defaultModes: Map<String, PresentationMode> = emptyMap(),
    val selectedMode: PresentationMode? = null,
    val quietMode: Boolean = false,
    val slowAudio: Boolean = false,
    val mode: WordsMode = WordsMode.Browse,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    settings: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val categoryId: String = checkNotNull(savedStateHandle["categoryId"])
    private val isFavorites = categoryId == FAVORITES

    private val selectedMode = MutableStateFlow<PresentationMode?>(null)
    private val mode = MutableStateFlow<WordsMode>(WordsMode.Browse)
    private val session = MutableStateFlow<PracticeSession?>(null)
    private var timer = SessionTimer(clock)
    private val title = MutableStateFlow(if (isFavorites) "المفضلة" else "")

    private val itemsFlow: Flow<List<LearnItem>> =
        if (isFavorites) progress.observeFavorites().mapLatest { ids -> content.getItems(ids.toList()) }
        else content.observeItemsByCategory(categoryId)

    init {
        if (!isFavorites) viewModelScope.launch { title.value = content.getCategory(categoryId)?.titleArabic.orEmpty() }
    }

    private val base = combine(itemsFlow, progress.observeFavorites(), progress.observeItemProgress(), settings.settings, title) { items, favs, prog, s, t ->
        WordsUi(
            loading = false,
            title = t,
            isFavorites = isFavorites,
            items = items,
            favorites = favs,
            defaultModes = items.associate { it.id to PresentationModePolicy.modeFor(prog[it.id], s.quietMode) },
            quietMode = s.quietMode,
            slowAudio = s.slowAudioByDefault,
        )
    }

    val state: StateFlow<WordsUi> = combine(base, selectedMode, mode) { b, sel, m -> b.copy(selectedMode = sel, mode = m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordsUi(title = title.value, isFavorites = isFavorites))

    val practice: StateFlow<PracticeUi?> = session.flatMapLatest { it?.state ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectMode(m: PresentationMode) = selectedMode.update { if (it == m) null else m }

    fun toggleFavorite(id: String) = viewModelScope.launch { progress.toggleFavorite(id) }

    fun startPractice() = viewModelScope.launch {
        val s = state.value
        val pool = content.getPool()
        val generator = ExerciseGenerator(Random(clock.now()))
        val chosen = s.items.shuffled(Random(clock.now())).take(8)
        session.value = PracticeSession(
            exercises = generator.lessonExercises(chosen, pool, s.quietMode),
            itemsById = chosen.associateBy { it.id },
            pool = pool,
            generator = generator,
            quietMode = s.quietMode,
            lessonId = null,
            progress = progress,
            clock = clock,
            scope = appScope,
        )
        timer = SessionTimer(clock)
        mode.value = WordsMode.Practice
    }

    fun answer(results: Map<String, Boolean>, selected: String) = session.value?.answer(results, selected)

    fun next() {
        val current = session.value ?: return
        current.next()
        val ui = current.state.value
        if (ui.finished) {
            val seconds = timer.takeSeconds()
            appScope.launch { progress.addLearningTime(seconds) }
            mode.value = WordsMode.Result(ui.scorePercent, ui.correctFirstTry, ui.scoredItems, ui.correctFirstTry * Xp.CORRECT_FIRST_TRY)
        }
    }

    fun stopPractice() {
        if (mode.value == WordsMode.Practice) {
            val seconds = timer.takeSeconds()
            if (seconds >= 3) appScope.launch { progress.addLearningTime(seconds) }
        }
        session.value = null
        mode.value = WordsMode.Browse
    }

    companion object { const val FAVORITES = "favorites" }
}
