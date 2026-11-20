package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.CallRoute
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.AmethystBg
import com.example.ui.theme.AmethystPrimary
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldLightPrimary
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OceanBg
import com.example.ui.theme.OceanPrimary
import com.example.ui.theme.PearlBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ContactAvatar(
    name: String?,
    photoUri: String? = null,
    size: Dp = 44.dp,
    accentOverride: Color? = null
) {
    val nova = LocalNovaColors.current
    val display = name?.trim().orEmpty()
    val initials = display.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "#" }

    val palette = listOf(
        nova.gold,
        nova.sim1,
        nova.sim2,
        nova.brilliant,
        Color(0xFFE07A5F)
    )
    val baseColor = accentOverride ?: palette[abs(display.hashCode()) % palette.size]

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        baseColor.copy(alpha = 0.28f),
                        baseColor.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, baseColor.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = display.ifEmpty { "Avatar" },
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = baseColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RouteBadge(
    route: CallRoute,
    sim1Label: String = "SIM 1",
    sim2Label: String = "SIM 2",
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val (bg, fg, text, icon) = when (route) {
        CallRoute.SIM_2 -> Quad(nova.sim2Container, nova.sim2, sim2Label.take(10), Icons.Filled.SimCard)
        else -> Quad(nova.sim1Container, nova.sim1, sim1Label.take(10), Icons.Filled.SimCard)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.75.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PermissionAccessCard(
    strings: NovaStrings,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_access_card"),
        shape = RoundedCornerShape(20.dp),
        color = nova.glassSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, nova.gold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(nova.goldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = nova.gold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.permissionBannerTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.permissionBannerDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.testTag("grant_permissions_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = strings.grantPermission,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private val TIME_ONLY_FMT = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
private val DATE_TIME_FMT = ThreadLocal.withInitial { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

fun formatRelativeTime(timestamp: Long, language: AppLanguage): String {
    if (timestamp <= 0L) return ""
    val diffMs = System.currentTimeMillis() - timestamp
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    return when {
        minutes < 1 -> if (language == AppLanguage.BENGALI) "এইমাত্র" else "Just now"
        minutes < 60 -> if (language == AppLanguage.BENGALI) "$minutes মি. আগে" else "${minutes}m ago"
        hours < 24 -> TIME_ONLY_FMT.get()?.format(Date(timestamp)).orEmpty()
        else -> DATE_TIME_FMT.get()?.format(Date(timestamp)).orEmpty()
    }
}

fun formatCallDuration(durationSeconds: Long, language: AppLanguage): String {
    if (durationSeconds <= 0L) return ""
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return if (mins > 0) {
        if (language == AppLanguage.BENGALI) "${mins}মি ${secs}সে" else "${mins}m ${secs}s"
    } else {
        if (language == AppLanguage.BENGALI) "${secs}সে" else "${secs}s"
    }
}

private data class ThemeSwatchSpec(
    val palette: AppThemePalette,
    val labelBn: String,
    val labelEn: String,
    val bgColor: Color,
    val accentColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePalettePicker(
    selectedPalette: AppThemePalette,
    language: AppLanguage,
    onSelectPalette: (AppThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    val options = listOf(
        ThemeSwatchSpec(AppThemePalette.ROYAL_SAPPHIRE, "রয়েল নীল", "Sapphire", ObsidianBg, ChampagneGold),
        ThemeSwatchSpec(AppThemePalette.EMERALD_FOREST, "সবুজ ফরেস্ট", "Emerald", EmeraldBg, EmeraldPrimary),
        ThemeSwatchSpec(AppThemePalette.ROYAL_AMETHYST, "রয়েল বেগুনি", "Amethyst", AmethystBg, AmethystPrimary),
        ThemeSwatchSpec(AppThemePalette.OCEAN_TEAL, "ওশান টিল", "Ocean", OceanBg, OceanPrimary),
        ThemeSwatchSpec(AppThemePalette.CRIMSON_VELVET, "মেরুন ভেলভেট", "Crimson", CrimsonBg, CrimsonPrimary),
        ThemeSwatchSpec(AppThemePalette.PEARL_WHITE, "পার্ল হোয়াইট", "Pearl", PearlBg, GoldLightPrimary)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = null,
                tint = nova.gold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isBn) "অ্যাপের ব্যাকগ্রাউন্ড কালার" else "App Background Color",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { spec ->
                val isSelected = selectedPalette == spec.palette
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectPalette(spec.palette) }
                        .testTag("theme_palette_${spec.palette.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) spec.accentColor else nova.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(spec.bgColor)
                                .border(2.dp, spec.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = spec.accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isBn) spec.labelBn else spec.labelEn,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
