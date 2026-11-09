package com.turkcekolay.app.data.content

import com.turkcekolay.app.domain.model.TURKISH
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled curriculum: every reference must resolve and Turkish rules must hold. */
class ContentValidationTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dir = listOf(File("src/main/assets/content"), File("app/src/main/assets/content")).first { it.exists() }

    private fun <T> read(name: String, s: DeserializationStrategy<T>): T = json.decodeFromString(s, File(dir, name).readText())

    private val alphabet = read("alphabet.json", AlphabetFile.serializer())
    private val vocabulary = read("vocabulary.json", VocabularyFile.serializer())
    private val sentences = read("sentences.json", SentencesFile.serializer())
    private val lessons = read("lessons.json", LessonsFile.serializer())
    private val conversations = read("conversations.json", ConversationsFile.serializer())

    @Test
    fun `alphabet has the 29 Turkish letters in order`() {
        assertEquals("ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ", alphabet.letters.joinToString("") { it.upper })
        assertTrue(alphabet.letters.all { it.hintAr.isNotBlank() && it.exampleTr.isNotBlank() })
    }

    @Test
    fun `dotted and dotless i lowercase correctly`() {
        assertEquals("ı", "I".lowercase(TURKISH))
        assertEquals("i", "İ".lowercase(TURKISH))
        assertEquals("İ", "i".uppercase(TURKISH))
    }

    @Test
    fun `ids are unique and every item has an image`() {
        val all = alphabet.letters.map { it.id } + vocabulary.items.map { it.id } + sentences.items.map { it.id }
        assertEquals(all.size, all.toSet().size)
        assertTrue((vocabulary.items + sentences.items).all { it.image.startsWith("emoji:") || it.image.startsWith("images/") })
    }

    @Test
    fun `items reference existing categories`() {
        val cats = vocabulary.categories.map { it.id }.toSet()
        (vocabulary.items + sentences.items).forEach { assertTrue("${it.id} → ${it.categoryId}", it.categoryId in cats) }
    }

    @Test
    fun `first words module has 30 words`() {
        assertEquals(30, vocabulary.items.count { it.categoryId == "first_words" })
    }

    @Test
    fun `lessons reference existing content and stay small`() {
        val ids = (alphabet.letters.map { it.id } + vocabulary.items.map { it.id } + sentences.items.map { it.id }).toSet()
        val convs = conversations.conversations.map { it.id }.toSet()
        lessons.levels.flatMap { it.modules }.flatMap { it.lessons }.forEach { l ->
            if (l.type == "ALPHABET_INTRO") {
                assertEquals(29, l.items.size)
            } else if (l.type == "CONVERSATION") {
                assertTrue(l.id, l.conversationId in convs)
            } else {
                assertTrue("${l.id} size ${l.items.size}", l.items.size in 3..8)
                l.items.forEach { assertTrue("${l.id} → $it", it in ids) }
            }
        }
    }

    @Test
    fun `every conversation turn has exactly one correct reply`() {
        conversations.conversations.flatMap { it.turns }.forEach { t ->
            assertEquals(t.tr, 1, t.choices.count { it.correct })
        }
    }

    @Test
    fun `content versions agree`() {
        val versions = setOf(alphabet.contentVersion, vocabulary.contentVersion, sentences.contentVersion, lessons.contentVersion, conversations.contentVersion)
        assertEquals(1, versions.size)
    }
}
