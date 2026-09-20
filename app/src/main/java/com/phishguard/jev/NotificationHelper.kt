package com.phishguard.jev

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "phishing_alerts"
    private var nextId = 1000

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Phishing alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-confidence phishing SMS detections"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun showPhishingAlert(context: Context, sender: String, signals: List<String>) {
        ensureChannel(context)
        val reason = if (signals.isEmpty()) "suspicious pattern" else signals.joinToString(", ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Possible phishing SMS")
            .setContentText("From $sender — $reason")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("From $sender\nSignals: $reason\n\nOpen Jev Phish Guard to review.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(nextId++, notification)
        }
    }
}
