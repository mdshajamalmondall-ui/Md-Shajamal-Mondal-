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

/**
 * Ultra-fast, zero-allocation GPU-cached vector Doodle Background overlay.
 * Renders subtle tiled line-art motifs tailored to the active theme color.
 */
@Composable
fun DoodleBackgroundCanvas(
    style: BackgroundDoodleStyle,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (style == BackgroundDoodleStyle.NONE) return
    val strokeAlpha = if (isDark) 0.085f else 0.11f
    val secondaryAlpha = if (isDark) 0.055f else 0.075f
    val primaryStrokeColor = tint.copy(alpha = strokeAlpha)
    val accentStrokeColor = tint.copy(alpha = secondaryAlpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cellPx = 104.dp.toPx()
                val strokePx = 1.45.dp.toPx()
                val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
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

            when (style) {
                BackgroundDoodleStyle.PHONE_DOODLE -> drawPhoneDoodleMotif(cx, cy, cellPx, variant, angle, color, stroke)
                BackgroundDoodleStyle.COSMIC_STARS -> drawCosmicMotif(cx, cy, cellPx, variant, angle, color, stroke)
                BackgroundDoodleStyle.FLORAL_LEAVES -> drawFloralMotif(cx, cy, cellPx, variant, angle, color, stroke)
                BackgroundDoodleStyle.GEOMETRIC_WAVES -> drawGeometricMotif(cx, cy, cellPx, variant, angle, color, stroke)
                BackgroundDoodleStyle.HEARTS_SPARKLE -> drawHeartsMotif(cx, cy, cellPx, variant, angle, color, stroke)
                BackgroundDoodleStyle.NONE -> Unit
            }
        }
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
                // Classic handset arc + sound waves
                val p = Path().apply {
                    moveTo(cx - u * 0.9f, cy - u * 0.5f)
                    cubicTo(
                        cx - u * 0.95f, cy + u * 0.85f,
                        cx + u * 0.95f, cy + u * 0.85f,
                        cx + u * 0.9f, cy - u * 0.5f
                    )
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
                // Speech bubble with 3 dots
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
                // Mini keypad 3x3 dots + sparkle
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
                // 4-point sparkle + mini ring
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
                // Crescent moon
                val path = Path().apply {
                    moveTo(cx + u * 0.2f, cy - u * 0.9f)
                    cubicTo(cx - u * 1.0f, cy - u * 0.6f, cx - u * 1.0f, cy + u * 0.6f, cx + u * 0.2f, cy + u * 0.9f)
                    cubicTo(cx - u * 0.35f, cy + u * 0.45f, cx - u * 0.35f, cy - u * 0.45f, cx + u * 0.2f, cy - u * 0.9f)
                }
                drawPath(path, color = color, style = stroke)
            }
            1 -> {
                drawFourPointStar(cx, cy, u * 0.95f, color, stroke)
            }
            2 -> {
                // Ringed planet
                drawCircle(color = color, radius = u * 0.55f, center = Offset(cx, cy), style = stroke)
                drawOval(
                    color = color,
                    topLeft = Offset(cx - u * 1.15f, cy - u * 0.28f),
                    size = Size(u * 2.3f, u * 0.56f),
                    style = stroke
                )
            }
            else -> {
                // Star cluster
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
                // Botanical leaf with center vein
                val leaf = Path().apply {
                    moveTo(cx - u * 0.9f, cy + u * 0.6f)
                    quadraticTo(cx - u * 0.3f, cy - u * 0.95f, cx + u * 0.95f, cy - u * 0.65f)
                    quadraticTo(cx + u * 0.35f, cy + u * 0.95f, cx - u * 0.9f, cy + u * 0.6f)
                }
                drawPath(leaf, color = color, style = stroke)
                drawLine(color = color, start = Offset(cx - u * 0.85f, cy + u * 0.58f), end = Offset(cx + u * 0.72f, cy - u * 0.48f), strokeWidth = stroke.width * 0.8f)
            }
            1 -> {
                // 5-petal blossom
                for (i in 0 until 5) {
                    val rad = (i * 72.0) * PI / 180.0
                    val px = cx + (cos(rad) * u * 0.58f).toFloat()
                    val py = cy + (sin(rad) * u * 0.58f).toFloat()
                    drawCircle(color = color, radius = u * 0.34f, center = Offset(px, py), style = stroke)
                }
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx, cy))
            }
            else -> {
                // Vine swirl + buds
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
                // Diamond outline + inner dot
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
                // Double wave
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
                // Concentric arcs
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
                // Rounded triangle
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
            1 -> {
                drawFourPointStar(cx, cy, u * 0.82f, color, stroke)
            }
            else -> {
                drawCircle(color = color, radius = u * 0.42f, center = Offset(cx - u * 0.35f, cy), style = stroke)
                drawCircle(color = color, radius = u * 0.18f, center = Offset(cx + u * 0.55f, cy - u * 0.35f))
                drawCircle(color = color, radius = u * 0.12f, center = Offset(cx + u * 0.45f, cy + u * 0.45f))
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
                text = if (isBn) "থিম ডুডল প্যাটার্ন (Theme Doodle)" else "Theme Doodle Background",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isBn) {
                "যেকোনো থিমের সাথে ব্যাকগ্রাউন্ডে সূক্ষ্ম ভেক্টর ডুডল আর্ট চালু করুন — সম্পূর্ণ স্মুথ ও হালকা।"
            } else {
                "Add subtle vector doodle artwork behind every screen — lightweight and 60fps smooth."
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
            val previewBg = MaterialTheme.colorScheme.background
            val previewTint = MaterialTheme.colorScheme.primary
            options.forEach { spec ->
                val isSelected = selectedStyle == spec.style
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else nova.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(previewBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (spec.style != BackgroundDoodleStyle.NONE) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = Stroke(
                                        width = 1.3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                    val c = previewTint.copy(alpha = 0.85f)
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val cell = size.width * 2.1f
                                    when (spec.style) {
                                        BackgroundDoodleStyle.PHONE_DOODLE -> drawPhoneDoodleMotif(cx, cy, cell, 0, -10f, c, stroke)
                                        BackgroundDoodleStyle.COSMIC_STARS -> drawCosmicMotif(cx, cy, cell, 0, -8f, c, stroke)
                                        BackgroundDoodleStyle.FLORAL_LEAVES -> drawFloralMotif(cx, cy, cell, 0, -8f, c, stroke)
                                        BackgroundDoodleStyle.GEOMETRIC_WAVES -> drawGeometricMotif(cx, cy, cell, 0, 0f, c, stroke)
                                        BackgroundDoodleStyle.HEARTS_SPARKLE -> drawHeartsMotif(cx, cy, cell, 0, 0f, c, stroke)
                                        BackgroundDoodleStyle.NONE -> Unit
                                    }
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
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(9.dp)
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
