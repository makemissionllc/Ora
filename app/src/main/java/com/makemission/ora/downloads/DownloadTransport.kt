package com.makemission.ora.downloads

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** One HTTP response as the engine uses it. [close] ends the body at once, even while a read is blocked. */
class RemoteResponse(
    val code: Int,
    private val headers: Map<String, String>,
    val body: InputStream,
    private val onClose: () -> Unit,
) : AutoCloseable {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    override fun close() = onClose()
}

/**
 * Makes the requests the engine needs. The app sends them through GeckoWebExecutor, so cookies and logins
 * match the tab; tests send them to a MockWebServer through a plain connection.
 */
interface DownloadTransport {
    /** Sends a GET with [headers] and returns once the status and headers have arrived. Blocking; call on IO. */
    fun get(url: String, headers: Map<String, String>): RemoteResponse
}

/** Plain HttpURLConnection transport, used by host tests. Not used by the app. */
class HttpUrlTransport : DownloadTransport {
    override fun get(url: String, headers: Map<String, String>): RemoteResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Accept-Encoding", "identity")
        headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
        val code = conn.responseCode
        val map = conn.headerFields.filterKeys { it != null }.mapValues { it.value.lastOrNull().orEmpty() }
            .mapKeys { it.key }
        val body = runCatching { conn.inputStream }.getOrElse { conn.errorStream ?: InputStream.nullInputStream() }
        return RemoteResponse(code, map, body) { conn.disconnect() }
    }
}
