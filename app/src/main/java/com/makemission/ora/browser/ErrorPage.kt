package com.makemission.ora.browser

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.WebRequestError

/**
 * The page shown in place of a failed load: one card per cause, with an icon, a plain title, one reason
 * sentence, its buttons, and a folded "Details for support" block. It is built as a data: URI, so it needs no
 * network. Its colours are the Mineral values written into its CSS, because it is web content built outside
 * Compose. It follows the system light or dark setting.
 */
object ErrorPage {

    /** The icon on the card. Each cause has its own. */
    enum class Icon { OFFLINE, NOT_FOUND, TIMEOUT, SECURITY, GENERIC }

    /** A card button. TRY_AGAIN loads the failed address again; GO_BACK returns to the page before it. */
    enum class Action { TRY_AGAIN, GO_BACK }

    /**
     * One card for one cause. [title] and [detail] are plain words. [support] is the short cause shown after
     * "Details for support:", and [category] names the GeckoView category in the support block.
     * [secondary] is null when the card has one button.
     */
    /**
     * What "Open anyway" does for a blocked page, always for this one load only.
     * HTTP_ONCE loads the plain-HTTP page with HTTPS-only paused until it finishes. RISK_ONCE loads a page Ora's
     * safe-browsing list flagged. CERT hands the page to the engine's own warning, which asks again before continuing.
     */
    enum class Allow(val key: String) { HTTP_ONCE("http"), RISK_ONCE("risk"), CERT("cert") }

    /** The link scheme the card's Open anyway button uses. The tab catches it; it is never loaded. */
    const val ALLOW_SCHEME = "ora-allow"

    /** Whether the cards offer Open anyway. Set from the "Offer to open blocked sites" setting. */
    @Volatile var offerAllow: Boolean = true

    data class Message(
        val icon: Icon,
        val title: String,
        val detail: String,
        val support: String,
        val category: String,
        val primary: Action,
        val secondary: Action?,
        /** The one-time override this cause allows, or null when none is safe to offer. */
        val allow: Allow? = null,
    )

    fun messageFor(error: WebRequestError): Message = messageFor(error.code, error.category)

