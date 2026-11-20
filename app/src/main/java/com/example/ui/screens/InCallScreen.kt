package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.SlideToAnswerMode
import com.example.data.model.formatDisplayNumber
import com.example.data.telephony.ActiveCallPhase
import com.example.data.telephony.ActiveCallUiState
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.InCallGradBottom
import com.example.ui.theme.InCallGradMid
import com.example.ui.theme.InCallGradTop
import com.example.ui.theme.InCallSpamBottom
import com.example.ui.theme.InCallSpamMid
import com.example.ui.theme.InCallSpamTop
import com.example.ui.theme.LocalNovaColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private val TruecallerBlue = Color(0xFF0087FF)
private val LiquidGlassSkyTop = Color(0xFF47789F)
private val LiquidGlassCloudMid = Color(0xFF8EA7BA)
private val LiquidGlassHorizonGlow = Color(0xFFD6A86E)
private val LiquidGlassMeadow = Color(0xFF4F5D4A)
private val LiquidGlassEarthBottom = Color(0xFF1A221B)

@Composable
fun InCallScreen(
    callState: ActiveCallUiState,
    language: AppLanguage,
    quickReplies: List<String>,
    liquidGlassEnabled: Boolean = true,
    slideToAnswerMode: SlideToAnswerMode = SlideToAnswerMode.LOCKED_ONLY,
    isDeviceLocked: Boolean = false,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onRejectWithSms: (String) -> Unit,
    onSetReminder: (String) -> Unit = {},
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleRecord: () -> Unit,
    onPlayDtmf: (Char) -> Unit,
    onOpenDialerToAddCall: () -> Unit
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    var showKeypad by remember { mutableStateOf(false) }
    var showQuickReject by remember { mutableStateOf(false) }
    var showReminderOptions by remember { mutableStateOf(false) }
    var reminderSavedLabel by remember { mutableStateOf<String?>(null) }
    var dtmfInput by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var recElapsedSec by remember { mutableLongStateOf(0L) }

    LaunchedEffect(callState.phase, callState.connectedAtMillis) {
        if (callState.phase == ActiveCallPhase.ACTIVE && callState.connectedAtMillis > 0L) {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - callState.connectedAtMillis) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            elapsedSec = 0L
        }
    }

    LaunchedEffect(callState.isRecording, callState.recordingStartedAtMillis) {
        if (callState.isRecording && callState.recordingStartedAtMillis > 0L) {
            while (true) {
                recElapsedSec = ((System.currentTimeMillis() - callState.recordingStartedAtMillis) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            recElapsedSec = 0L
        }
    }

    val anim = rememberInfiniteTransition(label = "incall_anim")
    val ringWave1 by anim.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.62f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_1"
    )
    val ringWave1Alpha by anim.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_1_alpha"
    )
    val ringWave2 by anim.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.62f,
        animationSpec = infiniteRepeatable(tween(1700, delayMillis = 560, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_2"
    )
    val ringWave2Alpha by anim.animateFloat(
        initialValue = 0.36f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1700, delayMillis = 560, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_2_alpha"
    )
    val ringWave3 by anim.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.62f,
        animationSpec = infiniteRepeatable(tween(1700, delayMillis = 1120, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_3"
    )
    val ringWave3Alpha by anim.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1700, delayMillis = 1120, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_3_alpha"
    )
    val orbitAngle by anim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit_angle"
    )
    val avatarPulse by anim.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(880, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "avatar_pulse"
    )
    val orbDrift by anim.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb_drift"
    )
    val chevronPhase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "chevron_phase"
    )
    val statusDotAlpha by anim.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "status_dot_alpha"
    )

    val isIncoming = callState.phase == ActiveCallPhase.RINGING
    val isOutgoing = callState.phase == ActiveCallPhase.CONNECTING ||
        callState.phase == ActiveCallPhase.OUTGOING_RINGING
    val isRingingOrDialing = isIncoming || isOutgoing
    val useGlassLook = liquidGlassEnabled && !callState.isSpamCaller

    val statusText = when (callState.phase) {
        ActiveCallPhase.RINGING -> if (isBn) "ইনকামিং কল আসছে..." else "Incoming call"
        ActiveCallPhase.CONNECTING -> if (isBn) "কল যাচ্ছে..." else "Calling..."
        ActiveCallPhase.OUTGOING_RINGING -> if (isBn) "রিং হচ্ছে..." else "Ringing..."
        ActiveCallPhase.ACTIVE -> String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
        ActiveCallPhase.HOLDING -> if (isBn) "কল হোল্ডে আছে" else "On Hold"
        ActiveCallPhase.DISCONNECTING -> if (isBn) "কল কাটা হচ্ছে..." else "Ending..."
        ActiveCallPhase.DISCONNECTED -> if (isBn) "কল শেষ হয়েছে" else "Call Ended"
    }

    val statusColor = when {
        callState.isSpamCaller -> nova.missed
        useGlassLook -> Color.White
        callState.phase == ActiveCallPhase.ACTIVE -> nova.sim1
        isOutgoing -> nova.gold
        else -> nova.sim1
    }

    val primaryTextColor = Color.White
    val secondaryTextColor = Color.White.copy(alpha = 0.82f)

    val callBackgroundBrush = remember(callState.isSpamCaller, useGlassLook) {
        when {
            callState.isSpamCaller -> Brush.verticalGradient(
                colors = listOf(InCallSpamTop, InCallSpamMid, InCallSpamBottom)
            )
            useGlassLook -> Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to LiquidGlassSkyTop,
                    0.30f to LiquidGlassCloudMid,
                    0.52f to LiquidGlassHorizonGlow,
                    0.74f to LiquidGlassMeadow,
                    1.00f to LiquidGlassEarthBottom
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(InCallGradTop, InCallGradMid, InCallGradBottom)
            )
        }
    }

    val glassTileFill = if (useGlassLook) Color.White.copy(alpha = 0.16f) else nova.glassSurface.copy(alpha = 0.82f)
    val glassTileBorder = if (useGlassLook) Color.White.copy(alpha = 0.36f) else nova.glassBorder.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(callBackgroundBrush)
            .drawBehind {
                if (useGlassLook) {
                    // Soft sunlit Liquid Glass atmospheric bokeh blooms
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD699).copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width * (0.48f + orbDrift), size.height * 0.46f),
                            radius = size.width * 0.78f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFB3D8F5).copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * (0.22f - orbDrift), size.height * 0.16f),
                            radius = size.width * 0.65f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0E140F).copy(alpha = 0.42f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.92f),
                            radius = size.width * 0.85f
                        )
                    )
                } else {
                    val topOrbColor = when {
                        callState.isSpamCaller -> nova.missed
                        isOutgoing -> nova.gold
                        else -> nova.sim1
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(topOrbColor.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width * (0.5f + orbDrift), size.height * 0.22f),
                            radius = size.width * 0.80f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(nova.sim2.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * (0.84f - orbDrift), size.height * 0.58f),
                            radius = size.width * 0.68f
                        )
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Status timer/label, Caller Name, Number, Line pill & Liquid Glass Caller ID
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Status + animated equalizer bar (matches iOS Liquid Glass top timer/status)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (useGlassLook) Color.Black.copy(alpha = 0.18f) else statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (useGlassLook) Color.White.copy(alpha = 0.30f) else statusColor.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRingingOrDialing || callState.phase == ActiveCallPhase.ACTIVE) {
                            AnimatedCallWaveBars(
                                color = if (useGlassLook) Color.White else statusColor,
                                activeSpeed = if (isIncoming) 420 else if (isOutgoing) 520 else 720
                            )
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.testTag("incall_status_text")
                        )
                        if (isOutgoing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((-6).dp)
                            ) {
                                for (i in 0..2) {
                                    val dist = abs(chevronPhase - i)
                                    val alpha = (1f - (dist / 1.4f)).coerceIn(0.22f, 1f)
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = statusColor.copy(alpha = alpha),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = callState.displayName.ifBlank { formatDisplayNumber(callState.number) },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 31.sp
                    ),
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = listOfNotNull(
                        formatDisplayNumber(callState.number).takeIf { it.isNotBlank() },
                        callState.lineLabel.takeIf { it.isNotBlank() },
                        callState.carrierLabel?.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Truecaller-style Verified / Spam Badge
                if (!callState.callerIdBadge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val badgeAccent = if (callState.isSpamCaller) nova.missed else TruecallerBlue
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeAccent.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.78f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (callState.isSpamCaller) Icons.Filled.ReportProblem else Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = callState.callerIdBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Live Recording / Reminder Saved / Caller Note Pill
                if (callState.isRecording) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = nova.missed.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(statusDotAlpha)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = (if (isBn) "রেকর্ড চলছে " else "REC ") +
                                    String.format("%02d:%02d", recElapsedSec / 60, recElapsedSec % 60),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                } else if (!reminderSavedLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = reminderSavedLabel!!,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                } else if (!callState.recordingStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = callState.recordingStatusMessage,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!callState.callerNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.34f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.StickyNote2, contentDescription = null, tint = nova.gold, modifier = Modifier.size(15.dp))
                            Text(
                                text = callState.callerNote,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // CENTER: Large Luminous Liquid Glass Avatar Sphere with Animated Ripples & Orbital Halo
            AnimatedVisibility(visible = !showKeypad && !showQuickReject && !showReminderOptions) {
                Box(
                    modifier = Modifier.size(204.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRingingOrDialing) {
                        val rippleColor = when {
                            callState.isSpamCaller -> nova.missed
                            useGlassLook -> Color.White
                            isIncoming -> nova.sim1
                            else -> nova.gold
                        }
                        val secondaryArcColor = when {
                            callState.isSpamCaller -> Color(0xFFFF8A80)
                            useGlassLook -> Color(0xFFFFE0A3)
                            isIncoming -> Color(0xFF69F0AE)
                            else -> nova.sim2
                        }
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .scale(ringWave1)
                                .alpha(ringWave1Alpha)
                                .clip(CircleShape)
                                .background(rippleColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .scale(ringWave2)
                                .alpha(ringWave2Alpha)
                                .clip(CircleShape)
                                .background(rippleColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .scale(ringWave3)
                                .alpha(ringWave3Alpha)
                                .clip(CircleShape)
                                .background(secondaryArcColor)
                        )
                        Canvas(modifier = Modifier.size(156.dp)) {
                            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = rippleColor.copy(alpha = 0.88f),
                                startAngle = orbitAngle,
                                sweepAngle = 115f,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = secondaryArcColor.copy(alpha = 0.68f),
                                startAngle = orbitAngle + 180f,
                                sweepAngle = 80f,
                                useCenter = false,
                                style = stroke
                            )
                        }
                    } else if (callState.phase == ActiveCallPhase.ACTIVE) {
                        Box(
                            modifier = Modifier
                                .size(152.dp)
                                .scale(avatarPulse)
                                .alpha(if (useGlassLook) 0.22f else 0.24f)
                                .clip(CircleShape)
                                .background(if (useGlassLook) Color.White else nova.sim1)
                        )
                    }

                    // Frosted Liquid Glass Avatar Rim Sphere (matches Screenshot 1 & 4)
                    Surface(
                        modifier = Modifier
                            .size(138.dp)
                            .scale(if (isRingingOrDialing) avatarPulse else 1f),
                        shape = CircleShape,
                        color = if (useGlassLook) Color.White.copy(alpha = 0.20f) else nova.glassSurface,
                        border = BorderStroke(
                            width = 1.6.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.65f)
                                )
                            )
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (!callState.photoUri.isNullOrBlank()) {
                                ContactAvatar(
                                    name = callState.displayName,
                                    photoUri = callState.photoUri,
                                    size = 132.dp
                                )
                            } else {
                                val initial = callState.displayName
                                    .trim()
                                    .firstOrNull { it.isLetterOrDigit() }
                                    ?.uppercaseChar()
                                    ?.toString()
                                    ?: "#"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 62.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Middle Overlays: In-Call DTMF Keypad / Quick SMS / Callback Reminders
            AnimatedVisibility(visible = showKeypad && !isIncoming) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (dtmfInput.isNotEmpty()) {
                        Text(
                            text = dtmfInput,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    val rows = listOf(
                        listOf('1', '2', '3'),
                        listOf('4', '5', '6'),
                        listOf('7', '8', '9'),
                        listOf('*', '0', '#')
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { ch ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            dtmfInput += ch
                                            onPlayDtmf(ch)
                                        },
                                    shape = CircleShape,
                                    color = glassTileFill,
                                    border = BorderStroke(1.dp, glassTileBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ch.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showQuickReject && isIncoming) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickReplies.take(3).forEach { msg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onRejectWithSms(msg) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.34f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showReminderOptions && isIncoming) {
                val reminderPresets = if (isBn) {
                    listOf("১৫ মিনিট পর কল করুন", "১ ঘণ্টা পর কল করুন", "বাসায় গিয়ে কল করুন")
                } else {
                    listOf("Call back in 15 minutes", "Call back in 1 hour", "Call back when I get home")
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    reminderPresets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSetReminder(preset)
                                    reminderSavedLabel = if (isBn) "রিমাইন্ডার সেট: $preset" else "Reminder set: $preset"
                                    showReminderOptions = false
                                }
                                .testTag("incall_reminder_option"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.34f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM CONTROLS
            if (isIncoming) {
                val useSlideToAnswer = when (slideToAnswerMode) {
                    SlideToAnswerMode.ALWAYS -> true
                    SlideToAnswerMode.LOCKED_ONLY -> isDeviceLocked
                    SlideToAnswerMode.NEVER -> false
                }
                IncomingCallLiquidControls(
                    isBn = isBn,
                    useSlideToAnswer = useSlideToAnswer,
                    glassFill = glassTileFill,
                    glassBorder = glassTileBorder,
                    onToggleMessage = {
                        showQuickReject = !showQuickReject
                        if (showQuickReject) showReminderOptions = false
                    },
                    onToggleReminder = {
                        showReminderOptions = !showReminderOptions
                        if (showReminderOptions) showQuickReject = false
                    },
                    onAnswer = onAnswer,
                    onDecline = onDecline
                )
            } else {
                // Active / Outgoing Call: Frosted Record Pill + Liquid Glass 3x2 Action Grid (Screenshot 1)
                val isConnected = callState.phase == ActiveCallPhase.ACTIVE || callState.phase == ActiveCallPhase.HOLDING
                val canRecord = callState.phase != ActiveCallPhase.DISCONNECTED &&
                    callState.phase != ActiveCallPhase.DISCONNECTING

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    // 1-Tap Frosted Call Record Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = canRecord, onClick = onToggleRecord)
                            .testTag("incall_record_button"),
                        shape = RoundedCornerShape(50),
                        color = if (callState.isRecording) nova.missed.copy(alpha = 0.90f) else glassTileFill,
                        border = BorderStroke(
                            1.dp,
                            if (callState.isRecording) Color.White.copy(alpha = 0.7f) else glassTileBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (callState.isRecording) Icons.Filled.StopCircle else Icons.Filled.FiberManualRecord,
                                contentDescription = "Record",
                                tint = if (callState.isRecording) Color.White else nova.missed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (callState.isRecording) {
                                    (if (isBn) "রেকর্ড বন্ধ করুন • " else "Stop Rec • ") +
                                        String.format("%02d:%02d", recElapsedSec / 60, recElapsedSec % 60)
                                } else {
                                    if (isBn) "কল রেকর্ড" else "Record Call"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Row 1 (Screenshot 1): Speaker • Hold • Mute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LiquidGlassCallTile(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = if (isBn) "স্পিকার" else "Speaker",
                            active = callState.isSpeakerOn,
                            enabled = true,
                            glassFill = glassTileFill,
                            glassBorder = glassTileBorder,
                            testTagName = "incall_speaker_button",
                            onClick = onToggleSpeaker
                        )
                        LiquidGlassCallTile(
                            icon = if (callState.isOnHold) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            label = if (callState.isOnHold) {
                                if (isBn) "রিজিউম" else "Resume"
                            } else {
                                if (isBn) "হোল্ড" else "Hold"
                            },
                            active = callState.isOnHold,
                            enabled = isConnected,
                            glassFill = glassTileFill,
                            glassBorder = glassTileBorder,
                            testTagName = "incall_hold_button",
                            onClick = onToggleHold
                        )
                        LiquidGlassCallTile(
                            icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (isBn) "মিউট" else "Mute",
                            active = callState.isMuted,
                            enabled = true,
                            glassFill = glassTileFill,
                            glassBorder = glassTileBorder,
                            testTagName = "incall_mute_button",
                            onClick = onToggleMute
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Row 2 (Screenshot 1): Add • End Call (Red Center) • Keypad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        LiquidGlassCallTile(
                            icon = Icons.Filled.Add,
                            label = if (isBn) "অ্যাড কল" else "Add",
                            active = false,
                            enabled = isConnected,
                            glassFill = glassTileFill,
                            glassBorder = glassTileBorder,
                            testTagName = "incall_add_call_button",
                            onClick = onOpenDialerToAddCall
                        )

                        // Center Red End Call Circle (matches Screenshot 1)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(88.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onEndCall)
                                .testTag("incall_end_button")
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE52222)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBn) "কল কাটুন" else "End call",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1
                            )
                        }

                        LiquidGlassCallTile(
                            icon = Icons.Filled.Dialpad,
                            label = if (isBn) "কি-প্যাড" else "Keypad",
                            active = showKeypad,
                            enabled = true,
                            glassFill = glassTileFill,
                            glassBorder = glassTileBorder,
                            testTagName = "incall_keypad_button",
                            onClick = { showKeypad = !showKeypad }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingCallLiquidControls(
    isBn: Boolean,
    useSlideToAnswer: Boolean,
    glassFill: Color,
    glassBorder: Color,
    onToggleMessage: () -> Unit,
    onToggleReminder: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val nova = LocalNovaColors.current
    val transition = rememberInfiniteTransition(label = "incoming_actions")

    val handsetTilt by transition.animateFloat(
        initialValue = -13f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        label = "handset_tilt"
    )
    val floatOffset by transition.animateFloat(
        initialValue = 3f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_offset"
    )
    val sonarScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.44f,
        animationSpec = infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "sonar_scale"
    )
    val sonarAlpha by transition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "sonar_alpha"
    )

    var answerDragY by remember { mutableFloatStateOf(0f) }
    var declineDragY by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Screenshot 4: Message & Reminder frosted glass quick-action circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IncomingMiniGlassButton(
                icon = Icons.AutoMirrored.Filled.Message,
                label = if (isBn) "মেসেজ" else "Message",
                glassFill = glassFill,
                glassBorder = glassBorder,
                testTagName = "incall_message_button",
                onClick = onToggleMessage
            )
            IncomingMiniGlassButton(
                icon = Icons.Filled.Alarm,
                label = if (isBn) "রিমাইন্ডার" else "Reminder",
                glassFill = glassFill,
                glassBorder = glassBorder,
                testTagName = "incall_reminder_button",
                onClick = onToggleReminder
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (useSlideToAnswer) {
            // Horizontal Liquid Glass "Slide to Answer" track (when locked or Always enabled)
            SlideToAnswerBar(
                label = if (isBn) "রিসিভ করতে স্লাইড করুন →" else "Slide to answer →",
                glassFill = glassFill,
                glassBorder = glassBorder,
                onAnswered = onAnswer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDecline)
                    .testTag("incall_decline_button"),
                shape = RoundedCornerShape(50),
                color = Color(0xFFE52222).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (isBn) "কল কেটে দিন" else "Decline",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        } else {
            // Screenshot 4: Decline & Accept Circular Buttons with Animated Ripples
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // DECLINE
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset { IntOffset(0, declineDragY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (declineDragY < -90f || declineDragY > 90f) onDecline()
                                    declineDragY = 0f
                                },
                                onDragCancel = { declineDragY = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    declineDragY = (declineDragY + dragAmount).coerceIn(-120f, 120f)
                                }
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = (-floatOffset * 0.5f).dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .scale(sonarScale)
                                .alpha(sonarAlpha)
                                .clip(CircleShape)
                                .background(nova.missed)
                        )
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE52222), Color(0xFFFF4D4D))
                                    )
                                )
                                .clickable(onClick = onDecline)
                                .testTag("incall_decline_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "Decline",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBn) "কেটে দিন" else "Decline",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                // ACCEPT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset { IntOffset(0, answerDragY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (answerDragY < -90f) onAnswer()
                                    answerDragY = 0f
                                },
                                onDragCancel = { answerDragY = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    answerDragY = (answerDragY + dragAmount).coerceIn(-140f, 30f)
                                }
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF69F0AE).copy(alpha = 0.90f),
                        modifier = Modifier
                            .size(22.dp)
                            .offset(y = (floatOffset * 0.7f).dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .scale(sonarScale)
                                .alpha(sonarAlpha)
                                .clip(CircleShape)
                                .background(Color(0xFF1DB954))
                        )
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .offset(y = (floatOffset * 0.35f).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E9E49), Color(0xFF2ED566))
                                    )
                                )
                                .clickable(onClick = onAnswer)
                                .testTag("incall_answer_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(handsetTilt)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBn) "রিসিভ করুন" else "Accept",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideToAnswerBar(
    label: String,
    glassFill: Color,
    glassBorder: Color,
    onAnswered: () -> Unit
) {
    val density = LocalDensity.current
    var dragPx by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    val shimmer = rememberInfiniteTransition(label = "slide_shimmer")
    val textAlpha by shimmer.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "slide_text_alpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("incall_slide_to_answer_track")
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val thumbSize = 58.dp
        val thumbPx = with(density) { thumbSize.toPx() }
        val paddingPx = with(density) { 5.dp.toPx() }
        val maxDragPx = (trackWidthPx - thumbPx - paddingPx * 2f).coerceAtLeast(1f)

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = glassFill,
            border = BorderStroke(1.2.dp, glassBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = Color.White.copy(alpha = (1f - (dragPx / maxDragPx) * 0.8f).coerceIn(0.15f, 1f) * textAlpha),
                    modifier = Modifier.padding(start = 42.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 5.dp)
                        .offset { IntOffset(dragPx.roundToInt(), 0) }
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E9E49), Color(0xFF2ED566))
                            )
                        )
                        .pointerInput(maxDragPx) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (!triggered && dragPx >= maxDragPx * 0.70f) {
                                        triggered = true
                                        onAnswered()
                                    } else {
                                        dragPx = 0f
                                    }
                                },
                                onDragCancel = { dragPx = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (!triggered) {
                                        dragPx = (dragPx + dragAmount).coerceIn(0f, maxDragPx)
                                        if (dragPx >= maxDragPx * 0.92f) {
                                            triggered = true
                                            onAnswered()
                                        }
                                    }
                                }
                            )
                        }
                        .clickable {
                            if (!triggered) {
                                triggered = true
                                onAnswered()
                            }
                        }
                        .testTag("incall_answer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Answer",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingMiniGlassButton(
    icon: ImageVector,
    label: String,
    glassFill: Color,
    glassBorder: Color,
    testTagName: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = glassFill,
            border = BorderStroke(1.dp, glassBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1
        )
    }
}

@Composable
private fun LiquidGlassCallTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    glassFill: Color,
    glassBorder: Color,
    testTagName: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTagName)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(66.dp),
            shape = CircleShape,
            color = if (active) Color.White else glassFill,
            border = BorderStroke(
                width = 1.dp,
                color = if (active) Color.White else glassBorder
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) Color(0xFF14181C) else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = if (active) 1f else 0.90f),
            maxLines = 1
        )
    }
}

@Composable
private fun AnimatedCallWaveBars(
    color: Color,
    activeSpeed: Int
) {
    val transition = rememberInfiniteTransition(label = "call_wave_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(activeSpeed, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween((activeSpeed * 1.15f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween((activeSpeed * 0.85f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween((activeSpeed * 1.05f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar4"
    )
    val heights = listOf(bar1, bar2, bar3, bar4)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(14.dp)
    ) {
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14f * frac).dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
