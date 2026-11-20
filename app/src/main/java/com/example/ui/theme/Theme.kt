package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.data.model.AppFontScale
import com.example.data.model.AppThemePalette
import com.example.data.model.DisplayThemeMode

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
    val isDark: Boolean,
    val liquidGlassEnabled: Boolean = true
)

private fun buildDarkExtended(surfaceElevated: Color, outline: Color, goldAccent: Color = ChampagneGold): NovaExtendedColors =
    NovaExtendedColors(
        sim1 = Sim1Emerald,
        sim1Container = Sim1EmeraldContainer,
        sim2 = Sim2Azure,
        sim2Container = Sim2AzureContainer,
        brilliant = BrilliantViolet,
        brilliantGlow = BrilliantVioletGlow,
        brilliantContainer = BrilliantVioletContainer,
        missed = MissedCoral,
        missedContainer = MissedCoralContainer,
        gold = goldAccent,
        goldContainer = ChampagneGoldContainer,
        whatsApp = WhatsAppGreen,
        glassSurface = surfaceElevated,
        glassBorder = outline,
        isDark = true
    )

private val SapphireExtendedColors = buildDarkExtended(ObsidianSurfaceElevated, ObsidianOutline)
private val EmeraldExtendedColors = buildDarkExtended(EmeraldSurfaceElevated, EmeraldOutline, EmeraldPrimary)
private val AmethystExtendedColors = buildDarkExtended(AmethystSurfaceElevated, AmethystOutline, AmethystPrimary)
private val OceanExtendedColors = buildDarkExtended(OceanSurfaceElevated, OceanOutline, OceanPrimary)
private val CrimsonExtendedColors = buildDarkExtended(CrimsonSurfaceElevated, CrimsonOutline, CrimsonPrimary)
private val SunsetExtendedColors = buildDarkExtended(SunsetSurfaceElevated, SunsetOutline, SunsetPrimary)
private val MidnightRoseExtendedColors = buildDarkExtended(MidnightRoseSurfaceElevated, MidnightRoseOutline, MidnightRosePrimary)
private val CyberIndigoExtendedColors = buildDarkExtended(CyberIndigoSurfaceElevated, CyberIndigoOutline, CyberIndigoPrimary)

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

private val MintLightExtendedColors = LightExtendedColors.copy(
    gold = MintLightPrimary,
    goldContainer = MintLightContainer,
    glassSurface = MintSurface,
    glassBorder = MintOutline
)

val LocalNovaColors = staticCompositionLocalOf { SapphireExtendedColors }

private fun buildDarkScheme(
    primary: Color,
    primaryContainer: Color,
    bg: Color,
    surface: Color,
    surfaceElevated: Color,
    surfaceHighest: Color,
    outline: Color,
    outlineSubtle: Color
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = OnChampagneGold,
    primaryContainer = primaryContainer,
    onPrimaryContainer = primary,
    secondary = BrilliantViolet,
    onSecondary = Color.White,
    secondaryContainer = BrilliantVioletContainer,
    onSecondaryContainer = Color(0xFFE7DCFF),
    tertiary = Sim1Emerald,
    onTertiary = Color(0xFF042619),
    tertiaryContainer = Sim1EmeraldContainer,
    onTertiaryContainer = Sim1Emerald,
    background = bg,
    onBackground = TextPrimaryDark,
    surface = surface,
    onSurface = TextPrimaryDark,
    surfaceVariant = surfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = bg,
    surfaceContainerLow = surface,
    surfaceContainer = surfaceElevated,
    surfaceContainerHigh = surfaceHighest,
    outline = outline,
    outlineVariant = outlineSubtle,
    error = MissedCoral,
    onError = Color.White,
    errorContainer = MissedCoralContainer,
    onErrorContainer = Color(0xFFFFD9DD)
)

private val SapphireColorScheme = buildDarkScheme(
    primary = ChampagneGold,
    primaryContainer = ChampagneGoldContainer,
    bg = ObsidianBg,
    surface = ObsidianSurface,
    surfaceElevated = ObsidianSurfaceElevated,
    surfaceHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineSubtle = ObsidianOutlineSubtle
)

private val EmeraldColorScheme = buildDarkScheme(
    primary = EmeraldPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    bg = EmeraldBg,
    surface = EmeraldSurface,
    surfaceElevated = EmeraldSurfaceElevated,
    surfaceHighest = EmeraldSurfaceHighest,
    outline = EmeraldOutline,
    outlineSubtle = EmeraldOutlineSubtle
)

private val AmethystColorScheme = buildDarkScheme(
    primary = AmethystPrimary,
    primaryContainer = AmethystPrimaryContainer,
    bg = AmethystBg,
    surface = AmethystSurface,
    surfaceElevated = AmethystSurfaceElevated,
    surfaceHighest = AmethystSurfaceHighest,
    outline = AmethystOutline,
    outlineSubtle = AmethystOutlineSubtle
)

private val OceanColorScheme = buildDarkScheme(
    primary = OceanPrimary,
    primaryContainer = OceanPrimaryContainer,
    bg = OceanBg,
    surface = OceanSurface,
    surfaceElevated = OceanSurfaceElevated,
    surfaceHighest = OceanSurfaceHighest,
    outline = OceanOutline,
    outlineSubtle = OceanOutlineSubtle
)

private val CrimsonColorScheme = buildDarkScheme(
    primary = CrimsonPrimary,
    primaryContainer = CrimsonPrimaryContainer,
    bg = CrimsonBg,
    surface = CrimsonSurface,
    surfaceElevated = CrimsonSurfaceElevated,
    surfaceHighest = CrimsonSurfaceHighest,
    outline = CrimsonOutline,
    outlineSubtle = CrimsonOutlineSubtle
)

