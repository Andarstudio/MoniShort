package app.monishort

import android.content.Context
import androidx.room.Room
import app.monishort.actions.defaultActionRegistry
import app.monishort.automation.ActionRegistry
import app.monishort.automation.ExecutionResult
import app.monishort.automation.PlatformServices
import app.monishort.automation.WorkflowEngine
import app.monishort.data.ExecutionHistoryEntity
import app.monishort.data.MoniDatabase
import app.monishort.data.SettingsRepository
import app.monishort.feature.charging.ChargingHandler
import app.monishort.media.SoundPlayer
import app.monishort.system.AndroidPlatformServices
import app.monishort.system.Notifier
import app.monishort.triggers.TriggerDispatcher
import app.monishort.triggers.defaultTriggerRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-written dependency container (one instance per process, owned by [MoniShortApp]). */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: MoniDatabase = Room.databaseBuilder(context, MoniDatabase::class.java, "monishort.db").build()
    val settings = SettingsRepository(context)
    val notifier = Notifier(context)
    val soundPlayer = SoundPlayer(context)
    val platform: PlatformServices = AndroidPlatformServices(context, soundPlayer, notifier)

    val actions: ActionRegistry = defaultActionRegistry()
    val triggers = defaultTriggerRegistry()
    val engine = WorkflowEngine(actions, appScope)
    val chargingHandler = ChargingHandler(settings, soundPlayer, notifier)
    val dispatcher = TriggerDispatcher(appScope, triggers, { db.automations() }, engine, platform)

    fun start() {
        notifier.ensureChannels()
        dispatcher.addListener { chargingHandler.handle(it) }
        appScope.launch {
            engine.results.collect { r ->
                db.history().insert(r.toEntity())
                db.history().prune()
            }
        }
    }

    private fun ExecutionResult.toEntity() = ExecutionHistoryEntity(
        executionId = executionId,
        name = name,
        status = status.name,
        message = message,
        startedAt = startedAt,
        finishedAt = finishedAt,
        log = log.joinToString("\n"),
    )
}
