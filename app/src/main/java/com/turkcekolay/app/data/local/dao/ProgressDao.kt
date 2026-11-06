package com.turkcekolay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.turkcekolay.app.data.local.entities.DailyActivityEntity
import com.turkcekolay.app.data.local.entities.ExerciseAttemptEntity
import com.turkcekolay.app.data.local.entities.FavoriteVocabularyEntity
import com.turkcekolay.app.data.local.entities.LessonProgressEntity
import com.turkcekolay.app.data.local.entities.UserProfileEntity
import com.turkcekolay.app.data.local.entities.UserVocabularyProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    // ---- profile
    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getProfile(id: Long): UserProfileEntity?

    @Upsert suspend fun upsertProfile(profile: UserProfileEntity)

    // ---- vocabulary memory
    @Query("SELECT * FROM user_vocabulary_progress")
    fun observeAllProgress(): Flow<List<UserVocabularyProgressEntity>>

    @Query("SELECT * FROM user_vocabulary_progress WHERE vocabularyId = :vocabularyId")
    suspend fun getProgress(vocabularyId: String): UserVocabularyProgressEntity?

    @Query("SELECT * FROM user_vocabulary_progress WHERE vocabularyId IN (:ids)")
    suspend fun getProgressFor(ids: List<String>): List<UserVocabularyProgressEntity>

    @Upsert suspend fun upsertProgress(progress: UserVocabularyProgressEntity)

    @Query(
        "SELECT * FROM user_vocabulary_progress WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now " +
            "ORDER BY nextReviewAt ASC, confidence ASC LIMIT :limit",
    )
    suspend fun getDue(now: Long, limit: Int): List<UserVocabularyProgressEntity>

    @Query("SELECT COUNT(*) FROM user_vocabulary_progress WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now")
    suspend fun countDue(now: Long): Int

    @Query(
        "SELECT * FROM user_vocabulary_progress WHERE learningState != 'NEW' " +
            "ORDER BY confidence ASC, incorrectCount DESC LIMIT :limit",
    )
    suspend fun getWeakest(limit: Int): List<UserVocabularyProgressEntity>

    // ---- lessons
    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getLessonProgress(lessonId: String): LessonProgressEntity?

    @Upsert suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    // ---- attempts
    @Insert suspend fun insertAttempt(attempt: ExerciseAttemptEntity)

    // ---- daily activity
    @Query("SELECT * FROM daily_activity WHERE activityDate = :date")
    suspend fun getDailyActivity(date: String): DailyActivityEntity?

    @Upsert suspend fun upsertDailyActivity(activity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activity ORDER BY activityDate DESC LIMIT 400")
    fun observeDailyActivity(): Flow<List<DailyActivityEntity>>

    // ---- favorites
    @Query("SELECT vocabularyId FROM favorite_vocabulary ORDER BY createdAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorite_vocabulary WHERE vocabularyId = :vocabularyId")
    suspend fun isFavorite(vocabularyId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteVocabularyEntity)

    @Query("DELETE FROM favorite_vocabulary WHERE vocabularyId = :vocabularyId")
    suspend fun deleteFavorite(vocabularyId: String)

    // ---- reset
    @Query("DELETE FROM user_vocabulary_progress") suspend fun clearVocabularyProgress()
    @Query("DELETE FROM lesson_progress") suspend fun clearLessonProgress()
    @Query("DELETE FROM exercise_attempt") suspend fun clearAttempts()
    @Query("DELETE FROM daily_activity") suspend fun clearDailyActivity()
    @Query("DELETE FROM favorite_vocabulary") suspend fun clearFavorites()
}