    /**
     * The card for a GeckoView load error. [online] is the connectivity check: with no network the card says so,
     * whatever GeckoView reported. [host] is the domain only, and goes into titles and the support block.
     * The log uses the same words, so the log and the page agree.
     */
    internal fun messageFor(code: Int, category: Int, host: String? = null, online: Boolean = true): Message {
        val name = categoryName(category)
        val site = host?.takeIf { it.isNotBlank() }
        if (!online) return offline(name)
        return when (code) {
            WebRequestError.ERROR_OFFLINE -> offline(name)
            WebRequestError.ERROR_NET_TIMEOUT -> Message(
                Icon.TIMEOUT,
                if (site != null) "$site isn't responding" else "The site isn't responding",
                "Your connection works, but the site took too long to answer. It may be busy or down for a while.",
                "connection timed out", name, Action.TRY_AGAIN, Action.GO_BACK
            )
            WebRequestError.ERROR_UNKNOWN_HOST -> Message(
                Icon.NOT_FOUND,
                if (site != null) "Can't find $site" else "The address could not be found",
                "Check the spelling. If it's right, the site may be down or its name may have expired.",
                "name not resolved", name, Action.TRY_AGAIN, Action.GO_BACK
            )
            WebRequestError.ERROR_CONNECTION_REFUSED -> Message(
                Icon.GENERIC,
                "The site refused the connection",
                "The server is not accepting visitors right now. Try again in a little while.",
                "connection refused", name, Action.TRY_AGAIN, Action.GO_BACK
            )
            WebRequestError.ERROR_HTTPS_ONLY -> Message(
                Icon.SECURITY,
                if (site != null) "$site has no secure version" else "This site has no secure version",
                "HTTPS-only mode is on, and this site only answers over plain HTTP, so anything you send could be read on the way. Open it only if you are not entering passwords or personal details.",
                "https only", name, Action.GO_BACK, null, Allow.HTTP_ONCE
            )
            WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI,
            WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> flagged(name, "This site may harm your device", "malware list", site)
            WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> flagged(name, "This site may be a scam", "phishing list", site)
            WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> flagged(name, "This site may install unwanted software", "unwanted software list", site)
            WebRequestError.ERROR_SECURITY_BAD_CERT,
            WebRequestError.ERROR_SECURITY_SSL -> insecure(name, "certificate", Allow.CERT)
            WebRequestError.ERROR_BAD_HSTS_CERT -> insecure(name, "certificate")
            WebRequestError.ERROR_REDIRECT_LOOP -> Message(
                Icon.GENERIC,
                "The page keeps redirecting",
                "The site sent you around in a loop and never settled on a page.",
                "redirect loop", name, Action.TRY_AGAIN, Action.GO_BACK
            )
            WebRequestError.ERROR_PORT_BLOCKED -> Message(
                Icon.GENERIC,
                "This address uses a blocked port",
                "Browsers keep this port closed for safety, so Ora didn't open it. Check the address, then try again.",
                "port blocked", name, Action.TRY_AGAIN, Action.GO_BACK
            )
            WebRequestError.ERROR_MALFORMED_URI -> badAddress(name)
            else -> when (category) {
                WebRequestError.ERROR_CATEGORY_URI -> badAddress(name)
                WebRequestError.ERROR_CATEGORY_PROXY -> Message(
                    Icon.GENERIC,
                    "A proxy stopped the page",
                    "The connection goes through a proxy that refused the request. Check the proxy settings on this device.",
                    "proxy refused", name, Action.TRY_AGAIN, Action.GO_BACK
                )
                WebRequestError.ERROR_CATEGORY_NETWORK -> Message(
                    Icon.GENERIC,
                    "The page could not be loaded",
                    "There was a problem with the connection. Try again.",
                    "network error", name, Action.TRY_AGAIN, Action.GO_BACK
                )
                WebRequestError.ERROR_CATEGORY_SAFEBROWSING -> flagged(name, "This site may be harmful", "safe browsing", site)
                WebRequestError.ERROR_CATEGORY_SECURITY -> insecure(name, "error")
                else -> Message(
                    Icon.GENERIC,
                    "The page could not be loaded",
                    "Something stopped this page from opening. Try again, or check the address.",
                    "unknown error", name, Action.TRY_AGAIN, Action.GO_BACK
                )
            }
        }
    }

    private fun offline(category: String) = Message(
        Icon.OFFLINE,
        "You're offline",
        "Your phone isn't connected to the internet, so Ora can't open pages right now.",
        "no network", category, Action.TRY_AGAIN, Action.GO_BACK
    )

    private fun flagged(category: String, title: String, support: String, site: String?) = Message(
        Icon.SECURITY,
        title,
        "${if (site != null) "$site is" else "This page is"} on a list of harmful sites, so Ora stopped it before anything loaded. If you are sure it is safe, you can open it this once.",
        support, category, Action.GO_BACK, null, Allow.RISK_ONCE
    )

    private fun insecure(category: String, support: String, allow: Allow? = null) = Message(
        Icon.SECURITY,
        "This connection is not private",
        "Ora stopped the page because the site's security could not be verified, so what you send to it could be seen or changed.",
        "security $support", category, Action.GO_BACK, null, allow
    )

    private fun badAddress(category: String) = Message(
        Icon.GENERIC,
        "That address doesn't look right",
        "Check the address for typos, then try again.",
        "address not valid", category, Action.TRY_AGAIN, Action.GO_BACK
    )

    private fun categoryName(category: Int): String = when (category) {
        WebRequestError.ERROR_CATEGORY_NETWORK -> "network"
        WebRequestError.ERROR_CATEGORY_SECURITY -> "security"
        WebRequestError.ERROR_CATEGORY_URI -> "address"
        WebRequestError.ERROR_CATEGORY_PROXY -> "proxy"
        else -> "other"
    }

