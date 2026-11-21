package com.makemission.ora.downloads

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebResponse

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

/**
 * Sends the requests through GeckoWebExecutor, so cookies, logins, proxy and HTTPS settings match the tab that
 * started the download. Private downloads use FETCH_FLAGS_PRIVATE, so they share no cookies with normal tabs.
 */
class GeckoTransport(
    private val runtime: GeckoRuntime,
    private val isPrivate: Boolean,
    private val referrer: String?,
) : DownloadTransport {

    private val executor by lazy { GeckoWebExecutor(runtime) }

    override fun get(url: String, headers: Map<String, String>): RemoteResponse {
        val builder = WebRequest.Builder(url).cacheMode(WebRequest.CACHE_MODE_NO_STORE)
        headers.forEach { (name, value) -> builder.header(name, value) }
        referrer?.let { builder.referrer(it) }
        val flags = if (isPrivate) GeckoWebExecutor.FETCH_FLAGS_PRIVATE else GeckoWebExecutor.FETCH_FLAGS_NONE
        val response = try {
            executor.fetch(builder.build(), flags).poll(FETCH_TIMEOUT_MS)
        } catch (e: Exception) {
            throw IOException(e.message ?: "The request failed", e)
        } ?: throw IOException("The request returned no response")
        val body = response.body ?: InputStream.nullInputStream()
        return RemoteResponse(response.statusCode, response.headers, body) { runCatching { body.close() } }
    }

    private companion object {
        const val FETCH_TIMEOUT_MS = 60_000L
    }
}

/**
 * Hands the engine the stream a page gave to Ora, for blob: and data: files that no request can reach.
 * It answers once; a second request fails, because the stream is used up.
 */
class PageStreamTransport(private val response: WebResponse) : DownloadTransport {
    private var used = false

    override fun get(url: String, headers: Map<String, String>): RemoteResponse {
        if (used) throw IllegalStateException("This file only exists in the page it came from. Open the page and save it again.")
        used = true
        val body = response.body ?: throw IllegalStateException("The page gave no data for this file")
        return RemoteResponse(200, emptyMap(), body) { runCatching { body.close() } }
    }
}
