@file:OptIn(ExperimentalMaterial3Api::class)

package com.turkcekolay.app.features.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.BuildConfig
import com.turkcekolay.app.audio.AudioManager
import com.turkcekolay.app.audio.TtsStatus
import com.turkcekolay.app.core.designsystem.SecondaryButton
import com.turkcekolay.app.core.designsystem.TkCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkTopBar
import com.turkcekolay.app.domain.model.SpeechSpeed
import com.turkcekolay.app.domain.model.UserSettings
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import com.turkcekolay.app.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
    val audio: AudioManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
    val ttsStatus: StateFlow<TtsStatus> = audio.ttsStatus

    fun setGoal(m: Int) = viewModelScope.launch { settings.setDailyGoal(m) }
    fun setQuiet(v: Boolean) = viewModelScope.launch { settings.setQuietMode(v) }
    fun setSpeed(v: SpeechSpeed) = viewModelScope.launch { settings.setSpeechSpeed(v) }
    fun setRepeat(v: Int) = viewModelScope.launch { settings.setRepeatCount(v) }
    fun setUnlockAll(v: Boolean) = viewModelScope.launch { settings.setUnlockAll(v) }
    fun setReminders(v: Boolean) = viewModelScope.launch {
        settings.setReminders(v)
        if (v) ReminderScheduler.enable(context) else ReminderScheduler.disable(context)
    }
    fun resetProgress() = viewModelScope.launch { progress.resetAllProgress() }
}

@Composable
fun SettingsScreen(onOpenCredits: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val tts by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setReminders(granted)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TkTopBar("الإعدادات")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("هدفي اليومي", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15).forEach { m ->
                            FilterChip(selected = s.dailyGoalMinutes == m, onClick = { viewModel.setGoal(m) }, label = { Text("$m دقائق") })
                        }
                    }
                }
            }
            TkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ToggleRow("وضع التعلّم الهادئ", "بدون تشغيل صوت تلقائي وبدون تمارين استماع", s.quietMode, viewModel::setQuiet)
                    ToggleRow("تذكير يومي بالمراجعة", "إشعار لطيف عند وجود كلمات للمراجعة", s.remindersEnabled) { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.setReminders(enabled)
                    }
                    ToggleRow("فتح كل المستويات", "لمن يعرف بعض التركية مسبقًا", s.unlockAllLevels, viewModel::setUnlockAll)
                }
            }
            TkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصوت", style = MaterialTheme.typography.titleMedium)
                    Text("سرعة النطق", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpeechSpeed.entries.forEach { sp ->
                            FilterChip(selected = s.speechSpeed == sp, onClick = { viewModel.setSpeed(sp) }, label = { Text(sp.label) })
                        }
                    }
                    Text("تكرار الكلمة في البطاقات", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "مرة", 2 to "مرتان", 3 to "3 مرات").forEach { (n, label) ->
                            FilterChip(selected = s.repeatCount == n, onClick = { viewModel.setRepeat(n) }, label = { Text(label) })
                        }
                    }
                    val (label, ok) = when (tts) {
                        TtsStatus.READY -> "الصوت التركي (TTS) جاهز ✅" to true
                        TtsStatus.INITIALIZING -> "جارٍ تجهيز الصوت التركي…" to true
                        TtsStatus.MISSING_DATA -> "بيانات الصوت التركي غير مثبّتة" to false
                        TtsStatus.NOT_SUPPORTED -> "محرك النطق في جهازك لا يدعم التركية" to false
                        TtsStatus.ERROR -> "لا يوجد محرك نطق على الجهاز" to false
                    }
                    Text(label, color = if (ok) MaterialTheme.colorScheme.onSurface else TkColors.Gentle)
                    Text(
                        "نستخدم تسجيلات ناطقين أصليين عند توفرها، ثم الصوت التركي في جهازك كبديل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!ok) {
                        SecondaryButton("تثبيت الصوت التركي", {
                            runCatching { context.startActivity(viewModel.audio.installVoiceIntent()) }
                        }, Modifier.fillMaxWidth())
                    }
                    SecondaryButton("إعادة فحص الصوت", { viewModel.audio.refreshTts() }, Modifier.fillMaxWidth())
                }
            }
            TkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("البيانات", style = MaterialTheme.typography.titleMedium)
                    Text("يُحفظ تقدّمك على هذا الجهاز فقط. لا حاجة لإنشاء حساب.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onOpenCredits) { Text("مصادر الصور والأصوات") }
                    TextButton(onClick = { confirmReset = true }) { Text("مسح كل التقدّم", color = TkColors.Gentle) }
                }
            }
            Text(
                "Türkçe Kolay ${BuildConfig.VERSION_NAME} · صور حقيقية من Wikimedia Commons وتسجيلات ناطقين أصليين من Lingua Libre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("مسح كل التقدّم؟") },
            text = { Text("ستُحذف الكلمات المتعلَّمة والدروس المكتملة والمفضلة. لا يمكن التراجع.") },
            confirmButton = { TextButton(onClick = { viewModel.resetProgress(); confirmReset = false }) { Text("مسح", color = TkColors.Gentle) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
