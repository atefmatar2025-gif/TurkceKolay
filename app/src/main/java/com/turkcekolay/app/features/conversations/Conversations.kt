package com.turkcekolay.app.features.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.ApplicationScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.core.designsystem.AudioButton
import com.turkcekolay.app.core.designsystem.ChoiceState
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LessonProgressCard
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.LocalAudioManager
import com.turkcekolay.app.core.designsystem.MascotMessage
import com.turkcekolay.app.core.designsystem.MascotMood
import com.turkcekolay.app.core.designsystem.TextChoiceCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkTopBar
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.domain.learning.Xp
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.model.Conversation
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.UserSettings
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.features.exercises.SessionTimer
import com.turkcekolay.app.features.lessons.ResultContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ------------------------------------------------------------------ list

@HiltViewModel
class ConversationListViewModel @Inject constructor(private val content: ContentRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<Conversation>?>(null)
    val items: StateFlow<List<Conversation>?> = _items.asStateFlow()

    init {
        viewModelScope.launch { _items.value = content.getConversations() }
    }
}

@Composable
fun ConversationListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: ConversationListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TkTopBar("محادثات قصيرة", onBack)
        val list = items
        if (list == null) { LoadingState(); return@Column }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MascotMessage("اقرأ ما يقوله الشخص، ثم اختر الرد المناسب. لا بأس بالخطأ!") }
            items(list, key = { it.id }) { c ->
                LessonProgressCard(
                    title = c.titleArabic,
                    subtitle = "${c.turns.size} أدوار",
                    image = c.image,
                    progress = 0f,
                    completed = false,
                    locked = false,
                    onClick = { onOpen(c.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ practice

data class Line(val fromLearner: Boolean, val turkish: String, val arabic: String, val turnIndex: Int, val choiceIndex: Int?)

data class ConversationUi(
    val loading: Boolean = true,
    val conversation: Conversation? = null,
    val turn: Int = 0,
    val transcript: List<Line> = emptyList(),
    val wrongChoices: Set<Int> = emptySet(),
    val hint: String? = null,
    val finished: Boolean = false,
    val firstTryCorrect: Int = 0,
    val settings: UserSettings = UserSettings(),
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val lessonId: String? = savedStateHandle.get<String>("lessonId")?.takeIf { it != NO_LESSON }
    private val timer = SessionTimer(clock)

    private val _state = MutableStateFlow(ConversationUi())
    val state: StateFlow<ConversationUi> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val c = content.getConversation(conversationId)
            val s = settingsRepository.settings.first()
            lessonId?.let { progress.markLessonStarted(it) }
            _state.value = ConversationUi(
                loading = false,
                conversation = c,
                transcript = c?.let { listOf(Line(false, it.turns[0].turkish, it.turns[0].arabic, 0, null)) }.orEmpty(),
                settings = s,
            )
        }
    }

    fun choose(index: Int) {
        val s = _state.value
        val c = s.conversation ?: return
        if (s.finished || index in s.wrongChoices) return
        val turn = c.turns[s.turn]
        val choice = turn.choices[index]
        val firstTry = s.wrongChoices.isEmpty()
        appScope.launch {
            progress.recordAnswer(null, "${c.id}_${s.turn}", ExerciseType.CONVERSATION, lessonId, choice.turkish, choice.correct, 0, updateMemory = false)
        }
        if (!choice.correct) {
            _state.update { it.copy(wrongChoices = it.wrongChoices + index, hint = "💡 ${turn.hintArabic}") }
            return
        }
        val learnerLine = Line(true, choice.turkish, choice.arabic, s.turn, index)
        val nextTurn = s.turn + 1
        val done = nextTurn >= c.turns.size
        val transcript = s.transcript + learnerLine +
            if (done) emptyList() else listOf(Line(false, c.turns[nextTurn].turkish, c.turns[nextTurn].arabic, nextTurn, null))
        _state.update {
            it.copy(
                turn = if (done) it.turn else nextTurn,
                transcript = transcript,
                wrongChoices = emptySet(),
                hint = null,
                finished = done,
                firstTryCorrect = it.firstTryCorrect + if (firstTry) 1 else 0,
            )
        }
        if (done) {
            val seconds = timer.takeSeconds()
            val score = (_state.value.firstTryCorrect * 100) / c.turns.size
            appScope.launch {
                if (lessonId != null) progress.completeLesson(lessonId, score, seconds, Xp.CONVERSATION_COMPLETED)
                progress.addLearningTime(seconds)
            }
        }
    }

    override fun onCleared() {
        if (!_state.value.finished) {
            val seconds = timer.takeSeconds()
            if (seconds >= 3) appScope.launch { progress.addLearningTime(seconds) }
        }
        super.onCleared()
    }

    companion object { const val NO_LESSON = "none" }
}

@Composable
fun ConversationScreen(onClose: () -> Unit, viewModel: ConversationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio = LocalAudioManager.current
    if (state.loading) { LoadingState(); return }
    val c = state.conversation
    if (c == null) {
        EmptyState("عذرًا", "لم نجد هذه المحادثة.")
        return
    }
    if (state.finished) {
        ResultContent(
            title = "أكملت المحادثة! 🗣️",
            scorePercent = state.firstTryCorrect * 100 / c.turns.size,
            correct = state.firstTryCorrect,
            total = c.turns.size,
            xp = Xp.CONVERSATION_COMPLETED,
            onDone = onClose,
        )
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.transcript.size) {
        if (state.transcript.isNotEmpty()) listState.animateScrollToItem(state.transcript.lastIndex)
        val last = state.transcript.lastOrNull()
        if (last != null && !last.fromLearner && !state.settings.quietMode) audio?.play(c.turnAudio(last.turnIndex))
    }

    Column(Modifier.fillMaxSize()) {
        TkTopBar(c.titleArabic, onClose)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.transcript) { _, line -> Bubble(line, c) }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.hint?.let { MascotMessage(it, mood = MascotMood.THINKING) }
            Text("اختر ردّك:", style = MaterialTheme.typography.titleMedium)
            c.turns[state.turn].choices.forEachIndexed { i, choice ->
                TextChoiceCard(
                    option = ChoiceOption(text = choice.turkish, isTurkish = true),
                    state = if (i in state.wrongChoices) ChoiceState.WRONG else ChoiceState.IDLE,
                    onClick = { viewModel.choose(i) },
                )
            }
        }
    }
}

@Composable
private fun Bubble(line: Line, c: Conversation) {
    val bg = if (line.fromLearner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromLearner) Arrangement.Start else Arrangement.End) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(bg, RoundedCornerShape(18.dp))
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TurkishText(line.turkish, style = TurkishStyle.Medium, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                val ref = if (line.fromLearner && line.choiceIndex != null) c.choiceAudio(line.turnIndex, line.choiceIndex) else c.turnAudio(line.turnIndex)
                AudioButton(ref, size = 36.dp)
            }
            Text(line.arabic, style = MaterialTheme.typography.bodyMedium, color = if (line.fromLearner) TkColors.TurquoiseDark else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
