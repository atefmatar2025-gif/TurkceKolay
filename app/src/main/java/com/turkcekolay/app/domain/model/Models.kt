package com.turkcekolay.app.domain.model

import java.util.Locale

val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

enum class ItemKind { LETTER, WORD, PHRASE, SENTENCE }

enum class CategoryGroup { ALPHABET, FIRST_WORDS, EXPRESSIONS, EVERYDAY, SENTENCES }

enum class LearningState { NEW, LEARNING, REVIEW, MASTERED }

enum class LessonType { ALPHABET, VOCABULARY, EXPRESSIONS, SENTENCES, CONVERSATION }

enum class LessonStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

/** Link between a piece of content and its audio. [text] is what TTS says when no recording exists. */
data class AudioRef(
    val contentId: String,
    val folder: String,
    val text: String,
    val assetFileName: String = "$contentId.mp3",
)

/** One learnable unit: a letter, a word, a phrase or a sentence. */
data class LearnItem(
    val id: String,
    val kind: ItemKind,
    val turkish: String,
    val arabic: String,
    val categoryId: String,
    /** Stable image reference: "emoji:🍎" or an asset path such as "images/vocabulary/w_elma.webp". */
    val image: String,
    val audio: AudioRef,
    val exampleTurkish: String? = null,
    val exampleArabic: String? = null,
    val note: String? = null,
    /** Approximate Arabic pronunciation hint (letters only). */
    val hintArabic: String? = null,
    val difficulty: Int = 1,
) {
    val exampleAudio: AudioRef?
        get() = exampleTurkish?.let { AudioRef("${id}_ex", "sentences", it) }

    /** Lowercase using Turkish rules: I → ı, İ → i. */
    val turkishLower: String get() = turkish.lowercase(TURKISH)
}

data class ItemProgress(
    val vocabularyId: String,
    val state: LearningState = LearningState.NEW,
    /** Index into the scheduler's interval ladder that the item has reached. */
    val repetitions: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val lapses: Int = 0,
    val confidence: Float = 0f,
    val lastExerciseType: ExerciseType? = null,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
) {
    val totalAttempts: Int get() = correctCount + incorrectCount

    /** The learner keeps forgetting this item: re-teach it before testing again. */
    val needsReinforcement: Boolean get() = lapses >= 2 && confidence < 0.5f
}

data class Category(
    val id: String,
    val titleArabic: String,
    val titleTurkish: String,
    val image: String,
    val group: CategoryGroup,
)

data class LessonInfo(
    val id: String,
    val moduleId: String,
    val title: String,
    val description: String,
    val type: LessonType,
    val estimatedMinutes: Int,
    val itemIds: List<String>,
    val conversationId: String?,
)

data class ModuleInfo(
    val id: String,
    val levelId: String,
    val title: String,
    val description: String,
    val image: String,
    val estimatedMinutes: Int,
    val lessons: List<LessonInfo>,
)

data class LevelInfo(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val sortOrder: Int,
    val unlockedByDefault: Boolean,
    val modules: List<ModuleInfo>,
) {
    val lessonIds: List<String> get() = modules.flatMap { m -> m.lessons.map { it.id } }
}

data class LessonProgressInfo(
    val lessonId: String,
    val status: LessonStatus,
    val score: Int,
    val completedAt: Long?,
)

data class ConversationChoice(val turkish: String, val arabic: String, val correct: Boolean)

data class ConversationTurn(
    val turkish: String,
    val arabic: String,
    val hintArabic: String,
    val choices: List<ConversationChoice>,
)

data class Conversation(
    val id: String,
    val titleArabic: String,
    val image: String,
    val turns: List<ConversationTurn>,
) {
    fun turnAudio(index: Int) = AudioRef("${id}_$index", "conversations", turns[index].turkish)
    fun choiceAudio(index: Int, choice: Int) =
        AudioRef("${id}_${index}_r$choice", "conversations", turns[index].choices[choice].turkish)
}

data class DailyStats(
    val date: String,
    val xp: Int,
    val learningSeconds: Int,
    val completedLessons: Int,
    val reviewedWords: Int,
)

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val dailyGoalMinutes: Int = 10,
    val quietMode: Boolean = false,
    val slowAudioByDefault: Boolean = false,
    val remindersEnabled: Boolean = false,
    val unlockAllLevels: Boolean = false,
)

enum class PresentationMode(val label: String, val description: String) {
    A("أ", "صورة + تركي + عربي"),
    B("ب", "صورة + تركي"),
    C("ج", "صورة فقط"),
    D("د", "صوت + صورة"),
    E("هـ", "صوت فقط"),
}
