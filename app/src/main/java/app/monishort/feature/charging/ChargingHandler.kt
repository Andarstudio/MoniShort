package app.monishort.feature.charging

import android.net.Uri
import app.monishort.automation.ActionException
import app.monishort.data.SettingsRepository
import app.monishort.media.SoundPlayer
import app.monishort.system.Notifier
import app.monishort.triggers.TriggerEvent
import app.monishort.triggers.TriggerTypes
import kotlinx.coroutines.flow.MutableStateFlow

/** Built-in charging automation: plays the configured sound / shows a notification on plug and unplug. */
class ChargingHandler(
    private val settings: SettingsRepository,
    private val sound: SoundPlayer,
    private val notifier: Notifier,
) {
    /** Last problem (missing file, notifications disabled...) so the UI can show it. */
    val lastError = MutableStateFlow<String?>(null)

    suspend fun handle(event: TriggerEvent, force: Boolean = false) {
        val connect = when (event.type) {
            TriggerTypes.CHARGER_CONNECTED -> true
            TriggerTypes.CHARGER_DISCONNECTED -> false
            else -> return
        }
        val s = settings.currentCharging()
        if (!s.enabled && !force) return
        lastError.value = null

        val uri = if (connect) s.connectSoundUri else if (s.disconnectSoundEnabled || force) s.disconnectSoundUri else null
        if (uri != null) {
            try {
                sound.play(Uri.parse(uri), s.volumePercent / 100f)
            } catch (e: ActionException) {
                lastError.value = e.message
            }
        } else if (force) {
            lastError.value = "No sound selected for this event"
        }

        val notify = if (connect) s.notifyOnConnect else s.notifyOnDisconnect
        if (notify) {
            val shown = notifier.show(
                if (connect) 2001 else 2002,
                if (connect) "Charger connected" else "Charger disconnected",
                if (connect) "Charging started" else "Charging stopped",
            )
            if (!shown) lastError.value = "Notifications are disabled for MoniShort"
        }
    }
}
