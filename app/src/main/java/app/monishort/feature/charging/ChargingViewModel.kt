package app.monishort.feature.charging

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.monishort.AppContainer
import app.monishort.automation.ActionException
import app.monishort.data.ChargingSettings
import app.monishort.media.SoundFiles
import app.monishort.triggers.TriggerEvent
import app.monishort.triggers.TriggerTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChargingViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<ChargingSettings> =
        c.settings.charging.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChargingSettings())

    val message = MutableStateFlow<String?>(null)

    fun update(transform: (ChargingSettings) -> ChargingSettings) {
        viewModelScope.launch { c.settings.updateCharging(transform) }
    }

    fun pickSound(context: Context, uri: Uri, connect: Boolean) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            message.value = "Android did not keep access to this file. If the sound stops playing, pick it again."
        }
        val name = SoundFiles.displayName(context, uri)
        update {
            if (connect) it.copy(connectSoundUri = uri.toString(), connectSoundName = name)
            else it.copy(disconnectSoundUri = uri.toString(), disconnectSoundName = name)
        }
    }

    fun clearSound(connect: Boolean) = update {
        if (connect) it.copy(connectSoundUri = null, connectSoundName = null)
        else it.copy(disconnectSoundUri = null, disconnectSoundName = null)
    }

    fun preview(uri: String?) {
        if (uri == null) return
        try {
            c.soundPlayer.play(Uri.parse(uri), settings.value.volumePercent / 100f)
            message.value = null
        } catch (e: ActionException) {
            message.value = e.message
        }
    }

    fun stopPreview() = c.soundPlayer.stop()

    /** Simulates the plug/unplug event with the current settings, ignoring the master switch. */
    fun test(connect: Boolean) {
        viewModelScope.launch {
            val type = if (connect) TriggerTypes.CHARGER_CONNECTED else TriggerTypes.CHARGER_DISCONNECTED
            c.chargingHandler.handle(TriggerEvent(type), force = true)
            message.value = c.chargingHandler.lastError.value
        }
    }

    companion object {
        fun factory(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChargingViewModel(c) }
        }
    }
}
