package app.monishort.triggers

import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec

object TriggerTypes {
    const val CHARGER_CONNECTED = "charger_connected"
    const val CHARGER_DISCONNECTED = "charger_disconnected"
    const val BATTERY_LOW = "battery_low"
    const val BATTERY_LEVEL = "battery_level"
    const val DEVICE_BOOT = "device_boot"
}

data class TriggerEvent(val type: String, val values: Map<String, String> = emptyMap())

interface Trigger {
    val type: String
    val title: String
    val description: String
    val configFields: List<ParamSpec> get() = emptyList()

    fun validate(config: Map<String, String>): List<String> = emptyList()
    fun matches(event: TriggerEvent, config: Map<String, String>): Boolean = event.type == type
}

object ChargerConnectedTrigger : Trigger {
    override val type = TriggerTypes.CHARGER_CONNECTED
    override val title = "Charger connected"
    override val description = "Runs when a charger or USB power is plugged in. Needs the background monitor."
}

object ChargerDisconnectedTrigger : Trigger {
    override val type = TriggerTypes.CHARGER_DISCONNECTED
    override val title = "Charger disconnected"
    override val description = "Runs when the charger is unplugged. Needs the background monitor."
}

object BatteryLowTrigger : Trigger {
    override val type = TriggerTypes.BATTERY_LOW
    override val title = "Battery low"
    override val description = "Runs when Android reports low battery (usually 15%). Needs the background monitor."
}

object DeviceBootTrigger : Trigger {
    override val type = TriggerTypes.DEVICE_BOOT
    override val title = "Device boot"
    override val description = "Runs after the phone starts. Some manufacturers require Autostart to be enabled for MoniShort."
}

/** Fires when the battery level crosses the configured threshold (not on every change). */
object BatteryLevelTrigger : Trigger {
    override val type = TriggerTypes.BATTERY_LEVEL
    override val title = "Battery level reached"
    override val description = "Runs once when the battery crosses a level. Needs the background monitor."
    override val configFields = listOf(
        ParamSpec("level", "Level 1-100", kind = ParamKind.NUMBER),
        ParamSpec("direction", "Direction: below or above"),
    )

    override fun validate(config: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        val level = config["level"]?.trim()?.toIntOrNull()
        if (level == null || level !in 1..100) errors += "Level must be a number from 1 to 100"
        val dir = config["direction"]?.trim()?.lowercase()
        if (dir != "below" && dir != "above") errors += "Direction must be below or above"
        return errors
    }

    override fun matches(event: TriggerEvent, config: Map<String, String>): Boolean {
        if (event.type != type) return false
        val level = config["level"]?.trim()?.toIntOrNull() ?: return false
        val cur = event.values["level"]?.toIntOrNull() ?: return false
        val prev = event.values["previous"]?.toIntOrNull() ?: return false
        return when (config["direction"]?.trim()?.lowercase()) {
            "below" -> prev > level && cur <= level
            "above" -> prev < level && cur >= level
            else -> false
        }
    }
}

class TriggerRegistry {
    private val map = LinkedHashMap<String, Trigger>()
    fun register(t: Trigger): TriggerRegistry { map[t.type] = t; return this }
    fun get(type: String): Trigger? = map[type]
    fun all(): List<Trigger> = map.values.toList()
}

fun defaultTriggerRegistry(): TriggerRegistry = TriggerRegistry()
    .register(ChargerConnectedTrigger)
    .register(ChargerDisconnectedTrigger)
    .register(BatteryLowTrigger)
    .register(BatteryLevelTrigger)
    .register(DeviceBootTrigger)
