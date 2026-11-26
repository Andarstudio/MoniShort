package app.monishort

import app.monishort.actions.defaultActionRegistry
import app.monishort.automation.Step
import app.monishort.automation.Workflow
import app.monishort.automation.WorkflowJson
import app.monishort.automation.WorkflowValidator
import app.monishort.conditions.CompareOp
import app.monishort.conditions.Condition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowJsonAndValidatorTest {
    private val sample = Workflow(
        listOf(
            Step.Action("1", "set_variable", mapOf("name" to "a", "value" to "1")),
            Step.If(
                "2",
                Condition.And(listOf(Condition.Compare("{{a}}", CompareOp.EQ, "1"), Condition.Not(Condition.Truthy("{{b}}")))),
                listOf(Step.Action("3", "wait", mapOf("seconds" to "1"))),
                listOf(Step.Repeat("4", "3", listOf(Step.Action("5", "stop_sound")))),
            ),
            Step.RepeatUntil("6", Condition.Or(listOf(Condition.Compare("x", CompareOp.IS_EMPTY))), emptyList(), 7),
        )
    )

    @Test fun jsonRoundTripKeepsEverything() {
        assertEquals(sample, WorkflowJson.decode(WorkflowJson.encode(sample)))
    }

    @Test fun mapRoundTrip() {
        val m = mapOf("level" to "20", "direction" to "below")
        assertEquals(m, WorkflowJson.decodeMap(WorkflowJson.encodeMap(m)))
        assertTrue(WorkflowJson.decodeMap("").isEmpty())
    }

    @Test fun validatorAcceptsGoodWorkflow() {
        assertTrue(WorkflowValidator.validate(sample, defaultActionRegistry()).isEmpty())
    }

    @Test fun validatorReportsProblemsInsideNestedSteps() {
        val bad = Workflow(listOf(Step.If("1", Condition.Truthy("x"), listOf(Step.Action("2", "nope")), listOf(Step.Action("3", "wait")))))
        val errors = WorkflowValidator.validate(bad, defaultActionRegistry())
        assertEquals(2, errors.size)
    }

    @Test fun templatesSkipTypeCheckButNotRequiredCheck() {
        val reg = defaultActionRegistry()
        val ok = Workflow(listOf(Step.Action("1", "wait", mapOf("seconds" to "{{n}}"))))
        assertTrue(WorkflowValidator.validate(ok, reg).isEmpty())
        val missing = Workflow(listOf(Step.Action("1", "wait")))
        assertEquals(1, WorkflowValidator.validate(missing, reg).size)
    }
}
