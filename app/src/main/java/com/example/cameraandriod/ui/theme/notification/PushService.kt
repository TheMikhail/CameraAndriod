package com.example.cameraandriod.ui.theme.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.res.painterResource
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cameraandriod.R
import com.google.firebase.messaging.FirebaseMessagingService
import kotlin.io.path.Path

class PushService {
    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "channelID"
        const val CHANNEL_NAME = "AndroidApp"
    }

    fun sendNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            //.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setSmallIcon(R.drawable.img)
            .setContentTitle("Осторожно")
            .setContentText("Подозрительный человек!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}