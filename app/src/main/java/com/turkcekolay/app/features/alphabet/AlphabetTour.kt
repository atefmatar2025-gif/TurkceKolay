package com.turkcekolay.app.features.alphabet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.turkcekolay.app.audio.SpeechStep
import com.turkcekolay.app.core.designsystem.AudioButton
import com.turkcekolay.app.core.designsystem.LocalAudioManager
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.features.lessons.LetterExample
import kotlinx.coroutines.CancellationException

private val SPECIAL = setOf("Ç", "Ş", "Ğ", "I", "İ", "Ö", "Ü")

/**
 * Step one of the course: the learner hears every letter (its Turkish name, several times, slowly)
 * followed by an example word with its picture. Plays automatically letter after letter;
 * the learner can pause, go back, or tap any letter in the strip.
 */
@Composable
fun AlphabetTourContent(
    letters: List<LearnItem>,
    quietMode: Boolean,
    onClose: () -> Unit,
    onFinished: () -> Unit,
) {
    val audio = LocalAudioManager.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    var auto by rememberSaveable { mutableStateOf(!quietMode) }
    var reachedEnd by rememberSaveable { mutableStateOf(false) }
    val letter = letters[index]

    LaunchedEffect(index, auto) {
        if (index == letters.lastIndex) reachedEnd = true
        if (!auto || audio == null) return@LaunchedEffect
        if (!audio.canPlay(letter.audio)) {
            auto = false
            return@LaunchedEffect
        }
        val steps = buildList {
            add(SpeechStep(letter.audio, times = audio.repeatCount, pauseAfterMs = 700))
            letter.exampleAudio?.let { add(SpeechStep(it, times = 2, pauseAfterMs = 1200)) }
        }
        val job = audio.playSequence(steps)
        try {
            job.join()
        } catch (e: CancellationException) {
            audio.stop()
            throw e
        }
        if (index < letters.lastIndex) index++ else auto = false
    }
    DisposableEffect(Unit) { onDispose { audio?.stop() } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("الخطوة الأولى: استمع إلى الحروف · ${index + 1}/${letters.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                TkProgressIndicator((index + 1f) / letters.size)
            }
        }

        LetterStrip(letters, index) { auto = false; index = it }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TurkishText(
                "${letter.turkish} ${letter.turkishLower}",
                style = TurkishStyle.Hero.copy(fontSize = TurkishStyle.Hero.fontSize * 2f, lineHeight = TurkishStyle.Hero.lineHeight * 2f),
                color = if (letter.turkish in SPECIAL) TkColors.Orange else MaterialTheme.colorScheme.secondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AudioButton(letter.audio, size = 60.dp, repeat = true)
                AudioButton(letter.audio, size = 48.dp, slow = true)
            }
            letter.hintArabic?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Text("(تقريب فقط — الصوت التركي هو المرجع)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LetterExample(letter, imageHeight = 190.dp)
            letter.note?.let {
                Text("💡 $it", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }

        // Controls follow the letter strip: A is on the left, so "previous" is on the left too.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { auto = false; if (index > 0) index-- }, enabled = index > 0) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "الحرف السابق", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(onClick = { auto = !auto; if (!auto) audio?.stop() }, modifier = Modifier.size(72.dp)) {
                    Icon(if (auto) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (auto) "إيقاف" else "تشغيل تلقائي", modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { auto = false; if (index < letters.lastIndex) index++ }, enabled = index < letters.lastIndex) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "الحرف التالي", modifier = Modifier.size(36.dp))
                }
            }
        }
        if (reachedEnd) {
            PrimaryButton("أنهيت الخطوة الأولى ✔", onFinished, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LetterStrip(letters: List<LearnItem>, current: Int, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(current) { state.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    // Alphabet order is left-to-right, like Turkish writing.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LazyRow(state = state, contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(letters, key = { _, l -> l.id }) { i, l ->
                val selected = i == current
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.primary
                                i < current -> TkColors.SuccessSoft
                                else -> MaterialTheme.colorScheme.surface
                            },
                        )
                        .border(1.5.dp, if (l.turkish in SPECIAL) TkColors.Orange else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    TurkishText(l.turkish, style = TurkishStyle.Small, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
