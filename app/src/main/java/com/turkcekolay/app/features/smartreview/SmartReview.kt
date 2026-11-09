package com.turkcekolay.app.features.smartreview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.ApplicationScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.MascotMessage
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.SecondaryButton
import com.turkcekolay.app.core.designsystem.TkTopBar
import com.turkcekolay.app.domain.learning.ExerciseGenerator
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.UserSettings
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.features.exercises.PracticeContent
import com.turkcekolay.app.features.exercises.PracticeSession
import com.turkcekolay.app.features.exercises.PracticeUi
import com.turkcekolay.app.features.exercises.SessionTimer
import com.turkcekolay.app.features.lessons.LearnCardsContent
import com.turkcekolay.app.features.lessons.ResultContent
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed interface ReviewPhase {
    data object Loading : ReviewPhase
    data class Empty(val canPracticeWeak: Boolean) : ReviewPhase
    data class Ready(val count: Int, val weakMode: Boolean) : ReviewPhase
    /** Forgotten words are shown again (picture + audio) before they are tested. */
    data class Reteach(val items: List<LearnItem>, val index: Int) : ReviewPhase
    data object Practice : ReviewPhase
    data class Result(val score: Int, val correct: Int, val total: Int, val xp: Int) : ReviewPhase
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmartReviewViewModel @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _phase = MutableStateFlow<ReviewPhase>(ReviewPhase.Loading)
    val phase: StateFlow<ReviewPhase> = _phase.asStateFlow()

    private val session = MutableStateFlow<PracticeSession?>(null)
    val practice: StateFlow<PracticeUi?> = session.flatMapLatest { it?.state ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var settings = UserSettings()
        private set
    private var entries: List<Pair<LearnItem, ItemProgress>> = emptyList()
    private var timer = SessionTimer(clock)

    fun refresh() = viewModelScope.launch {
        if (_phase.value is ReviewPhase.Practice || _phase.value is ReviewPhase.Reteach) return@launch
        _phase.value = ReviewPhase.Loading
        settings = settingsRepository.settings.first()
        val due = progress.dueItems(MAX_ITEMS)
        if (due.isEmpty()) {
            _phase.value = ReviewPhase.Empty(canPracticeWeak = progress.weakestItems(1).isNotEmpty())
            return@launch
        }
        entries = pair(due)
        _phase.value = ReviewPhase.Ready(entries.size, weakMode = false)
    }

    fun practiceWeakest() = viewModelScope.launch {
        entries = pair(progress.weakestItems(8))
        if (entries.isNotEmpty()) begin()
    }

    fun begin() {
        val forgotten = entries.filter { it.second.needsReinforcement }.map { it.first }
        if (forgotten.isNotEmpty()) _phase.value = ReviewPhase.Reteach(forgotten, 0) else startPractice()
    }

    fun nextReteach() {
        val p = _phase.value as? ReviewPhase.Reteach ?: return
        if (p.index < p.items.lastIndex) _phase.value = p.copy(index = p.index + 1) else startPractice()
    }

    fun previousReteach() {
        val p = _phase.value as? ReviewPhase.Reteach ?: return
        if (p.index > 0) _phase.value = p.copy(index = p.index - 1)
    }

    private fun startPractice() = viewModelScope.launch {
        val pool = content.getPool()
        val generator = ExerciseGenerator(Random(clock.now()))
        session.value = PracticeSession(
            exercises = generator.reviewExercises(entries, pool, settings.quietMode),
            itemsById = entries.associate { it.first.id to it.first },
            pool = pool,
            generator = generator,
            quietMode = settings.quietMode,
            lessonId = null,
            progress = progress,
            clock = clock,
            scope = appScope,
        )
        timer = SessionTimer(clock)
        _phase.value = ReviewPhase.Practice
    }

    fun answer(results: Map<String, Boolean>, selected: String) = session.value?.answer(results, selected)

    fun next() {
        val current = session.value ?: return
        current.next()
        val ui = current.state.value
        if (ui.finished) {
            val seconds = timer.takeSeconds()
            appScope.launch { progress.addLearningTime(seconds) }
            _phase.value = ReviewPhase.Result(ui.scorePercent, ui.correctFirstTry, ui.scoredItems, ui.correctFirstTry * 10)
            session.value = null
        }
    }

    fun stop() {
        if (_phase.value is ReviewPhase.Practice) {
            val seconds = timer.takeSeconds()
            if (seconds >= 3) appScope.launch { progress.addLearningTime(seconds) }
        }
        session.value = null
        _phase.value = ReviewPhase.Loading
        refresh()
    }

    private suspend fun pair(list: List<ItemProgress>): List<Pair<LearnItem, ItemProgress>> {
        val items = content.getItems(list.map { it.vocabularyId }).associateBy { it.id }
        return list.mapNotNull { p -> items[p.vocabularyId]?.let { it to p } }
    }

    companion object { const val MAX_ITEMS = 12 }
}

@Composable
fun SmartReviewScreen(onBack: (() -> Unit)? = null, viewModel: SmartReviewViewModel = hiltViewModel()) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val practice by viewModel.practice.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    when (val p = phase) {
        ReviewPhase.Loading -> LoadingState()
        is ReviewPhase.Empty -> Column(Modifier.fillMaxSize()) {
            TkTopBar("مراجعتي الذكية", onBack)
            EmptyState(
                "لا توجد مراجعة الآن 🎉",
                "ذاكرتك في حالة جيدة. سنذكّرك عندما يحين وقت مراجعة الكلمات.",
                action = {
                    if (p.canPracticeWeak) SecondaryButton("تدرّب على أصعب الكلمات", { viewModel.practiceWeakest() })
                },
            )
        }
        is ReviewPhase.Ready -> Column(Modifier.fillMaxSize()) {
            TkTopBar("مراجعتي الذكية", onBack)
            Column(Modifier.padding(20.dp)) {
                MascotMessage("اخترت لك ${p.count} كلمات تحتاج إلى تقوية. سأغيّر طريقة السؤال كل مرة!")
                PrimaryButton("ابدأ المراجعة", viewModel::begin, Modifier.fillMaxWidth().padding(top = 20.dp))
            }
        }
        is ReviewPhase.Reteach -> LearnCardsContent(
            title = "لنتذكّر معًا",
            items = p.items,
            index = p.index,
            favorites = emptySet(),
            quietMode = viewModel.settings.quietMode,
            onToggleFavorite = {},
            onPrevious = viewModel::previousReteach,
            onNext = viewModel::nextReteach,
            onClose = viewModel::stop,
            lastLabel = "لنختبر أنفسنا",
        )
        ReviewPhase.Practice -> {
            val ui = practice
            val ex = ui?.exercise
            if (ui == null || ex == null) LoadingState()
            else PracticeContent(
                ex, ui.progress, viewModel.settings.quietMode,
                onAnswered = { r, s -> viewModel.answer(r, s) },
                onContinue = viewModel::next,
                onClose = viewModel::stop,
            )
        }
        is ReviewPhase.Result -> ResultContent("أنهيت المراجعة! 💪", p.score, p.correct, p.total, p.xp, onDone = viewModel::stop)
    }
}
