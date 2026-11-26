package app.monishort.actions

import app.monishort.automation.ActionDefinition
import app.monishort.automation.ActionException
import app.monishort.automation.ExecutionContext
import app.monishort.automation.ParamKind
import app.monishort.automation.ParamSpec
import app.monishort.automation.Value
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HttpGetAction : ActionDefinition {
    override val type = "http_get"
    override val title = "HTTP GET"
    override val category = "Network"
    override val parameters = listOf(
        ParamSpec("url", "URL (https)", kind = ParamKind.URI),
        ParamSpec("timeoutSeconds", "Timeout seconds (1-60, default 15)", required = false, kind = ParamKind.NUMBER),
    )

    override suspend fun execute(ctx: ExecutionContext, params: Map<String, String>): Value {
        val url = params.getValue("url").trim()
        if (!url.startsWith("https://") && !url.startsWith("http://")) throw ActionException("Only http and https URLs are supported")
        val timeout = (params["timeoutSeconds"]?.trim()?.toDoubleOrNull() ?: 15.0).coerceIn(1.0, 60.0)
        val ms = (timeout * 1000).toInt()
        return withContext(Dispatchers.IO) {
            val conn = try {
                URL(url).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw ActionException("Invalid URL: ${e.message}")
            }
            try {
                conn.connectTimeout = ms
                conn.readTimeout = ms
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = true
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.use { readLimited(it) }.orEmpty()
                if (code !in 200..299) throw ActionException("HTTP $code")
                Value.Dict(mapOf("status" to Value.Num(code.toDouble()), "body" to Value.Text(body)))
            } catch (e: IOException) {
                throw ActionException("Network error: ${e.message ?: e::class.java.simpleName}")
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun readLimited(s: InputStream): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw ActionException("Response is larger than 1 MB")
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }

    private companion object { const val MAX_BYTES = 1_048_576 }
}
