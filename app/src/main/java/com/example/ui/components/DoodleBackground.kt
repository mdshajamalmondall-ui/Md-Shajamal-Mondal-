package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.BackgroundDoodleStyle
import com.example.ui.theme.LocalNovaColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class DoodlePaletteBackdrop(
    val topDark: Color,
    val bottomDark: Color,
    val glowDark: Color,
    val accentDark: Color,
    val topLight: Color,
    val bottomLight: Color,
    val glowLight: Color,
    val accentLight: Color
)

private fun backdropForStyle(style: BackgroundDoodleStyle, fallbackTint: Color): DoodlePaletteBackdrop =
    when (style) {
        BackgroundDoodleStyle.PHONE_DOODLE -> DoodlePaletteBackdrop(
            topDark = Color(0xFF0A2444),
            bottomDark = Color(0xFF071326),
            glowDark = Color(0xFF38BDF8),
            accentDark = Color(0xFF7DD3FC),
            topLight = Color(0xFFEAF3FF),
            bottomLight = Color(0xFFF6FAFF),
            glowLight = Color(0xFF60A5FA),
            accentLight = Color(0xFF2563EB)
        )
        BackgroundDoodleStyle.COSMIC_STARS -> DoodlePaletteBackdrop(
            topDark = Color(0xFF1A103D),
            bottomDark = Color(0xFF08091E),
            glowDark = Color(0xFFA78BFA),
            accentDark = Color(0xFFFDE68A),
            topLight = Color(0xFFF1ECFF),
            bottomLight = Color(0xFFFBF9FF),
            glowLight = Color(0xFFA78BFA),
            accentLight = Color(0xFF6D28D9)
        )
        BackgroundDoodleStyle.FLORAL_LEAVES -> DoodlePaletteBackdrop(
            topDark = Color(0xFF0A2E25),
            bottomDark = Color(0xFF061914),
            glowDark = Color(0xFF34D399),
            accentDark = Color(0xFF6EE7B7),
            topLight = Color(0xFFE6F9F0),
            bottomLight = Color(0xFFF6FCF9),
            glowLight = Color(0xFF34D399),
            accentLight = Color(0xFF059669)
        )
        BackgroundDoodleStyle.GEOMETRIC_WAVES -> DoodlePaletteBackdrop(
            topDark = Color(0xFF0D2742),
            bottomDark = Color(0xFF151838),
            glowDark = Color(0xFF38BDF8),
            accentDark = Color(0xFF93C5FD),
            topLight = Color(0xFFEBF4FF),
            bottomLight = Color(0xFFF2F1FF),
            glowLight = Color(0xFF818CF8),
            accentLight = Color(0xFF4F46E5)
        )
        BackgroundDoodleStyle.HEARTS_SPARKLE -> DoodlePaletteBackdrop(
            topDark = Color(0xFF361127),
            bottomDark = Color(0xFF180814),
            glowDark = Color(0xFFF472B6),
            accentDark = Color(0xFFFDA4AF),
            topLight = Color(0xFFFFEDF4),
            bottomLight = Color(0xFFFFF8FA),
            glowLight = Color(0xFFF472B6),
            accentLight = Color(0xFFDB2777)
        )
        BackgroundDoodleStyle.ISLAMIC_MANDALA -> DoodlePaletteBackdrop(
            topDark = Color(0xFF0A2C2E),
            bottomDark = Color(0xFF1A160B),
            glowDark = Color(0xFFEAB308),
            accentDark = Color(0xFFFACC15),
            topLight = Color(0xFFFCF6E6),
            bottomLight = Color(0xFFF3FAF7),
            glowLight = Color(0xFFD97706),
            accentLight = Color(0xFFB45309)
        )
        BackgroundDoodleStyle.CYBER_CIRCUIT -> DoodlePaletteBackdrop(
            topDark = Color(0xFF062336),
            bottomDark = Color(0xFF050B1C),
            glowDark = Color(0xFF06B6D4),
            accentDark = Color(0xFF22D3EE),
            topLight = Color(0xFFE6FAFF),
            bottomLight = Color(0xFFF0F5FF),
            glowLight = Color(0xFF06B6D4),
            accentLight = Color(0xFF0284C7)
        )
        BackgroundDoodleStyle.OCEAN_RIPPLES -> DoodlePaletteBackdrop(
            topDark = Color(0xFF07314A),
            bottomDark = Color(0xFF051829),
            glowDark = Color(0xFF2DD4BF),
            accentDark = Color(0xFF5EEAD4),
            topLight = Color(0xFFE3F7FC),
            bottomLight = Color(0xFFF2FBFF),
            glowLight = Color(0xFF14B8A6),
            accentLight = Color(0xFF0D9488)
        )
        BackgroundDoodleStyle.MUSIC_BEATS -> DoodlePaletteBackdrop(
            topDark = Color(0xFF291140),
            bottomDark = Color(0xFF0F0924),
            glowDark = Color(0xFFC084FC),
            accentDark = Color(0xFFE879F9),
            topLight = Color(0xFFF7EBFF),
            bottomLight = Color(0xFFFDF5FF),
            glowLight = Color(0xFFA855F7),
            accentLight = Color(0xFF9333EA)
        )
        BackgroundDoodleStyle.BUTTERFLY_BREEZE -> DoodlePaletteBackdrop(
            topDark = Color(0xFF122C38),
            bottomDark = Color(0xFF1A1D2B),
            glowDark = Color(0xFF6EE7B7),
            accentDark = Color(0xFF99F6E4),
            topLight = Color(0xFFECFCF6),
            bottomLight = Color(0xFFF5F3FF),
            glowLight = Color(0xFF34D399),
            accentLight = Color(0xFF10B981)
        )
        BackgroundDoodleStyle.AURORA_MOUNTAINS -> DoodlePaletteBackdrop(
            topDark = Color(0xFF08262E),
            bottomDark = Color(0xFF0D142E),
            glowDark = Color(0xFF34D399),
            accentDark = Color(0xFF67E8F9),
            topLight = Color(0xFFE6F8FA),
            bottomLight = Color(0xFFEDF2FF),
            glowLight = Color(0xFF2DD4BF),
            accentLight = Color(0xFF0F766E)
        )
        BackgroundDoodleStyle.NONE -> DoodlePaletteBackdrop(
            topDark = Color.Transparent,
            bottomDark = Color.Transparent,
            glowDark = fallbackTint,
            accentDark = fallbackTint,
            topLight = Color.Transparent,
            bottomLight = Color.Transparent,
            glowLight = fallbackTint,
            accentLight = fallbackTint
        )
    }

