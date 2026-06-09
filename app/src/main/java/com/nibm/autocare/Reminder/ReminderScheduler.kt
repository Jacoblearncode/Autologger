package com.nibm.autocare.Reminder

import android.content.Context
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    // Called when a new vehicle is added
    fun scheduleForVehicle(
        context: Context,
        registration: String,
        currentMileage: Int,
        weeklyDistance: Int
    ) {
        val kmUntilNext = if (currentMileage % 5000 == 0) 5000 else 5000 - (currentMileage % 5000)
        val days = calculateDays(kmUntilNext, weeklyDistance)
        enqueue(context, registration, days)
    }

    // Called after a service is logged (resets the countdown from 0)
    fun scheduleAfterService(
        context: Context,
        registration: String,
        weeklyDistance: Int
    ) {
        val days = calculateDays(kmUntilNext = 5000, weeklyDistance = weeklyDistance)
        enqueue(context, registration, days)
    }

    /**
     * Schedules 30-day and 7-day warning notifications for a vehicle document.
     * Called whenever the user saves an expiry date in VehicleDocumentsActivity.
     */
    fun scheduleDocumentReminder(
        context: Context,
        registration: String,
        field: String,
        expiryDateStr: String
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expiry = try { dateFormat.parse(expiryDateStr) } catch (e: Exception) { return }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(expiry.time - Date().time).toInt()

        val docLabel = when (field) {
            "insurance" -> "Insurance"
            "road_tax" -> "Road Tax"
            "fitness" -> "Fitness Certificate"
            else -> field.replaceFirstChar { it.uppercase() }
        }

        // Cancel any existing reminders for this doc before scheduling new ones
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork("doc_${registration}_${docLabel}_30d")
        wm.cancelUniqueWork("doc_${registration}_${docLabel}_7d")

        if (daysLeft > 30) {
            enqueueDocReminder(context, registration, docLabel, 30, (daysLeft - 30).toLong())
        }
        if (daysLeft > 7) {
            enqueueDocReminder(context, registration, docLabel, 7, (daysLeft - 7).toLong())
        } else if (daysLeft in 1..7) {
            // Already within the 7-day window — fire a same-day notification
            enqueueDocReminder(context, registration, docLabel, daysLeft, 0L)
        }
    }

    /**
     * Schedules 30-day and 7-day warning notifications for a replaced part's warranty.
     * Reuses DocumentExpiryWorker so no new worker class is needed.
     */
    fun scheduleWarrantyReminder(
        context: Context,
        registration: String,
        partName: String,
        warrantyExpiry: String
    ) {
        if (warrantyExpiry.isBlank()) return
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expiry = try { dateFormat.parse(warrantyExpiry) } catch (e: Exception) { return }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(expiry.time - Date().time).toInt()

        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork("warranty_${registration}_${partName}_30d")
        wm.cancelUniqueWork("warranty_${registration}_${partName}_7d")

        val label = "Warranty: $partName"
        if (daysLeft > 30) {
            enqueueDocReminder(context, registration, label, 30, (daysLeft - 30).toLong())
        }
        if (daysLeft > 7) {
            enqueueDocReminder(context, registration, label, 7, (daysLeft - 7).toLong())
        } else if (daysLeft in 1..7) {
            enqueueDocReminder(context, registration, label, daysLeft, 0L)
        }
    }

    // Whichever comes first: mileage estimate or 90 days (3 months)
    private fun calculateDays(kmUntilNext: Int, weeklyDistance: Int): Long {
        val milageDays = if (weeklyDistance > 0) {
            (kmUntilNext.toDouble() / weeklyDistance * 7).toLong()
        } else Long.MAX_VALUE

        return minOf(milageDays, 90L).coerceAtLeast(1L)
    }

    private fun enqueue(context: Context, registration: String, delayDays: Long) {
        val data = workDataOf(ServiceReminderWorker.KEY_REGISTRATION to registration)

        val request = OneTimeWorkRequestBuilder<ServiceReminderWorker>()
            .setInitialDelay(delayDays, TimeUnit.DAYS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$registration",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun enqueueDocReminder(
        context: Context,
        registration: String,
        docLabel: String,
        daysLeft: Int,
        delayDays: Long
    ) {
        val data = workDataOf(
            DocumentExpiryWorker.KEY_REGISTRATION to registration,
            DocumentExpiryWorker.KEY_DOC_TYPE to docLabel,
            DocumentExpiryWorker.KEY_DAYS_LEFT to daysLeft
        )
        val request = OneTimeWorkRequestBuilder<DocumentExpiryWorker>()
            .setInitialDelay(delayDays.coerceAtLeast(0L), TimeUnit.DAYS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "doc_${registration}_${docLabel}_${daysLeft}d",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
