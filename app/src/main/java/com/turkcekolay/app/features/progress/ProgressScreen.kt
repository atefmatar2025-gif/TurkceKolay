package com.turkcekolay.app.features.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.core.designsystem.AchievementBadge
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.SectionTitle
import com.turkcekolay.app.core.designsystem.StatChip
import com.turkcekolay.app.core.designsystem.TkCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.domain.learning.Achievement
import com.turkcekolay.app.domain.learning.Achievements
import com.turkcekolay.app.domain.learning.LearnerStats
import com.turkcekolay.app.domain.learning.StreakCalculator
import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LessonType
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DayBar(val label: String, val minutes: Int)

data class ProgressUi(
    val loading: Boolean = true,
    val byState: Map<LearningState, Int> = emptyMap(),
    val learned: Int = 0,
    val totalXp: Int = 0,
    val streak: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val week: List<DayBar> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
)

private val ARABIC_DAYS = mapOf(1 to "إثن", 2 to "ثلا", 3 to "أرب", 4 to "خمي", 5 to "جمع", 6 to "سبت", 7 to "أحد")

@HiltViewModel
class ProgressViewModel @Inject constructor(
    content: ContentRepository,
    progress: ProgressRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<ProgressUi> = combine(
        content.observeCurriculum(),
        progress.observeItemProgress(),
        progress.observeLessonProgress(),
        progress.observeDailyStats(),
    ) { levels, items, lessons, daily ->
        val allLessons = levels.flatMap { l -> l.modules.flatMap { it.lessons } }
        val completed = allLessons.count { lessons[it.id]?.status == LessonStatus.COMPLETED }
        val alphabetLessons = allLessons.filter { it.type == LessonType.ALPHABET }
        val conversationsDone = allLessons.count { it.type == LessonType.CONVERSATION && lessons[it.id]?.status == LessonStatus.COMPLETED }
        val byState = items.values.groupingBy { it.state }.eachCount()
        val today = clock.today()
        val streak = StreakCalculator.currentStreak(StreakCalculator.activeDates(daily), today)
        val totalXp = daily.sumOf { it.xp }
        val week = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayBar(ARABIC_DAYS[d.dayOfWeek.value].orEmpty(), (daily.firstOrNull { it.date == d.toString() }?.learningSeconds ?: 0) / 60)
        }
        val learned = Achievements.learnedCount(items.values)
        ProgressUi(
            loading = false,
            byState = byState,
            learned = learned,
            totalXp = totalXp,
            streak = streak,
            completedLessons = completed,
            totalLessons = allLessons.size,
            week = week,
            achievements = Achievements.evaluate(
                LearnerStats(
                    completedLessons = completed,
                    alphabetCompleted = alphabetLessons.isNotEmpty() && alphabetLessons.all { lessons[it.id]?.status == LessonStatus.COMPLETED },
                    learnedItems = learned,
                    masteredItems = byState[LearningState.MASTERED] ?: 0,
                    totalXp = totalXp,
                    streak = streak,
                    conversationsCompleted = conversationsDone,
                ),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingState(); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(110.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Text("تقدّمي", style = MaterialTheme.typography.headlineMedium) }
        item { StatChip("🔥", "${s.streak}", "أيام متتالية") }
        item { StatChip("⭐", "${s.totalXp}", "نقاط الخبرة") }
        item { StatChip("📘", "${s.learned}", "كلمات تعلّمتها") }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("الدروس: ${s.completedLessons} من ${s.totalLessons}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TkProgressIndicator(if (s.totalLessons == 0) 0f else s.completedLessons.toFloat() / s.totalLessons)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { MemoryCard(s.byState) }
        item(span = { GridItemSpan(maxLineSpan) }) { WeekChart(s.week) }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("الإنجازات") }
        items(s.achievements, key = { it.id }) { a -> AchievementBadge(a.emoji, a.title, a.description, a.unlocked) }
    }
}

@Composable
private fun MemoryCard(byState: Map<LearningState, Int>) {
    TkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("حالة ذاكرتي", style = MaterialTheme.typography.titleMedium)
            listOf(
                Triple(LearningState.LEARNING, "قيد التعلّم", TkColors.Orange),
                Triple(LearningState.REVIEW, "في المراجعة", TkColors.Turquoise),
                Triple(LearningState.MASTERED, "متقنة", TkColors.Success),
            ).forEach { (state, label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(14.dp).height(14.dp).background(color, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(label, modifier = Modifier.weight(1f))
                    Text("${byState[state] ?: 0}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "لا تُعدّ الكلمة «متقنة» إلا بعد إجابات صحيحة متكررة على مدى أسابيع — مشاهدة البطاقة وحدها لا تكفي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekChart(week: List<DayBar>) {
    TkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("دقائق التعلّم هذا الأسبوع", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                week.forEach { d ->
                    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("${d.minutes}", style = MaterialTheme.typography.labelSmall)
                        Box(
                            Modifier
                                .width(22.dp)
                                .fillMaxHeight(fraction = (d.minutes.toFloat() / max).coerceIn(0.03f, 0.8f))
                                .background(if (d.minutes > 0) TkColors.Turquoise else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        )
                        Text(d.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
