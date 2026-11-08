package com.memoise.ledger.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.memoise.ledger.data.database.EntryActivityEntity
import com.memoise.ledger.data.database.MemoiseDatabase
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.MainActivity
import java.util.UUID
import kotlinx.coroutines.runBlocking

class ReminderNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val database = Room.databaseBuilder(
            applicationContext,
            MemoiseDatabase::class.java,
            "memoise-ledger.db",
        ).addMigrations(*MemoiseDatabase.ALL_MIGRATIONS).build()

        try {
            val reminder = runBlocking { database.entryReminderDao().findById(reminderId) }
                ?: return Result.success()
            if (!reminder.enabled) return Result.success()
            val entry = runBlocking { database.entryDao().findById(reminder.entryId) }
                ?: return Result.success()
            if (entry.status == EntryStatus.COMPLETED || entry.status == EntryStatus.CANCELLED) {
                runBlocking {
                    database.entryReminderDao().upsert(
                        reminder.copy(enabled = false, updatedAt = System.currentTimeMillis()),
                    )
                }
                return Result.success()
            }
            if (
                Build.VERSION.SDK_INT >= 33 &&
                applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Ledger reminders", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(applicationContext, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(applicationContext)
            }
            val openEntryIntent = Intent(applicationContext, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ENTRY
                putExtra(MainActivity.EXTRA_ENTRY_ID, entry.id)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                applicationContext,
                reminderId.hashCode(),
                openEntryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(entry.title)
                .setContentText("A reminder from your Memoise ledger")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
            try {
                manager.notify(reminderId.hashCode(), notification)
            } catch (_: SecurityException) {
                return Result.success()
            }

            val sentAt = System.currentTimeMillis()
            runBlocking {
                database.entryReminderDao().upsert(reminder.copy(enabled = false, updatedAt = sentAt))
                database.entryActivityDao().insert(
                    EntryActivityEntity(
                        id = UUID.randomUUID().toString(),
                        entryId = entry.id,
                        action = "Reminder sent",
                        details = null,
                        occurredAt = sentAt,
                    ),
                )
            }
            return Result.success()
        } finally {
            database.close()
        }
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        private const val CHANNEL_ID = "memoise_ledger_reminders"
    }
}
