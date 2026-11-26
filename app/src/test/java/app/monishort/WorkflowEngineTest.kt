package app.monishort

import app.monishort.actions.defaultActionRegistry
import app.monishort.automation.ExecutionResult
import app.monishort.automation.ExecutionStatus
import app.monishort.automation.PlatformServices
import app.monishort.automation.Step
import app.monishort.automation.Value
import app.monishort.automation.Workflow
import app.monishort.automation.WorkflowEngine
import app.monishort.conditions.CompareOp
import app.monishort.conditions.Condition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowEngineTest {
    private var n = 0
    private fun act(type: String, vararg p: Pair<String, String>) = Step.Action("s${n++}", type, mapOf(*p))
    private fun set(name: String, value: String, type: String = "text") =
        act("set_variable", "name" to name, "value" to value, "valueType" to type)
    private fun add(name: String, by: String) =
        act("modify_variable", "name" to name, "operation" to "add", "operand" to by)

    private fun engine(scope: CoroutineScope, limits: WorkflowEngine.Limits = WorkflowEngine.Limits()) =
        WorkflowEngine(defaultActionRegistry(), scope, limits)

    private suspend fun WorkflowEngine.exec(vararg steps: Step, platform: PlatformServices = FakePlatform()): ExecutionResult =
        run("id", "test", Workflow(steps.toList()), platform)

    @Test fun setAndModifyVariables() = runTest {
        val r = engine(this).exec(set("a", "5", "number"), add("a", "3"), set("greeting", "n={{a}}"))
        assertEquals(ExecutionStatus.SUCCESS, r.status)
        assertEquals("8", r.variables["a"]?.asText())
        assertEquals("n=8", r.variables["greeting"]?.asText())
    }

    @Test fun ifOtherwiseTakesCorrectBranch() = runTest {
        fun wf(x: String) = arrayOf(
            set("x", x, "number"),
            Step.If("i", Condition.Compare("{{x}}", CompareOp.GT, "5"), listOf(set("y", "big")), listOf(set("y", "small"))),
        )
        assertEquals("big", engine(this).exec(*wf("10")).variables["y"]?.asText())
        assertEquals("small", engine(this).exec(*wf("1")).variables["y"]?.asText())
    }

    @Test fun repeatRunsBodyNTimes() = runTest {
        val r = engine(this).exec(set("c", "0", "number"), Step.Repeat("r", "4", listOf(add("c", "1"))))
        assertEquals("4", r.variables["c"]?.asText())
        assertEquals("4", r.variables["repeat_index"]?.asText())
    }

    @Test fun repeatUntilStopsWhenConditionTrue() = runTest {
        val r = engine(this).exec(
            set("c", "0", "number"),
            Step.RepeatUntil("u", Condition.Compare("{{c}}", CompareOp.GTE, "5"), listOf(add("c", "1"))),
        )
        assertEquals("5", r.variables["c"]?.asText())
    }

    @Test fun repeatUntilIsLimited() = runTest {
        val never = Condition.Compare("1", CompareOp.EQ, "2")
        val r = engine(this).exec(Step.RepeatUntil("u", never, listOf(set("a", "1")), maxIterations = 50))
        assertEquals(ExecutionStatus.FAILED, r.status)
        assertTrue(r.message.contains("50"))
    }

    @Test fun stepLimitStopsRunawayWorkflows() = runTest {
        val e = engine(this, WorkflowEngine.Limits(maxSteps = 20))
        val never = Condition.Compare("1", CompareOp.EQ, "2")
        val r = e.exec(Step.RepeatUntil("u", never, listOf(set("a", "1")), maxIterations = 100_000))
        assertEquals(ExecutionStatus.FAILED, r.status)
        assertTrue(r.message.contains("Step limit"))
    }

    @Test fun unknownActionFails() = runTest {
        val r = engine(this).exec(act("does_not_exist"))
        assertEquals(ExecutionStatus.FAILED, r.status)
        assertTrue(r.message.contains("Unknown action"))
    }

    @Test fun missingRequiredParameterFails() = runTest {
        val r = engine(this).exec(act("set_variable"))
        assertEquals(ExecutionStatus.FAILED, r.status)
        assertTrue(r.message.contains("required"))
    }

    @Test fun badNumberFails() = runTest {
        val r = engine(this).exec(act("wait", "seconds" to "abc"))
        assertEquals(ExecutionStatus.FAILED, r.status)
    }

    @Test fun continueOnErrorKeepsGoing() = runTest {
        val r = engine(this).exec(act("wait", "seconds" to "abc", "continueOnError" to "true"), set("after", "yes"))
        assertEquals(ExecutionStatus.SUCCESS, r.status)
        assertEquals("yes", r.variables["after"]?.asText())
    }

    @Test fun outputVariableStoresActionResult() = runTest {
        val p = FakePlatform().apply { battery = app.monishort.automation.BatteryStatus(42, false) }
        val r = engine(this).exec(act("get_battery", "outputVar" to "b"), set("msg", "level {{b.level}}"), platform = p)
        assertEquals("level 42", r.variables["msg"]?.asText())
        assertEquals("42", r.variables["battery_level"]?.asText())
    }

    @Test fun platformActionsReachThePlatform() = runTest {
        val p = FakePlatform()
        val r = engine(this).exec(
            act("show_notification", "title" to "Hi", "text" to "There"),
            act("play_sound", "uri" to "content://x/1", "volume" to "50"),
            act("stop_sound"),
            act("open_url", "url" to "https://example.com"),
            platform = p,
        )
        assertEquals(ExecutionStatus.SUCCESS, r.status)
        assertEquals(listOf("Hi" to "There"), p.notifications)
        assertEquals(listOf("content://x/1"), p.played)
        assertEquals(1, p.stopCount)
        assertEquals(listOf("https://example.com"), p.opened)
    }

    @Test fun openUrlRejectsOtherSchemes() = runTest {
        val r = engine(this).exec(act("open_url", "url" to "file:///etc/passwd"))
        assertEquals(ExecutionStatus.FAILED, r.status)
    }

    @Test fun inputIsAvailableToWorkflow() = runTest {
        val e = engine(this)
        val r = e.run("id", "t", Workflow(listOf(set("copy", "{{input.level}}"))), FakePlatform(),
            Value.Dict(mapOf("level" to Value.Text("15"))))
        assertEquals("15", r.variables["copy"]?.asText())
    }

    @Test fun cancellationProducesCancelledResult() = runTest {
        val e = engine(this)
        val results = mutableListOf<ExecutionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { e.results.collect { results += it } }
        val id = e.start("t", Workflow(listOf(act("wait", "seconds" to "60"))), FakePlatform())
        runCurrent()
        assertTrue(e.runningIds.contains(id))
        e.cancel(id)
        runCurrent()
        assertEquals(1, results.size)
        assertEquals(ExecutionStatus.CANCELLED, results[0].status)
        assertTrue(e.runningIds.isEmpty())
    }

    @Test fun concurrentExecutionsAreIndependent() = runTest {
        val e = engine(this)
        val results = mutableListOf<ExecutionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { e.results.collect { results += it } }
        repeat(6) { i -> e.start("run$i", Workflow(listOf(act("wait", "seconds" to "1"), set("v", "$i"))), FakePlatform()) }
        testScheduler.advanceUntilIdle()
        assertEquals(6, results.size)
        assertTrue(results.all { it.status == ExecutionStatus.SUCCESS })
        assertEquals(6, results.map { it.variables["v"]?.asText() }.toSet().size)
    }
}
