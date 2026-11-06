package com.turkcekolay.app.features.aitutor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.ai.AiTutorRepository
import com.turkcekolay.app.ai.ConversationManager
import com.turkcekolay.app.ai.TutorMessage
import com.turkcekolay.app.ai.TutorMode
import com.turkcekolay.app.ai.TutorRole
import com.turkcekolay.app.core.designsystem.EmptyState
import com.turkcekolay.app.core.designsystem.LearningCharacter
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiTutorViewModel @Inject constructor(private val repository: AiTutorRepository) : ViewModel() {
    private val manager = ConversationManager()
    val messages: StateFlow<List<TutorMessage>> = manager.messages
    val available: Boolean get() = repository.isAvailable

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun send(text: String, mode: TutorMode = TutorMode.FREE_CHAT) {
        val clean = text.trim()
        if (clean.isEmpty() || _sending.value) return
        manager.addLearner(clean)
        _sending.value = true
        viewModelScope.launch {
            repository.ask(manager.wireHistory(), mode)
                .onSuccess { manager.addTutor(it) }
                .onFailure { manager.addError("تعذّر الوصول إلى المعلّم الآن. تحقّق من الاتصال وحاول مرة أخرى. الدروس تعمل دون إنترنت.") }
            _sending.value = false
        }
    }
}

private data class QuickAction(val label: String, val prompt: String, val mode: TutorMode)

private val quickActions = listOf(
    QuickAction("اشرح كلمة", "ما معنى كلمة: ", TutorMode.EXPLAIN_WORD),
    QuickAction("صحّح جملتي", "صحّح هذه الجملة: ", TutorMode.CORRECT_SENTENCE),
    QuickAction("اختبار قصير", "أعطني اختبارًا قصيرًا من 3 أسئلة على كلماتي", TutorMode.QUIZ),
    QuickAction("محادثة بسيطة", "لنتحدث بالتركية بجمل قصيرة جدًا", TutorMode.FREE_CHAT),
    QuickAction("ماذا أراجع؟", "ماذا تقترح أن أراجع اليوم؟", TutorMode.SUGGEST_REVIEW),
)

@Composable
fun AiTutorScreen(viewModel: AiTutorViewModel = hiltViewModel()) {
    if (!viewModel.available) {
        Column(Modifier.fillMaxSize()) {
            TkTopBar("المعلّم الذكي")
            EmptyState(
                "المعلّم الذكي غير مفعّل",
                "هذه ميزة اختيارية تحتاج إلى اتصال بخادم المعلّم. كل الدروس والتمارين والمراجعة تعمل بالكامل دون إنترنت.",
            )
        }
        return
    }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(TutorMode.FREE_CHAT) }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Column(Modifier.fillMaxSize().imePadding()) {
        TkTopBar("المعلّم الذكي")
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { m -> MessageBubble(m) }
            if (sending) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LearningCharacter(size = 36.dp)
                    CircularProgressIndicator(Modifier.padding(start = 8.dp).widthIn(max = 24.dp))
                }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickActions) { a ->
                AssistChip(onClick = { input = a.prompt; mode = a.mode }, label = { Text(a.label) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب سؤالك بالعربية أو جملة بالتركية…") },
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
            )
            FilledIconButton(
                onClick = { viewModel.send(input, mode); input = ""; mode = TutorMode.FREE_CHAT },
                enabled = !sending && input.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp),
            ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال") }
        }
        Text(
            "قد يخطئ المعلّم الذكي أحيانًا. للمفردات الأساسية اعتمد على الدروس.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun MessageBubble(m: TutorMessage) {
    val mine = m.role == TutorRole.LEARNER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
        Text(
            m.text,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    when {
                        m.isError -> TkColors.OrangeSoft
                        mine -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    RoundedCornerShape(18.dp),
                )
                .padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
