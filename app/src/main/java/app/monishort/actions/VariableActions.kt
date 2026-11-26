package app.monishort.actions

import app.monishort.automation.ActionDefinition
import app.monishort.automation.ActionException
import app.monishort.automation.ExecutionContext
import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec
import app.monishort.automation.Value
import kotlinx.coroutines.delay

class SetVariableAction : ActionDefinition {
    override val type = "set_variable"
    override val title = "Set Variable"
    override val category = "Variables"
    override val parameters = listOf(
        ParamSpec("name", "Variable name", kind = ParamKind.VARIABLE_NAME),
        ParamSpec("value", "Value", required = false),
        ParamSpec("valueType", "Type: text, number, boolean or list", required = false),
    )

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val raw = params["value"].orEmpty()
        val v: Value = when (val t = params["valueType"]?.trim()?.lowercase().orEmpty().ifEmpty { "text" }) {
            "text" -> Value.Text(raw)
            "number" -> Value.Num(raw.trim().toDoubleOrNull() ?: throw ActionException("'$raw' is not a number"))
            "boolean" -> when (raw.trim().lowercase()) {
                "true" -> Value.Bool(true)
                "false" -> Value.Bool(false)
                else -> throw ActionException("'$raw' is not true or false")
            }
            "list" -> Value.ListV(raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { Value.Text(it) })
            else -> throw ActionException("Unknown value type '$t'")
        }
        ctx.set(params.getValue("name").trim(), v)
        return v
    }
}

class ModifyVariableAction : ActionDefinition {
    override val type = "modify_variable"
    override val title = "Modify Variable"
    override val category = "Variables"
    override val parameters = listOf(
        ParamSpec("name", "Variable name", kind = ParamKind.VARIABLE_NAME),
        ParamSpec("operation", "Operation: add, subtract, multiply, divide or append"),
        ParamSpec("operand", "Operand"),
    )

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val name = params.getValue("name").trim()
        val cur = ctx.get(name) ?: throw ActionException("Variable '$name' is not set")
        val operand = params.getValue("operand")
        val op = params.getValue("operation").trim().lowercase()
        val result: Value = if (op == "append") {
            if (cur is Value.ListV) Value.ListV(cur.v + Value.Text(operand)) else Value.Text(cur.asText() + operand)
        } else {
            val a = cur.asNumber() ?: throw ActionException("Variable '$name' is not a number")
            val b = operand.trim().toDoubleOrNull() ?: throw ActionException("'$operand' is not a number")
            Value.Num(
                when (op) {
                    "add" -> a + b
                    "subtract" -> a - b
                    "multiply" -> a * b
                    "divide" -> {
                        if (b == 0.0) throw ActionException("Cannot divide by zero")
                        a / b
                    }
                    else -> throw ActionException("Unknown operation '$op'")
                }
            )
        }
        ctx.set(name, result)
        return result
    }
}

class WaitAction : ActionDefinition {
    override val type = "wait"
    override val title = "Wait"
    override val category = "Logic"
    override val parameters = listOf(ParamSpec("seconds", "Seconds (0-3600)", kind = ParamKind.NUMBER))

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val s = params.getValue("seconds").trim().toDoubleOrNull() ?: throw ActionException("Seconds must be a number")
        if (s < 0 || s > 3600) throw ActionException("Wait must be between 0 and 3600 seconds")
        delay((s * 1000).toLong())
        return Value.Nil
    }
}
