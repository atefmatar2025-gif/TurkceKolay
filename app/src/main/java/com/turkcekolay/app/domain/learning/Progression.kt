package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.DailyStats
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LevelInfo
import com.turkcekolay.app.domain.model.PresentationMode
import java.time.LocalDate

object Xp {
    const val CORRECT_FIRST_TRY = 10
    const val EFFORT = 2 // a wrong answer still earns something: effort counts
    const val LESSON_COMPLETED = 20
    const val CONVERSATION_COMPLETED = 15
}

/** A level opens when it is open by default, when the learner chose to open everything,
 *  or when at least half of the previous level's lessons are completed. */
object CurriculumUnlockPolicy {
    const val REQUIRED_RATIO = 0.5f

    fun unlockedLevelIds(
        levels: List<LevelInfo>,
        lessonProgress: Map<String, LessonProgressInfo>,
        unlockAll: Boolean,
    ): Set<String> {
        val sorted = levels.sortedBy { it.sortOrder }
        val unlocked = mutableSetOf<String>()
        sorted.forEachIndexed { index, level ->
            if (level.modules.isEmpty()) return@forEachIndexed // "coming soon" levels stay closed
            val open = when {
                unlockAll || level.unlockedByDefault -> true
                index == 0 -> true
                else -> {
                    val previous = sorted[index - 1]
                    previous.id in unlocked && completionRatio(previous, lessonProgress) >= REQUIRED_RATIO
                }
            }
            if (open) unlocked += level.id
        }
        return unlocked
    }

    fun completionRatio(level: LevelInfo, lessonProgress: Map<String, LessonProgressInfo>): Float {
        val ids = level.lessonIds
        if (ids.isEmpty()) return 0f
        val done = ids.count { lessonProgress[it]?.status == LessonStatus.COMPLETED }
        return done.toFloat() / ids.size
    }
}

object StreakCalculator {
    /** Consecutive active days ending today (or yesterday, so the streak survives until tonight). */
    fun currentStreak(activeDates: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in activeDates) today else today.minusDays(1)
        var count = 0
        while (day in activeDates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun activeDates(stats: List<DailyStats>): Set<LocalDate> =
        stats.filter { it.xp > 0 || it.learningSeconds > 0 }
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSet()
}

/** Chooses how a vocabulary card is presented: support is removed gradually as memory grows. */
object PresentationModePolicy {
    fun modeFor(progress: ItemProgress?, quietMode: Boolean): PresentationMode {
        val mode = when (progress?.state ?: LearningState.NEW) {
            LearningState.NEW -> PresentationMode.A
            LearningState.LEARNING -> if (progress?.needsReinforcement == true) PresentationMode.A else PresentationMode.B
            LearningState.REVIEW -> PresentationMode.C
            LearningState.MASTERED -> PresentationMode.D
        }
        return if (quietMode && (mode == PresentationMode.D || mode == PresentationMode.E)) PresentationMode.C else mode
    }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val unlocked: Boolean,
)

data class LearnerStats(
    val completedLessons: Int,
    val alphabetCompleted: Boolean,
    val learnedItems: Int,
    val masteredItems: Int,
    val totalXp: Int,
    val streak: Int,
    val conversationsCompleted: Int,
)

object Achievements {
    fun evaluate(s: LearnerStats): List<Achievement> = listOf(
        Achievement("first_lesson", "البداية", "أكملت أول درس", "🌱", s.completedLessons >= 1),
        Achievement("alphabet", "سيّد الحروف", "أكملت دروس الحروف", "🔤", s.alphabetCompleted),
        Achievement("words_10", "10 كلمات", "تعلّمت 10 كلمات", "📘", s.learnedItems >= 10),
        Achievement("words_50", "50 كلمة", "تعلّمت 50 كلمة", "📚", s.learnedItems >= 50),
        Achievement("first_mastered", "أول إتقان", "أتقنت أول كلمة", "🏅", s.masteredItems >= 1),
        Achievement("streak_3", "3 أيام", "تعلّمت 3 أيام متتالية", "🔥", s.streak >= 3),
        Achievement("streak_7", "أسبوع كامل", "تعلّمت 7 أيام متتالية", "🌟", s.streak >= 7),
        Achievement("xp_500", "500 نقطة", "جمعت 500 نقطة خبرة", "💎", s.totalXp >= 500),
        Achievement("talker", "محادِث", "أكملت محادثتين", "💬", s.conversationsCompleted >= 2),
    )

    fun learnedCount(progress: Collection<ItemProgress>) =
        progress.count { it.state != LearningState.NEW && it.correctCount > 0 }
}
