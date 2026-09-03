package com.alterego.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alterego.app.domain.models.Persona

/**
 * The app takes its colour from whoever the user chose to walk with. Changing companion changes
 * the whole surface, which is the clearest signal that this is a relationship, not a dashboard.
 */
data class PersonaColors(
    val primary: Color,
    val accent: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val muted: Color,
)

val LocalPersonaColors = staticCompositionLocalOf {
    PersonaColors(
        primary = Color(0xFF3E5C76),
        accent = Color(0xFFC9A227),
        background = Color(0xFF0F1B2B),
        onBackground = Color(0xFFF2EFE9),
        surface = Color(0xFF17263A),
        muted = Color(0xFF8FA3B8),
    )
}

fun personaColors(persona: Persona?): PersonaColors {
    val background = Color(persona?.backgroundColor ?: 0xFF0F1B2BL)
    val isLight = background.luminanceApprox() > 0.55f
    return PersonaColors(
        primary = Color(persona?.primaryColor ?: 0xFF3E5C76L),
        accent = Color(persona?.accentColor ?: 0xFFC9A227L),
        background = background,
        onBackground = if (isLight) Color(0xFF17171A) else Color(0xFFF2EFE9),
        surface = if (isLight) background.darken(0.06f) else background.lighten(0.06f),
        muted = if (isLight) Color(0xFF5B6470) else Color(0xFF8FA3B8),
    )
}

private fun Color.luminanceApprox(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)
private fun Color.lighten(amount: Float) = Color(
    (red + amount).coerceAtMost(1f), (green + amount).coerceAtMost(1f), (blue + amount).coerceAtMost(1f), alpha,
)
private fun Color.darken(amount: Float) = Color(
    (red - amount).coerceAtLeast(0f), (green - amount).coerceAtLeast(0f), (blue - amount).coerceAtLeast(0f), alpha,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, fontSize = 44.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 1.2.sp),
)

@Composable
fun AlterEgoTheme(persona: Persona? = null, content: @Composable () -> Unit) {
    val colors = personaColors(persona)
    val scheme = if (colors.onBackground == Color(0xFFF2EFE9)) {
        darkColorScheme(
            primary = colors.primary, secondary = colors.accent, background = colors.background,
            surface = colors.surface, onBackground = colors.onBackground, onSurface = colors.onBackground,
            onPrimary = Color.White, onSecondary = Color(0xFF17171A),
        )
    } else {
        lightColorScheme(
            primary = colors.primary, secondary = colors.accent, background = colors.background,
            surface = colors.surface, onBackground = colors.onBackground, onSurface = colors.onBackground,
            onPrimary = Color.White, onSecondary = Color(0xFF17171A),
        )
    }
    CompositionLocalProvider(LocalPersonaColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

@Composable
fun isDarkPersona(): Boolean = LocalPersonaColors.current.onBackground == Color(0xFFF2EFE9) || isSystemInDarkTheme()
