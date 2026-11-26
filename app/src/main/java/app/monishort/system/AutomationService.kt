package app.monishort.system

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.monishort.MoniShortApp
import app.monishort.R
import app.monishort.triggers.TriggerEvent
import app.monishort.triggers.TriggerTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that listens for system broadcasts which Android 8+ does not deliver to
 * manifest receivers (power connected/disconnected, battery changes) and forwards them as trigger events.
 */
class AutomationService : Service() {
    private var lastLevel = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dispatcher = MoniShortApp.container(context).dispatcher
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> dispatcher.dispatch(TriggerEvent(TriggerTypes.CHARGER_CONNECTED))
                Intent.ACTION_POWER_DISCONNECTED -> dispatcher.dispatch(TriggerEvent(TriggerTypes.CHARGER_DISCONNECTED))
                Intent.ACTION_BATTERY_LOW -> dispatcher.dispatch(TriggerEvent(TriggerTypes.BATTERY_LOW))
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level < 0 || scale <= 0) return
                    val pct = level * 100 / scale
                    val prev = lastLevel
                    if (pct != prev) {
                        lastLevel = pct
                        if (prev >= 0) {
                            dispatcher.dispatch(
                                TriggerEvent(
                                    TriggerTypes.BATTERY_LEVEL,
                                    mapOf("level" to pct.toString(), "previous" to prev.toString()),
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        running.value = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = MoniShortApp.container(this)
        container.notifier.ensureChannels()
        val notification: Notification = NotificationCompat.Builder(this, Notifier.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("MoniShort is monitoring triggers")
            .setContentText("Charger and battery automations are active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(container.notifier.openAppIntent())
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private val _running = MutableStateFlow(false)
        private val running get() = _running
        val isRunning: StateFlow<Boolean> get() = _running
        val lastStartError = MutableStateFlow<String?>(null)

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AutomationService::class.java))
                lastStartError.value = null
            } catch (e: Exception) {
                lastStartError.value = e.message ?: "Android refused to start the background monitor"
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
