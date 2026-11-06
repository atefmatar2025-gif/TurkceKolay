package com.turkcekolay.app.data.content

import com.turkcekolay.app.data.local.entities.LessonEntity
import com.turkcekolay.app.data.local.entities.LessonProgressEntity
import com.turkcekolay.app.data.local.entities.UserVocabularyProgressEntity
import com.turkcekolay.app.data.local.entities.VocabularyCategoryEntity
import com.turkcekolay.app.data.local.entities.VocabularyEntity
import com.turkcekolay.app.domain.model.AudioRef
import com.turkcekolay.app.domain.model.Category
import com.turkcekolay.app.domain.model.CategoryGroup
import com.turkcekolay.app.domain.model.Conversation
import com.turkcekolay.app.domain.model.ConversationChoice
import com.turkcekolay.app.domain.model.ConversationTurn
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ItemKind
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LearningState
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonProgressInfo
import com.turkcekolay.app.domain.model.LessonStatus
import com.turkcekolay.app.domain.model.LessonType

private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
    value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

fun audioFolderFor(kind: ItemKind): String = when (kind) {
    ItemKind.LETTER -> "alphabet"
    ItemKind.WORD -> "vocabulary"
    ItemKind.PHRASE -> "greetings"
    ItemKind.SENTENCE -> "sentences"
}

fun VocabularyEntity.toDomain(): LearnItem {
    val k = enumOr(kind, ItemKind.WORD)
    return LearnItem(
        id = id,
        kind = k,
        turkish = turkishWord,
        arabic = arabicMeaning,
        categoryId = categoryId,
        image = imagePath,
        audio = AudioRef(
            contentId = id,
            folder = audioFolderFor(k),
            text = audioText,
            assetFileName = audioPath ?: "$id.mp3",
        ),
        exampleTurkish = exampleTurkish,
        exampleArabic = exampleArabic,
        note = note,
        hintArabic = hintArabic,
        difficulty = difficulty,
    )
}

fun VocabularyCategoryEntity.toDomain() = Category(
    id = id,
    titleArabic = titleArabic,
    titleTurkish = titleTurkish,
    image = imagePath,
    group = enumOr(groupName, CategoryGroup.EVERYDAY),
)

fun LessonEntity.toDomain() = LessonInfo(
    id = id,
    moduleId = moduleId,
    title = title,
    description = description,
    type = enumOr(lessonType, LessonType.VOCABULARY),
    estimatedMinutes = estimatedMinutes,
    itemIds = itemIds,
    conversationId = conversationId,
)

fun UserVocabularyProgressEntity.toDomain() = ItemProgress(
    vocabularyId = vocabularyId,
    state = enumOr(learningState, LearningState.NEW),
    repetitions = repetitions,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    lapses = lapses,
    confidence = confidence,
    lastExerciseType = lastExerciseType?.let { t -> ExerciseType.entries.firstOrNull { it.name == t } },
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
)

fun ItemProgress.toEntity(id: Long, now: Long) = UserVocabularyProgressEntity(
    id = id,
    vocabularyId = vocabularyId,
    learningState = state.name,
    repetitions = repetitions,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    lapses = lapses,
    confidence = confidence,
    lastExerciseType = lastExerciseType?.name,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    updatedAt = now,
)

fun LessonProgressEntity.toDomain() = LessonProgressInfo(
    lessonId = lessonId,
    status = enumOr(status, LessonStatus.NOT_STARTED),
    score = score,
    completedAt = completedAt,
)

fun ConversationDto.toDomain() = Conversation(
    id = id,
    titleArabic = titleAr,
    image = image,
    turns = turns.map { t ->
        ConversationTurn(t.tr, t.ar, t.hintAr, t.choices.map { ConversationChoice(it.tr, it.ar, it.correct) })
    },
)
