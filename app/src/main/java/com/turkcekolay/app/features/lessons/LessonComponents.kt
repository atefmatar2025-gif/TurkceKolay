package com.turkcekolay.app.features.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.turkcekolay.app.core.designsystem.AudioButton
import com.turkcekolay.app.core.designsystem.ContentImage
import com.turkcekolay.app.core.designsystem.LearningCharacter
import com.turkcekolay.app.core.designsystem.MascotMood
import com.turkcekolay.app.core.designsystem.PrimaryButton
import com.turkcekolay.app.core.designsystem.SecondaryButton
import com.turkcekolay.app.core.designsystem.StatChip
import com.turkcekolay.app.core.designsystem.TkCard
import com.turkcekolay.app.core.designsystem.TkColors
import com.turkcekolay.app.core.designsystem.TkProgressIndicator
import com.turkcekolay.app.core.designsystem.TurkishStyle
import com.turkcekolay.app.core.designsystem.TurkishText
import com.turkcekolay.app.core.designsystem.VocabularyCard
import com.turkcekolay.app.core.designsystem.FavoriteButton
import com.turkcekolay.app.core.designsystem.HighlightedWord
import com.turkcekolay.app.domain.model.ItemKind
import com.turkcekolay.app.domain.model.LearnItem
import com.turkcekolay.app.domain.model.PresentationMode

/** Learn phase: one card at a time, picture first. */
@Composable
fun LearnCardsContent(
    title: String,
    items: List<LearnItem>,
    index: Int,
    favorites: Set<String>,
    quietMode: Boolean,
    mode: PresentationMode = PresentationMode.A,
    onToggleFavorite: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    nextLabel: String = "التالي",
    lastLabel: String = "لنتدرّب!",
) {
    val item = items[index]
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("$title · ${index + 1}/${items.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                TkProgressIndicator((index + 1f) / items.size)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (item.kind == ItemKind.LETTER) {
                LetterCard(item, isFavorite = item.id in favorites, onToggleFavorite = { onToggleFavorite(item.id) }, autoPlay = !quietMode)
            } else {
                VocabularyCard(
                    image = item.image,
                    turkish = item.turkish,
                    arabic = item.arabic,
                    audio = item.audio,
                    showTurkish = mode == PresentationMode.A || mode == PresentationMode.B,
                    showArabic = mode == PresentationMode.A,
                    showImage = mode != PresentationMode.E,
                    example = item.exampleTurkish?.let { it to item.exampleArabic.orEmpty() },
                    exampleAudio = item.exampleAudio,
                    note = item.note,
                    isFavorite = item.id in favorites,
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    autoPlayKey = if (quietMode) null else item.id,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (index > 0) SecondaryButton("السابق", onPrevious, Modifier.weight(1f))
            PrimaryButton(if (index == items.lastIndex) lastLabel else nextLabel, onNext, Modifier.weight(2f))
        }
    }
}

@Composable
fun LetterCard(item: LearnItem, isFavorite: Boolean, onToggleFavorite: () -> Unit, autoPlay: Boolean) {
    TkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { FavoriteButton(isFavorite, onToggleFavorite) }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
                // Always shown as "Upper lower" in reading order (LTR)
                TurkishText("${item.turkish} ${item.turkishLower}", style = TurkishStyle.Hero.copy(fontSize = TurkishStyle.Hero.fontSize * 1.6f), color = MaterialTheme.colorScheme.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AudioButton(item.audio, size = 64.dp, autoPlayKey = if (autoPlay) item.id else null, repeat = true)
                AudioButton(item.audio, slow = true, size = 52.dp)
            }
            item.hintArabic?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("النطق التقريبي:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                        Text("(تقريب فقط — استمع للصوت التركي الحقيقي)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (item.exampleTurkish != null) {
                LetterExample(item)
            }
            item.note?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = TkColors.Orange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Picture of the example word, the word with its first letter highlighted, and its meaning. */
@Composable
fun LetterExample(item: LearnItem, imageHeight: androidx.compose.ui.unit.Dp = 200.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().height(imageHeight).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) { ContentImage(item.image, size = imageHeight * 0.7f, fill = !item.image.startsWith("emoji:")) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightedWord(item.exampleTurkish.orEmpty(), style = TurkishStyle.Hero, color = MaterialTheme.colorScheme.secondary)
            item.exampleAudio?.let { AudioButton(it, size = 48.dp) }
        }
        Text("= ${item.exampleArabic.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun ResultContent(
    title: String,
    scorePercent: Int,
    correct: Int,
    total: Int,
    xp: Int,
    onDone: () -> Unit,
    onRepeat: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LearningCharacter(size = 120.dp, mood = MascotMood.CHEER)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                scorePercent >= 90 -> "عمل رائع! أنت تتقدّم بثبات."
                scorePercent >= 60 -> "أحسنت! كل محاولة تقرّبك من الإتقان."
                else -> "بداية جيدة! سنراجع الكلمات الصعبة معًا في «مراجعتي الذكية»."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (total > 0) StatChip("🎯", "$correct/$total", "من أول محاولة", Modifier.weight(1f))
            StatChip("⭐", "+$xp", "نقاط خبرة", Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("تم", onDone, Modifier.fillMaxWidth())
        if (onRepeat != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("أعد الدرس", onRepeat, Modifier.fillMaxWidth())
        }
    }
}
