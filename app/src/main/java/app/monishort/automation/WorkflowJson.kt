package app.monishort.automation

import app.monishort.conditions.CompareOp
import app.monishort.conditions.Condition
import org.json.JSONArray
import org.json.JSONObject

/** Persistence format for workflows: plain structured JSON, never executable code. */
object WorkflowJson {
    private const val VERSION = 1

    fun encode(w: Workflow): String =
        JSONObject().put("version", VERSION).put("steps", encodeSteps(w.steps)).toString()

    fun decode(json: String): Workflow {
        val root = JSONObject(json)
        return Workflow(decodeSteps(root.getJSONArray("steps")))
    }

    fun encodeMap(m: Map<String, String>): String = mapObject(m).toString()

    fun decodeMap(s: String): Map<String, String> {
        if (s.isBlank()) return emptyMap()
        val o = JSONObject(s)
        return o.keys().asSequence().associateWith { o.optString(it) }
    }

    private fun mapObject(m: Map<String, String>): JSONObject {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        return o
    }

    private fun encodeSteps(steps: List<Step>): JSONArray {
        val a = JSONArray()
        steps.forEach { a.put(encodeStep(it)) }
        return a
    }

    private fun encodeStep(s: Step): JSONObject = when (s) {
        is Step.Action -> JSONObject().put("kind", "action").put("id", s.id)
            .put("type", s.type).put("params", mapObject(s.params))
        is Step.If -> JSONObject().put("kind", "if").put("id", s.id)
            .put("condition", encodeCondition(s.condition))
            .put("then", encodeSteps(s.thenSteps)).put("else", encodeSteps(s.elseSteps))
        is Step.Repeat -> JSONObject().put("kind", "repeat").put("id", s.id)
            .put("count", s.count).put("body", encodeSteps(s.body))
        is Step.RepeatUntil -> JSONObject().put("kind", "repeat_until").put("id", s.id)
            .put("condition", encodeCondition(s.condition)).put("body", encodeSteps(s.body))
            .put("max", s.maxIterations)
    }

    private fun decodeSteps(a: JSONArray): List<Step> = (0 until a.length()).map { decodeStep(a.getJSONObject(it)) }

    private fun decodeStep(o: JSONObject): Step {
        val id = o.getString("id")
        return when (val kind = o.getString("kind")) {
            "action" -> Step.Action(id, o.getString("type"), decodeMap(o.optJSONObject("params")?.toString() ?: ""))
            "if" -> Step.If(
                id, decodeCondition(o.getJSONObject("condition")),
                decodeSteps(o.getJSONArray("then")), decodeSteps(o.getJSONArray("else")),
            )
            "repeat" -> Step.Repeat(id, o.getString("count"), decodeSteps(o.getJSONArray("body")))
            "repeat_until" -> Step.RepeatUntil(
                id, decodeCondition(o.getJSONObject("condition")),
                decodeSteps(o.getJSONArray("body")), o.optInt("max", 1000),
            )
            else -> throw IllegalArgumentException("Unknown step kind '$kind'")
        }
    }

    private fun encodeCondition(c: Condition): JSONObject = when (c) {
        is Condition.Compare -> JSONObject().put("kind", "compare").put("left", c.left)
            .put("op", c.op.name).put("right", c.right)
        is Condition.Truthy -> JSONObject().put("kind", "truthy").put("expr", c.expr)
        is Condition.And -> JSONObject().put("kind", "and").put("items", encodeConditions(c.items))
        is Condition.Or -> JSONObject().put("kind", "or").put("items", encodeConditions(c.items))
        is Condition.Not -> JSONObject().put("kind", "not").put("item", encodeCondition(c.item))
    }

    private fun encodeConditions(items: List<Condition>): JSONArray {
        val a = JSONArray()
        items.forEach { a.put(encodeCondition(it)) }
        return a
    }

    private fun decodeConditions(a: JSONArray): List<Condition> =
        (0 until a.length()).map { decodeCondition(a.getJSONObject(it)) }

    private fun decodeCondition(o: JSONObject): Condition = when (val kind = o.getString("kind")) {
        "compare" -> Condition.Compare(o.getString("left"), CompareOp.valueOf(o.getString("op")), o.optString("right", ""))
        "truthy" -> Condition.Truthy(o.getString("expr"))
        "and" -> Condition.And(decodeConditions(o.getJSONArray("items")))
        "or" -> Condition.Or(decodeConditions(o.getJSONArray("items")))
        "not" -> Condition.Not(decodeCondition(o.getJSONObject("item")))
        else -> throw IllegalArgumentException("Unknown condition kind '$kind'")
    }
}
