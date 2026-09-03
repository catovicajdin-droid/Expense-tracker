package com.catovicajdin.expensetracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Modernist" tokens ported from the Budget Tracker Claude Design handoff: off-white paper, a red
 * accent, flat/square edges (radius 0 throughout), bold condensed-feel type, hairline dividers
 * instead of elevation. Light theme only, per the design - no dark variant defined here.
 */
val ColorBg = Color(0xFFF3F2F2)
val ColorSurface = Color(0xFFEAE9E9)
val ColorText = Color(0xFF201E1D)
val ColorAccent = Color(0xFFEC3013)
val ColorDivider = Color(0x66201E1D)

val Neutral200 = Color(0xFFEAE7E7)
val Neutral300 = Color(0xFFD7D3D3)
val Neutral400 = Color(0xFFBAB6B6)
val Neutral600 = Color(0xFF7D7979)
val Neutral900 = Color(0xFF2D2B2B)

val Accent100 = Color(0xFFFFF2EF)
val Accent200 = Color(0xFFFFE0D9)
val Accent800 = Color(0xFF7C1405)

private val ModernistColors = lightColorScheme(
    primary = ColorAccent,
    onPrimary = ColorBg,
    secondary = Accent800,
    onSecondary = ColorBg,
    background = ColorBg,
    onBackground = ColorText,
    surface = ColorSurface,
    onSurface = ColorText,
    surfaceVariant = Neutral200,
    onSurfaceVariant = ColorText,
    outline = ColorDivider,
    error = ColorAccent,
)

// A bold system sans stands in for the design's Archivo font here: bundling/downloading a real
// font is a integration this session couldn't verify compiles without another build-breaking
// gamble (three already this session). Swap in real Archivo later if wanted.
private val baseTypography = Typography()
private val ModernistTypography = baseTypography.copy(
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
    labelMedium = baseTypography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
    labelSmall = baseTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
    bodyLarge = baseTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyMedium = baseTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
)

private val ModernistShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ModernistColors, typography = ModernistTypography, shapes = ModernistShapes, content = content)
}
