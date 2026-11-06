package com.turkcekolay.app.domain.learning

import com.turkcekolay.app.domain.model.ChoiceExercise
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.model.Exercise
import com.turkcekolay.app.domain.model.ExerciseType
import com.turkcekolay.app.domain.model.ExerciseType.LETTER_ARRANGEMENT
import com.turkcekolay.app.domain.model.ExerciseType.LETTER_RECOGNITION
import com.turkcekolay.app.domain.model.ExerciseType.LISTENING_RECOGNITION
import com.turkcekolay.app.domain.model.ExerciseType.PICTURE_SELECTION
import com.turkcekolay.app.domain.model.ExerciseType.SENTENCE_COMPLETION
import com.turkcekolay.app.domain.model.ExerciseType.WORD_SELECTION
import com.turkcekolay.app.domain.model.ItemKind
import com.turkcekolay.app.domain.model.ItemProgress
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.LetterArrangementExercise
import com.turkcekolay.app.domain.model.ListenAndRepeatExercise
import com.turkcekolay.app.domain.model.MatchPair
import com.turkcekolay.app.domain.model.MatchingExercise
import com.turkcekolay.app.domain.model.MemoryCard
import com.turkcekolay.app.domain.model.MemoryGameExercise
import com.turkcekolay.app.domain.model.TURKISH
import kotlin.random.Random

/**
 * Builds exercises from content. Pure Kotlin and fully deterministic for a given [random] seed,
 * so lessons and reviews are reproducible in tests.
 */
class ExerciseGenerator(private val random: Random) {

    private var counter = 0
    private fun nextId(prefix: String) = "${prefix}_${counter++}"

    // ------------------------------------------------------------------ public API

    fun lessonExercises(items: List<LearnItem>, pool: List<LearnItem>, quietMode: Boolean): List<Exercise> {
        if (items.isEmpty()) return emptyList()
        val offset = random.nextInt(4)
        val result = mutableListOf<Exercise>()
        items.forEachIndexed { index, item ->
            val types = candidateTypes(item, quietMode)
            buildFirst(item, rotate(types, index + offset), pool)?.let(result::add)
        }
        if (items.size >= 3) matching(items.take(4))?.let(result::add)
        if (!quietMode) {
            val repeatItem = items[random.nextInt(items.size)]
            result.add(result.size / 2, listenAndRepeat(repeatItem))
        }
        val wordLike = items.count { it.kind == ItemKind.WORD || it.kind == ItemKind.PHRASE }
        if (wordLike >= 4) memoryGame(items.shuffled(random).take(4))?.let(result::add)
        return result
    }

    /**
     * Review exercises. Each item gets a different exercise type from the one it was last
     * tested with, so a forgotten word is never asked the exact same way twice in a row.
     */
    fun reviewExercises(
        entries: List<Pair<LearnItem, ItemProgress>>,
        pool: List<LearnItem>,
        quietMode: Boolean,
    ): List<Exercise> {
        val result = mutableListOf<Exercise>()
        entries.forEachIndexed { index, (item, progress) ->
            val all = candidateTypes(item, quietMode)
            val types = all.filter { it != progress.lastExerciseType }.ifEmpty { all }
            buildFirst(item, rotate(types, index), pool)?.let(result::add)
        }
        if (entries.size >= 4) matching(entries.take(4).map { it.first })?.let(result::add)
        return result
    }

    /** A different exercise for an item answered wrongly, used to re-queue it in the session. */
    fun alternativeFor(item: LearnItem, avoid: ExerciseType, pool: List<LearnItem>, quietMode: Boolean): Exercise? {
        val all = candidateTypes(item, quietMode)
        val types = all.filter { it != avoid }.ifEmpty { all }
        return buildFirst(item, rotate(types, random.nextInt(types.size.coerceAtLeast(1))), pool)
    }

    fun candidateTypes(item: LearnItem, quietMode: Boolean): List<ExerciseType> {
        val types = when (item.kind) {
            ItemKind.LETTER -> listOf(LETTER_RECOGNITION, PICTURE_SELECTION)
            ItemKind.WORD, ItemKind.PHRASE -> buildList {
                add(PICTURE_SELECTION)
                add(WORD_SELECTION)
                add(LISTENING_RECOGNITION)
                if (blankSpan(item) != null) add(SENTENCE_COMPLETION)
                if (canArrange(item)) add(LETTER_ARRANGEMENT)
            }
            ItemKind.SENTENCE -> listOf(WORD_SELECTION, LISTENING_RECOGNITION, SENTENCE_COMPLETION)
        }
        return if (quietMode) types.filterNot { it.requiresAudio } else types
    }