private val SunsetColorScheme = buildDarkScheme(
    primary = SunsetPrimary,
    primaryContainer = SunsetPrimaryContainer,
    bg = SunsetBg,
    surface = SunsetSurface,
    surfaceElevated = SunsetSurfaceElevated,
    surfaceHighest = SunsetSurfaceHighest,
    outline = SunsetOutline,
    outlineSubtle = SunsetOutlineSubtle
)

private val MidnightRoseColorScheme = buildDarkScheme(
    primary = MidnightRosePrimary,
    primaryContainer = MidnightRosePrimaryContainer,
    bg = MidnightRoseBg,
    surface = MidnightRoseSurface,
    surfaceElevated = MidnightRoseSurfaceElevated,
    surfaceHighest = MidnightRoseSurfaceHighest,
    outline = MidnightRoseOutline,
    outlineSubtle = MidnightRoseOutlineSubtle
)

private val CyberIndigoColorScheme = buildDarkScheme(
    primary = CyberIndigoPrimary,
    primaryContainer = CyberIndigoPrimaryContainer,
    bg = CyberIndigoBg,
    surface = CyberIndigoSurface,
    surfaceElevated = CyberIndigoSurfaceElevated,
    surfaceHighest = CyberIndigoSurfaceHighest,
    outline = CyberIndigoOutline,
    outlineSubtle = CyberIndigoOutlineSubtle
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

private val MintLightColorScheme = lightColorScheme(
    primary = MintLightPrimary,
    onPrimary = Color.White,
    primaryContainer = MintLightContainer,
    onPrimaryContainer = Color(0xFF043827),
    secondary = Sim2AzureLight,
    onSecondary = Color.White,
    secondaryContainer = Sim2AzureContainerLight,
    onSecondaryContainer = Color(0xFF0A3263),
    tertiary = Sim1EmeraldLight,
    onTertiary = Color.White,
    tertiaryContainer = Sim1EmeraldContainerLight,
    onTertiaryContainer = Color(0xFF053825),
    background = MintBg,
    onBackground = TextPrimaryLight,
    surface = MintSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MintSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = MintSurface,
    surfaceContainerLow = MintBg,
    surfaceContainer = MintSurfaceElevated,
    surfaceContainerHigh = MintSurfaceHighest,
    outline = MintOutline,
    outlineVariant = Color(0xFFD5EDE3),
    error = MissedCoralLight,
    onError = Color.White,
    errorContainer = MissedCoralContainerLight,
    onErrorContainer = Color(0xFF540610)
)

@Composable
fun NovaCallTheme(
    darkTheme: Boolean = true,
    themePalette: AppThemePalette? = null,
    displayThemeMode: DisplayThemeMode = DisplayThemeMode.DARK,
    fontScale: AppFontScale = AppFontScale.MEDIUM,
    liquidGlassEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (displayThemeMode) {
        DisplayThemeMode.SYSTEM -> systemDark
        DisplayThemeMode.LIGHT -> false
        DisplayThemeMode.DARK -> true
    }
    val resolvedPalette = when {
        !effectiveDark -> if (themePalette?.isLightPalette == true) themePalette else AppThemePalette.PEARL_WHITE
        themePalette != null && !themePalette.isLightPalette -> themePalette
        else -> AppThemePalette.ROYAL_SAPPHIRE
    }
    val isDark = !resolvedPalette.isLightPalette

    val colorScheme = when (resolvedPalette) {
        AppThemePalette.ROYAL_SAPPHIRE -> SapphireColorScheme
        AppThemePalette.EMERALD_FOREST -> EmeraldColorScheme
        AppThemePalette.ROYAL_AMETHYST -> AmethystColorScheme
        AppThemePalette.OCEAN_TEAL -> OceanColorScheme
        AppThemePalette.CRIMSON_VELVET -> CrimsonColorScheme
        AppThemePalette.SUNSET_AMBER -> SunsetColorScheme
        AppThemePalette.MIDNIGHT_ROSE -> MidnightRoseColorScheme
        AppThemePalette.CYBER_INDIGO -> CyberIndigoColorScheme
        AppThemePalette.PEARL_WHITE -> LightColorScheme
        AppThemePalette.MINT_BREEZE -> MintLightColorScheme
    }
    val extendedColors = when (resolvedPalette) {
        AppThemePalette.ROYAL_SAPPHIRE -> SapphireExtendedColors
        AppThemePalette.EMERALD_FOREST -> EmeraldExtendedColors
        AppThemePalette.ROYAL_AMETHYST -> AmethystExtendedColors
        AppThemePalette.OCEAN_TEAL -> OceanExtendedColors
        AppThemePalette.CRIMSON_VELVET -> CrimsonExtendedColors
        AppThemePalette.SUNSET_AMBER -> SunsetExtendedColors
        AppThemePalette.MIDNIGHT_ROSE -> MidnightRoseExtendedColors
        AppThemePalette.CYBER_INDIGO -> CyberIndigoExtendedColors
        AppThemePalette.PEARL_WHITE -> LightExtendedColors
        AppThemePalette.MINT_BREEZE -> MintLightExtendedColors
    }.copy(liquidGlassEnabled = liquidGlassEnabled)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = (currentDensity.fontScale * fontScale.multiplier).coerceIn(0.8f, 1.55f)
    )

    CompositionLocalProvider(
        LocalNovaColors provides extendedColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            content = content
        )
    }
}
