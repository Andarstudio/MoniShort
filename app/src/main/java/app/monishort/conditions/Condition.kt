package app.monishort.conditions

import app.monishort.automation.ExecutionContext

enum class CompareOp { EQ, NEQ, GT, GTE, LT, LTE, CONTAINS, STARTS_WITH, ENDS_WITH, IS_EMPTY, IS_NOT_EMPTY }

/** Structured, serializable conditions. No arbitrary code is ever evaluated. */
sealed class Condition {
    abstract fun evaluate(ctx: ExecutionContext): Boolean

    /** Compares two templated texts; numerically when both parse as numbers, otherwise as text. */
    data class Compare(val left: String, val op: CompareOp, val right: String = "") : Condition() {
        override fun evaluate(ctx: ExecutionContext): Boolean {
            val l = ctx.resolve(left)
            val r = ctx.resolve(right)
            val ln = l.trim().toDoubleOrNull()
            val rn = r.trim().toDoubleOrNull()
            val cmp = if (ln != null && rn != null) ln.compareTo(rn) else l.compareTo(r)
            return when (op) {
                CompareOp.EQ -> cmp == 0
                CompareOp.NEQ -> cmp != 0
                CompareOp.GT -> cmp > 0
                CompareOp.GTE -> cmp >= 0
                CompareOp.LT -> cmp < 0
                CompareOp.LTE -> cmp <= 0
                CompareOp.CONTAINS -> l.contains(r, ignoreCase = true)
                CompareOp.STARTS_WITH -> l.startsWith(r, ignoreCase = true)
                CompareOp.ENDS_WITH -> l.endsWith(r, ignoreCase = true)
                CompareOp.IS_EMPTY -> l.isEmpty()
                CompareOp.IS_NOT_EMPTY -> l.isNotEmpty()
            }
        }
    }

    /** True when the templated text is "true" / a non-zero number / non-empty (and not "false"). */
    data class Truthy(val expr: String) : Condition() {
        override fun evaluate(ctx: ExecutionContext): Boolean {
            val t = ctx.resolve(expr).trim()
            if (t.isEmpty() || t.equals("false", ignoreCase = true)) return false
            val n = t.toDoubleOrNull()
            return n == null || n != 0.0
        }
    }

    data class And(val items: List<Condition>) : Condition() {
        override fun evaluate(ctx: ExecutionContext) = items.all { it.evaluate(ctx) }
    }

    data class Or(val items: List<Condition>) : Condition() {
        override fun evaluate(ctx: ExecutionContext) = items.any { it.evaluate(ctx) }
    }

    data class Not(val item: Condition) : Condition() {
        override fun evaluate(ctx: ExecutionContext) = !item.evaluate(ctx)
    }
}
