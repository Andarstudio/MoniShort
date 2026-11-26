package app.monishort.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import app.monishort.automation.ActionException
import app.monishort.automation.BatteryStatus
import app.monishort.automation.PlatformServices
import app.monishort.media.SoundPlayer
import java.util.concurrent.atomic.AtomicInteger

class AndroidPlatformServices(
    private val context: Context,
    private val sound: SoundPlayer,
    private val notifier: Notifier,
) : PlatformServices {
    private val counter = AtomicInteger(0)

    override fun showNotification(title: String, text: String) {
        val id = 1000 + counter.incrementAndGet() % 1000
        if (!notifier.show(id, title, text)) {
            throw ActionException("Notifications are disabled for MoniShort. Allow them in Settings.")
        }
    }

    override suspend fun playSound(uri: String, volume: Float, waitUntilDone: Boolean) {
        val u = Uri.parse(uri)
        if (waitUntilDone) sound.playAndWait(u, volume) else sound.play(u, volume)
    }

    override fun stopSound() = sound.stop()

    override fun batteryStatus(): BatteryStatus {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: throw ActionException("Battery information is unavailable")
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryStatus(if (level < 0 || scale <= 0) 0 else level * 100 / scale, charging)
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw ActionException("No app can open this URL")
        }
    }
}
