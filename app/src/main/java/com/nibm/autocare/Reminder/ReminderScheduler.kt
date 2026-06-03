package com.nibm.autocare.Reminder

import android.content.Context
import androidx.work.*
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
}
