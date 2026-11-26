package app.monishort.automation

import app.monishort.conditions.Condition

/** A workflow step. Parameters are plain strings that may contain `{{variable}}` templates. */
sealed class Step {
    abstract val id: String

    data class Action(
        override val id: String,
        val type: String,
        val params: Map<String, String> = emptyMap(),
    ) : Step()

    data class If(
        override val id: String,
        val condition: Condition,
        val thenSteps: List<Step>,
        val elseSteps: List<Step> = emptyList(),
    ) : Step()

    data class Repeat(override val id: String, val count: String, val body: List<Step>) : Step()

    /** Do-while: runs the body, then stops once the condition is true. */
    data class RepeatUntil(
        override val id: String,
        val condition: Condition,
        val body: List<Step>,
        val maxIterations: Int = 1000,
    ) : Step()
}

data class Workflow(val steps: List<Step>)