    fun build(item: LearnItem, type: ExerciseType, pool: List<LearnItem>): Exercise? = when (type) {
        PICTURE_SELECTION -> pictureSelection(item, pool)
        WORD_SELECTION -> wordSelection(item, pool)
        LISTENING_RECOGNITION -> listeningRecognition(item, pool)
        LETTER_RECOGNITION -> letterRecognition(item, pool)
        SENTENCE_COMPLETION -> sentenceCompletion(item, pool)
        LETTER_ARRANGEMENT -> letterArrangement(item)
        ExerciseType.LISTEN_AND_REPEAT -> listenAndRepeat(item)
        else -> null
    }

    // ------------------------------------------------------------------ builders

    private fun pictureSelection(item: LearnItem, pool: List<LearnItem>): Exercise? {
        val distractors = distractors(item, pool, needDistinctImage = true)
        if (distractors.size < 2) return null
        val (options, correct) = shuffledOptions(item, distractors) { ChoiceOption(image = it.image, itemId = it.id) }
        val isLetter = item.kind == ItemKind.LETTER
        return ChoiceExercise(
            id = nextId("pic"),
            type = PICTURE_SELECTION,
            instruction = if (isLetter) "أي صورة تبدأ بهذا الحرف؟" else "اختر الصورة الصحيحة",
            targetItemId = item.id,
            promptText = if (isLetter) letterDisplay(item) else item.turkish,
            promptIsTurkish = true,
            promptAudio = item.audio,
            options = options,
            correctIndex = correct,
            explanation = if (isLetter) {
                "كلمة «${item.exampleTurkish}» (${item.exampleArabic}) تبدأ بحرف ${item.turkish}."
            } else explain(item),
        )
    }

    private fun wordSelection(item: LearnItem, pool: List<LearnItem>): Exercise? {
        val distractors = distractors(item, pool, needDistinctImage = false)
        if (distractors.size < 2) return null
        val (options, correct) = shuffledOptions(item, distractors) { ChoiceOption(text = it.turkish, isTurkish = true, itemId = it.id) }
        val sentence = item.kind == ItemKind.SENTENCE
        return ChoiceExercise(
            id = nextId("word"),
            type = WORD_SELECTION,
            instruction = if (sentence) "اختر الجملة التركية المناسبة" else "ما اسم هذه الصورة بالتركية؟",
            targetItemId = item.id,
            promptImage = item.image,
            promptSubText = if (sentence) item.arabic else null,
            options = options,
            correctIndex = correct,
            explanation = explain(item),
        )
    }

    private fun listeningRecognition(item: LearnItem, pool: List<LearnItem>): Exercise? {
        val distractors = distractors(item, pool, needDistinctImage = false)
        if (distractors.size < 2) return null
        val (options, correct) = shuffledOptions(item, distractors) { ChoiceOption(text = it.arabic, itemId = it.id) }
        return ChoiceExercise(
            id = nextId("listen"),
            type = LISTENING_RECOGNITION,
            instruction = "استمع واختر المعنى",
            targetItemId = item.id,
            promptAudio = item.audio,
            autoPlayAudio = true,
            options = options,
            correctIndex = correct,
            explanation = explain(item),
        )
    }

    private fun letterRecognition(item: LearnItem, pool: List<LearnItem>): Exercise? {
        val distractors = distractors(item, pool, needDistinctImage = false)
        if (distractors.size < 2) return null
        val (options, correct) = shuffledOptions(item, distractors) {
            ChoiceOption(text = letterDisplay(it), isTurkish = true, itemId = it.id)
        }
        return ChoiceExercise(
            id = nextId("letter"),
            type = LETTER_RECOGNITION,
            instruction = "استمع: أي حرف هذا؟",
            targetItemId = item.id,
            promptAudio = item.audio,
            autoPlayAudio = true,
            options = options,
            correctIndex = correct,
            explanation = explain(item),
        )
    }

