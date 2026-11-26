package app.monishort.automation

import kotlin.math.abs

/** Typed value used by variables, action results and inputs. */
sealed class Value {
    data class Text(val v: String) : Value()
    data class Num(val v: Double) : Value()
    data class Bool(val v: Boolean) : Value()
    data class ListV(val v: List<Value>) : Value()
    data class Dict(val v: Map<String, Value>) : Value()
    object Nil : Value()

    fun asText(): String = when (this) {
        is Text -> v
        is Num -> if (v == Math.floor(v) && !v.isInfinite() && abs(v) < 1e15) v.toLong().toString() else v.toString()
        is Bool -> v.toString()
        is ListV -> v.joinToString(", ") { it.asText() }
        is Dict -> v.entries.joinToString(", ") { "${it.key}: ${it.value.asText()}" }
        Nil -> ""
    }

    fun asNumber(): Double? = when (this) {
        is Num -> v
        is Text -> v.trim().toDoubleOrNull()
        is Bool -> if (v) 1.0 else 0.0
        else -> null
    }

    fun isTruthy(): Boolean = when (this) {
        is Bool -> v
        is Num -> v != 0.0
        is Text -> v.isNotEmpty() && !v.equals("false", ignoreCase = true)
        is ListV -> v.isNotEmpty()
        is Dict -> v.isNotEmpty()
        Nil -> false
    }
}
