package com.turkcekolay.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.turkcekolay.app.data.local.converters.Converters
import com.turkcekolay.app.data.local.dao.ContentDao
import com.turkcekolay.app.data.local.dao.ProgressDao
import com.turkcekolay.app.data.local.entities.DailyActivityEntity
import com.turkcekolay.app.data.local.entities.ExerciseAttemptEntity
import com.turkcekolay.app.data.local.entities.FavoriteVocabularyEntity
import com.turkcekolay.app.data.local.entities.LearningLevelEntity
import com.turkcekolay.app.data.local.entities.LearningModuleEntity
import com.turkcekolay.app.data.local.entities.LessonEntity
import com.turkcekolay.app.data.local.entities.LessonProgressEntity
import com.turkcekolay.app.data.local.entities.UserProfileEntity
import com.turkcekolay.app.data.local.entities.UserVocabularyProgressEntity
import com.turkcekolay.app.data.local.entities.VocabularyCategoryEntity
import com.turkcekolay.app.data.local.entities.VocabularyEntity

/**
 * Schema version 1. Any future schema change MUST ship a Migration so learners never
 * lose progress — destructive fallback is intentionally not enabled.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LearningLevelEntity::class,
        LearningModuleEntity::class,
        LessonEntity::class,
        VocabularyCategoryEntity::class,
        VocabularyEntity::class,
        UserVocabularyProgressEntity::class,
        LessonProgressEntity::class,
        ExerciseAttemptEntity::class,
        DailyActivityEntity::class,
        FavoriteVocabularyEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "turkce_kolay.db"
    }
}
