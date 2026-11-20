package com.example.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallRecordingFile(
    val filePath: String,
    val fileName: String,
    val displayTitle: String,
    val timestampMillis: Long,
    val sizeBytes: Long
)

/**
 * Manages real in-call audio recording using Android MediaRecorder and playback via MediaPlayer.
 * Stores recordings in app-scoped external/internal storage (no broad storage permission needed).
 */
object CallRecorderManager {

    private const val TAG = "ShajamalRecorder"

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isCurrentlyRecording: Boolean = false

    private var mediaPlayer: MediaPlayer? = null
    private var playingPath: String? = null

    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isRecording(): Boolean = isCurrentlyRecording

    fun getRecordingsDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
            ?: File(context.filesDir, "call_recordings")
        if (!base.exists()) {
            base.mkdirs()
        }
        return base
    }

    @Synchronized
    fun startRecording(context: Context, callerLabel: String): Result<File> {
        if (!hasRecordPermission(context)) {
            return Result.failure(SecurityException("Microphone permission required"))
        }
        if (isCurrentlyRecording && currentFile != null) {
            return Result.success(currentFile!!)
        }

        val dir = getRecordingsDir(context)
        val safeCaller = callerLabel
            .replace(Regex("[^A-Za-z0-9+_-]"), "_")
            .trim('_')
            .ifBlank { "Call" }
            .take(24)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(dir, "Shajamal_${safeCaller}_${stamp}.m4a")

        // Try VOICE_COMMUNICATION first (optimized for calls), then fall back to MIC
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        var lastError: Throwable? = null
        for (source in sources) {
            val recorder = createRecorder(context)
            try {
                recorder.setAudioSource(source)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioSamplingRate(44100)
                recorder.setAudioEncodingBitRate(96000)
                recorder.setOutputFile(outFile.absolutePath)
                recorder.prepare()
                recorder.start()

                mediaRecorder = recorder
                currentFile = outFile
                isCurrentlyRecording = true
                return Result.success(outFile)
            } catch (t: Throwable) {
                Log.w(TAG, "AudioSource $source failed: ${t.message}")
                lastError = t
                runCatching { recorder.reset() }
                runCatching { recorder.release() }
            }
        }

        runCatching { outFile.delete() }
        return Result.failure(lastError ?: IllegalStateException("Unable to start call recording"))
    }

    @Synchronized
    fun stopRecording(): File? {
        if (!isCurrentlyRecording) return null
        val saved = currentFile
        try {
            mediaRecorder?.stop()
        } catch (t: Throwable) {
            Log.w(TAG, "Stop recording warning: ${t.message}")
            if (saved != null && saved.length() < 512L) {
                runCatching { saved.delete() }
            }
        } finally {
            runCatching { mediaRecorder?.reset() }
            runCatching { mediaRecorder?.release() }
            mediaRecorder = null
            currentFile = null
            isCurrentlyRecording = false
        }
        return saved?.takeIf { it.exists() && it.length() > 0L }
    }

    fun listRecordings(context: Context): List<CallRecordingFile> {
        val dir = getRecordingsDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".m4a") || it.name.endsWith(".3gp")) && it.length() > 0L }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val cleanTitle = file.nameWithoutExtension
                    .removePrefix("Shajamal_")
                    .substringBeforeLast("_")
                    .substringBeforeLast("_")
                    .replace("_", " ")
                    .ifBlank { file.nameWithoutExtension }
                CallRecordingFile(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    displayTitle = cleanTitle,
                    timestampMillis = file.lastModified(),
                    sizeBytes = file.length()
                )
            }
            ?: emptyList()
    }

    fun deleteRecording(filePath: String): Boolean {
        if (playingPath == filePath) {
            stopPlayback()
        }
        return runCatching { File(filePath).delete() }.getOrDefault(false)
    }

    fun togglePlayback(filePath: String, onFinished: () -> Unit): Boolean {
        if (playingPath == filePath && mediaPlayer?.isPlaying == true) {
            stopPlayback()
            return false
        }
        stopPlayback()
        return try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    stopPlayback()
                    onFinished()
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playingPath = filePath
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Playback failed: ${t.message}")
            stopPlayback()
            onFinished()
            false
        }
    }

    fun stopPlayback() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingPath = null
    }

    private fun createRecorder(context: Context): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