    private fun sentenceCompletion(item: LearnItem, pool: List<LearnItem>): Exercise? {
        val sentence: String
        val translation: String?
        val answer: String
        val blanked: String
        val candidates: List<String>

        if (item.kind == ItemKind.SENTENCE) {
            val tokens = item.turkish.split(" ")
            val idx = tokens.indices.maxByOrNull { clean(tokens[it]).length } ?: return null
            answer = clean(tokens[idx])
            if (answer.length < 2) return null
            sentence = item.turkish
            translation = item.arabic
            blanked = tokens.mapIndexed { i, t -> if (i == idx) t.replace(answer, BLANK) else t }.joinToString(" ")
            candidates = pool.asSequence()
                .filter { it.kind == ItemKind.SENTENCE && it.id != item.id }
                .flatMap { s -> s.turkish.split(" ").map(::clean).asSequence() }
                .filter { it.length >= 3 }
                .toList()
        } else {
            val span = blankSpan(item) ?: return null
            sentence = item.exampleTurkish ?: return null
            translation = item.exampleArabic
            answer = span.answer
            blanked = span.blanked
            val capitalize = answer.first().isUpperCase()
            candidates = pool.filter { (it.kind == ItemKind.WORD || it.kind == ItemKind.PHRASE) && it.id != item.id }
                .map { if (capitalize) it.turkish else it.turkishLower }
        }

        val distinct = candidates.map { it }.distinctBy { it.lowercase(TURKISH) }
            .filter { it.lowercase(TURKISH) != answer.lowercase(TURKISH) }
            .shuffled(random)
            .take(3)
        if (distinct.size < 2) return null
        val texts = (distinct + answer).shuffled(random)
        return ChoiceExercise(
            id = nextId("complete"),
            type = SENTENCE_COMPLETION,
            instruction = "اختر الكلمة الناقصة",
            targetItemId = item.id,
            promptText = blanked,
            promptIsTurkish = true,
            promptSubText = translation,
            promptImage = item.image,
            options = texts.map { ChoiceOption(text = it, isTurkish = true) },
            correctIndex = texts.indexOf(answer),
            explanation = "الجملة الكاملة: «$sentence»" + (translation?.let { " = $it" } ?: ""),
        )
    }

    private fun letterArrangement(item: LearnItem): Exercise? {
        if (!canArrange(item)) return null
        val answer = item.turkishLower
        val letters = answer.map { it.toString() }
        var tiles = letters.shuffled(random)
        var tries = 0
        while (tiles == letters && tries++ < 6) tiles = letters.shuffled(random)
        return LetterArrangementExercise(
            id = nextId("arrange"),
            instruction = "رتّب الحروف لتكوين الكلمة",
            targetItemId = item.id,
            answer = answer,
            tiles = tiles,
            image = item.image,
            arabic = item.arabic,
            audio = item.audio,
            explanation = "الكلمة الصحيحة: «$answer» = ${item.arabic}",
        )
    }

    fun matching(items: List<LearnItem>): MatchingExercise? {
        val chosen = items.distinctBy { it.id }.take(4)
        if (chosen.size < 3) return null
        val letters = chosen.all { it.kind == ItemKind.LETTER }
        val distinctImages = chosen.map { it.image }.distinct().size == chosen.size
        val usePictures = !letters && distinctImages && chosen.none { it.kind == ItemKind.SENTENCE }
        val pairs = chosen.map { item ->
            when {
                letters -> MatchPair(
                    item.id,
                    ChoiceOption(text = item.turkish, isTurkish = true, itemId = item.id),
                    ChoiceOption(text = item.turkishLower, isTurkish = true, itemId = item.id),
                )
                usePictures -> MatchPair(
                    item.id,
                    ChoiceOption(text = item.turkish, isTurkish = true, itemId = item.id),
                    ChoiceOption(image = item.image, itemId = item.id),
                )
                else -> MatchPair(
                    item.id,
                    ChoiceOption(text = item.turkish, isTurkish = true, itemId = item.id),
                    ChoiceOption(text = item.arabic, itemId = item.id),
                )
            }
        }
        val order = pairs.indices.toList()
        var right = order.shuffled(random)
        var tries = 0
        while (right == order && tries++ < 6) right = order.shuffled(random)
        return MatchingExercise(
            id = nextId("match"),
            type = if (usePictures) ExerciseType.PICTURE_MATCHING else ExerciseType.TRANSLATION_MATCHING,
            instruction = when {
                letters -> "صِل الحرف الكبير بالحرف الصغير"
                usePictures -> "صِل كل كلمة بصورتها"
                else -> "صِل الكلمة التركية بمعناها"
            },
            pairs = pairs,
            rightOrder = right,
        )
    }

    fun memoryGame(items: List<LearnItem>): MemoryGameExercise? {
        val chosen = items.distinctBy { it.id }.take(4)
        if (chosen.size < 3) return null
        val distinctImages = chosen.map { it.image }.distinct().size == chosen.size
        val cards = chosen.flatMap { item ->
            listOf(
                MemoryCard(item.id, ChoiceOption(text = item.turkish, isTurkish = true, itemId = item.id)),
                MemoryCard(
                    item.id,
                    if (distinctImages) ChoiceOption(image = item.image, itemId = item.id)
                    else ChoiceOption(text = item.arabic, itemId = item.id),
                ),
            )
        }.shuffled(random)
        return MemoryGameExercise(
            id = nextId("memory"),
            instruction = "لعبة الذاكرة: اقلب بطاقتين متطابقتين",
            cards = cards,
            itemIds = chosen.map { it.id },
        )
    }

