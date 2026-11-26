package app.monishort.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.monishort.MoniShortApp
import app.monishort.triggers.TriggerEvent
import app.monishort.triggers.TriggerTypes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val c = MoniShortApp.container(context)
        val pending = goAsync()
        c.appScope.launch {
            try {
                if (c.settings.engineEnabled.first()) AutomationService.start(context)
                c.dispatcher.dispatch(TriggerEvent(TriggerTypes.DEVICE_BOOT))
            } finally {
                pending.finish()
            }
        }
    }
}
