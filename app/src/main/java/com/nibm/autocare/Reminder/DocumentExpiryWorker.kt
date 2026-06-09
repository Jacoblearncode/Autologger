package com.nibm.autocare.Reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nibm.autocare.MainActivity
import com.nibm.autocare.R

class DocumentExpiryWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val registration = inputData.getString(KEY_REGISTRATION) ?: return Result.failure()
        val docType = inputData.getString(KEY_DOC_TYPE) ?: return Result.failure()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)

        val isWarranty = docType.startsWith("Warranty:")
        val notifOn = if (isWarranty)
            com.nibm.autocare.SettingsManager.isWarrantyNotifEnabled(context)
        else
            com.nibm.autocare.SettingsManager.isDocNotifEnabled(context)
        if (!notifOn) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return Result.success()
        }

        ensureChannelExists()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, "$registration$docType".hashCode(), tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            daysLeft <= 0 -> "$docType Expired — $registration"
            daysLeft <= 7 -> "$docType Expiring Soon — $registration"
            else -> "$docType Reminder — $registration"
        }
        val body = when {
            daysLeft <= 0 -> "Your $docType for $registration has expired. Please renew immediately."
            daysLeft == 1 -> "Your $docType for $registration expires tomorrow!"
            else -> "Your $docType for $registration expires in $daysLeft days."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_service)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify("$registration$docType".hashCode(), notification)
        return Result.success()
    }

    private fun ensureChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders when vehicle documents are about to expire"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_REGISTRATION = "registration"
        const val KEY_DOC_TYPE = "doc_type"
        const val KEY_DAYS_LEFT = "days_left"
        const val CHANNEL_ID = "doc_expiry_reminders"
    }
}