/**
 * Ultra-fast, zero-allocation GPU-cached vector Doodle + Atmospheric Background overlay.
 */
@Composable
fun DoodleBackgroundCanvas(
    style: BackgroundDoodleStyle,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (style == BackgroundDoodleStyle.NONE) return
    val spec = backdropForStyle(style, tint)
    val gradTop = (if (isDark) spec.topDark else spec.topLight).copy(alpha = if (isDark) 0.52f else 0.72f)
    val gradBottom = (if (isDark) spec.bottomDark else spec.bottomLight).copy(alpha = if (isDark) 0.35f else 0.60f)
    val glowColor = (if (isDark) spec.glowDark else spec.glowLight).copy(alpha = if (isDark) 0.17f else 0.16f)
    val motifTint = if (isDark) spec.accentDark else spec.accentLight
    val primaryStrokeColor = motifTint.copy(alpha = if (isDark) 0.115f else 0.135f)
    val accentStrokeColor = tint.copy(alpha = if (isDark) 0.075f else 0.09f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cellPx = 104.dp.toPx()
                val strokePx = 1.45.dp.toPx()
                val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val bgBrush = Brush.verticalGradient(
                    colors = listOf(gradTop, gradBottom, gradTop.copy(alpha = gradTop.alpha * 0.65f))
                )
                val topGlowBrush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.14f),
                    radius = size.maxDimension * 0.58f
                )
                val bottomGlowBrush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowColor.alpha * 0.85f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.84f),
                    radius = size.maxDimension * 0.52f
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawRect(brush = topGlowBrush)
                    drawRect(brush = bottomGlowBrush)
                    drawTiledDoodlePattern(
                        style = style,
                        cellPx = cellPx,
                        primaryColor = primaryStrokeColor,
                        secondaryColor = accentStrokeColor,
                        stroke = stroke
                    )
                }
            }
    )
}

