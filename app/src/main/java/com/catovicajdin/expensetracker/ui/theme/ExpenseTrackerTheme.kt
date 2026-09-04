package com.catovicajdin.expensetracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catovicajdin.expensetracker.R

/**
 * "Modernist" tokens, refreshed per the second design pass: soft white cards with subtle shadows
 * on a light-gray paper (replacing the earlier flat/bordered/square look), rounded corners
 * throughout, and a two-font system - Archivo for headings and numbers, Instrument Sans for body
 * and UI chrome. Light theme only, per the design - no dark variant defined here.
 */
val ColorBg = Color(0xFFF3F2F2)
val ScreenBg = Color(0xFFEEECEC)
val CardBg = Color(0xFFFFFFFF)
val ColorText = Color(0xFF201E1D)
val ColorAccent = Color(0xFFEC3013)
val DividerSoft = Color(0x17201E1D)
val InputBg = ScreenBg

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
    onSecondary = CardBg,
    background = ScreenBg,
    onBackground = ColorText,
    surface = CardBg,
    onSurface = ColorText,
    surfaceVariant = InputBg,
    onSurfaceVariant = ColorText.copy(alpha = 0.62f),
    outline = DividerSoft,
    error = ColorAccent,
)

/** Static instances (width=100) cut from Google's Archivo variable font - headings and numbers. */
private val ArchivoFontFamily = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
    Font(R.font.archivo_black, FontWeight.Black),
)

/** Static instances (width=100) cut from Google's Instrument Sans variable font - body and UI chrome. */
private val InstrumentSansFontFamily = FontFamily(
    Font(R.font.instrument_regular, FontWeight.Normal),
    Font(R.font.instrument_medium, FontWeight.Medium),
    Font(R.font.instrument_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_bold, FontWeight.Bold),
)

/** OpenType tabular-figures feature, matching the design's font-variant-numeric:tabular-nums on every amount. */
private const val TabularNums = "tnum"

// Typography() has no single "apply to every style" shortcut in Material3 (unlike Material2) -
// fontFamily is set explicitly on all 15 styles below. Archivo carries headlines/titles (screen
// titles and every money figure); Instrument Sans carries body text, labels, and button chrome.
private val baseTypography = Typography()
private val ModernistTypography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = InstrumentSansFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = InstrumentSansFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = InstrumentSansFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(
        fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, letterSpacing = (-1).sp, fontFeatureSettings = TabularNums,
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
    ),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold),
    titleLarge = baseTypography.titleLarge.copy(
        fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp, fontFeatureSettings = TabularNums,
    ),
    titleMedium = baseTypography.titleMedium.copy(
        fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp, fontFeatureSettings = TabularNums,
    ),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = ArchivoFontFamily, fontWeight = FontWeight.ExtraBold),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Normal),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Normal),
)

private val ModernistShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ModernistColors, typography = ModernistTypography, shapes = ModernistShapes, content = content)
}
