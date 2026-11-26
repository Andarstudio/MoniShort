package app.monishort.automation

enum class ParamKind { TEXT, NUMBER, BOOLEAN, URI, VARIABLE_NAME }

data class ParamSpec(
    val key: String,
    val label: String,
    val required: Boolean = true,
    val kind: ParamKind = ParamKind.TEXT,
)

internal val VAR_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")

/** One kind of step. Add a new action by implementing this and registering it - the engine never changes. */
interface ActionDefinition {
    val type: String
    val title: String
    val category: String
    val parameters: List<ParamSpec>

    /** Values that still contain `{{templates}}` are not type-checked until run time. */
    fun validate(params: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        for (p in parameters) {
            val v = params[p.key]?.trim().orEmpty()
            if (v.isEmpty()) {
                if (p.required) errors += "${p.label} is required"
                continue
            }
            if (v.contains("{{")) continue
            when (p.kind) {
                ParamKind.NUMBER -> if (v.toDoubleOrNull() == null) errors += "${p.label} must be a number"
                ParamKind.BOOLEAN -> if (v.lowercase() != "true" && v.lowercase() != "false") errors += "${p.label} must be true or false"
                ParamKind.VARIABLE_NAME -> if (!VAR_NAME.matches(v)) errors += "${p.label} must be a valid variable name"
                else -> Unit
            }
        }
        return errors
    }

    suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value
}

class ActionRegistry {
    private val map = LinkedHashMap<String, ActionDefinition>()

    fun register(action: ActionDefinition): ActionRegistry {
        require(action.type !in map) { "Duplicate action type ${action.type}" }
        map[action.type] = action
        return this
    }

    fun get(type: String): ActionDefinition? = map[type]
    fun all(): List<ActionDefinition> = map.values.toList()
    fun byCategory(): Map<String, List<ActionDefinition>> = all().groupBy { it.category }
}
