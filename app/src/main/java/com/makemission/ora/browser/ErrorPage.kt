package com.makemission.ora.browser

import android.net.Uri
import org.mozilla.geckoview.WebRequestError

/**
 * A friendly page shown in place of a failed load. It is built as a data: URI, so it needs no
 * network and no app colours (it is web content, so it follows the system light or dark setting).
 */
object ErrorPage {

    /** Plain-words title and explanation for a failed load. */
    data class Message(val title: String, val detail: String)

    fun messageFor(error: WebRequestError): Message = when (error.code) {
        WebRequestError.ERROR_OFFLINE -> Message(
            "You're offline",
            "This device has no connection right now. Check Wi-Fi or mobile data, then try again."
        )
        WebRequestError.ERROR_NET_TIMEOUT -> Message(
            "The site took too long to answer",
            "The server did not respond in time. It may be busy, or your connection may be slow."
        )
        WebRequestError.ERROR_UNKNOWN_HOST -> Message(
            "The address could not be found",
            "Check the spelling of the address. If it is right, the site may be down or the name may have expired."
        )
        WebRequestError.ERROR_CONNECTION_REFUSED -> Message(
            "The site refused the connection",
            "The server is not accepting visitors right now. Try again in a little while."
        )
        WebRequestError.ERROR_SECURITY_BAD_CERT,
        WebRequestError.ERROR_SECURITY_SSL,
        WebRequestError.ERROR_BAD_HSTS_CERT -> Message(
            "This connection is not private",
            "The site's security certificate could not be verified, so Ora stopped the page. Only continue if you trust the site."
        )
        WebRequestError.ERROR_REDIRECT_LOOP -> Message(
            "The page keeps redirecting",
            "The site sent you around in a loop and never settled on a page."
        )
        WebRequestError.ERROR_MALFORMED_URI -> Message(
            "That address doesn't look right",
            "Check the address for typos, then try again."
        )
        else -> when (error.category) {
            WebRequestError.ERROR_CATEGORY_NETWORK -> Message(
                "The page could not be loaded",
                "There was a problem with the connection. Try again."
            )
            WebRequestError.ERROR_CATEGORY_SECURITY -> Message(
                "This connection is not private",
                "The site's security could not be verified, so Ora stopped the page."
            )
            else -> Message(
                "The page could not be loaded",
                "Something stopped this page from opening. Try again, or check the address."
            )
        }
    }

    /** Builds the data: URI that GeckoView shows instead of the failed page. */
    fun uriFor(failedUrl: String, error: WebRequestError): String {
        val message = messageFor(error)
        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            append("<title>").append(escape(message.title)).append("</title>")
            append("<style>")
            append(":root{color-scheme:light dark;--fg:#1c1c1e;--muted:#6b6b72;--card:#f2f2f6;--btn:#3b6df0;--btnfg:#fff}")
            append("@media (prefers-color-scheme:dark){:root{--fg:#ecedf0;--muted:#9097a6;--card:#1f2026;--btn:#6c93ff;--btnfg:#0b0c10}}")
            append("body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;font-family:system-ui,sans-serif;color:var(--fg);background:transparent;padding:24px;box-sizing:border-box}")
            append(".card{max-width:440px;width:100%;background:var(--card);border-radius:16px;padding:28px 24px;text-align:center}")
            append("h1{font-size:22px;margin:0 0 10px;font-weight:600}")
            append("p{font-size:15px;line-height:1.45;color:var(--muted);margin:0 0 18px}")
            append(".addr{font-size:13px;color:var(--muted);word-break:break-all;margin:0 0 20px}")
            append("a.btn{display:inline-block;background:var(--btn);color:var(--btnfg);text-decoration:none;font-weight:600;padding:12px 22px;border-radius:999px}")
            append("</style></head><body><div class=\"card\">")
            append("<h1>").append(escape(message.title)).append("</h1>")
            append("<p>").append(escape(message.detail)).append("</p>")
            append("<p class=\"addr\">").append(escape(failedUrl)).append("</p>")
            // The button reloads the same address through the normal load path.
            append("<a class=\"btn\" href=\"").append(escape(failedUrl)).append("\">Try again</a>")
            append("</div></body></html>")
        }
        return "data:text/html;charset=utf-8," + Uri.encode(html)
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
