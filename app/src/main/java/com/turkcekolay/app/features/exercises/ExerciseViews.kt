@file:OptIn(ExperimentalLayoutApi::class)

package com.turkcekolay.app.features.exercises

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turkcekolay.app.core.designsystem.AudioButton
import com.turkcekolay.app.core.designsystem.ChoiceState
import com.turkcekolay.app.core.designsystem.ContentImage
import com.turkcekolay.app.core.designsystem.ExerciseFeedback
import com.turkcekolay.app.core.designsystem.LearningCharacter
import com.turkcekolay.app.core.designsystem.LocalAudioManager
import com.turkcekolay.app.core.designsystem.MascotMood
import com.turkcekolay.app.core.designsystem.PictureChoiceCard
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.SecondaryButton
import com.turkcekolay.app.core.designsystem.TextChoiceCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.domain.model.ChoiceExercise
import com.turkcekolay.app.domain.model.ChoiceOption
import com.turkcekolay.app.domain.model.Exercise
import com.turkcekolay.app.domain.model.LetterArrangementExercise
import com.turkcekolay.app.domain.model.ListenAndRepeatExercise
import com.turkcekolay.app.domain.model.MatchingExercise
import com.turkcekolay.app.domain.model.MemoryGameExercise
import kotlinx.coroutines.delay

private data class Feedback(val correct: Boolean, val explanation: String?)

/** Full practice screen: progress, close button, one exercise at a time. */
@Composable
fun PracticeContent(
    exercise: Exercise,
    progress: Float,
    quietMode: Boolean,
    slowAudio: Boolean,
    onAnswered: (Map<String, Boolean>, String) -> Unit,
    onContinue: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
            TkProgressIndicator(progress, Modifier.weight(1f).padding(end = 12.dp))
        }
        key(exercise.id) {
            ExerciseHost(exercise, quietMode, slowAudio, onAnswered, onContinue, Modifier.weight(1f))
        }
    }
}

@Composable
fun ExerciseHost(
    exercise: Exercise,
    quietMode: Boolean,
    slowAudio: Boolean,
    onAnswered: (Map<String, Boolean>, String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    val answer: (Map<String, Boolean>, String, Boolean, String?) -> Unit = { results, selected, correct, explanation ->
        if (feedback == null) {
            onAnswered(results, selected)
            feedback = Feedback(correct, explanation)
        }
    }
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(exercise.instruction, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            val locked = feedback != null
            when (exercise) {
                is ChoiceExercise -> ChoiceView(exercise, locked, quietMode, slowAudio) { index ->
                    val correct = index == exercise.correctIndex
                    val opt = exercise.options[index]
                    answer(mapOf(exercise.targetItemId to correct), opt.text ?: opt.itemId ?: index.toString(), correct, exercise.explanation)
                }
                is MatchingExercise -> MatchingView(exercise, locked) { results, explanation ->
                    answer(results, "matching", results.values.all { it }, explanation)
                }
                is LetterArrangementExercise -> ArrangementView(exercise, locked, quietMode, slowAudio) { typed ->
                    val correct = typed == exercise.answer
                    answer(mapOf(exercise.targetItemId to correct), typed, correct, exercise.explanation)
                }
                is MemoryGameExercise -> MemoryView(exercise) { moves ->
                    answer(emptyMap(), "moves:$moves", true, null)
                }
                is ListenAndRepeatExercise -> ListenRepeatView(exercise, slowAudio, locked) {
                    answer(emptyMap(), "repeated", true, null)
                }
            }
        }
        ExerciseFeedback(
            visible = feedback != null,
            correct = feedback?.correct == true,
            explanation = feedback?.explanation,
            onContinue = onContinue,
        )
    }
}

// ------------------------------------------------------------------ choice (picture / word / listening / letters / completion)

