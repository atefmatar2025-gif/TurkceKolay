package com.turkcekolay.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TkColors {
    val Turquoise = Color(0xFF14A3A3)
    val TurquoiseDark = Color(0xFF0B7C7C)
    val TurquoiseSoft = Color(0xFFD6F2F0)
    val DeepBlue = Color(0xFF1D3E6E)
    val DeepBlueSoft = Color(0xFFDCE5F2)
    val Orange = Color(0xFFF2994A)
    val OrangeSoft = Color(0xFFFDE8D4)
    val Background = Color(0xFFF8F7F2)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1F2933)
    val InkSoft = Color(0xFF5B6773)
    val Success = Color(0xFF2E9E5B)
    val SuccessSoft = Color(0xFFDDF3E5)
    val Gentle = Color(0xFFE07A5F) // used for "not quite" feedback — warm, not alarming red
    val GentleSoft = Color(0xFFFBE4DD)
    val Divider = Color(0xFFE7E5DE)
}

private val LightScheme = lightColorScheme(
    primary = TkColors.Turquoise,
    onPrimary = Color.White,
    primaryContainer = TkColors.TurquoiseSoft,
    onPrimaryContainer = TkColors.TurquoiseDark,
    secondary = TkColors.DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = TkColors.DeepBlueSoft,
    onSecondaryContainer = TkColors.DeepBlue,
    tertiary = TkColors.Orange,
    onTertiary = Color.White,
    tertiaryContainer = TkColors.OrangeSoft,
    onTertiaryContainer = Color(0xFF7A3E0A),
    background = TkColors.Background,
    onBackground = TkColors.Ink,
    surface = TkColors.Card,
    onSurface = TkColors.Ink,
    surfaceVariant = Color(0xFFF1EFE8),
    onSurfaceVariant = TkColors.InkSoft,
    outline = TkColors.Divider,
    error = TkColors.Gentle,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4FD1C9),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF0B4F4D),
    onPrimaryContainer = Color(0xFFB8F0EC),
    secondary = Color(0xFFA9C4EC),
    onSecondary = Color(0xFF0E2747),
    secondaryContainer = Color(0xFF223D63),
    tertiary = Color(0xFFFFB77C),
    tertiaryContainer = Color(0xFF5A3210),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE6E8E6),
    surface = Color(0xFF1B201E),
    onSurface = Color(0xFFE6E8E6),
    surfaceVariant = Color(0xFF242A28),
    onSurfaceVariant = Color(0xFFB5BDBA),
    outline = Color(0xFF39413E),
    error = Color(0xFFFFB4A1),
)

private val base = Typography()

/** System fonts render Arabic (Noto Naskh/Sans Arabic) and Turkish Latin glyphs well on all devices. */
val TkTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

/** Styles for Turkish text: always left-to-right, Latin-friendly, large. */
object TurkishStyle {
    val Hero = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, textDirection = TextDirection.Ltr)
    val Large = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, textDirection = TextDirection.Ltr)
    val Medium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, textDirection = TextDirection.Ltr)
    val Small = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, textDirection = TextDirection.Ltr)
}

val TkShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun TurkceKolayTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // The interface language is Arabic, so the whole UI is laid out right-to-left
    // regardless of the device language. Turkish text opts back into LTR via TurkishStyle.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = TkTypography,
            shapes = TkShapes,
            content = content,
        )
    }
}
