package com.turkcekolay.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.domain.learning.CurriculumUnlockPolicy
import com.turkcekolay.app.domain.learning.StreakCalculator
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LevelInfo
import com.turkcekolay.app.domain.repository.ContentRepository
import com.turkcekolay.app.domain.repository.ProgressRepository
import com.turkcekolay.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeLevel(val level: LevelInfo, val unlocked: Boolean, val completion: Float)

data class HomeUiState(
    val loading: Boolean = true,
    val levels: List<HomeLevel> = emptyList(),
    val lessonProgress: Map<String, LessonProgressInfo> = emptyMap(),
    val nextLesson: LessonInfo? = null,
    val dueCount: Int = 0,
    val todayMinutes: Int = 0,
    val dailyGoalMinutes: Int = 10,
    val streak: Int = 0,
    val todayXp: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    content: ContentRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    clock: Clock,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        content.observeCurriculum(),
        progress.observeLessonProgress(),
        progress.observeDueCount(),
        progress.observeDailyStats(),
        settings.settings,
    ) { levels, lessonProgress, due, daily, userSettings ->
        val unlockedIds = CurriculumUnlockPolicy.unlockedLevelIds(levels, lessonProgress, userSettings.unlockAllLevels)
        val homeLevels = levels.sortedBy { it.sortOrder }.map {
            HomeLevel(it, it.id in unlockedIds, CurriculumUnlockPolicy.completionRatio(it, lessonProgress))
        }
        val next = homeLevels.asSequence()
            .filter { it.unlocked }
            .flatMap { it.level.modules.asSequence() }
            .flatMap { it.lessons.asSequence() }
            .firstOrNull { lessonProgress[it.id]?.status != LessonStatus.COMPLETED }
        val today = clock.today().toString()
        val todayStats = daily.firstOrNull { it.date == today }
        HomeUiState(
            loading = false,
            levels = homeLevels,
            lessonProgress = lessonProgress,
            nextLesson = next,
            dueCount = due,
            todayMinutes = (todayStats?.learningSeconds ?: 0) / 60,
            dailyGoalMinutes = userSettings.dailyGoalMinutes,
            streak = StreakCalculator.currentStreak(StreakCalculator.activeDates(daily), clock.today()),
            todayXp = todayStats?.xp ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
