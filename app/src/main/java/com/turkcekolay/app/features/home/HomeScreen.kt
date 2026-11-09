package com.turkcekolay.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcekolay.app.core.designsystem.CategoryCard
import com.turkcekolay.app.core.designsystem.LessonProgressCard
import com.turkcekolay.app.core.designsystem.LoadingState
import com.turkcekolay.app.core.designsystem.MascotMessage
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.SectionTitle
import com.turkcekolay.app.core.designsystem.TkCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LessonType

@Composable
fun HomeScreen(
    onOpenLesson: (LessonInfo) -> Unit,
    onOpenAlphabet: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenConversations: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenReview: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingState()
        return
    }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Türkçe Kolay", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            Text("التركية ببساطة", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            MascotMessage(
                when {
                    state.dueCount > 0 -> "لديك ${state.dueCount} كلمات للمراجعة اليوم. هيا نبدأ!"
                    state.todayMinutes >= state.dailyGoalMinutes -> "حققت هدفك اليوم! 🌟 أنت رائع."
                    state.streak >= 2 -> "${state.streak} أيام متتالية! استمر 🔥"
                    else -> "خطوة صغيرة كل يوم تكفي. لنتعلّم معًا!"
                },
            )
        }
        item { DailyGoalCard(state.todayMinutes, state.dailyGoalMinutes, state.streak, state.todayXp) }

        state.nextLesson?.let { next ->
            item {
                TkCard(Modifier.fillMaxWidth(), onClick = { onOpenLesson(next) }, color = MaterialTheme.colorScheme.secondary) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("تابع التعلّم", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                            Text(next.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text("${next.estimatedMinutes} دقائق تقريبًا", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        }
                        Box(Modifier.size(56.dp).background(TkColors.Orange, CircleShape), contentAlignment = Alignment.Center) {
                            Text("▶", color = Color.White, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
        if (state.dueCount > 0) {
            item {
                PrimaryButton("مراجعتي الذكية (${state.dueCount})", onOpenReview, Modifier.fillMaxWidth())
            }
        }

        item { SectionTitle("تعلّم بطريقتك") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CategoryCard("الحروف", "Alfabe", "emoji:🔤", onOpenAlphabet, Modifier.width(130.dp)) }
                item { CategoryCard("المفردات", "Kelimeler", "emoji:🖼️", onOpenVocabulary, Modifier.width(130.dp)) }
                item { CategoryCard("المحادثات", "Konuşmalar", "emoji:💬", onOpenConversations, Modifier.width(130.dp)) }
                item { CategoryCard("المفضلة", "Favoriler", "emoji:❤️", onOpenFavorites, Modifier.width(130.dp)) }
            }
        }

        item { SectionTitle("مسار التعلّم") }
        state.levels.forEach { homeLevel ->
            val level = homeLevel.level
            item(key = level.id) {
                LevelHeader(level.code, level.title, level.description, homeLevel.unlocked, homeLevel.completion, comingSoon = level.modules.isEmpty())
            }
            if (homeLevel.unlocked) {
                level.modules.forEach { module ->
                    val done = module.lessons.count { state.lessonProgress[it.id]?.status == LessonStatus.COMPLETED }
                    val isExpanded = expanded[module.id] ?: (done < module.lessons.size && module.lessons.any { it.id == state.nextLesson?.id })
                    item(key = module.id) {
                        LessonProgressCard(
                            title = module.title,
                            subtitle = "${module.description} · $done/${module.lessons.size}",
                            image = module.image,
                            progress = if (module.lessons.isEmpty()) 0f else done.toFloat() / module.lessons.size,
                            completed = module.lessons.isNotEmpty() && done == module.lessons.size,
                            locked = false,
                            onClick = { expanded[module.id] = !isExpanded },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (isExpanded) {
                        module.lessons.forEach { lesson ->
                            item(key = lesson.id) {
                                val status = state.lessonProgress[lesson.id]?.status
                                LessonProgressCard(
                                    title = lesson.title,
                                    subtitle = lessonSubtitle(lesson),
                                    image = lessonIcon(lesson.type),
                                    progress = if (status == LessonStatus.IN_PROGRESS) 0.3f else 0f,
                                    completed = status == LessonStatus.COMPLETED,
                                    locked = false,
                                    highlighted = lesson.id == state.nextLesson?.id,
                                    onClick = { onOpenLesson(lesson) },
                                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "المستويات مبنية بالاستناد إلى الإطار الأوروبي المرجعي (CEFR) كمرجع للمنهج فقط، ولا يمنح التطبيق شهادة رسمية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

private fun lessonSubtitle(lesson: LessonInfo): String = when (lesson.type) {
    LessonType.CONVERSATION -> "محادثة · ${lesson.estimatedMinutes} د"
    LessonType.ALPHABET_INTRO -> "استمع وشاهد · ${lesson.estimatedMinutes} د"
    else -> "${lesson.itemIds.size} عناصر · ${lesson.estimatedMinutes} د"
}

private fun lessonIcon(type: LessonType) = when (type) {
    LessonType.ALPHABET_INTRO -> "emoji:👂"
    LessonType.ALPHABET -> "emoji:🔤"
    LessonType.VOCABULARY -> "emoji:🖼️"
    LessonType.EXPRESSIONS -> "emoji:💬"
    LessonType.SENTENCES -> "emoji:📝"
    LessonType.CONVERSATION -> "emoji:🗣️"
}

@Composable
private fun DailyGoalCard(minutes: Int, goal: Int, streak: Int, xp: Int) {
    TkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("هدف اليوم", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("🔥 $streak", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                Text("⭐ $xp", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            TkProgressIndicator(if (goal == 0) 0f else minutes.toFloat() / goal, color = TkColors.Orange)
            Spacer(Modifier.height(6.dp))
            Text("${minutes.coerceAtMost(goal)} من $goal دقائق", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LevelHeader(code: String, title: String, description: String, unlocked: Boolean, completion: Float, comingSoon: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .background(if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(code, color = if (unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    comingSoon -> "قريبًا"
                    unlocked -> "$description · ${(completion * 100).toInt()}%"
                    else -> "يُفتح بعد إكمال نصف دروس المستوى السابق"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!unlocked) Icon(Icons.Rounded.Lock, contentDescription = "مقفل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