    fun listenAndRepeat(item: LearnItem) = ListenAndRepeatExercise(
        id = nextId("repeat"),
        instruction = "استمع، ثم كرّر بصوت مسموع",
        item = item,
    )

    // ------------------------------------------------------------------ helpers

    private fun buildFirst(item: LearnItem, types: List<ExerciseType>, pool: List<LearnItem>): Exercise? {
        for (t in types) build(item, t, pool)?.let { return it }
        return null
    }

    private fun <T> rotate(list: List<T>, by: Int): List<T> {
        if (list.isEmpty()) return list
        val k = ((by % list.size) + list.size) % list.size
        return list.drop(k) + list.take(k)
    }

    private fun sameFamily(a: ItemKind, b: ItemKind): Boolean = when (a) {
        ItemKind.LETTER -> b == ItemKind.LETTER
        ItemKind.SENTENCE -> b == ItemKind.SENTENCE
        ItemKind.WORD, ItemKind.PHRASE -> b == ItemKind.WORD || b == ItemKind.PHRASE
    }

    internal fun distractors(item: LearnItem, pool: List<LearnItem>, needDistinctImage: Boolean, count: Int = 3): List<LearnItem> {
        val candidates = pool.filter {
            it.id != item.id && sameFamily(item.kind, it.kind) &&
                it.turkishLower != item.turkishLower && it.arabic != item.arabic &&
                (!needDistinctImage || it.image != item.image)
        }.shuffled(random).sortedBy { if (it.categoryId == item.categoryId) 0 else 1 }

        val chosen = mutableListOf<LearnItem>()
        for (c in candidates) {
            if (chosen.size == count) break
            if (chosen.any { it.turkishLower == c.turkishLower || it.arabic == c.arabic }) continue
            if (needDistinctImage && chosen.any { it.image == c.image }) continue
            chosen += c
        }
        return chosen
    }

    private fun shuffledOptions(
        item: LearnItem,
        distractors: List<LearnItem>,
        toOption: (LearnItem) -> ChoiceOption,
    ): Pair<List<ChoiceOption>, Int> {
        val all = (distractors + item).shuffled(random)
        return all.map(toOption) to all.indexOfFirst { it.id == item.id }
    }

    private fun explain(item: LearnItem): String = buildString {
        if (item.kind == ItemKind.LETTER) {
            append("هذا الحرف «${letterDisplay(item)}»")
            item.hintArabic?.let { append(" — يُنطق تقريبًا: $it") }
            append(".")
        } else {
            append("«${item.turkish}» تعني «${item.arabic}».")
        }
        item.note?.let { append("\n💡 ").append(it) }
    }

    private fun letterDisplay(item: LearnItem) = "${item.turkish} ${item.turkishLower}"

    private fun canArrange(item: LearnItem): Boolean =
        item.kind == ItemKind.WORD && item.turkish.length in 2..9 && item.turkish.all { it.isLetter() }

    data class BlankSpan(val answer: String, val blanked: String)

    /** Finds the item's words as whole tokens inside its example sentence. */
    internal fun blankSpan(item: LearnItem): BlankSpan? {
        val example = item.exampleTurkish ?: return null
        val tokens = example.split(" ").filter { it.isNotEmpty() }
        val target = item.turkish.split(" ").map { clean(it).lowercase(TURKISH) }
        if (target.isEmpty() || target.any { it.isEmpty() }) return null
        val cleaned = tokens.map { clean(it).lowercase(TURKISH) }
        for (start in 0..cleaned.size - target.size) {
            if (cleaned.subList(start, start + target.size) == target) {
                val originalWords = tokens.subList(start, start + target.size).map(::clean)
                val answer = originalWords.joinToString(" ")
                val last = tokens[start + target.size - 1]
                val trailing = last.substring(last.indexOfLast { it.isLetterOrDigit() } + 1)
                val blanked = (tokens.take(start) + (BLANK + trailing) + tokens.drop(start + target.size)).joinToString(" ")
                return BlankSpan(answer, blanked)
            }
        }
        return null
    }

    /** Strips punctuation at the edges, keeping Turkish letters and apostrophes inside words. */
    private fun clean(token: String): String = token.trim { !it.isLetterOrDigit() }

    companion object {
        const val BLANK = "____"
    }
}