private fun DrawScope.drawTiledDoodlePattern(
    style: BackgroundDoodleStyle,
    cellPx: Float,
    primaryColor: Color,
    secondaryColor: Color,
    stroke: Stroke
) {
    val cols = (size.width / cellPx).toInt() + 2
    val rows = (size.height / cellPx).toInt() + 2

    for (r in 0 until rows) {
        val xOffset = if (r % 2 == 1) cellPx * 0.5f else 0f
        for (c in -1 until cols) {
            val cx = c * cellPx + xOffset + cellPx * 0.5f
            val cy = r * cellPx + cellPx * 0.5f
            val variant = (r * 3 + c * 5).and(3)
            val color = if (variant % 2 == 0) primaryColor else secondaryColor
            val angle = when (variant) {
                0 -> -14f
                1 -> 12f
                2 -> -8f
                else -> 18f
            }
            drawSingleMotif(style, cx, cy, cellPx, variant, angle, color, stroke)
        }
    }
}

private fun DrawScope.drawSingleMotif(
    style: BackgroundDoodleStyle,
    cx: Float,
    cy: Float,
    cellPx: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    when (style) {
        BackgroundDoodleStyle.PHONE_DOODLE -> drawPhoneDoodleMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.COSMIC_STARS -> drawCosmicMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.FLORAL_LEAVES -> drawFloralMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.GEOMETRIC_WAVES -> drawGeometricMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.HEARTS_SPARKLE -> drawHeartsMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.ISLAMIC_MANDALA -> drawMandalaMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.CYBER_CIRCUIT -> drawCircuitMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.OCEAN_RIPPLES -> drawOceanMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.MUSIC_BEATS -> drawMusicMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.BUTTERFLY_BREEZE -> drawButterflyMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.AURORA_MOUNTAINS -> drawAuroraMountainMotif(cx, cy, cellPx, variant, angle, color, stroke)
        BackgroundDoodleStyle.NONE -> Unit
    }
}

private fun DrawScope.drawPhoneDoodleMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.16f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0 -> {
                val p = Path().apply {
                    moveTo(cx - u * 0.9f, cy - u * 0.5f)
                    cubicTo(cx - u * 0.95f, cy + u * 0.85f, cx + u * 0.95f, cy + u * 0.85f, cx + u * 0.9f, cy - u * 0.5f)
                }
                drawPath(p, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.24f, center = Offset(cx - u * 0.82f, cy - u * 0.52f), style = stroke)
                drawCircle(color = color, radius = u * 0.24f, center = Offset(cx + u * 0.82f, cy - u * 0.52f), style = stroke)
                drawArc(
                    color = color,
                    startAngle = 225f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - u * 0.55f, cy - u * 1.15f),
                    size = Size(u * 1.1f, u * 0.8f),
                    style = stroke
                )
            }
            1 -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - u, cy - u * 0.72f),
                    size = Size(u * 2f, u * 1.35f),
                    cornerRadius = CornerRadius(u * 0.5f, u * 0.5f),
                    style = stroke
                )
                drawCircle(color = color, radius = u * 0.12f, center = Offset(cx - u * 0.42f, cy - u * 0.05f))
                drawCircle(color = color, radius = u * 0.12f, center = Offset(cx, cy - u * 0.05f))
                drawCircle(color = color, radius = u * 0.12f, center = Offset(cx + u * 0.42f, cy - u * 0.05f))
            }
            2 -> {
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        drawCircle(
                            color = color,
                            radius = u * 0.14f,
                            center = Offset(cx + dx * u * 0.52f, cy + dy * u * 0.52f),
                            style = stroke
                        )
                    }
                }
            }
            else -> {
                drawFourPointStar(cx - u * 0.25f, cy - u * 0.15f, u * 0.78f, color, stroke)
                drawCircle(color = color, radius = u * 0.25f, center = Offset(cx + u * 0.85f, cy + u * 0.55f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawCosmicMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.17f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0 -> {
                val path = Path().apply {
                    moveTo(cx + u * 0.2f, cy - u * 0.9f)
                    cubicTo(cx - u * 1.0f, cy - u * 0.6f, cx - u * 1.0f, cy + u * 0.6f, cx + u * 0.2f, cy + u * 0.9f)
                    cubicTo(cx - u * 0.35f, cy + u * 0.45f, cx - u * 0.35f, cy - u * 0.45f, cx + u * 0.2f, cy - u * 0.9f)
                }
                drawPath(path, color = color, style = stroke)
            }
            1 -> drawFourPointStar(cx, cy, u * 0.95f, color, stroke)
            2 -> {
                drawCircle(color = color, radius = u * 0.55f, center = Offset(cx, cy), style = stroke)
                drawOval(
                    color = color,
                    topLeft = Offset(cx - u * 1.15f, cy - u * 0.28f),
                    size = Size(u * 2.3f, u * 0.56f),
                    style = stroke
                )
            }
            else -> {
                drawFourPointStar(cx - u * 0.45f, cy + u * 0.2f, u * 0.52f, color, stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx + u * 0.55f, cy - u * 0.42f))
                drawCircle(color = color, radius = u * 0.11f, center = Offset(cx + u * 0.3f, cy + u * 0.6f))
            }
        }
    }
}

