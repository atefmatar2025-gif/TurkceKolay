package com.turkcekolay.app.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MascotMood { HAPPY, CHEER, THINKING }

/**
 * «Kolay» the turquoise bird — the learning character. Drawn in code so it scales crisply,
 * stays small and gently bobs; it never covers content.
 */
@Composable
fun LearningCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    mood: MascotMood = MascotMood.HAPPY,
    animate: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) -4f else 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bob",
    )
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { translationY = bob }
            .semantics { contentDescription = "Kolay" },
    ) {
        val w = this.size.width
        val h = this.size.height
        val body = TkColors.Turquoise
        val belly = Color(0xFFE9FAF8)
        // wing behind
        drawOval(TkColors.TurquoiseDark, topLeft = Offset(w * 0.08f, h * 0.42f), size = Size(w * 0.34f, h * 0.36f))
        // body
        drawOval(body, topLeft = Offset(w * 0.16f, h * 0.18f), size = Size(w * 0.68f, h * 0.72f))
        drawOval(belly, topLeft = Offset(w * 0.30f, h * 0.48f), size = Size(w * 0.40f, h * 0.36f))
        // tuft
        val tuft = Path().apply {
            moveTo(w * 0.46f, h * 0.20f); lineTo(w * 0.52f, h * 0.04f); lineTo(w * 0.58f, h * 0.20f); close()
        }
        drawPath(tuft, TkColors.Orange)
        // eyes
        val eyeY = h * 0.38f
        listOf(w * 0.38f, w * 0.62f).forEach { x ->
            drawCircle(Color.White, radius = w * 0.085f, center = Offset(x, eyeY))
            if (mood == MascotMood.CHEER) {
                drawArc(TkColors.DeepBlue, 200f, 140f, false, topLeft = Offset(x - w * 0.05f, eyeY - w * 0.02f),
                    size = Size(w * 0.10f, w * 0.08f), style = Stroke(width = w * 0.025f))
            } else {
                val dx = if (mood == MascotMood.THINKING) w * 0.02f else 0f
                drawCircle(TkColors.DeepBlue, radius = w * 0.045f, center = Offset(x + dx, eyeY - (if (mood == MascotMood.THINKING) w * 0.02f else 0f)))
            }
        }
        // beak
        val beak = Path().apply {
            moveTo(w * 0.45f, h * 0.47f); lineTo(w * 0.55f, h * 0.47f); lineTo(w * 0.50f, h * 0.56f); close()
        }
        drawPath(beak, TkColors.Orange)
        // feet
        drawLine(TkColors.Orange, Offset(w * 0.42f, h * 0.89f), Offset(w * 0.42f, h * 0.97f), strokeWidth = w * 0.03f)
        drawLine(TkColors.Orange, Offset(w * 0.58f, h * 0.89f), Offset(w * 0.58f, h * 0.97f), strokeWidth = w * 0.03f)
    }
}

/** Mascot with a short speech bubble. Messages stay one or two short Arabic lines. */
@Composable
fun MascotMessage(
    message: String,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LearningCharacter(size = 56.dp, mood = mood)
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
