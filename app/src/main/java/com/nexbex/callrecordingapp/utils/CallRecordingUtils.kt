package com.nexbex.callrecordingapp.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

object CallRecordingUtils {
    private var mediaRecorder: MediaRecorder? = null

    fun startRecording(context: Context): String? {
        try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val filePath = File(directory, "recording_${System.currentTimeMillis()}.3gp").absolutePath // Save as .3gp for 3GPP format

            val mediaRecorder = MediaRecorder()
            mediaRecorder.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION) // Recommended for Android 10+
                } else {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_CALL) // For older versions (if available)
                }
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePath)
                prepare()
                start()
            }

            Log.d("CallRecordingUtils", "Recording started: $filePath")
            return filePath
        } catch (e: Exception) {
            Log.e("CallRecordingUtils", "Failed to start recording: ${e.message}")
            return null
        }
    }


    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("CallRecordingUtils", "Recording stopped.")
        } catch (e: Exception) {
            Log.e("CallRecordingUtils", "Failed to stop recording: ${e.message}")
        }
    }
}
