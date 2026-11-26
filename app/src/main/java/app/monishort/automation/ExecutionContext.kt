package app.monishort.automation

/** State of one workflow execution. Used by a single coroutine, so it is not shared between executions. */
class ExecutionContext(
    val executionId: String,
    val input: Value,
    val platform: PlatformServices,
) {
    private val vars = LinkedHashMap<String, Value>()
    val log = mutableListOf<String>()
    var output: Value = Value.Nil

    fun set(name: String, value: Value) { vars[name] = value }
    fun get(name: String): Value? = vars[name]
    fun snapshot(): Map<String, Value> = LinkedHashMap(vars)
    fun log(line: String) { if (log.size < 500) log += line }

    /** Replaces `{{name}}` / `{{name.key}}` / `{{list.0}}` placeholders. Unknown names become empty text. */
    fun resolve(template: String): String =
        if (!template.contains("{{")) template
        else TEMPLATE.replace(template) { m -> lookup(m.groupValues[1])?.asText() ?: "" }

    fun lookup(path: String): Value? {
        val parts = path.split('.')
        var cur: Value = if (parts[0] == "input") input else (vars[parts[0]] ?: return null)
        for (p in parts.drop(1)) {
            cur = when (val c = cur) {
                is Value.Dict -> c.v[p] ?: return null
                is Value.ListV -> c.v.getOrNull(p.toIntOrNull() ?: return null) ?: return null
                else -> return null
            }
        }
        return cur
    }

    companion object {
        private val TEMPLATE = Regex("""\{\{\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\}\}""")
    }
}
