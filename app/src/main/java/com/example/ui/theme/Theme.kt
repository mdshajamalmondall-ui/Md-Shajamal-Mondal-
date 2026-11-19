package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class NovaExtendedColors(
    val sim1: Color,
    val sim1Container: Color,
    val sim2: Color,
    val sim2Container: Color,
    val brilliant: Color,
    val brilliantGlow: Color,
    val brilliantContainer: Color,
    val missed: Color,
    val missedContainer: Color,
    val gold: Color,
    val goldContainer: Color,
    val whatsApp: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val isDark: Boolean
)

private val DarkExtendedColors = NovaExtendedColors(
    sim1 = Sim1Emerald,
    sim1Container = Sim1EmeraldContainer,
    sim2 = Sim2Azure,
    sim2Container = Sim2AzureContainer,
    brilliant = BrilliantViolet,
    brilliantGlow = BrilliantVioletGlow,
    brilliantContainer = BrilliantVioletContainer,
    missed = MissedCoral,
    missedContainer = MissedCoralContainer,
    gold = ChampagneGold,
    goldContainer = ChampagneGoldContainer,
    whatsApp = WhatsAppGreen,
    glassSurface = ObsidianSurfaceElevated,
    glassBorder = ObsidianOutline,
    isDark = true
)

private val LightExtendedColors = NovaExtendedColors(
    sim1 = Sim1EmeraldLight,
    sim1Container = Sim1EmeraldContainerLight,
    sim2 = Sim2AzureLight,
    sim2Container = Sim2AzureContainerLight,
    brilliant = BrilliantVioletLight,
    brilliantGlow = BrilliantViolet,
    brilliantContainer = BrilliantVioletContainerLight,
    missed = MissedCoralLight,
    missedContainer = MissedCoralContainerLight,
    gold = GoldLightPrimary,
    goldContainer = GoldLightContainer,
    whatsApp = WhatsAppGreen,
    glassSurface = PearlSurface,
    glassBorder = PearlOutline,
    isDark = false
)

val LocalNovaColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = OnChampagneGold,
    primaryContainer = ChampagneGoldContainer,
    onPrimaryContainer = ChampagneGold,
    secondary = BrilliantViolet,
    onSecondary = Color.White,
    secondaryContainer = BrilliantVioletContainer,
    onSecondaryContainer = Color(0xFFE7DCFF),
    tertiary = Sim1Emerald,
    onTertiary = Color(0xFF042619),
    tertiaryContainer = Sim1EmeraldContainer,
    onTertiaryContainer = Sim1Emerald,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = MissedCoral,
    onError = Color.White,
    errorContainer = MissedCoralContainer,
    onErrorContainer = Color(0xFFFFD9DD)
)

private val LightColorScheme = lightColorScheme(
    primary = GoldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = GoldLightContainer,
    onPrimaryContainer = Color(0xFF3B2400),
    secondary = BrilliantVioletLight,
    onSecondary = Color.White,
    secondaryContainer = BrilliantVioletContainerLight,
    onSecondaryContainer = Color(0xFF280D6B),
    tertiary = Sim1EmeraldLight,
    onTertiary = Color.White,
    tertiaryContainer = Sim1EmeraldContainerLight,
    onTertiaryContainer = Color(0xFF053825),
    background = PearlBg,
    onBackground = TextPrimaryLight,
    surface = PearlSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PearlSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = PearlSurface,
    surfaceContainerLow = PearlBg,
    surfaceContainer = PearlSurfaceElevated,
    surfaceContainerHigh = PearlSurfaceHighest,
    outline = PearlOutline,
    outlineVariant = Color(0xFFE4E8F2),
    error = MissedCoralLight,
    onError = Color.White,
    errorContainer = MissedCoralContainerLight,
    onErrorContainer = Color(0xFF540610)
)

@Composable
fun NovaCallTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalNovaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            content = content
        )
    }
}
