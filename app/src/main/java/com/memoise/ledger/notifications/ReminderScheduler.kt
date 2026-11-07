package com.memoise.ledger.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(reminderId: String, triggerAt: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay((triggerAt - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(ReminderNotificationWorker.KEY_REMINDER_ID, reminderId).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(reminderId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(reminderId))
    }

    private fun workName(reminderId: String) = "memoise-reminder-$reminderId"
}