private fun DrawScope.drawFloralMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.17f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                val leaf = Path().apply {
                    moveTo(cx - u * 0.9f, cy + u * 0.6f)
                    quadraticTo(cx - u * 0.3f, cy - u * 0.95f, cx + u * 0.95f, cy - u * 0.65f)
                    quadraticTo(cx + u * 0.35f, cy + u * 0.95f, cx - u * 0.9f, cy + u * 0.6f)
                }
                drawPath(leaf, color = color, style = stroke)
                drawLine(color = color, start = Offset(cx - u * 0.85f, cy + u * 0.58f), end = Offset(cx + u * 0.72f, cy - u * 0.48f), strokeWidth = stroke.width * 0.8f)
            }
            1 -> {
                for (i in 0 until 5) {
                    val rad = (i * 72.0) * PI / 180.0
                    val px = cx + (cos(rad) * u * 0.58f).toFloat()
                    val py = cy + (sin(rad) * u * 0.58f).toFloat()
                    drawCircle(color = color, radius = u * 0.34f, center = Offset(px, py), style = stroke)
                }
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx, cy))
            }
            else -> {
                val vine = Path().apply {
                    moveTo(cx - u, cy + u * 0.4f)
                    cubicTo(cx - u * 0.3f, cy - u * 0.9f, cx + u * 0.3f, cy + u * 0.9f, cx + u, cy - u * 0.4f)
                }
                drawPath(vine, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx, cy - u * 0.55f), style = stroke)
                drawCircle(color = color, radius = u * 0.14f, center = Offset(cx + u * 0.55f, cy + u * 0.42f))
            }
        }
    }
}

private fun DrawScope.drawGeometricMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.17f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0 -> {
                val diamond = Path().apply {
                    moveTo(cx, cy - u * 0.95f)
                    lineTo(cx + u * 0.95f, cy)
                    lineTo(cx, cy + u * 0.95f)
                    lineTo(cx - u * 0.95f, cy)
                    close()
                }
                drawPath(diamond, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx, cy))
            }
            1 -> {
                val wave = Path().apply {
                    moveTo(cx - u * 1.1f, cy - u * 0.25f)
                    quadraticTo(cx - u * 0.55f, cy - u * 0.9f, cx, cy - u * 0.25f)
                    quadraticTo(cx + u * 0.55f, cy + u * 0.4f, cx + u * 1.1f, cy - u * 0.25f)
                }
                drawPath(wave, color = color, style = stroke)
                val wave2 = Path().apply {
                    moveTo(cx - u * 1.1f, cy + u * 0.38f)
                    quadraticTo(cx - u * 0.55f, cy - u * 0.25f, cx, cy + u * 0.38f)
                    quadraticTo(cx + u * 0.55f, cy + u * 1.0f, cx + u * 1.1f, cy + u * 0.38f)
                }
                drawPath(wave2, color = color, style = stroke)
            }
            2 -> {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - u * 0.9f, cy - u * 0.45f),
                    size = Size(u * 1.8f, u * 1.8f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - u * 0.45f, cy),
                    size = Size(u * 0.9f, u * 0.9f),
                    style = stroke
                )
            }
            else -> {
                val tri = Path().apply {
                    moveTo(cx, cy - u * 0.85f)
                    lineTo(cx + u * 0.85f, cy + u * 0.65f)
                    lineTo(cx - u * 0.85f, cy + u * 0.65f)
                    close()
                }
                drawPath(tri, color = color, style = stroke)
            }
        }
    }
}

