package com.turkcekolay.app.data.repository

import com.turkcekolay.app.data.content.AssetReader
import com.turkcekolay.app.data.content.ContentSeeder
import com.turkcekolay.app.data.content.ConversationsFile
import com.turkcekolay.app.data.content.toDomain
import com.turkcekolay.app.data.local.dao.ContentDao
import com.turkcekolay.app.domain.model.Category
import com.turkcekolay.app.domain.model.CategoryGroup
import com.turkcekolay.app.domain.model.Conversation
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LevelInfo
import com.turkcekolay.app.domain.model.ModuleInfo
import com.turkcekolay.app.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val dao: ContentDao,
    private val seeder: ContentSeeder,
    private val assets: AssetReader,
) : ContentRepository {

    private val cacheLock = Mutex()
    private var poolCache: List<LearnItem>? = null
    private var conversationCache: List<Conversation>? = null

    override suspend fun ensureContentReady() {
        seeder.seedIfNeeded()
        cacheLock.withLock { poolCache = null }
    }

    override fun observeCurriculum(): Flow<List<LevelInfo>> =
        combine(dao.observeLevels(), dao.observeModules(), dao.observeLessons()) { levels, modules, lessons ->
            val lessonsByModule = lessons.map { it.toDomain() }.groupBy { it.moduleId }
            val modulesByLevel = modules.groupBy { it.levelId }
            levels.map { level ->
                LevelInfo(
                    id = level.id,
                    code = level.code,
                    title = level.title,
                    description = level.description,
                    sortOrder = level.sortOrder,
                    unlockedByDefault = level.isUnlocked,
                    modules = modulesByLevel[level.id].orEmpty().map { m ->
                        ModuleInfo(
                            id = m.id,
                            levelId = m.levelId,
                            title = m.title,
                            description = m.description,
                            image = m.imagePath,
                            estimatedMinutes = m.estimatedMinutes,
                            lessons = lessonsByModule[m.id].orEmpty(),
                        )
                    },
                )
            }
        }

    override suspend fun getLesson(id: String): LessonInfo? = dao.getLesson(id)?.toDomain()

    override suspend fun getItems(ids: List<String>): List<LearnItem> {
        if (ids.isEmpty()) return emptyList()
        val byId = dao.getVocabulary(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    override suspend fun getItem(id: String): LearnItem? = dao.getVocabularyItem(id)?.toDomain()

    override fun observeItemsByCategory(categoryId: String): Flow<List<LearnItem>> =
        dao.observeVocabularyByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun observeBrowsableCategories(): Flow<List<Category>> =
        dao.observeCategories(
            listOf(CategoryGroup.FIRST_WORDS.name, CategoryGroup.EXPRESSIONS.name, CategoryGroup.EVERYDAY.name),
        ).map { list -> list.map { it.toDomain() } }

    override suspend fun getCategory(id: String): Category? = dao.getCategory(id)?.toDomain()

    override suspend fun getPool(): List<LearnItem> = cacheLock.withLock {
        poolCache ?: dao.getAllVocabulary().map { it.toDomain() }.also { poolCache = it }
    }

    override suspend fun getConversations(): List<Conversation> = cacheLock.withLock {
        conversationCache ?: withContext(Dispatchers.IO) {
            assets.read(ContentSeeder.CONVERSATIONS, ConversationsFile.serializer()).conversations.map { it.toDomain() }
        }.also { conversationCache = it }
    }

    override suspend fun getConversation(id: String): Conversation? = getConversations().firstOrNull { it.id == id }
}
