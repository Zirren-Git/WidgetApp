package com.zirren.tessera.core.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** The one network primitive Tessera needs. Tests swap in a fake. */
fun interface HttpTransport {
    @Throws(IOException::class)
    fun get(url: String, headers: Map<String, String>): HttpResponse
}

class HttpResponse(
    val code: Int,
    headers: Map<String, List<String>>,
    val body: String?,
) {
    private val headers: Map<String, List<String>> = headers.mapKeys { it.key.lowercase() }

    fun header(name: String): String? = headers[name.lowercase()]?.firstOrNull()

    override fun toString(): String = "HttpResponse(code=$code, bodyLength=${body?.length ?: 0})"
}

/**
 * [HttpURLConnection]-based transport. Works on Android and on a plain JVM, so the
 * core module needs no HTTP library. Android transparently un-gzips responses.
 * Blocking: call it from a background thread.
 */
class UrlConnectionTransport(
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 20_000,
) : HttpTransport {

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.useCaches = false
            connection.instanceFollowRedirects = true
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

            val code = connection.responseCode
            val stream = when {
                code == HttpURLConnection.HTTP_NOT_MODIFIED || code == HttpURLConnection.HTTP_NO_CONTENT -> null
                code >= 400 -> connection.errorStream
                else -> connection.inputStream
            }
            val body = stream?.use { it.readBytes().toString(Charsets.UTF_8) }
            val responseHeaders = buildMap {
                for ((name, values) in connection.headerFields) {
                    if (name != null) put(name, values)
                }
            }
            return HttpResponse(code, responseHeaders, body)
        } finally {
            connection.disconnect()
        }
    }
}
