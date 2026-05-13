package com.example.nammahasiru.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class StatusReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val treeName = inputData.getString("TREE_NAME") ?: "A Tree"
        
        sendNotification(
            title = "Check-up Reminder \uD83C\uDF31",
            message = "It's time to check the status of your $treeName. Has it survived?"
        )
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "NAMMA_HASIRU_REMINDERS"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tree Status Reminders"
            val descriptionText = "Reminders to check on your planted trees"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            // Using default system icon for simplicity since mipmap-hdpi/ic_launcher might not exist yet
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}
