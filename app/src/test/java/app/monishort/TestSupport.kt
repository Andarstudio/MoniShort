package app.monishort

import app.monishort.automation.BatteryStatus
import app.monishort.automation.PlatformServices

class FakePlatform : PlatformServices {
    val notifications = mutableListOf<Pair<String, String>>()
    val played = mutableListOf<String>()
    val opened = mutableListOf<String>()
    var stopCount = 0
    var battery = BatteryStatus(80, true)

    override fun showNotification(title: String, text: String) { notifications += title to text }
    override suspend fun playSound(uri: String, volume: Float, waitUntilDone: Boolean) { played += uri }
    override fun stopSound() { stopCount++ }
    override fun batteryStatus() = battery
    override fun openUrl(url: String) { opened += url }
}
