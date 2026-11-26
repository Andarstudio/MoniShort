package app.monishort.actions

import app.monishort.automation.ActionDefinition
import app.monishort.automation.ActionException
import app.monishort.automation.ExecutionContext
import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec
import app.monishort.automation.Value
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GetBatteryAction : ActionDefinition {
    override val type = "get_battery"
    override val title = "Get Battery"
    override val category = "Device"
    override val parameters = listOf(ParamSpec("outputVar", "Save result as variable", required = false, kind = ParamKind.VARIABLE_NAME))

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val b = ctx.platform.batteryStatus()
        ctx.set("battery_level", Value.Num(b.level.toDouble()))
        ctx.set("battery_charging", Value.Bool(b.charging))
        return Value.Dict(mapOf("level" to Value.Num(b.level.toDouble()), "charging" to Value.Bool(b.charging)))
    }
}

class GetDateTimeAction : ActionDefinition {
    override val type = "get_datetime"
    override val title = "Get Date & Time"
    override val category = "Device"
    override val parameters = listOf(ParamSpec("outputVar", "Save result as variable", required = false, kind = ParamKind.VARIABLE_NAME))

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val now = LocalDateTime.now()
        return Value.Dict(
            mapOf(
                "date" to Value.Text(now.format(DateTimeFormatter.ISO_LOCAL_DATE)),
                "time" to Value.Text(now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))),
                "hour" to Value.Num(now.hour.toDouble()),
                "minute" to Value.Num(now.minute.toDouble()),
                "weekday" to Value.Text(now.dayOfWeek.name),
                "epochMillis" to Value.Num(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toDouble()),
            )
        )
    }
}

class ShowNotificationAction : ActionDefinition {
    override val type = "show_notification"
    override val title = "Show Notification"
    override val category = "Notifications"
    override val parameters = listOf(
        ParamSpec("title", "Title"),
        ParamSpec("text", "Text", required = false),
    )

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        ctx.platform.showNotification(params.getValue("title").take(100), params["text"].orEmpty().take(500))
        return Value.Nil
    }
}

class OpenUrlAction : ActionDefinition {
    override val type = "open_url"
    override val title = "Open URL"
    override val category = "Applications"
    override val parameters = listOf(ParamSpec("url", "URL (http or https)", kind = ParamKind.URI))

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val url = params.getValue("url").trim()
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw ActionException("Only http and https URLs can be opened")
        }
        ctx.platform.openUrl(url)
        return Value.Text(url)
    }
}
