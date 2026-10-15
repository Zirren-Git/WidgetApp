package com.zirren.tessera.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R

/** Paper (day). Values and contrast ratios are documented in docs/PRODUCT_SPEC.md §9. */
private val Paper = lightColorScheme(
    primary = Color(0xFFB03A12),
    onPrimary = Color(0xFFFFF8F0),
    primaryContainer = Color(0xFFF4DED2),
    onPrimaryContainer = Color(0xFF4A1604),
    secondary = Color(0xFF615B51),
    onSecondary = Color(0xFFFAF7F1),
    background = Color(0xFFF3EEE4),
    onBackground = Color(0xFF1C1A17),
    surface = Color(0xFFF3EEE4),
    onSurface = Color(0xFF1C1A17),
    surfaceVariant = Color(0xFFFAF7F1),
    onSurfaceVariant = Color(0xFF615B51),
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = Color(0xFFF7F3EC),
    surfaceContainer = Color(0xFFFAF7F1),
    surfaceContainerHigh = Color(0xFFFAF7F1),
    surfaceContainerHighest = Color(0xFFFFFCF7),
    outline = Color(0xFF615B51),
    outlineVariant = Color(0xFFD9D0C0),
)

/** Darkroom (night): near-black, warm off-white text, safelight accent. */
private val Darkroom = darkColorScheme(
    primary = Color(0xFFF07445),
    onPrimary = Color(0xFF1A0D07),
    primaryContainer = Color(0xFF4A2112),
    onPrimaryContainer = Color(0xFFFFDCCD),
    secondary = Color(0xFFA39C8F),
    onSecondary = Color(0xFF11100E),
    background = Color(0xFF11100E),
    onBackground = Color(0xFFEDE6D8),
    surface = Color(0xFF11100E),
    onSurface = Color(0xFFEDE6D8),
    surfaceVariant = Color(0xFF23201C),
    onSurfaceVariant = Color(0xFFA39C8F),
    surfaceContainerLowest = Color(0xFF0C0B0A),
    surfaceContainerLow = Color(0xFF171513),
    surfaceContainer = Color(0xFF1A1815),
    surfaceContainerHigh = Color(0xFF23201C),
    surfaceContainerHighest = Color(0xFF2B2823),
    outline = Color(0xFFA39C8F),
    outlineVariant = Color(0xFF302C27),
)

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium))

/** Heavy, tight display serif; calm sans for reading; tracked mono for labels and numbers. */
private val TesseraTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 72.sp, lineHeight = 76.sp, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.9.sp),
)

/**
 * Paper by day, Darkroom by night (following the phone), or the phone's wallpaper colours
 * when the user turns that on (Android 12+). Typography stays Tessera's either way.
 */
@Composable
fun TesseraTheme(useWallpaperColors: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        useWallpaperColors && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Darkroom
        else -> Paper
    }
    MaterialTheme(colorScheme = colors, typography = TesseraTypography, content = content)
}
