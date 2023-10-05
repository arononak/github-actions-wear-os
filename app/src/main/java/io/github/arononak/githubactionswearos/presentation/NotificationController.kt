package io.github.arononak.githubactionswearos.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.arononak.githubactionswearos.R

object NotificationController {

    fun showCompletedBuild(context: Context, success: Boolean) {
        val title = if (success) "Success" else "Failure"
        val description =
            if (success) "The workflow has been successfully built" else "Something went wrong :/"

        showNotification(context, title, description)
    }

    private fun showNotification(context: Context, title: String, description: String) {
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = CHANNEL_DESCRIPTION

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.github_white)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private const val CHANNEL_ID = "github-actions-channel-id"
    private const val CHANNEL_NAME = "Github Actions channel"
    private const val CHANNEL_DESCRIPTION = "Github Actions main channel"
    private const val NOTIFICATION_ID = 1
}
