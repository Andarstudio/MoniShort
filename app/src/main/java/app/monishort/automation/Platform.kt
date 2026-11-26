package app.monishort.automation

data class BatteryStatus(val level: Int, val charging: Boolean)

/**
 * Everything the engine needs from the device. The engine and actions only talk to this
 * interface, so they run (and are unit-tested) without Android or Compose.
 */
interface PlatformServices {
    /** @throws ActionException if notifications are disabled. */
    fun showNotification(title: String, text: String)
    suspend fun playSound(uri: String, volume: Float, waitUntilDone: Boolean)
    fun stopSound()
    fun batteryStatus(): BatteryStatus
    fun openUrl(url: String)
}
