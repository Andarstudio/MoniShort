package app.monishort

import app.monishort.automation.ExecutionContext
import app.monishort.automation.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueAndContextTest {
    private fun ctx() = ExecutionContext("1", Value.Text("in"), FakePlatform())

    @Test fun numbersPrintWithoutTrailingZero() {
        assertEquals("5", Value.Num(5.0).asText())
        assertEquals("2.5", Value.Num(2.5).asText())
    }

    @Test fun conversions() {
        assertEquals(3.0, Value.Text(" 3 ").asNumber()!!, 0.0)
        assertNull(Value.Text("abc").asNumber())
        assertEquals(1.0, Value.Bool(true).asNumber()!!, 0.0)
        assertEquals("a, b", Value.ListV(listOf(Value.Text("a"), Value.Text("b"))).asText())
    }

    @Test fun truthiness() {
        assertTrue(Value.Text("x").isTruthy())
        assertFalse(Value.Text("false").isTruthy())
        assertFalse(Value.Num(0.0).isTruthy())
        assertFalse(Value.Nil.isTruthy())
        assertFalse(Value.ListV(emptyList()).isTruthy())
    }

    @Test fun variablesAndTemplates() {
        val c = ctx()
        c.set("name", Value.Text("Moni"))
        c.set("n", Value.Num(3.0))
        assertEquals("Hi Moni #3", c.resolve("Hi {{name}} #{{ n }}"))
        assertEquals("", c.resolve("{{missing}}"))
        assertEquals("in", c.resolve("{{input}}"))
    }

    @Test fun nestedLookup() {
        val c = ctx()
        c.set("d", Value.Dict(mapOf("level" to Value.Num(42.0), "tags" to Value.ListV(listOf(Value.Text("x"), Value.Text("y"))))))
        assertEquals("42", c.resolve("{{d.level}}"))
        assertEquals("y", c.resolve("{{d.tags.1}}"))
        assertEquals("", c.resolve("{{d.nope}}"))
    }
}