@Composable
private fun ChoiceView(ex: ChoiceExercise, locked: Boolean, quietMode: Boolean, slowAudio: Boolean, onPick: (Int) -> Unit) {
    var picked by remember { mutableStateOf<Int?>(null) }
    val audio = LocalAudioManager.current

    ex.promptImage?.let { image ->
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) { ContentImage(image, size = 130.dp) }
    }
    ex.promptText?.let {
        if (ex.promptIsTurkish) TurkishText(it, style = if (it.length > 14) TurkishStyle.Large else TurkishStyle.Hero, color = MaterialTheme.colorScheme.secondary)
        else Text(it, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
    ex.promptSubText?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }

    if (ex.promptAudio != null && !quietMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AudioButton(
                ex.promptAudio,
                size = if (ex.autoPlayAudio) 84.dp else 60.dp,
                slow = slowAudio,
                autoPlayKey = if (ex.autoPlayAudio) ex.id else null,
            )
            AudioButton(ex.promptAudio, slow = !slowAudio, size = 50.dp)
        }
    }

    val stateFor: (Int) -> ChoiceState = { i ->
        val p = picked
        when {
            p == null -> if (locked) ChoiceState.DISABLED else ChoiceState.IDLE
            i == p -> if (i == ex.correctIndex) ChoiceState.CORRECT else ChoiceState.WRONG
            i == ex.correctIndex -> ChoiceState.REVEALED_CORRECT
            else -> ChoiceState.DISABLED
        }
    }
    val pick: (Int) -> Unit = { i ->
        if (picked == null && !locked) {
            picked = i
            onPick(i)
            if (!quietMode && ex.promptAudio != null) audio?.play(ex.promptAudio)
        }
    }

    if (ex.options.all { it.image != null }) {
        ex.options.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEachIndexed { col, option ->
                    val index = row * 2 + col
                    PictureChoiceCard(option, stateFor(index), { pick(index) }, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ex.options.forEachIndexed { index, option ->
                TextChoiceCard(option, stateFor(index), { pick(index) })
            }
        }
    }
}

// ------------------------------------------------------------------ matching (pictures or translations)

private enum class TileState { IDLE, SELECTED, MATCHED, WRONG }

