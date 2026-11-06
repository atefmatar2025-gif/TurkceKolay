package com.turkcekolay.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ----------------------------------------------------------------- learner (never overwritten by content updates)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = DEFAULT_ID,
    val displayName: String = "",
    val nativeLanguage: String = "ar",
    val dailyGoalMinutes: Int = 10,
    val quietModeEnabled: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object { const val DEFAULT_ID = 1L }
}

@Entity(
    tableName = "user_vocabulary_progress",
    indices = [Index(value = ["vocabularyId"], unique = true), Index("nextReviewAt")],
)
data class UserVocabularyProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vocabularyId: String,
    val learningState: String,
    val repetitions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val lapses: Int,
    val confidence: Float,
    val lastExerciseType: String?,
    val lastReviewedAt: Long?,
    val nextReviewAt: Long?,
    val updatedAt: Long,
)

@Entity(tableName = "lesson_progress", indices = [Index(value = ["lessonId"], unique = true)])
data class LessonProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val status: String,
    val score: Int,
    val completedAt: Long?,
    val timeSpentSeconds: Int,
)

@Entity(tableName = "exercise_attempt", indices = [Index("lessonId"), Index("vocabularyId")])
data class ExerciseAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val exerciseType: String,
    val lessonId: String?,
    val vocabularyId: String?,
    val selectedAnswer: String,
    val isCorrect: Boolean,
    val timeSpentSeconds: Int,
    val createdAt: Long,
)

@Entity(tableName = "daily_activity", indices = [Index(value = ["activityDate"], unique = true)])
data class DailyActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO date, e.g. 2026-11-06 */
    val activityDate: String,
    val xpEarned: Int = 0,
    val learningMinutes: Int = 0,
    val learningSeconds: Int = 0,
    val completedLessons: Int = 0,
    val reviewedWords: Int = 0,
)

@Entity(tableName = "favorite_vocabulary", indices = [Index(value = ["vocabularyId"], unique = true)])
data class FavoriteVocabularyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vocabularyId: String,
    val createdAt: Long,
)

// ----------------------------------------------------------------- curriculum (seeded from assets/content)

@Entity(tableName = "learning_level")
data class LearningLevelEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val description: String,
    val sortOrder: Int,
    /** Default from content. The effective lock state is computed from progress (CurriculumUnlockPolicy). */
    val isUnlocked: Boolean,
)

@Entity(
    tableName = "learning_module",
    foreignKeys = [ForeignKey(
        entity = LearningLevelEntity::class,
        parentColumns = ["id"],
        childColumns = ["levelId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("levelId")],
)
data class LearningModuleEntity(
    @PrimaryKey val id: String,
    val levelId: String,
    val title: String,
    val description: String,
    val imagePath: String,
    val sortOrder: Int,
    val estimatedMinutes: Int,
    val isUnlocked: Boolean,
)

@Entity(
    tableName = "lesson",
    foreignKeys = [ForeignKey(
        entity = LearningModuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["moduleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("moduleId")],
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val title: String,
    val description: String,
    val lessonType: String,
    val sortOrder: Int,
    val estimatedMinutes: Int,
    val itemIds: List<String>,
    val conversationId: String?,
)

@Entity(tableName = "vocabulary_category")
data class VocabularyCategoryEntity(
    @PrimaryKey val id: String,
    val titleArabic: String,
    val titleTurkish: String,
    val imagePath: String,
    val sortOrder: Int,
    val groupName: String,
)

@Entity(
    tableName = "vocabulary",
    foreignKeys = [ForeignKey(
        entity = VocabularyCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("categoryId"), Index("kind")],
)
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val turkishWord: String,
    val arabicMeaning: String,
    val categoryId: String,
    val imagePath: String,
    /** Optional explicit recording file name; defaults to "<id>.mp3" in the kind's audio folder. */
    val audioPath: String?,
    /** Text spoken by TTS when no recording exists (letters use their Turkish names, e.g. "çe"). */
    val audioText: String,
    val exampleTurkish: String?,
    val exampleArabic: String?,
    val note: String?,
    val hintArabic: String?,
    val difficulty: Int,
    val sortOrder: Int,
    val contentVersion: Int,
)
