package com.turkcekolay.app.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.turkcekolay.app.core.designsystem.ChoiceState
import com.turkcekolay.app.core.designsystem.LearningCharacter
import com.turkcekolay.app.core.designsystem.MascotMood
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.TextChoiceCard
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    suspend fun complete(goal: Int, quiet: Boolean) = settings.completeOnboarding(goal, quiet)
}

/**
 * Five short screens. No placement test: learners are absolute beginners.
 * Skippable at any point; choices are saved locally.
 */
@Composable
fun OnboardingScreen(
    onFinished: (startFirstLesson: Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableIntStateOf(10) }
    var quiet by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun finish(start: Boolean) {
        if (saving) return
        saving = true
        scope.launch { viewModel.complete(goal, quiet); onFinished(start) }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TkProgressIndicator((page + 1) / 5f, Modifier.weight(1f))
            if (page < 4) TextButton(onClick = { finish(false) }) { Text("تخطٍّ") }
        }
        AnimatedContent(targetState = page, modifier = Modifier.weight(1f), label = "onboarding") { p ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (p) {
                    0 -> {
                        LearningCharacter(size = 140.dp, mood = MascotMood.CHEER)
                        Spacer(Modifier.height(20.dp))
                        TurkishText("Türkçe Kolay", style = TurkishStyle.Hero, color = MaterialTheme.colorScheme.secondary)
                        Text("التركية ببساطة", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Text("أهلًا بك! أنا «كولاي»، سأرافقك في رحلتك.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                    1 -> {
                        Text("🖼️  👂  ✋", fontSize = 48.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("سنتعلّم بالصور والاستماع", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "صورة كبيرة، كلمة واحدة، وصوت تركي حقيقي.\nتمارين قصيرة، ولا عقاب على الأخطاء.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                    2 -> {
                        Text("⏱️", fontSize = 56.sp)
                        Text("كم دقيقة في اليوم؟", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        listOf(5 to "5 دقائق — بداية لطيفة", 10 to "10 دقائق — مناسب لمعظم الناس", 15 to "15 دقيقة — تقدّم أسرع").forEach { (m, label) ->
                            TextChoiceCard(
                                ChoiceOption(text = label),
                                if (goal == m) ChoiceState.CORRECT else ChoiceState.IDLE,
                                onClick = { goal = m },
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                        Text("يمكنك تغييره لاحقًا من الإعدادات.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    3 -> {
                        Text("🤫", fontSize = 56.sp)
                        Text("هل تريد وضع التعلّم الهادئ؟", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "مفيد عندما لا تستطيع تشغيل الصوت. نوقف التشغيل التلقائي وتمارين الاستماع.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        TextChoiceCard(ChoiceOption(text = "لا، أريد الصوت 🔊"), if (!quiet) ChoiceState.CORRECT else ChoiceState.IDLE, { quiet = false }, Modifier.padding(vertical = 6.dp))
                        TextChoiceCard(ChoiceOption(text = "نعم، وضع هادئ 🤫"), if (quiet) ChoiceState.CORRECT else ChoiceState.IDLE, { quiet = true }, Modifier.padding(vertical = 6.dp))
                    }
                    else -> {
                        LearningCharacter(size = 120.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("الخطوة الأولى: نستمع إلى الحروف كلها", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(10.dp))
                        TurkishText("A  B  C  Ç  D  E", style = TurkishStyle.Large, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (page < 4) {
            PrimaryButton("التالي", { page++ }, Modifier.fillMaxWidth())
        } else {
            PrimaryButton("ابدأ الخطوة الأولى", { finish(true) }, Modifier.fillMaxWidth(), enabled = !saving)
            TextButton(onClick = { finish(false) }, modifier = Modifier.fillMaxWidth()) { Text("اذهب إلى الصفحة الرئيسية") }
        }
    }
}
