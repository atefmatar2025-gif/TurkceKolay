package com.turkcekolay.app.domain.model

enum class ExerciseType(val requiresAudio: Boolean, val updatesMemory: Boolean) {
    PICTURE_SELECTION(false, true),
    WORD_SELECTION(false, true),
    LISTENING_RECOGNITION(true, true),
    LETTER_RECOGNITION(true, true),
    PICTURE_MATCHING(false, true),
    TRANSLATION_MATCHING(false, true),
    LETTER_ARRANGEMENT(false, true),
    SENTENCE_COMPLETION(false, true),
    MEMORY_GAME(false, false),
    LISTEN_AND_REPEAT(true, false),
    CONVERSATION(false, false),
}

/** A single visual or text option. Turkish text is rendered left-to-right. */
data class ChoiceOption(
    val text: String? = null,
    val image: String? = null,
    val isTurkish: Boolean = false,
    val itemId: String? = null,
)

sealed interface Exercise {
    val id: String
    val type: ExerciseType
    val instruction: String
    /** Items whose memory state this exercise tests. */
    val targetItemIds: List<String>
}

data class ChoiceExercise(
    override val id: String,
    override val type: ExerciseType,
    override val instruction: String,
    val targetItemId: String,
    val promptText: String? = null,
    val promptIsTurkish: Boolean = false,
    val promptSubText: String? = null,
    val promptImage: String? = null,
    val promptAudio: AudioRef? = null,
    val autoPlayAudio: Boolean = false,
    val options: List<ChoiceOption>,
    val correctIndex: Int,
    val explanation: String,
) : Exercise {
    override val targetItemIds: List<String> get() = listOf(targetItemId)
}

data class MatchPair(val itemId: String, val left: ChoiceOption, val right: ChoiceOption)

data class MatchingExercise(
    override val id: String,
    override val type: ExerciseType,
    override val instruction: String,
    val pairs: List<MatchPair>,
    /** Order in which the right column is displayed (indices into [pairs]). */
    val rightOrder: List<Int>,
) : Exercise {
    override val targetItemIds: List<String> get() = pairs.map { it.itemId }
}

data class LetterArrangementExercise(
    override val id: String,
    override val instruction: String,
    val targetItemId: String,
    val answer: String,
    val tiles: List<String>,
    val image: String,
    val arabic: String,
    val audio: AudioRef,
    val explanation: String,
) : Exercise {
    override val type: ExerciseType get() = ExerciseType.LETTER_ARRANGEMENT
    override val targetItemIds: List<String> get() = listOf(targetItemId)
}

data class MemoryCard(val pairKey: String, val option: ChoiceOption)

data class MemoryGameExercise(
    override val id: String,
    override val instruction: String,
    val cards: List<MemoryCard>,
    val itemIds: List<String>,
) : Exercise {
    override val type: ExerciseType get() = ExerciseType.MEMORY_GAME
    override val targetItemIds: List<String> get() = itemIds
}

data class ListenAndRepeatExercise(
    override val id: String,
    override val instruction: String,
    val item: LearnItem,
) : Exercise {
    override val type: ExerciseType get() = ExerciseType.LISTEN_AND_REPEAT
    override val targetItemIds: List<String> get() = listOf(item.id)
}
