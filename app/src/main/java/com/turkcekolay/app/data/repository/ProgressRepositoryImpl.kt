package com.turkcekolay.app.data.repository

import androidx.room.withTransaction
import com.turkcekolay.app.core.common.Clock
import com.turkcekolay.app.data.content.toDomain
import com.turkcekolay.app.data.content.toEntity
import com.turkcekolay.app.data.local.database.AppDatabase
import com.turkcekolay.app.data.local.entities.DailyActivityEntity
import com.turkcekolay.app.data.local.entities.ExerciseAttemptEntity
import com.turkcekolay.app.data.local.entities.FavoriteVocabularyEntity
import com.turkcekolay.app.data.local.entities.LessonProgressEntity
import com.turkcekolay.app.domain.learning.SpacedRepetitionScheduler
import com.turkcekolay.app.domain.learning.Xp
import com.turkcekolay.app.domain.model.DailyStats
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val scheduler: SpacedRepetitionScheduler,
    private val clock: Clock,
) : ProgressRepository {

    private val dao get() = database.progressDao()

    override fun observeItemProgress(): Flow<Map<String, ItemProgress>> =
        dao.observeAllProgress().map { list -> list.associate { it.vocabularyId to it.toDomain() } }

    override suspend fun getProgress(ids: List<String>): Map<String, ItemProgress> =
        if (ids.isEmpty()) emptyMap()
        else dao.getProgressFor(ids).associate { it.vocabularyId to it.toDomain() }

    override suspend fun recordAnswer(
        itemId: String?,
        exerciseId: String,
        exerciseType: ExerciseType,
        lessonId: String?,
        selectedAnswer: String,
        correct: Boolean,
        timeSpentSeconds: Int,
        updateMemory: Boolean,
    ) {
        val now = clock.now()
        database.withTransaction {
            dao.insertAttempt(
                ExerciseAttemptEntity(
                    exerciseId = exerciseId,
                    exerciseType = exerciseType.name,
                    lessonId = lessonId,
                    vocabularyId = itemId,
                    selectedAnswer = selectedAnswer,
                    isCorrect = correct,
                    timeSpentSeconds = timeSpentSeconds,
                    createdAt = now,
                ),
            )
            var reviewed = 0
            if (updateMemory && itemId != null) {
                val existing = dao.getProgress(itemId)
                val current = existing?.toDomain() ?: ItemProgress(vocabularyId = itemId)
                val updated = scheduler.onAnswer(current, correct, now, exerciseType)
                dao.upsertProgress(updated.toEntity(id = existing?.id ?: 0L, now = now))
                reviewed = 1
            }
            addDaily(xp = if (correct) Xp.CORRECT_FIRST_TRY else Xp.EFFORT, reviewedWords = reviewed)
        }
    }

    override suspend fun dueItems(limit: Int): List<ItemProgress> =
        dao.getDue(clock.now(), limit).map { it.toDomain() }

    override suspend fun weakestItems(limit: Int): List<ItemProgress> =
        dao.getWeakest(limit).map { it.toDomain() }

    override fun observeDueCount(): Flow<Int> = dao.observeAllProgress().map { list ->
        val now = clock.now()
        list.count { p -> p.nextReviewAt?.let { it <= now } == true }
    }

    override fun observeLessonProgress(): Flow<Map<String, LessonProgressInfo>> =
        dao.observeLessonProgress().map { list -> list.associate { it.lessonId to it.toDomain() } }

    override suspend fun markLessonStarted(lessonId: String) {
        if (dao.getLessonProgress(lessonId) == null) {
            dao.upsertLessonProgress(
                LessonProgressEntity(
                    lessonId = lessonId,
                    status = LessonStatus.IN_PROGRESS.name,
                    score = 0,
                    completedAt = null,
                    timeSpentSeconds = 0,
                ),
            )
        }
    }

    override suspend fun completeLesson(lessonId: String, score: Int, timeSpentSeconds: Int, xp: Int) {
        val now = clock.now()
        database.withTransaction {
            val existing = dao.getLessonProgress(lessonId)
            dao.upsertLessonProgress(
                LessonProgressEntity(
                    id = existing?.id ?: 0L,
                    lessonId = lessonId,
                    status = LessonStatus.COMPLETED.name,
                    score = maxOf(existing?.score ?: 0, score),
                    completedAt = now,
                    timeSpentSeconds = (existing?.timeSpentSeconds ?: 0) + timeSpentSeconds,
                ),
            )
            addDaily(xp = xp, completedLessons = 1)
        }
    }

    override fun observeDailyStats(): Flow<List<DailyStats>> = dao.observeDailyActivity().map { list ->
        list.map { DailyStats(it.activityDate, it.xpEarned, it.learningSeconds, it.completedLessons, it.reviewedWords) }
    }

    override suspend fun addLearningTime(seconds: Int) {
        if (seconds > 0) addDaily(seconds = seconds)
    }

    override fun observeFavorites(): Flow<Set<String>> = dao.observeFavoriteIds().map { it.toSet() }

    override suspend fun toggleFavorite(itemId: String) {
        if (dao.isFavorite(itemId) > 0) dao.deleteFavorite(itemId)
        else dao.insertFavorite(FavoriteVocabularyEntity(vocabularyId = itemId, createdAt = clock.now()))
    }

    override suspend fun resetAllProgress() {
        database.withTransaction {
            dao.clearVocabularyProgress()
            dao.clearLessonProgress()
            dao.clearAttempts()
            dao.clearDailyActivity()
            dao.clearFavorites()
        }
    }

    private suspend fun addDaily(xp: Int = 0, seconds: Int = 0, completedLessons: Int = 0, reviewedWords: Int = 0) {
        val date = clock.today().toString()
        val current = dao.getDailyActivity(date) ?: DailyActivityEntity(activityDate = date)
        val totalSeconds = current.learningSeconds + seconds
        dao.upsertDailyActivity(
            current.copy(
                xpEarned = current.xpEarned + xp,
                learningSeconds = totalSeconds,
                learningMinutes = totalSeconds / 60,
                completedLessons = current.completedLessons + completedLessons,
                reviewedWords = current.reviewedWords + reviewedWords,
            ),
        )
    }
}
