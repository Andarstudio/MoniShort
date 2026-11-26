package app.monishort.automation

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

enum class ExecutionStatus { SUCCESS, FAILED, CANCELLED }

data class ExecutionResult(
    val executionId: String,
    val name: String,
    val status: ExecutionStatus,
    val message: String,
    val startedAt: Long,
    val finishedAt: Long,
    val log: List<String>,
    val output: Value,
    val variables: Map<String, Value>,
)

class WorkflowEngine(
    private val registry: ActionRegistry,
    private val scope: CoroutineScope,
    private val limits: Limits = Limits(),
) {
    data class Limits(
        val maxSteps: Int = 10_000,
        val timeoutMs: Long = 10 * 60_000L,
        val maxConcurrent: Int = 4,
    )

    private val jobs = ConcurrentHashMap<String, Job>()
    private val permits = Semaphore(limits.maxConcurrent)
    private val _results = MutableSharedFlow<ExecutionResult>(extraBufferCapacity = 64)

    /** Emits one result per finished execution (success, failure or cancellation). */
    val results: SharedFlow<ExecutionResult> = _results.asSharedFlow()

    val runningIds: Set<String> get() = jobs.keys.toSet()

    /** Starts an execution in the background. At most [Limits.maxConcurrent] run at once; others queue. */
    fun start(name: String, workflow: Workflow, platform: PlatformServices, input: Value = Value.Nil): String {
        val id = UUID.randomUUID().toString()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            permits.withPermit {
                _results.tryEmit(run(id, name, workflow, platform, input))
            }
        }
        jobs[id] = job
        job.invokeOnCompletion { jobs.remove(id) }
        job.start()
        return id
    }

    fun cancel(executionId: String) { jobs[executionId]?.cancel() }
    fun cancelAll() { jobs.values.forEach { it.cancel() } }

    /** Runs a workflow to completion and returns the result. Never throws for workflow errors. */
    suspend fun run(
        executionId: String,
        name: String,
        workflow: Workflow,
        platform: PlatformServices,
        input: Value = Value.Nil,
    ): ExecutionResult {
        val ctx = ExecutionContext(executionId, input, platform)
        val startedAt = System.currentTimeMillis()
        val counter = intArrayOf(0)
        var status = ExecutionStatus.SUCCESS
        var message = "Completed"
        try {
            withTimeout(limits.timeoutMs) { execute(workflow.steps, ctx, counter) }
        } catch (e: TimeoutCancellationException) {
            status = ExecutionStatus.FAILED
            message = "Timed out after ${limits.timeoutMs / 1000}s"
        } catch (e: CancellationException) {
            // The result must still be reported, so cancellation is turned into a CANCELLED result here.
            status = ExecutionStatus.CANCELLED
            message = "Cancelled"
        } catch (e: ActionException) {
            status = ExecutionStatus.FAILED
            message = e.message ?: "Action failed"
        } catch (e: ExecutionLimitException) {
            status = ExecutionStatus.FAILED
            message = e.message ?: "Limit exceeded"
        } catch (e: Exception) {
            status = ExecutionStatus.FAILED
            message = e.message ?: e::class.java.simpleName
        }
        return ExecutionResult(
            executionId, name, status, message, startedAt, System.currentTimeMillis(),
            ctx.log.toList(), ctx.output, ctx.snapshot(),
        )
    }

    private suspend fun execute(steps: List<Step>, ctx: ExecutionContext, counter: IntArray) {
        for (step in steps) {
            currentCoroutineContext().ensureActive()
            if (++counter[0] > limits.maxSteps) {
                throw ExecutionLimitException("Step limit of ${limits.maxSteps} exceeded")
            }
            when (step) {
                is Step.Action -> runAction(step, ctx)
                is Step.If ->
                    if (step.condition.evaluate(ctx)) execute(step.thenSteps, ctx, counter)
                    else execute(step.elseSteps, ctx, counter)
                is Step.Repeat -> {
                    val n = ctx.resolve(step.count).trim().toDoubleOrNull()?.toInt()
                        ?: throw ActionException("Repeat count must be a number")
                    if (n < 0 || n > limits.maxSteps) throw ActionException("Repeat count must be between 0 and ${limits.maxSteps}")
                    for (i in 1..n) {
                        currentCoroutineContext().ensureActive()
                        ctx.set("repeat_index", Value.Num(i.toDouble()))
                        execute(step.body, ctx, counter)
                    }
                }
                is Step.RepeatUntil -> {
                    var i = 0
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        if (++i > step.maxIterations) {
                            throw ExecutionLimitException("Repeat Until exceeded ${step.maxIterations} iterations")
                        }
                        ctx.set("repeat_index", Value.Num(i.toDouble()))
                        execute(step.body, ctx, counter)
                        if (step.condition.evaluate(ctx)) break
                    }
                }
            }
        }
    }

    private suspend fun runAction(step: Step.Action, ctx: ExecutionContext) {
        val continueOnError = step.params["continueOnError"].equals("true", ignoreCase = true)
        try {
            val def = registry.get(step.type) ?: throw ActionException("Unknown action '${step.type}'")
            val resolved = step.params.mapValues { ctx.resolve(it.value) }
            val errors = def.validate(resolved)
            if (errors.isNotEmpty()) throw ActionException("${def.title}: ${errors.joinToString("; ")}")
            val result = def.execute(ctx, resolved)
            ctx.output = result
            resolved["outputVar"]?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                if (!VAR_NAME.matches(name)) throw ActionException("Output variable '$name' is not a valid name")
                ctx.set(name, result)
            }
            ctx.log("${def.title}: OK")
        } catch (e: ActionException) {
            if (!continueOnError) throw e
            ctx.log("${step.type}: ${e.message} (continued)")
        }
    }
}