private fun DrawScope.drawHeartsMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.17f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                val heart = Path().apply {
                    moveTo(cx, cy + u * 0.85f)
                    cubicTo(cx - u * 1.25f, cy + u * 0.1f, cx - u * 0.95f, cy - u * 1.05f, cx, cy - u * 0.3f)
                    cubicTo(cx + u * 0.95f, cy - u * 1.05f, cx + u * 1.25f, cy + u * 0.1f, cx, cy + u * 0.85f)
                }
                drawPath(heart, color = color, style = stroke)
            }
            1 -> drawFourPointStar(cx, cy, u * 0.82f, color, stroke)
            else -> {
                drawCircle(color = color, radius = u * 0.42f, center = Offset(cx - u * 0.35f, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx + u * 0.55f, cy - u * 0.35f))
                drawCircle(color = color, radius = u * 0.12f, center = Offset(cx + u * 0.45f, cy + u * 0.45f))
            }
        }
    }
}

private fun DrawScope.drawMandalaMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.18f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                // 8-point geometric Rub-el-Hizb mandala star
                val s1 = u * 0.82f
                val d1 = Path().apply {
                    moveTo(cx - s1, cy - s1)
                    lineTo(cx + s1, cy - s1)
                    lineTo(cx + s1, cy + s1)
                    lineTo(cx - s1, cy + s1)
                    close()
                }
                drawPath(d1, color = color, style = stroke)
                rotate(45f, pivot = Offset(cx, cy)) {
                    drawPath(d1, color = color, style = stroke)
                }
                drawCircle(color = color, radius = u * 0.32f, center = Offset(cx, cy), style = stroke)
            }
            1 -> {
                // Scalloped lotus mandala
                for (i in 0 until 8) {
                    val rad = (i * 45.0) * PI / 180.0
                    val px = cx + (cos(rad) * u * 0.56f).toFloat()
                    val py = cy + (sin(rad) * u * 0.56f).toFloat()
                    drawCircle(color = color, radius = u * 0.28f, center = Offset(px, py), style = stroke)
                }
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx, cy))
            }
            else -> {
                // Crescent + lattice arch
                val arch = Path().apply {
                    moveTo(cx - u * 0.75f, cy + u * 0.75f)
                    lineTo(cx - u * 0.75f, cy - u * 0.1f)
                    quadraticTo(cx, cy - u * 1.15f, cx + u * 0.75f, cy - u * 0.1f)
                    lineTo(cx + u * 0.75f, cy + u * 0.75f)
                    close()
                }
                drawPath(arch, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx, cy + u * 0.15f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawCircuitMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.18f
    rotate(if (variant % 2 == 0) 0f else angle * 0.5f, pivot = Offset(cx, cy)) {
        when (variant) {
            0 -> {
                // Microchip with pins
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - u * 0.55f, cy - u * 0.55f),
                    size = Size(u * 1.1f, u * 1.1f),
                    cornerRadius = CornerRadius(u * 0.18f, u * 0.18f),
                    style = stroke
                )
                for (i in -1..1) {
                    val off = i * u * 0.34f
                    drawLine(color, Offset(cx - u * 0.92f, cy + off), Offset(cx - u * 0.55f, cy + off), stroke.width)
                    drawLine(color, Offset(cx + u * 0.55f, cy + off), Offset(cx + u * 0.92f, cy + off), stroke.width)
                }
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx, cy), style = stroke)
            }
            1 -> {
                // L-shaped PCB trace with terminal nodes
                val trace = Path().apply {
                    moveTo(cx - u * 0.9f, cy - u * 0.4f)
                    lineTo(cx - u * 0.1f, cy - u * 0.4f)
                    lineTo(cx + u * 0.3f, cy + u * 0.4f)
                    lineTo(cx + u * 0.9f, cy + u * 0.4f)
                }
                drawPath(trace, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx - u * 0.9f, cy - u * 0.4f), style = stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx + u * 0.9f, cy + u * 0.4f))
            }
            2 -> {
                // Hexagon tech node
                val hex = Path().apply {
                    for (i in 0 until 6) {
                        val rad = (i * 60.0) * PI / 180.0
                        val px = cx + (cos(rad) * u * 0.78f).toFloat()
                        val py = cy + (sin(rad) * u * 0.78f).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(hex, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.22f, center = Offset(cx, cy), style = stroke)
            }
            else -> {
                // Forked signal lines
                val p = Path().apply {
                    moveTo(cx - u * 0.9f, cy)
                    lineTo(cx, cy)
                    lineTo(cx + u * 0.7f, cy - u * 0.55f)
                    moveTo(cx, cy)
                    lineTo(cx + u * 0.7f, cy + u * 0.55f)
                }
                drawPath(p, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.15f, center = Offset(cx + u * 0.75f, cy - u * 0.55f))
                drawCircle(color = color, radius = u * 0.15f, center = Offset(cx + u * 0.75f, cy + u * 0.55f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawOceanMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.18f
    rotate(angle * 0.6f, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                // Flowing triple ocean wave
                for (row in -1..1) {
                    val yOff = row * u * 0.42f
                    val wave = Path().apply {
                        moveTo(cx - u, cy + yOff)
                        quadraticTo(cx - u * 0.5f, cy + yOff - u * 0.35f, cx, cy + yOff)
                        quadraticTo(cx + u * 0.5f, cy + yOff + u * 0.35f, cx + u, cy + yOff)
                    }
                    drawPath(wave, color = color, style = stroke)
                }
            }
            1 -> {
                // Water ripple rings
                drawCircle(color = color, radius = u * 0.85f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.45f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.14f, center = Offset(cx, cy))
            }
            else -> {
                // Floating bubbles + shell arc
                drawCircle(color = color, radius = u * 0.42f, center = Offset(cx - u * 0.3f, cy + u * 0.25f), style = stroke)
                drawCircle(color = color, radius = u * 0.26f, center = Offset(cx + u * 0.48f, cy - u * 0.15f), style = stroke)
                drawCircle(color = color, radius = u * 0.14f, center = Offset(cx + u * 0.15f, cy - u * 0.68f))
            }
        }
    }
}

private fun DrawScope.drawMusicMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.17f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0 -> {
                // Beamed eighth notes
                drawLine(color, Offset(cx - u * 0.45f, cy + u * 0.45f), Offset(cx - u * 0.45f, cy - u * 0.7f), stroke.width)
                drawLine(color, Offset(cx + u * 0.55f, cy + u * 0.28f), Offset(cx + u * 0.55f, cy - u * 0.85f), stroke.width)
                drawLine(color, Offset(cx - u * 0.45f, cy - u * 0.7f), Offset(cx + u * 0.55f, cy - u * 0.85f), stroke.width * 1.4f)
                drawCircle(color = color, radius = u * 0.25f, center = Offset(cx - u * 0.66f, cy + u * 0.45f), style = stroke)
                drawCircle(color = color, radius = u * 0.25f, center = Offset(cx + u * 0.34f, cy + u * 0.28f), style = stroke)
            }
            1 -> {
                // Equalizer sound bars
                val heights = listOf(0.45f, 0.95f, 0.65f, 0.85f, 0.4f)
                heights.forEachIndexed { idx, h ->
                    val x = cx + (idx - 2) * u * 0.4f
                    drawLine(color, Offset(x, cy - u * h * 0.65f), Offset(x, cy + u * h * 0.65f), stroke.width * 1.15f)
                }
            }
            2 -> {
                // Vinyl disc
                drawCircle(color = color, radius = u * 0.82f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.46f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx, cy))
            }
            else -> {
                // Single note + pulse arc
                drawLine(color, Offset(cx + u * 0.12f, cy + u * 0.45f), Offset(cx + u * 0.12f, cy - u * 0.78f), stroke.width)
                val flag = Path().apply {
                    moveTo(cx + u * 0.12f, cy - u * 0.78f)
                    quadraticTo(cx + u * 0.85f, cy - u * 0.45f, cx + u * 0.48f, cy - u * 0.05f)
                }
                drawPath(flag, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.26f, center = Offset(cx - u * 0.14f, cy + u * 0.48f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawButterflyMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.18f
    rotate(angle, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                // Butterfly wings
                val wings = Path().apply {
                    moveTo(cx, cy - u * 0.2f)
                    cubicTo(cx - u * 1.05f, cy - u * 1.15f, cx - u * 1.15f, cy + u * 0.15f, cx, cy + u * 0.15f)
                    cubicTo(cx - u * 0.9f, cy + u * 0.35f, cx - u * 0.55f, cy + u * 0.95f, cx, cy + u * 0.45f)
                    moveTo(cx, cy - u * 0.2f)
                    cubicTo(cx + u * 1.05f, cy - u * 1.15f, cx + u * 1.15f, cy + u * 0.15f, cx, cy + u * 0.15f)
                    cubicTo(cx + u * 0.9f, cy + u * 0.35f, cx + u * 0.55f, cy + u * 0.95f, cx, cy + u * 0.45f)
                }
                drawPath(wings, color = color, style = stroke)
                drawLine(color, Offset(cx, cy - u * 0.42f), Offset(cx, cy + u * 0.56f), stroke.width)
            }
            1 -> {
                // Dandelion breeze seed starburst
                for (i in 0 until 6) {
                    val rad = (i * 60.0) * PI / 180.0
                    val px = cx + (cos(rad) * u * 0.72f).toFloat()
                    val py = cy + (sin(rad) * u * 0.72f).toFloat()
                    drawLine(color, Offset(cx, cy), Offset(px, py), stroke.width * 0.85f)
                    drawCircle(color = color, radius = u * 0.11f, center = Offset(px, py))
                }
            }
            else -> {
                // Drifting swirl + petal
                val swirl = Path().apply {
                    moveTo(cx - u * 0.95f, cy + u * 0.2f)
                    cubicTo(cx - u * 0.35f, cy - u * 0.75f, cx + u * 0.45f, cy + u * 0.65f, cx + u * 0.95f, cy - u * 0.25f)
                }
                drawPath(swirl, color = color, style = stroke)
                drawCircle(color = color, radius = u * 0.16f, center = Offset(cx + u * 0.5f, cy - u * 0.58f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawAuroraMountainMotif(
    cx: Float,
    cy: Float,
    cell: Float,
    variant: Int,
    angle: Float,
    color: Color,
    stroke: Stroke
) {
    val u = cell * 0.18f
    rotate(if (variant == 1) angle else 0f, pivot = Offset(cx, cy)) {
        when (variant) {
            0, 2 -> {
                // Twin mountain peaks + aurora ribbon above
                val peaks = Path().apply {
                    moveTo(cx - u * 1.05f, cy + u * 0.65f)
                    lineTo(cx - u * 0.25f, cy - u * 0.48f)
                    lineTo(cx + u * 0.25f, cy + u * 0.2f)
                    lineTo(cx + u * 0.62f, cy - u * 0.22f)
                    lineTo(cx + u * 1.05f, cy + u * 0.65f)
                }
                drawPath(peaks, color = color, style = stroke)
                val aurora = Path().apply {
                    moveTo(cx - u * 0.85f, cy - u * 0.65f)
                    quadraticTo(cx, cy - u * 1.1f, cx + u * 0.85f, cy - u * 0.58f)
                }
                drawPath(aurora, color = color, style = stroke)
            }
            1 -> drawFourPointStar(cx, cy - u * 0.15f, u * 0.65f, color, stroke)
            else -> {
                // Pine tree + moon
                val pine = Path().apply {
                    moveTo(cx, cy - u * 0.75f)
                    lineTo(cx - u * 0.58f, cy + u * 0.35f)
                    lineTo(cx + u * 0.58f, cy + u * 0.35f)
                    close()
                }
                drawPath(pine, color = color, style = stroke)
                drawLine(color, Offset(cx, cy + u * 0.35f), Offset(cx, cy + u * 0.72f), stroke.width)
                drawCircle(color = color, radius = u * 0.2f, center = Offset(cx + u * 0.72f, cy - u * 0.55f), style = stroke)
            }
        }
    }
}

private fun DrawScope.drawFourPointStar(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    stroke: Stroke
) {
    val star = Path().apply {
        moveTo(cx, cy - radius)
        quadraticTo(cx, cy, cx + radius, cy)
        quadraticTo(cx, cy, cx, cy + radius)
        quadraticTo(cx, cy, cx - radius, cy)
        quadraticTo(cx, cy, cx, cy - radius)
        close()
    }
    drawPath(star, color = color, style = stroke)
}

private data class DoodleOptionSpec(
    val style: BackgroundDoodleStyle,
    val labelBn: String,
    val labelEn: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoodleStylePicker(
    selectedStyle: BackgroundDoodleStyle,
    language: AppLanguage,
    onSelectStyle: (BackgroundDoodleStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    val options = listOf(
        DoodleOptionSpec(BackgroundDoodleStyle.PHONE_DOODLE, "ফোন ডুডল", "Phone Doodle"),
        DoodleOptionSpec(BackgroundDoodleStyle.COSMIC_STARS, "স্টার ও মুন", "Cosmic Stars"),
        DoodleOptionSpec(BackgroundDoodleStyle.FLORAL_LEAVES, "ফ্লোরাল পাতা", "Floral Leaves"),
        DoodleOptionSpec(BackgroundDoodleStyle.GEOMETRIC_WAVES, "জিওমেট্রিক", "Geometric"),
        DoodleOptionSpec(BackgroundDoodleStyle.HEARTS_SPARKLE, "হার্ট ও স্পার্কল", "Hearts"),
        DoodleOptionSpec(BackgroundDoodleStyle.ISLAMIC_MANDALA, "রয়্যাল ম্যান্ডালা", "Royal Mandala"),
        DoodleOptionSpec(BackgroundDoodleStyle.CYBER_CIRCUIT, "সাইবার সার্কিট", "Cyber Circuit"),
        DoodleOptionSpec(BackgroundDoodleStyle.OCEAN_RIPPLES, "সাগরের ঢেউ", "Ocean Ripples"),
        DoodleOptionSpec(BackgroundDoodleStyle.MUSIC_BEATS, "মিউজিক ওয়েভ", "Music Beats"),
        DoodleOptionSpec(BackgroundDoodleStyle.BUTTERFLY_BREEZE, "প্রজাপতি প্রকৃতি", "Butterfly"),
        DoodleOptionSpec(BackgroundDoodleStyle.AURORA_MOUNTAINS, "অরোরা ও পাহাড়", "Aurora Peaks"),
        DoodleOptionSpec(BackgroundDoodleStyle.NONE, "ডুডল বন্ধ", "No Doodle")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = nova.gold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isBn) "ডুডল ও ব্যাকগ্রাউন্ড ডিজাইন (${options.size - 1}টি)" else "Doodle & Background Designs",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isBn) {
                "যেকোনো থিমের সাথে রঙিন ব্যাকগ্রাউন্ড গ্রেডিয়েন্ট ও ডুডল ডিজাইন বেছে নিন।"
            } else {
                "Pair atmospheric background gradients and vector doodle art with any theme."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isDark = nova.isDark
            val fallbackBg = MaterialTheme.colorScheme.background
            val fallbackTint = MaterialTheme.colorScheme.primary
            options.forEach { spec ->
                val isSelected = selectedStyle == spec.style
                val backdrop = backdropForStyle(spec.style, fallbackTint)
                val previewTop = if (spec.style == BackgroundDoodleStyle.NONE) fallbackBg else if (isDark) backdrop.topDark else backdrop.topLight
                val previewBottom = if (spec.style == BackgroundDoodleStyle.NONE) fallbackBg else if (isDark) backdrop.bottomDark else backdrop.bottomLight
                val previewAccent = if (spec.style == BackgroundDoodleStyle.NONE) fallbackTint else if (isDark) backdrop.accentDark else backdrop.accentLight

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectStyle(spec.style) }
                        .testTag("doodle_style_${spec.style.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) previewAccent else nova.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(colors = listOf(previewTop, previewBottom))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (spec.style != BackgroundDoodleStyle.NONE) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = Stroke(
                                        width = 1.35.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    drawSingleMotif(
                                        style = spec.style,
                                        cx = cx,
                                        cy = cy,
                                        cellPx = size.width * 2.0f,
                                        variant = 0,
                                        angle = -8f,
                                        color = previewAccent.copy(alpha = 0.92f),
                                        stroke = stroke
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
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
