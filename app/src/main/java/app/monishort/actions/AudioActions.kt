package app.monishort.actions

import app.monishort.automation.ActionDefinition
import app.monishort.automation.ActionException
import app.monishort.automation.ExecutionContext
import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec
import app.monishort.automation.Value

class PlaySoundAction : ActionDefinition {
    override val type = "play_sound"
    override val title = "Play Sound"
    override val category = "Audio"
    override val parameters = listOf(
        ParamSpec("uri", "Audio file (content:// URI)", kind = ParamKind.URI),
        ParamSpec("volume", "Volume 0-100", required = false, kind = ParamKind.NUMBER),
        ParamSpec("waitUntilDone", "Wait until finished (true/false)", required = false, kind = ParamKind.BOOLEAN),
    )

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val volume = params["volume"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            it.toDoubleOrNull() ?: throw ActionException("Volume must be a number")
        } ?: 100.0
        if (volume < 0 || volume > 100) throw ActionException("Volume must be between 0 and 100")
        val wait = params["waitUntilDone"].equals("true", ignoreCase = true)
        ctx.platform.playSound(params.getValue("uri").trim(), (volume / 100.0).toFloat(), wait)
        return Value.Nil
    }
}

class StopSoundAction : ActionDefinition {
    override val type = "stop_sound"
    override val title = "Stop Sound"
    override val category = "Audio"
    override val parameters = emptyList<ParamSpec>()

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        ctx.platform.stopSound()
        return Value.Nil
    }
}
