package com.turkcekolay.app.data.content

import kotlinx.serialization.Serializable

/** JSON shapes of the bundled files in assets/content. See docs/CONTENT_GUIDE.md. */

@Serializable
data class AlphabetFile(val contentVersion: Int, val letters: List<LetterDto>)

@Serializable
data class LetterDto(
    val id: String,
    val upper: String,
    val name: String,
    val hintAr: String,
    val exampleTr: String,
    val exampleAr: String,
    val image: String,
    val isSpecial: Boolean = false,
    val note: String? = null,
    val audio: String? = null,
)

@Serializable
data class VocabularyFile(
    val contentVersion: Int,
    val categories: List<CategoryDto>,
    val items: List<ItemDto>,
)

@Serializable
data class SentencesFile(val contentVersion: Int, val items: List<ItemDto>)

@Serializable
data class CategoryDto(
    val id: String,
    val titleAr: String,
    val titleTr: String,
    val image: String,
    val group: String,
    val sortOrder: Int,
)

@Serializable
data class ItemDto(
    val id: String,
    val kind: String,
    val tr: String,
    val ar: String,
    val categoryId: String,
    val image: String,
    val exampleTr: String? = null,
    val exampleAr: String? = null,
    val note: String? = null,
    val difficulty: Int = 1,
    val audio: String? = null,
)

@Serializable
data class LessonsFile(val contentVersion: Int, val levels: List<LevelDto>)

@Serializable
data class LevelDto(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val sortOrder: Int,
    val isUnlocked: Boolean,
    val modules: List<ModuleDto>,
)

@Serializable
data class ModuleDto(
    val id: String,
    val title: String,
    val description: String,
    val image: String,
    val estimatedMinutes: Int,
    val lessons: List<LessonDto>,
)

@Serializable
data class LessonDto(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val estimatedMinutes: Int,
    val items: List<String> = emptyList(),
    val conversationId: String? = null,
)

@Serializable
data class ConversationsFile(val contentVersion: Int, val conversations: List<ConversationDto>)

@Serializable
data class ConversationDto(val id: String, val titleAr: String, val image: String, val turns: List<TurnDto>)

@Serializable
data class TurnDto(val tr: String, val ar: String, val hintAr: String, val choices: List<ChoiceDto>)

@Serializable
data class ChoiceDto(val tr: String, val ar: String, val correct: Boolean)
