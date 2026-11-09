package com.turkcekolay.app.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turkcekolay.app.audio.AudioManager
import com.turkcekolay.app.audio.TtsStatus
import com.turkcekolay.app.domain.model.AudioRef
import com.turkcekolay.app.domain.model.ChoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalAudioManager = staticCompositionLocalOf<AudioManager?> { null }

// ------------------------------------------------------------------ text

@Composable
fun TurkishText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = TurkishStyle.Large,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(text = text, modifier = modifier, style = style, color = color, textAlign = textAlign)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

// ------------------------------------------------------------------ images

/** Renders a stable content image reference: "emoji:🍎" or a bundled asset path. */
@Composable
fun ContentImage(image: String, modifier: Modifier = Modifier, size: Dp = 120.dp, fill: Boolean = false) {
    if (image.startsWith("emoji:")) {
        Box(if (fill) modifier.fillMaxSize() else modifier.size(size), contentAlignment = Alignment.Center) {
            Text(image.removePrefix("emoji:"), fontSize = (size.value * 0.62f).sp, textAlign = TextAlign.Center)
        }
    } else {
        val context = LocalContext.current
        val bitmap by produceState<ImageBitmap?>(null, image) {
            value = withContext(Dispatchers.IO) {
                runCatching { context.assets.open(image).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
        // Photos fill their frame (cropped to the centre) with rounded corners.
        Box(
            (if (fill) modifier.fillMaxSize() else modifier.size(size)).clip(RoundedCornerShape(if (fill) 18.dp else 14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
    }
}

// ------------------------------------------------------------------ buttons

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}

/**
 * Plays a recording or the Turkish TTS fallback. When no playback path exists the button is shown
 * crossed out and explains why, instead of pretending to work.
 */
@Composable
fun AudioButton(
    audio: AudioRef,
    modifier: Modifier = Modifier,
    slow: Boolean = false,
    size: Dp = 56.dp,
    autoPlayKey: Any? = null,
    /** Say it several times (the learner's repeat setting, 3 by default). */
    repeat: Boolean = false,
) {
    val manager = LocalAudioManager.current
    val ttsStatus by (manager?.ttsStatus ?: remember { kotlinx.coroutines.flow.MutableStateFlow(TtsStatus.ERROR) }).collectAsState()
    val available = remember(audio, ttsStatus) { manager?.canPlay(audio) == true }
    var showWhy by remember { mutableStateOf(false) }

    if (autoPlayKey != null) {
        LaunchedEffect(autoPlayKey, available) { if (available) manager?.play(audio, slow, repeat) }
    }

    if (available) {
        FilledIconButton(
            onClick = { manager?.play(audio, slow, repeat) },
            modifier = modifier.size(size),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (slow) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                contentColor = if (slow) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                if (slow) Icons.Rounded.SlowMotionVideo else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = if (slow) "استماع ببطء شديد" else "استماع",
                modifier = Modifier.size(size * 0.5f),
            )
        }
    } else {
        IconButton(
            onClick = { showWhy = true },
            modifier = modifier
                .size(size)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = "الصوت غير متوفر",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            confirmButton = { TextButton(onClick = { showWhy = false }) { Text("حسنًا") } },
            title = { Text("الصوت غير متوفر") },
            text = {
                Text(
                    if (ttsStatus == TtsStatus.INITIALIZING) "جارٍ تجهيز الصوت التركي… حاول بعد لحظات."
                    else "لا يوجد تسجيل لهذا المحتوى، والصوت التركي غير مثبّت على جهازك. يمكنك تثبيته من الإعدادات ← الصوت.",
                )
            },
        )
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
            tint = if (isFavorite) TkColors.Orange else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ------------------------------------------------------------------ bars & indicators

@Composable
fun TkTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun TkProgressIndicator(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun LoadingState(message: String = "جارٍ التحميل…") {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        LearningCharacter(size = 88.dp, mood = MascotMood.THINKING)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LearningCharacter(size = 96.dp)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

// ------------------------------------------------------------------ cards

@Composable
fun TkCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, color: Color = MaterialTheme.colorScheme.surface, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val colors = CardDefaults.cardColors(containerColor = color)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation) { content() }
    else Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation) { content() }
}

@Composable
fun CategoryCard(title: String, subtitle: String, image: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TkCard(modifier, onClick = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContentImage(image, size = 64.dp)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1)
            TurkishText(subtitle, style = TurkishStyle.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LessonProgressCard(
    title: String,
    subtitle: String,
    image: String,
    progress: Float,
    completed: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    TkCard(
        modifier,
        onClick = if (locked) null else onClick,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    locked -> Icon(Icons.Rounded.Lock, contentDescription = "مقفل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    completed -> Icon(Icons.Rounded.CheckCircle, contentDescription = "مكتمل", tint = TkColors.Success, modifier = Modifier.size(36.dp))
                    else -> ContentImage(image, size = 40.dp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (!locked && progress > 0f && !completed) {
                    Spacer(Modifier.height(6.dp))
                    TkProgressIndicator(progress)
                }
            }
        }
    }
}

/** The core visual vocabulary card: large picture, Turkish, Arabic, audio and example. */
@Composable
fun VocabularyCard(
    image: String,
    turkish: String,
    arabic: String,
    audio: AudioRef,
    modifier: Modifier = Modifier,
    showTurkish: Boolean = true,
    showArabic: Boolean = true,
    showImage: Boolean = true,
    example: Pair<String, String>? = null,
    exampleAudio: AudioRef? = null,
    note: String? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: () -> Unit = {},
    autoPlayKey: Any? = null,
) {
    var revealed by rememberSaveable(turkish) { mutableStateOf(false) }
    TkCard(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isFavorite != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FavoriteButton(isFavorite, onToggleFavorite)
                }
            }
            if (showImage) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) { ContentImage(image, size = 150.dp, fill = true) }
            }
            val showT = showTurkish || revealed
            val showA = showArabic || revealed
            if (showT) TurkishText(turkish, style = TurkishStyle.Hero, color = MaterialTheme.colorScheme.secondary)
            if (showA) Text(arabic, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (!showT || !showA) {
                TextButton(onClick = { revealed = true }) { Text("اضغط لإظهار الكلمة") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AudioButton(audio, size = 64.dp, autoPlayKey = autoPlayKey, repeat = true)
                AudioButton(audio, slow = true, size = 52.dp)
            }
            if (example != null && (showT || revealed)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            TurkishText(example.first, style = TurkishStyle.Medium)
                            Text(example.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                        if (exampleAudio != null) AudioButton(exampleAudio, size = 44.dp)
                    }
                }
            }
            if (note != null && (showT || revealed)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = TkColors.Orange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

enum class ChoiceState { IDLE, CORRECT, WRONG, REVEALED_CORRECT, DISABLED }

private data class ChoicePalette(val container: Color, val border: Color)

@Composable
private fun choicePalette(state: ChoiceState): ChoicePalette {
    val container by animateColorAsState(
        when (state) {
            ChoiceState.CORRECT, ChoiceState.REVEALED_CORRECT -> TkColors.SuccessSoft
            ChoiceState.WRONG -> TkColors.GentleSoft
            else -> MaterialTheme.colorScheme.surface
        },
        label = "choiceBg",
    )
    val border = when (state) {
        ChoiceState.CORRECT, ChoiceState.REVEALED_CORRECT -> TkColors.Success
        ChoiceState.WRONG -> TkColors.Gentle
        else -> MaterialTheme.colorScheme.outline
    }
    return ChoicePalette(container, border)
}

/** A large tappable picture used in picture-selection exercises. */
@Composable
fun PictureChoiceCard(option: ChoiceOption, state: ChoiceState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = choicePalette(state)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.container)
            .border(2.dp, palette.border, RoundedCornerShape(22.dp))
            .clickable(enabled = state == ChoiceState.IDLE, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (option.image != null) {
            Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ContentImage(option.image, size = 96.dp, fill = true)
                }
                option.label?.let { HighlightedWord(it, style = TurkishStyle.Medium, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        option.text?.let {
            if (option.isTurkish) TurkishText(it, style = TurkishStyle.Medium)
            else Text(it, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

/** A Turkish word with its first letter enlarged and coloured, to link the letter to the word. */
@Composable
fun HighlightedWord(
    word: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = TurkishStyle.Large,
    color: Color = MaterialTheme.colorScheme.onSurface,
    highlightLength: Int = 1,
) {
    val n = highlightLength.coerceIn(0, word.length)
    val text = androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(color = TkColors.Orange, fontWeight = FontWeight.ExtraBold))
        append(word.take(n))
        pop()
        append(word.drop(n))
    }
    Text(text = text, modifier = modifier, style = style, color = color, textAlign = TextAlign.Center)
}

/** A wide tappable text option (Turkish or Arabic). */
@Composable
fun TextChoiceCard(option: ChoiceOption, state: ChoiceState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = choicePalette(state)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.container)
            .border(2.dp, palette.border, RoundedCornerShape(18.dp))
            .clickable(enabled = state == ChoiceState.IDLE, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (option.isTurkish) TurkishText(option.text.orEmpty(), style = TurkishStyle.Medium)
        else Text(option.text.orEmpty(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** Feedback shown after each answer. Wrong answers are explained kindly, never punished. */
@Composable
fun ExerciseFeedback(
    visible: Boolean,
    correct: Boolean,
    explanation: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }, modifier = modifier) {
        val bg = if (correct) TkColors.SuccessSoft else TkColors.OrangeSoft
        Surface(color = bg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LearningCharacter(size = 48.dp, mood = if (correct) MascotMood.CHEER else MascotMood.THINKING, animate = false)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (correct) encouragement(explanation.hashCode()) else "قريب! لنتعلّمها معًا",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (correct) TkColors.Success else Color(0xFF8A4A12),
                    )
                }
                if (!correct && !explanation.isNullOrBlank()) {
                    Text(explanation, style = MaterialTheme.typography.bodyLarge, color = TkColors.Ink)
                    Text("سنعود إليها بعد قليل بطريقة أخرى 🌱", style = MaterialTheme.typography.bodyMedium, color = TkColors.InkSoft)
                }
                PrimaryButton("متابعة", onContinue, Modifier.fillMaxWidth())
            }
        }
    }
}

private val praises = listOf("أحسنت! 🎉", "رائع! 🌟", "ممتاز! 👏", "صحيح! 💪", "برافو! ✨")
private fun encouragement(seed: Int) = praises[((seed % praises.size) + praises.size) % praises.size]

@Composable
fun AchievementBadge(emoji: String, title: String, description: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(if (unlocked) MaterialTheme.colorScheme.surface else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (unlocked) emoji else "🔒", fontSize = 30.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1)
        Text(description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
fun StatChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
