package app.monishort.actions

import app.monishort.automation.ActionRegistry

/** The built-in action library. New actions only need to be added here. */
fun defaultActionRegistry(): ActionRegistry = ActionRegistry()
    .register(SetVariableAction())
    .register(ModifyVariableAction())
    .register(WaitAction())
    .register(ShowNotificationAction())
    .register(PlaySoundAction())
    .register(StopSoundAction())
    .register(GetBatteryAction())
    .register(GetDateTimeAction())
    .register(OpenUrlAction())
    .register(HttpGetAction())
