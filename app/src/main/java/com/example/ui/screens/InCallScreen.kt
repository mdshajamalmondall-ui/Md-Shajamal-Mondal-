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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.StickyNote2
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.formatDisplayNumber
import com.example.data.telephony.ActiveCallPhase
import com.example.data.telephony.ActiveCallUiState
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.LocalNovaColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun InCallScreen(
    callState: ActiveCallUiState,
    language: AppLanguage,
    quickReplies: List<String>,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onRejectWithSms: (String) -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onPlayDtmf: (Char) -> Unit,
    onOpenDialerToAddCall: () -> Unit
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    var showKeypad by remember { mutableStateOf(false) }
    var showQuickReject by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableLongStateOf(0L) }

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

    val anim = rememberInfiniteTransition(label = "incall_anim")
    val ringWave1 by anim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_1"
    )
    val ringWave1Alpha by anim.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_1_alpha"
    )
    val ringWave2 by anim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(1500, delayMillis = 550, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_2"
    )
    val ringWave2Alpha by anim.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, delayMillis = 550, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_wave_2_alpha"
    )
    val statusDotAlpha by anim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "status_dot_alpha"
    )

    val isRingingOrDialing = callState.phase == ActiveCallPhase.RINGING ||
        callState.phase == ActiveCallPhase.CONNECTING ||
        callState.phase == ActiveCallPhase.OUTGOING_RINGING

    val statusText = when (callState.phase) {
        ActiveCallPhase.RINGING -> if (isBn) "ইনকামিং কল আসছে..." else "Incoming call..."
        ActiveCallPhase.CONNECTING -> if (isBn) "কল সংযোগ হচ্ছে..." else "Connecting..."
        ActiveCallPhase.OUTGOING_RINGING -> if (isBn) "রিং হচ্ছে..." else "Ringing..."
        ActiveCallPhase.ACTIVE -> String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
        ActiveCallPhase.HOLDING -> if (isBn) "কল হোল্ডে আছে" else "On Hold"
        ActiveCallPhase.DISCONNECTING -> if (isBn) "কল কাটা হচ্ছে..." else "Ending..."
        ActiveCallPhase.DISCONNECTED -> if (isBn) "কল শেষ হয়েছে" else "Call Ended"
    }

    val statusColor = when (callState.phase) {
        ActiveCallPhase.ACTIVE -> nova.sim1
        ActiveCallPhase.OUTGOING_RINGING -> nova.gold
        ActiveCallPhase.RINGING -> if (callState.isSpamCaller) nova.missed else nova.sim1
        else -> nova.gold
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            (if (callState.isSpamCaller) nova.missed else nova.brilliant).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.24f),
                        radius = size.width * 0.72f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            nova.gold.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.78f),
                        radius = size.width * 0.6f
                    )
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Line Badge + Caller Info + Cloud Caller ID Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = nova.brilliantContainer,
                    border = BorderStroke(1.dp, nova.brilliant.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "Shajamal • ${callState.lineLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = nova.brilliant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Box(contentAlignment = Alignment.Center) {
                    if (isRingingOrDialing) {
                        val rippleColor = if (callState.isSpamCaller) nova.missed
                        else if (callState.phase == ActiveCallPhase.RINGING) nova.sim1
                        else nova.gold
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .scale(ringWave1)
                                .alpha(ringWave1Alpha)
                                .clip(CircleShape)
                                .background(rippleColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .scale(ringWave2)
                                .alpha(ringWave2Alpha)
                                .clip(CircleShape)
                                .background(rippleColor)
                        )
                    }
                    ContactAvatar(
                        name = callState.displayName,
                        photoUri = callState.photoUri,
                        size = 104.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = callState.displayName.ifBlank { formatDisplayNumber(callState.number) },
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = listOfNotNull(
                        formatDisplayNumber(callState.number).takeIf { it.isNotBlank() },
                        callState.carrierLabel?.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Cloud Caller ID / Spam Verification Badge
                if (!callState.callerIdBadge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (callState.isSpamCaller) nova.missedContainer else nova.sim1Container,
                        border = BorderStroke(
                            1.dp,
                            (if (callState.isSpamCaller) nova.missed else nova.sim1).copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (callState.isSpamCaller) Icons.Filled.ReportProblem else Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = if (callState.isSpamCaller) nova.missed else nova.sim1,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = callState.callerIdBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (callState.isSpamCaller) nova.missed else nova.sim1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRingingOrDialing) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .alpha(statusDotAlpha)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        ),
                        color = statusColor,
                        modifier = Modifier.testTag("incall_status_text")
                    )
                }

                // Saved Caller Note Context Banner
                if (!callState.callerNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = nova.goldContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, nova.gold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.StickyNote2, contentDescription = null, tint = nova.gold, modifier = Modifier.size(16.dp))
                            Text(
                                text = callState.callerNote,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Middle: In-Call DTMF Keypad or Quick SMS Reject Sheet
            AnimatedVisibility(visible = showKeypad && callState.phase != ActiveCallPhase.RINGING) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (dtmfInput.isNotEmpty()) {
                        Text(
                            text = dtmfInput,
                            style = MaterialTheme.typography.titleLarge,
                            color = nova.gold
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            dtmfInput += ch
                                            onPlayDtmf(ch)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = nova.glassSurface,
                                    border = BorderStroke(1.dp, nova.glassBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ch.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showQuickReject && callState.phase == ActiveCallPhase.RINGING) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickReplies.take(3).forEach { msg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onRejectWithSms(msg) },
                            shape = RoundedCornerShape(14.dp),
                            color = nova.glassSurface,
                            border = BorderStroke(1.dp, nova.glassBorder)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            if (callState.phase == ActiveCallPhase.RINGING) {
                IncomingCallAnimatedControls(
                    isBn = isBn,
                    onToggleQuickReject = { showQuickReject = !showQuickReject },
                    onAnswer = onAnswer,
                    onDecline = onDecline
                )
            } else {
                // Active / Ringing-Out Controls
                val isConnected = callState.phase == ActiveCallPhase.ACTIVE || callState.phase == ActiveCallPhase.HOLDING
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (isBn) "মিউট" else "Mute",
                            active = callState.isMuted,
                            enabled = true,
                            onClick = onToggleMute
                        )
                        InCallControlButton(
                            icon = Icons.Filled.Dialpad,
                            label = if (isBn) "কি-প্যাড" else "Keypad",
                            active = showKeypad,
                            enabled = true,
                            onClick = { showKeypad = !showKeypad }
                        )
                        InCallControlButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = if (isBn) "স্পিকার" else "Speaker",
                            active = callState.isSpeakerOn,
                            enabled = true,
                            onClick = onToggleSpeaker
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callState.isOnHold) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            label = if (callState.isOnHold) (if (isBn) "রিজিউম" else "Resume") else (if (isBn) "হোল্ড" else "Hold"),
                            active = callState.isOnHold,
                            enabled = isConnected,
                            onClick = onToggleHold
                        )
                        InCallControlButton(
                            icon = Icons.Filled.AddIcCall,
                            label = if (isBn) "অ্যাড কল" else "Add Call",
                            active = false,
                            enabled = isConnected,
                            onClick = onOpenDialerToAddCall
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(nova.missed)
                            .clickable(onClick = onEndCall)
                            .testTag("incall_end_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingCallAnimatedControls(
    isBn: Boolean,
    onToggleQuickReject: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val nova = LocalNovaColors.current
    val transition = rememberInfiniteTransition(label = "incoming_actions")

    // Ringing handset vibration angle
    val handsetTilt by transition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        label = "handset_tilt"
    )
    // Smooth floating vertical bob for animated text + chevrons
    val floatOffset by transition.animateFloat(
        initialValue = 4f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_offset"
    )
    // Glowing text scale pulse
    val textScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "label_scale"
    )
    // Shimmer highlight sweep position
    val shimmerShift by transition.animateFloat(
        initialValue = -120f,
        targetValue = 320f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_shift"
    )
    // Expanding sonar wave behind Answer & Decline circles
    val sonarScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "sonar_scale"
    )
    val sonarAlpha by transition.animateFloat(
        initialValue = 0.55f,
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
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggleQuickReject),
            shape = RoundedCornerShape(50),
            color = nova.glassSurface,
            border = BorderStroke(1.dp, nova.glassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = nova.gold, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isBn) "মেসেজ দিয়ে কাটুন" else "Reject with SMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Animated DECLINE ("কল কেটে দিন")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(0, declineDragY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (declineDragY < -90f || declineDragY > 90f) {
                                    onDecline()
                                }
                                declineDragY = 0f
                            },
                            onDragCancel = { declineDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                declineDragY = (declineDragY + dragAmount).coerceIn(-120f, 120f)
                            }
                        )
                    }
            ) {
                // Animated floating arrow hint
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = nova.missed.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(22.dp)
                        .offset(y = (-floatOffset * 0.6f).dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(sonarScale)
                            .alpha(sonarAlpha)
                            .clip(CircleShape)
                            .background(nova.missed)
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(nova.missed, Color(0xFFFF5252))
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
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Animated Glowing Shimmer Text Pill for "কল কেটে দিন"
                Surface(
                    modifier = Modifier
                        .scale(textScale)
                        .offset(y = (floatOffset * 0.35f).dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDecline),
                    shape = RoundedCornerShape(50),
                    color = nova.missedContainer.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.4.dp,
                        Brush.linearGradient(
                            colors = listOf(nova.missed.copy(alpha = 0.4f), Color.White, nova.missed),
                            start = Offset(shimmerShift, 0f),
                            end = Offset(shimmerShift + 110f, 40f)
                        )
                    )
                ) {
                    Text(
                        text = if (isBn) "কল কেটে দিন" else "Decline Call",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Animated ANSWER ("কল রিসিভ করুন")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(0, answerDragY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (answerDragY < -90f) {
                                    onAnswer()
                                }
                                answerDragY = 0f
                            },
                            onDragCancel = { answerDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                answerDragY = (answerDragY + dragAmount).coerceIn(-140f, 30f)
                            }
                        )
                    }
            ) {
                // Animated floating upward arrow hint
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = nova.sim1,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(y = floatOffset.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(sonarScale)
                            .alpha(sonarAlpha)
                            .clip(CircleShape)
                            .background(nova.sim1)
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .offset(y = (floatOffset * 0.45f).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(nova.sim1, Color(0xFF00E676))
                                )
                            )
                            .clickable(onClick = onAnswer)
                            .testTag("incall_answer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Answer",
                            tint = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .rotate(handsetTilt)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Animated Glowing Shimmer Text Pill for "কল রিসিভ করুন"
                Surface(
                    modifier = Modifier
                        .scale(textScale)
                        .offset(y = (floatOffset * 0.45f).dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onAnswer),
                    shape = RoundedCornerShape(50),
                    color = nova.sim1Container.copy(alpha = 0.9f),
                    border = BorderStroke(
                        1.4.dp,
                        Brush.linearGradient(
                            colors = listOf(nova.sim1.copy(alpha = 0.4f), Color.White, nova.sim1),
                            start = Offset(shimmerShift, 0f),
                            end = Offset(shimmerShift + 110f, 40f)
                        )
                    )
                ) {
                    Text(
                        text = if (isBn) "কল রিসিভ করুন" else "Answer Call",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color(0xFF69F0AE),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val nova = LocalNovaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(84.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) nova.gold else nova.glassSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF1A1204) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) nova.gold else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
