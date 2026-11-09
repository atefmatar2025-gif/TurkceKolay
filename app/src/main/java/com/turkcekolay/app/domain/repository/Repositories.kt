package com.turkcekolay.app.domain.repository

import com.turkcekolay.app.domain.model.Category
import com.turkcekolay.app.domain.model.Conversation
import com.turkcekolay.app.domain.model.DailyStats
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LevelInfo
import com.turkcekolay.app.domain.model.SpeechSpeed
import com.turkcekolay.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    /** Seeds or updates the bundled curriculum in Room. Safe to call repeatedly. */
    suspend fun ensureContentReady()
    fun observeCurriculum(): Flow<List<LevelInfo>>
    suspend fun getLesson(id: String): LessonInfo?
    /** Returns items in the same order as [ids]. */
    suspend fun getItems(ids: List<String>): List<LearnItem>
    suspend fun getItem(id: String): LearnItem?
    fun observeItemsByCategory(categoryId: String): Flow<List<LearnItem>>
    fun observeBrowsableCategories(): Flow<List<Category>>
    suspend fun getCategory(id: String): Category?
    /** All items, used to draw exercise distractors. */
    suspend fun getPool(): List<LearnItem>
    suspend fun getConversations(): List<Conversation>
    suspend fun getConversation(id: String): Conversation?
}

interface ProgressRepository {
    fun observeItemProgress(): Flow<Map<String, ItemProgress>>
    suspend fun getProgress(ids: List<String>): Map<String, ItemProgress>
    suspend fun recordAnswer(
        itemId: String?,
        exerciseId: String,
        exerciseType: ExerciseType,
        lessonId: String?,
        selectedAnswer: String,
        correct: Boolean,
        timeSpentSeconds: Int,
        updateMemory: Boolean,
    )
    suspend fun dueItems(limit: Int): List<ItemProgress>
    suspend fun weakestItems(limit: Int): List<ItemProgress>
    fun observeDueCount(): Flow<Int>
    fun observeLessonProgress(): Flow<Map<String, LessonProgressInfo>>
    suspend fun markLessonStarted(lessonId: String)
    suspend fun completeLesson(lessonId: String, score: Int, timeSpentSeconds: Int, xp: Int)
    fun observeDailyStats(): Flow<List<DailyStats>>
    suspend fun addLearningTime(seconds: Int)
    fun observeFavorites(): Flow<Set<String>>
    suspend fun toggleFavorite(itemId: String)
    suspend fun resetAllProgress()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun completeOnboarding(dailyGoalMinutes: Int, quietMode: Boolean)
    suspend fun setDailyGoal(minutes: Int)
    suspend fun setQuietMode(enabled: Boolean)
    suspend fun setSpeechSpeed(speed: SpeechSpeed)
    suspend fun setRepeatCount(count: Int)
    suspend fun setReminders(enabled: Boolean)
    suspend fun setUnlockAll(enabled: Boolean)
    suspend fun contentVersion(): Int
    suspend fun setContentVersion(version: Int)
}
