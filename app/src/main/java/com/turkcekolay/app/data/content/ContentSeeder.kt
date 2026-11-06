package com.turkcekolay.app.data.content

import android.content.Context
import androidx.room.withTransaction
import com.turkcekolay.app.data.local.database.AppDatabase
import com.turkcekolay.app.data.local.entities.LearningLevelEntity
import com.turkcekolay.app.data.local.entities.LearningModuleEntity
import com.turkcekolay.app.data.local.entities.LessonEntity
import com.turkcekolay.app.data.local.entities.VocabularyCategoryEntity
import com.turkcekolay.app.data.local.entities.VocabularyEntity
import com.turkcekolay.app.domain.model.CategoryGroup
import com.turkcekolay.app.domain.model.ItemKind
import com.turkcekolay.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Reads JSON from assets. Kept separate so it can be reused for downloadable packs. */
@Singleton
class AssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    fun <T> read(path: String, strategy: DeserializationStrategy<T>): T =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { json.decodeFromString(strategy, it.readText()) }
}

/**
 * Copies the bundled curriculum into Room. Uses upserts so learner progress tables are never touched,
 * and runs again only when the bundled content version increases.
 */
@Singleton
class ContentSeeder @Inject constructor(
    private val database: AppDatabase,
    private val assets: AssetReader,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val dao = database.contentDao()
            val lessons = assets.read(LESSONS, LessonsFile.serializer())
            if (settings.contentVersion() >= lessons.contentVersion && dao.countVocabulary() > 0) return@withContext

            val alphabet = assets.read(ALPHABET, AlphabetFile.serializer())
            val vocabulary = assets.read(VOCABULARY, VocabularyFile.serializer())
            val sentences = assets.read(SENTENCES, SentencesFile.serializer())
            val version = lessons.contentVersion

            val categories = listOf(
                VocabularyCategoryEntity(
                    id = ALPHABET_CATEGORY, titleArabic = "الحروف", titleTurkish = "Alfabe",
                    imagePath = "emoji:🔤", sortOrder = -1, groupName = CategoryGroup.ALPHABET.name,
                ),
            ) + vocabulary.categories.map {
                VocabularyCategoryEntity(it.id, it.titleAr, it.titleTr, it.image, it.sortOrder, it.group)
            }

            val letterRows = alphabet.letters.mapIndexed { index, l ->
                VocabularyEntity(
                    id = l.id,
                    kind = ItemKind.LETTER.name,
                    turkishWord = l.upper,
                    arabicMeaning = "حرف ${l.upper}",
                    categoryId = ALPHABET_CATEGORY,
                    imagePath = l.image,
                    audioPath = l.audio,
                    audioText = l.name,
                    exampleTurkish = l.exampleTr,
                    exampleArabic = l.exampleAr,
                    note = l.note,
                    hintArabic = l.hintAr,
                    difficulty = if (l.isSpecial) 2 else 1,
                    sortOrder = index,
                    contentVersion = version,
                )
            }
            val itemRows = (vocabulary.items + sentences.items).mapIndexed { index, i ->
                VocabularyEntity(
                    id = i.id,
                    kind = i.kind,
                    turkishWord = i.tr,
                    arabicMeaning = i.ar,
                    categoryId = i.categoryId,
                    imagePath = i.image,
                    audioPath = i.audio,
                    audioText = i.tr.replace("…", ""),
                    exampleTurkish = i.exampleTr,
                    exampleArabic = i.exampleAr,
                    note = i.note,
                    hintArabic = null,
                    difficulty = i.difficulty,
                    sortOrder = index,
                    contentVersion = version,
                )
            }

            val levelRows = lessons.levels.map {
                LearningLevelEntity(it.id, it.code, it.title, it.description, it.sortOrder, it.isUnlocked)
            }
            val moduleRows = lessons.levels.flatMap { level ->
                level.modules.mapIndexed { index, m ->
                    LearningModuleEntity(
                        id = m.id, levelId = level.id, title = m.title, description = m.description,
                        imagePath = m.image, sortOrder = level.sortOrder * 100 + index,
                        estimatedMinutes = m.estimatedMinutes, isUnlocked = level.isUnlocked,
                    )
                }
            }
            val lessonRows = lessons.levels.flatMap { level ->
                level.modules.flatMapIndexed { mIndex, m ->
                    m.lessons.mapIndexed { index, l ->
                        LessonEntity(
                            id = l.id, moduleId = m.id, title = l.title, description = l.description,
                            lessonType = l.type, sortOrder = (level.sortOrder * 100 + mIndex) * 100 + index,
                            estimatedMinutes = l.estimatedMinutes, itemIds = l.items, conversationId = l.conversationId,
                        )
                    }
                }
            }

            database.withTransaction {
                dao.upsertCategories(categories)
                dao.upsertVocabulary(letterRows + itemRows)
                dao.upsertLevels(levelRows)
                dao.upsertModules(moduleRows)
                dao.upsertLessons(lessonRows)
            }
            settings.setContentVersion(version)
        }
    }

    companion object {
        const val ALPHABET_CATEGORY = "alphabet"
        const val ALPHABET = "content/alphabet.json"
        const val VOCABULARY = "content/vocabulary.json"
        const val SENTENCES = "content/sentences.json"
        const val LESSONS = "content/lessons.json"
        const val CONVERSATIONS = "content/conversations.json"
    }
}
