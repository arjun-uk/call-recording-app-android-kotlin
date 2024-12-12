package com.nexbex.callrecordingapp.services.receivers


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nexbex.callrecordingapp.services.api.GeneralService
import com.nexbex.callrecordingapp.ui.activity.ActivityHome
import com.nexbex.callrecordingapp.utils.CallRecordingUtils
import com.nexbex.callrecordingapp.utils.GeneralCallback
import com.nexbex.callrecordingapp.utils.Services
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    private var isRecording = false
    private var recordingFilePath: String? = null
    private val CHANNEL_ID = "call_recording_channel"
    private val NOTIFICATION_ID = 1
    private lateinit var notificationManager: NotificationManager
    private val LOG_TAG = "CallReceiver"
    private lateinit var context: Context
    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for call recording events"
            }
            notificationManager.createNotificationChannel(channel)
        }
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Log.w(LOG_TAG, "Received an unexpected action: ${intent.action}")
            return
        }
        when (val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d(LOG_TAG, "Phone is ringing.")
                CoroutineScope(Dispatchers.IO).launch {
                    val incomingNumber = getLastIncomingCallNumber(context)
                    if (incomingNumber != null){
                        showRecordingStartedNotification(context,"Incoming call from: $incomingNumber")
                    }
                    Log.d(LOG_TAG, "Incoming call from: $incomingNumber")
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d(LOG_TAG, "Call answered.")
                if (!isRecording) {
                    recordingFilePath = CallRecordingUtils.startRecording(context)
                    isRecording = recordingFilePath != null
                    showRecordingStartedNotification(context,"Recording started")
                    Log.d(LOG_TAG, "Recording started at: $recordingFilePath")
                    CoroutineScope(Dispatchers.IO).launch {
                        recordingFilePath?.let {
                            GeneralService().uploadRecording(it,genericCallback)
                        }
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d(LOG_TAG, "Call ended.")
                if (isRecording) {
                    CallRecordingUtils.stopRecording()
                    isRecording = false
                    showRecordingStartedNotification(context,"Recording stopped")
                    Log.d(LOG_TAG, "Recording saved at: $recordingFilePath")
                }
            }
            else -> Log.w(LOG_TAG, "Unknown state: $state")
        }
    }

    private fun getLastIncomingCallNumber(context: Context): String? {
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.INCOMING_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                }
            }
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Permission denied for reading call logs.")
        }
        return null
    }
    private fun showRecordingStartedNotification(context: Context,message:String) {
        val intent = Intent(context, ActivityHome::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Call Recording")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private val genericCallback = object : GeneralCallback {
        override fun onSuccess(serviceID: Int, response: Any?) {
            if (serviceID == Services.REQ_ID_UPLOAD_RECORDING) {
                Log.d(LOG_TAG, "onSuccess: "+response)
                showRecordingStartedNotification(context,"Recording uploaded")
            }
        }

        override fun onError(serviceID: Int, error: String) {
            showRecordingStartedNotification(context,error)
        }
    }
}
