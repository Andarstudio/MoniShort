package app.monishort.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.monishort.MainActivity
import app.monishort.R

class Notifier(private val context: Context) {
    companion object {
        const val CH_SERVICE = "monishort_service"
        const val CH_ALERTS = "monishort_alerts"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Background monitor", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps MoniShort listening for charger and battery events"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, "Automation notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications created by your shortcuts and automations"
            }
        )
    }

    fun canNotify(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Returns false when notifications are disabled for the app. */
    fun show(id: Int, title: String, text: String): Boolean {
        if (!canNotify()) return false
        val n = NotificationCompat.Builder(context, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(id, n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