    /**
     * True when the device has an internet connection, from ConnectivityManager. If the check cannot run,
     * the device counts as online, so a failed check never hides the real error.
     */
    fun isOnline(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: SecurityException) {
            BrowserLog.warn("page", "Connection check could not run, so the error is shown as GeckoView gave it", e)
            true
        }
    }

    /** Builds the data: URI that GeckoView shows instead of the failed page. [host] is the domain only. */
    fun uriFor(failedUrl: String, error: WebRequestError, host: String, online: Boolean): String {
        val message = messageFor(error.code, error.category, host, online)
        val retry = failedUrl.startsWith("http://") || failedUrl.startsWith("https://")
        val domain = host.ifBlank { "unknown" }
        val connection = if (online) "connected" else "offline"
        val copyText = listOf(
            "Ora: ${message.title}",
            "Category: ${message.category}",
            "Code: ${error.code}",
            "Domain: $domain",
            "Connection: $connection"
        ).joinToString("\n")
        val iconClass = if (message.icon == Icon.SECURITY) "icon alert" else "icon"
        val html = buildString {
            append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            append("<title>").append(escape(message.title)).append("</title>")
            append("<style>").append(STYLE).append("</style></head><body><main class=\"page\">")
            append("<span class=\"").append(iconClass).append("\">").append(svg(iconPaths(message.icon), 30)).append("</span>")
            append("<h1>").append(escape(message.title)).append("</h1>")
            append("<p class=\"reason\">").append(escape(message.detail)).append("</p>")
            append("<div class=\"actions\">")
            append(actionHtml(message.primary, failedUrl, retry, "primary"))
            message.secondary?.let { append(actionHtml(it, failedUrl, retry, "secondary")) }
            // The override is one tap away but never the first choice: it sits last, in the warning colour, and applies once.
            val allow = message.allow
            if (offerAllow && allow != null && retry) {
                append("<a class=\"btn danger\" href=\"").append(ALLOW_SCHEME).append("://").append(allow.key).append("?u=")
                    .append(escape(Uri.encode(failedUrl))).append("\">Open anyway, this time only</a>")
            }
            append("</div>")
            append("<details class=\"support\"><summary>").append(svg(CHEVRON, 16))
            append("<span>Details for support: ").append(escape(message.support)).append("</span></summary>")
            append("<dl><dt>Category</dt><dd>").append(escape(message.category)).append("</dd>")
            append("<dt>Code</dt><dd>").append(error.code).append("</dd>")
            append("<dt>Domain</dt><dd>").append(escape(domain)).append("</dd>")
            append("<dt>Connection</dt><dd>").append(connection).append("</dd></dl>")
            append("<button class=\"btn copy\" type=\"button\" id=\"copy\">Copy details</button>")
            append("</details>")
            append("<textarea id=\"details\" readonly tabindex=\"-1\" aria-hidden=\"true\">")
                .append(escape(copyText)).append("</textarea>")
            append("</main><script>").append(SCRIPT).append("</script></body></html>")
        }
        return "data:text/html;charset=utf-8," + Uri.encode(html)
    }

    private fun actionHtml(action: Action, failedUrl: String, retry: Boolean, style: String): String = when (action) {
        Action.TRY_AGAIN ->
            if (retry) "<a class=\"btn $style\" href=\"${escape(failedUrl)}\">Try again</a>" else ""
        Action.GO_BACK ->
            "<button class=\"btn $style\" type=\"button\" data-back>Go back</button>"
    }

    private fun iconPaths(icon: Icon): String = when (icon) {
        Icon.OFFLINE -> "<path d=\"M2 9a15 15 0 0 1 6-3.6\"/><path d=\"M22 9a15 15 0 0 0-9.5-4\"/>" +
            "<path d=\"M5.5 12.5a10 10 0 0 1 4-2.3\"/><path d=\"M18.5 12.5a10 10 0 0 0-2.2-1.6\"/>" +
            "<path d=\"M9 16a4 4 0 0 1 6 0\"/><path d=\"M12 19.5h.01\"/><path d=\"M3 3l18 18\"/>"
        Icon.NOT_FOUND -> "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M3 12h18\"/>" +
            "<path d=\"M12 3c3 3 3 15 0 18\"/><path d=\"M12 3c-3 3-3 15 0 18\"/>"
        Icon.TIMEOUT -> "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>"
        Icon.SECURITY -> "<rect x=\"5\" y=\"11\" width=\"14\" height=\"10\" rx=\"2\"/><path d=\"M8 11V7a4 4 0 0 1 7.5-2\"/>"
        Icon.GENERIC -> "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 8v5\"/><path d=\"M12 16h.01\"/>"
    }

    private fun svg(paths: String, size: Int): String =
        "<svg aria-hidden=\"true\" width=\"$size\" height=\"$size\" viewBox=\"0 0 24 24\" fill=\"none\" " +
            "stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\">$paths</svg>"

    private const val CHEVRON = "<path d=\"M5 9l7 7 7-7\"/>"

    /** Mineral values: Basalt, Slate, Shale, Chalk and Fog in dark; Paper, Stone and Ink in light. Ember marks the security card. */
    private const val STYLE = """
:root{--bg:#1A1B1E;--card:#232528;--quiet:#2D3034;--fg:#ECE6DC;--fg2:#A29D94;--action:#ECE6DC;--action-fg:#1A1B1E;--alert:#E58A6B}
@media (prefers-color-scheme: light){:root{--bg:#F6F5F2;--card:#E9E8E4;--quiet:#DDDBD5;--fg:#1A1B1E;--fg2:#6C6962;--action:#1A1B1E;--action-fg:#F6F5F2;--alert:#1A1B1E}}
*{box-sizing:border-box}
html,body{margin:0;background:var(--bg);color:var(--fg);font-family:system-ui,-apple-system,sans-serif}
.page{min-height:100vh;max-width:420px;margin:0 auto;padding:96px 20px 40px;display:flex;flex-direction:column;gap:16px}
.icon{width:64px;height:64px;border-radius:32px;background:var(--card);color:var(--fg);display:flex;align-items:center;justify-content:center;margin-bottom:6px}
.icon.alert{color:var(--alert)}
h1{margin:0;font-size:28px;line-height:1.15;font-weight:600;letter-spacing:-.5px}
.reason{margin:0;font-size:16px;line-height:1.5;color:var(--fg2)}
.actions{display:flex;flex-direction:column;gap:10px;margin-top:8px}
.btn{display:flex;align-items:center;justify-content:center;border:0;border-radius:26px;font:inherit;font-weight:600;text-decoration:none;cursor:pointer;transition:transform 160ms ease;padding:0 20px}
.btn:active{transform:scale(.94)}
.btn:disabled{opacity:.45;cursor:default;transform:none}
.primary{height:54px;border-radius:27px;background:var(--action);color:var(--action-fg);font-size:16px}
.secondary{height:52px;background:var(--quiet);color:var(--fg);font-size:15px}
.danger{height:52px;background:transparent;color:var(--alert);font-size:15px;box-shadow:inset 0 0 0 1.5px var(--alert)}
.support{margin-top:8px}
.support summary{list-style:none;min-height:44px;display:flex;align-items:center;gap:6px;color:var(--fg2);font-size:14px;cursor:pointer}
.support summary::-webkit-details-marker{display:none}
.support summary svg{flex-shrink:0;transition:transform 160ms ease}
.support[open] summary svg{transform:rotate(180deg)}
dl{display:grid;grid-template-columns:auto 1fr;gap:6px 14px;margin:8px 0 14px;font-size:14px;color:var(--fg2)}
dd{margin:0;color:var(--fg);word-break:break-all}
.copy{height:44px;padding:0 18px;background:var(--quiet);color:var(--fg);font-size:14px}
#details{position:absolute;left:-9999px;top:0;width:1px;height:1px}
"""

    /** Enables Go back only when there is a page to return to, and copies the details (the clipboard API, or a selection as a fallback). */
    private const val SCRIPT = """
(function () {
  var copy = document.getElementById('copy');
  var text = document.getElementById('details');
  document.querySelectorAll('[data-back]').forEach(function (button) {
    if (history.length < 2) button.disabled = true;
    button.addEventListener('click', function () { history.back(); });
  });
  if (!copy || !text) return;
  function done(ok) {
    copy.textContent = ok ? 'Copied' : 'Could not copy';
    setTimeout(function () { copy.textContent = 'Copy details'; }, 1500);
  }
  copy.addEventListener('click', function () {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text.value).then(function () { done(true); }, function () { done(false); });
      return;
    }
    text.select();
    var ok = false;
    try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
    done(ok);
  });
})();
"""

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
