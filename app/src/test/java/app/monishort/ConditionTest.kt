package app.monishort

import app.monishort.automation.ExecutionContext
import app.monishort.automation.Value
import app.monishort.conditions.CompareOp
import app.monishort.conditions.Condition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionTest {
    private val ctx = ExecutionContext("1", Value.Nil, FakePlatform()).also {
        it.set("x", Value.Num(10.0))
        it.set("s", Value.Text("Hello World"))
    }

    private fun cmp(l: String, op: CompareOp, r: String = "") = Condition.Compare(l, op, r)

    @Test fun numericComparisonIsNotTextual() {
        assertTrue(cmp("{{x}}", CompareOp.GT, "9").evaluate(ctx)) // text compare would say "10" < "9"
        assertTrue(cmp("{{x}}", CompareOp.EQ, "10.0").evaluate(ctx))
        assertTrue(cmp("{{x}}", CompareOp.LTE, "10").evaluate(ctx))
        assertFalse(cmp("{{x}}", CompareOp.LT, "10").evaluate(ctx))
        assertTrue(cmp("{{x}}", CompareOp.NEQ, "11").evaluate(ctx))
    }

    @Test fun textOperations() {
        assertTrue(cmp("{{s}}", CompareOp.CONTAINS, "world").evaluate(ctx))
        assertTrue(cmp("{{s}}", CompareOp.STARTS_WITH, "hello").evaluate(ctx))
        assertTrue(cmp("{{s}}", CompareOp.ENDS_WITH, "World").evaluate(ctx))
        assertTrue(cmp("{{unset}}", CompareOp.IS_EMPTY).evaluate(ctx))
        assertTrue(cmp("{{s}}", CompareOp.IS_NOT_EMPTY).evaluate(ctx))
    }

    @Test fun booleanOperators() {
        val t = cmp("1", CompareOp.EQ, "1")
        val f = cmp("1", CompareOp.EQ, "2")
        assertTrue(Condition.And(listOf(t, t)).evaluate(ctx))
        assertFalse(Condition.And(listOf(t, f)).evaluate(ctx))
        assertTrue(Condition.Or(listOf(f, t)).evaluate(ctx))
        assertFalse(Condition.Or(listOf(f, f)).evaluate(ctx))
        assertTrue(Condition.Not(f).evaluate(ctx))
    }

    @Test fun truthy() {
        assertTrue(Condition.Truthy("yes").evaluate(ctx))
        assertFalse(Condition.Truthy("false").evaluate(ctx))
        assertFalse(Condition.Truthy("0").evaluate(ctx))
        assertFalse(Condition.Truthy("{{unset}}").evaluate(ctx))
    }
}
