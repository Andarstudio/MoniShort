package app.monishort.triggers

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
            listeners.forEach { l -> try { l(event) } catch (e: Exception) { } }
            val trigger = triggers.get(event.type) ?: return@launch
            val input = Value.Dict(event.values.mapValues { Value.Text(it.value) })
            for (a in automations().enabledForTrigger(event.type)) {
                try {
                    val config = WorkflowJson.decodeMap(a.triggerConfigJson)
                    if (!trigger.matches(event, config)) continue
                    engine.start(a.name, WorkflowJson.decode(a.workflowJson), platform, input)
                } catch (e: Exception) {
                    // A corrupt automation must never break the others.
                }
            }
        }
    }
}
