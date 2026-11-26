package app.monishort.triggers

import android.util.Log
import app.monishort.automation.PlatformServices
import app.monishort.automation.Value
import app.monishort.automation.WorkflowEngine
import app.monishort.automation.WorkflowJson
import app.monishort.data.AutomationDao
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Receives trigger events, notifies built-in listeners (e.g. charging sounds) and starts matching automations. */
class TriggerDispatcher(
    private val scope: CoroutineScope,
    private val triggers: TriggerRegistry,
    private val automations: () -> AutomationDao,
    private val engine: WorkflowEngine,
    private val platform: PlatformServices,
) {
    private val listeners = CopyOnWriteArrayList<suspend (TriggerEvent) -> Unit>()

    fun addListener(l: suspend (TriggerEvent) -> Unit) { listeners += l }

    fun dispatch(event: TriggerEvent) {
        scope.launch {
            Log.d(TAG, "Received trigger: ${event.type}, values=${event.values}")
            listeners.forEach { listener ->
                try {
                    listener(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Trigger listener failed for ${event.type}", e)
                }
            }
            val trigger = triggers.get(event.type) ?: run {
                Log.w(TAG, "No trigger registered for ${event.type}")
                return@launch
            }
            val input = Value.Dict(event.values.mapValues { Value.Text(it.value) })
            val enabled = try {
                automations().enabledForTrigger(event.type)
            } catch (e: Exception) {
                Log.e(TAG, "Could not load enabled automations for ${event.type}", e)
                return@launch
            }
            Log.d(TAG, "Found ${enabled.size} enabled automation(s) for ${event.type}")
            for (automation in enabled) {
                try {
                    val config = WorkflowJson.decodeMap(automation.triggerConfigJson)
                    if (!trigger.matches(event, config)) {
                        Log.d(TAG, "Automation '${automation.name}' did not match ${event.type}")
                        continue
                    }
                    val workflow = WorkflowJson.decode(automation.workflowJson)
                    val executionId = engine.start(automation.name, workflow, platform, input)
                    Log.i(TAG, "Started automation '${automation.name}' as ${executionId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start automation '${automation.name}' for ${event.type}", e)
                }
            }
        }
    }

    private companion object {
        const val TAG = "MoniShortTriggers"
    }
}