@Composable
private fun MatchTile(option: ChoiceOption, state: TileState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(
        when (state) {
            TileState.MATCHED -> TkColors.SuccessSoft
            TileState.WRONG -> TkColors.GentleSoft
            TileState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
            TileState.IDLE -> MaterialTheme.colorScheme.surface
        },
        label = "tile",
    )
    val border = when (state) {
        TileState.MATCHED -> TkColors.Success
        TileState.WRONG -> TkColors.Gentle
        TileState.SELECTED -> MaterialTheme.colorScheme.primary
        TileState.IDLE -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(18.dp))
            .clickable(enabled = state != TileState.MATCHED, role = Role.Button, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            option.image != null -> ContentImage(option.image, size = 58.dp)
            option.isTurkish -> TurkishText(option.text.orEmpty(), style = TurkishStyle.Medium)
            else -> Text(option.text.orEmpty(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MatchingView(ex: MatchingExercise, locked: Boolean, onDone: (Map<String, Boolean>, String?) -> Unit) {
    var left by remember { mutableStateOf<Int?>(null) }
    var right by remember { mutableStateOf<Int?>(null) }
    val matched = remember { mutableStateListOf<Int>() }
    val mistakes = remember { mutableStateListOf<Int>() }
    var wrong by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun evaluate() {
        val l = left ?: return
        val r = right ?: return
        if (l == r) {
            matched += l
        } else {
            if (l !in mistakes) mistakes += l
            if (r !in mistakes) mistakes += r
            wrong = l to r
        }
        left = null
        right = null
        if (matched.size == ex.pairs.size) {
            val results = ex.pairs.mapIndexed { i, p -> p.itemId to (i !in mistakes) }.toMap()
            val explanation = mistakes.distinct().joinToString("\n") { i ->
                val p = ex.pairs[i]
                "«${p.left.text}» = ${p.right.text ?: "الصورة"}"
            }.ifBlank { null }
            onDone(results, explanation?.let { "تذكّر:\n$it" })
        }
    }

    LaunchedEffect(wrong) {
        if (wrong != null) {
            delay(700)
            wrong = null
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.pairs.forEachIndexed { i, pair ->
                val state = when {
                    i in matched -> TileState.MATCHED
                    wrong?.first == i -> TileState.WRONG
                    left == i -> TileState.SELECTED
                    else -> TileState.IDLE
                }
                MatchTile(pair.left, state, onClick = {
                    if (!locked) { left = i; evaluate() }
                })
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.rightOrder.forEach { pairIndex ->
                val state = when {
                    pairIndex in matched -> TileState.MATCHED
                    wrong?.second == pairIndex -> TileState.WRONG
                    right == pairIndex -> TileState.SELECTED
                    else -> TileState.IDLE
                }
                MatchTile(ex.pairs[pairIndex].right, state, onClick = {
                    if (!locked) { right = pairIndex; evaluate() }
                })
            }
        }
    }
}

// ------------------------------------------------------------------ letter arrangement

@Composable
private fun ArrangementView(
    ex: LetterArrangementExercise,
    locked: Boolean,
    quietMode: Boolean,
    slowAudio: Boolean,
    onCheck: (String) -> Unit,
) {
    val picked = remember { mutableStateListOf<Int>() }

    ContentImage(ex.image, size = 120.dp)
    Text(ex.arabic, style = MaterialTheme.typography.headlineSmall)
    if (!quietMode) AudioButton(ex.audio, slow = slowAudio, size = 56.dp)

    // Turkish is written left-to-right, so the letter rows are laid out LTR inside the RTL screen.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            picked.forEachIndexed { pos, tileIndex ->
                LetterTile(ex.tiles[tileIndex], filled = true) { if (!locked) picked.removeAt(pos) }
            }
            if (picked.isEmpty()) {
                Text("اضغط على الحروف بالترتيب", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ex.tiles.forEachIndexed { index, letter ->
                if (index !in picked) LetterTile(letter, filled = false) { if (!locked) picked += index }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton("مسح", { picked.clear() }, Modifier.weight(1f), enabled = !locked && picked.isNotEmpty())
        PrimaryButton(
            "تحقّق",
            { onCheck(picked.joinToString("") { ex.tiles[it] }) },
            Modifier.weight(1f),
            enabled = !locked && picked.size == ex.tiles.size,
        )
    }
}

@Composable
private fun LetterTile(letter: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TurkishText(letter, style = TurkishStyle.Medium) }
}

// ------------------------------------------------------------------ memory game

@Composable
private fun MemoryView(ex: MemoryGameExercise, onFinished: (Int) -> Unit) {
    val faceUp = remember { mutableStateListOf<Int>() }
    val matched = remember { mutableStateListOf<Int>() }
    var first by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    var pendingHide by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(pendingHide) {
        val pair = pendingHide ?: return@LaunchedEffect
        delay(900)
        faceUp.remove(pair.first)
        faceUp.remove(pair.second)
        pendingHide = null
        busy = false
    }

    fun tap(i: Int) {
        if (busy || i in matched || i in faceUp) return
        faceUp += i
        val f = first
        if (f == null) {
            first = i
            return
        }
        moves++
        first = null
        if (ex.cards[f].pairKey == ex.cards[i].pairKey) {
            matched += f
            matched += i
            if (matched.size == ex.cards.size) onFinished(moves)
        } else {
            busy = true
            pendingHide = f to i
        }
    }

    Text("المحاولات: $moves", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val columns = if (ex.cards.size > 6) 4 else 3
    ex.cards.indices.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { i ->
                val shown = i in faceUp || i in matched
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                i in matched -> TkColors.SuccessSoft
                                shown -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                        .border(2.dp, if (i in matched) TkColors.Success else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { tap(i) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (shown) {
                        val option = ex.cards[i].option
                        when {
                            option.image != null -> ContentImage(option.image, size = 46.dp)
                            option.isTurkish -> TurkishText(option.text.orEmpty(), style = TurkishStyle.Small)
                            else -> Text(option.text.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    } else {
                        Text("؟", fontSize = 28.sp, color = Color.White)
                    }
                }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// ------------------------------------------------------------------ listen and repeat

@Composable
private fun ListenRepeatView(ex: ListenAndRepeatExercise, slowAudio: Boolean, locked: Boolean, onDone: () -> Unit) {
    val item = ex.item
    ContentImage(item.image, size = 120.dp)
    TurkishText(item.turkish, style = TurkishStyle.Hero, color = MaterialTheme.colorScheme.secondary)
    Text(item.arabic, style = MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AudioButton(item.audio, size = 80.dp, slow = slowAudio, autoPlayKey = ex.id)
        AudioButton(item.audio, size = 52.dp, slow = !slowAudio)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LearningCharacter(size = 40.dp, mood = MascotMood.HAPPY, animate = false)
        Text(
            "استمع ثم قل الكلمة بصوتك، وكرّر حتى تشعر أنها قريبة من التسجيل. التطبيق لا يقيّم نطقك آليًا.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp).padding(start = 8.dp),
        )
    }
    PrimaryButton("كرّرتها ✔", onDone, Modifier.fillMaxWidth(), enabled = !locked)
}
