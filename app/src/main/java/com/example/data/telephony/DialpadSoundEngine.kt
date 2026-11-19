package com.example.data.telephony

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import com.example.data.model.KeypadSoundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-clarity dialpad sound synthesizer using PCM AudioTrack so dialpad tones
 * are always clearly audible even on devices where STREAM_DTMF is muted.
 */
class DialpadSoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private var fallbackToneGen: ToneGenerator? = null

    private val sampleRate = 24000

    private val dtmfFrequencies: Map<String, Pair<Double, Double>> = mapOf(
        "1" to (697.0 to 1209.0),
        "2" to (697.0 to 1336.0),
        "3" to (697.0 to 1477.0),
        "4" to (770.0 to 1209.0),
        "5" to (770.0 to 1336.0),
        "6" to (770.0 to 1477.0),
        "7" to (852.0 to 1209.0),
        "8" to (852.0 to 1336.0),
        "9" to (852.0 to 1477.0),
        "*" to (941.0 to 1209.0),
        "0" to (941.0 to 1336.0),
        "+" to (941.0 to 1336.0),
        "#" to (941.0 to 1477.0)
    )

    private val studioChimeFreqs: Map<String, Double> = mapOf(
        "1" to 523.25, // C5
        "2" to 587.33, // D5
        "3" to 659.25, // E5
        "4" to 698.46, // F5
        "5" to 783.99, // G5
        "6" to 880.00, // A5
        "7" to 987.77, // B5
        "8" to 1046.50, // C6
        "9" to 1174.66, // D6
        "*" to 440.00, // A4
        "0" to 493.88, // B4
        "+" to 493.88,
        "#" to 1318.51 // E6
    )

    fun playKey(symbol: String, style: KeypadSoundStyle, volume: Float = 0.9f) {
        val clampedVol = volume.coerceIn(0.2f, 1.0f)
        activeJob?.cancel()
        activeJob = scope.launch {
            val played = runCatching {
                val pcm = when (style) {
                    KeypadSoundStyle.DTMF_CLASSIC -> synthesizeDtmf(symbol, clampedVol)
                    KeypadSoundStyle.STUDIO_CHIME -> synthesizeStudioChime(symbol, clampedVol)
                    KeypadSoundStyle.SOFT_CLICK -> synthesizeSoftClick(clampedVol)
                }
                if (pcm.isNotEmpty()) {
                    playPcmBuffer(pcm)
                    true
                } else false
            }.getOrDefault(false)

            if (!played) {
                playFallbackTone(symbol, (clampedVol * 100).toInt())
            }
        }
    }

    private fun synthesizeDtmf(symbol: String, volume: Float): ShortArray {
        val (f1, f2) = dtmfFrequencies[symbol] ?: return ShortArray(0)
        val durationMs = 115
        val count = (sampleRate * durationMs) / 1000
        val samples = ShortArray(count)
        val fadeSamples = (sampleRate * 10) / 1000
        val amplitude = 0.45 * volume * Short.MAX_VALUE

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = when {
                i < fadeSamples -> 0.5 * (1.0 - cos(PI * i / fadeSamples))
                i > count - fadeSamples -> 0.5 * (1.0 - cos(PI * (count - 1 - i) / fadeSamples))
                else -> 1.0
            }
            val wave = sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)
            samples[i] = (wave * amplitude * env).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun synthesizeStudioChime(symbol: String, volume: Float): ShortArray {
        val baseFreq = studioChimeFreqs[symbol] ?: 659.25
        val durationMs = 135
        val count = (sampleRate * durationMs) / 1000
        val samples = ShortArray(count)
        val attackSamples = (sampleRate * 6) / 1000
        val amplitude = 0.58 * volume * Short.MAX_VALUE

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decay = exp(-t * 22.0)
            val wave = sin(2.0 * PI * baseFreq * t) * 0.72 +
                sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.28
            samples[i] = (wave * amplitude * attack * decay).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun synthesizeSoftClick(volume: Float): ShortArray {
        val durationMs = 45
        val count = (sampleRate * durationMs) / 1000
        val samples = ShortArray(count)
        val amplitude = 0.7 * volume * Short.MAX_VALUE

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 85.0)
            val freq = 1100.0 - (t * 6000.0).coerceAtMost(500.0)
            val wave = sin(2.0 * PI * freq * t)
            samples[i] = (wave * amplitude * env).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun playPcmBuffer(samples: ShortArray) {
        val byteLen = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteLen)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val sleepMs = ((samples.size * 1000L) / sampleRate) + 25L
            Thread.sleep(sleepMs)
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    private fun playFallbackTone(symbol: String, volumePercent: Int) {
        val tone = when (symbol) {
            "0", "+" -> ToneGenerator.TONE_DTMF_0
            "1" -> ToneGenerator.TONE_DTMF_1
            "2" -> ToneGenerator.TONE_DTMF_2
            "3" -> ToneGenerator.TONE_DTMF_3
            "4" -> ToneGenerator.TONE_DTMF_4
            "5" -> ToneGenerator.TONE_DTMF_5
            "6" -> ToneGenerator.TONE_DTMF_6
            "7" -> ToneGenerator.TONE_DTMF_7
            "8" -> ToneGenerator.TONE_DTMF_8
            "9" -> ToneGenerator.TONE_DTMF_9
            "*" -> ToneGenerator.TONE_DTMF_S
            "#" -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        runCatching {
            if (fallbackToneGen == null) {
                fallbackToneGen = ToneGenerator(AudioManager.STREAM_MUSIC, volumePercent.coerceIn(25, 100))
            }
            fallbackToneGen?.startTone(tone, 110)
        }
    }

    fun release() {
        activeJob?.cancel()
        runCatching { fallbackToneGen?.release() }
        fallbackToneGen = null
    }
}
