package app.monishort.automation

object WorkflowValidator {
    /** Returns human-readable problems; an empty list means the workflow can be saved and run. */
    fun validate(workflow: Workflow, registry: ActionRegistry): List<String> {
        val errors = mutableListOf<String>()
        fun walk(steps: List<Step>) {
            for (s in steps) {
                when (s) {
                    is Step.Action -> {
                        val def = registry.get(s.type)
                        if (def == null) errors += "Unknown action '${s.type}'"
                        else def.validate(s.params).forEach { errors += "${def.title}: $it" }
                    }
                    is Step.If -> { walk(s.thenSteps); walk(s.elseSteps) }
                    is Step.Repeat -> {
                        val c = s.count.trim()
                        if (c.isEmpty()) errors += "Repeat: count is required"
                        else if (!c.contains("{{") && c.toDoubleOrNull() == null) errors += "Repeat: count must be a number"
                        walk(s.body)
                    }
                    is Step.RepeatUntil -> {
                        if (s.maxIterations !in 1..100_000) errors += "Repeat Until: max iterations must be 1-100000"
                        walk(s.body)
                    }
                }
            }
        }
        walk(workflow.steps)
        return errors
    }
}
