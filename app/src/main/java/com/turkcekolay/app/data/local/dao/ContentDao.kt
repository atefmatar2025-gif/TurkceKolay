package com.turkcekolay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.turkcekolay.app.data.local.entities.LearningLevelEntity
import com.turkcekolay.app.data.local.entities.LearningModuleEntity
import com.turkcekolay.app.data.local.entities.LessonEntity
import com.turkcekolay.app.data.local.entities.VocabularyCategoryEntity
import com.turkcekolay.app.data.local.entities.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Upsert suspend fun upsertLevels(levels: List<LearningLevelEntity>)
    @Upsert suspend fun upsertModules(modules: List<LearningModuleEntity>)
    @Upsert suspend fun upsertLessons(lessons: List<LessonEntity>)
    @Upsert suspend fun upsertCategories(categories: List<VocabularyCategoryEntity>)
    @Upsert suspend fun upsertVocabulary(items: List<VocabularyEntity>)

    @Query("SELECT * FROM learning_level ORDER BY sortOrder")
    fun observeLevels(): Flow<List<LearningLevelEntity>>

    @Query("SELECT * FROM learning_module ORDER BY sortOrder")
    fun observeModules(): Flow<List<LearningModuleEntity>>

    @Query("SELECT * FROM lesson ORDER BY sortOrder")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lesson WHERE id = :id")
    suspend fun getLesson(id: String): LessonEntity?

    @Query("SELECT * FROM vocabulary_category WHERE groupName IN (:groups) ORDER BY sortOrder")
    fun observeCategories(groups: List<String>): Flow<List<VocabularyCategoryEntity>>

    @Query("SELECT * FROM vocabulary_category WHERE id = :id")
    suspend fun getCategory(id: String): VocabularyCategoryEntity?

    @Query("SELECT * FROM vocabulary WHERE categoryId = :categoryId ORDER BY sortOrder")
    fun observeVocabularyByCategory(categoryId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id IN (:ids)")
    suspend fun getVocabulary(ids: List<String>): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getVocabularyItem(id: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary ORDER BY sortOrder")
    suspend fun getAllVocabulary(): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun countVocabulary(): Int
}
