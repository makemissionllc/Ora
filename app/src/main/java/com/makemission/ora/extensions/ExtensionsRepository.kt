package com.makemission.ora.extensions

import android.content.Context
import android.net.Uri
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.notice.InAppNotices
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

/** What the extension manager shows for one installed WebExtension. */
data class ExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val enabled: Boolean,
    val builtIn: Boolean,
    /** Whether the extension also runs in private tabs. Off unless the user turns it on. */
    val allowedInPrivate: Boolean = false,
    /** What it can do, in plain words, from its permissions. */
    val abilities: List<String> = emptyList(),
    /** Its own settings page (a moz-extension address), or null. */
    val optionsUrl: String? = null,
)

/**
 * Single source of truth for installed extensions. Wraps GeckoView's
 * WebExtensionController so screens only see [ExtensionInfo] and a StateFlow.
 */
/**
 * The text shown for a failed extension action. GeckoView sometimes gives an error with an empty message, which
 * used to show as a blank line; that case gets the fallback sentence and a hint to check the address.
 */
internal fun readableFailure(message: String?, fallback: String): String {
    val trimmed = message?.trim().orEmpty()
    return if (trimmed.isEmpty()) "$fallback Check the address and your connection, then try again." else trimmed
}

/** An extension that downloaded and verified, waiting for the user to choose Add extension or Cancel on the install sheet. */
data class PendingInstall(
    val name: String,
    val version: String,
    val abilities: List<String>,
    /** Where it came from, as a site name: "addons.mozilla.org", another site, or "a file on this device". */
    val source: String,
    val fromAmo: Boolean,
    val fromFile: Boolean,
    /** True when GeckoView reports a Mozilla signature. Release GeckoView refuses unsigned ones before this point. */
    val signed: Boolean,
)

/** What a failed install means, in plain words. Unknown failures fall back to GeckoView's own text. */
internal fun plainInstallError(error: Throwable?, fallback: String): String {
    val code = (error as? WebExtension.InstallException)?.code
    return when (code) {
        WebExtension.InstallException.ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED ->
            "This extension isn't signed by Mozilla, so Ora can't install it."
        WebExtension.InstallException.ErrorCodes.ERROR_INCOMPATIBLE,
        WebExtension.InstallException.ErrorCodes.ERROR_UNEXPECTED_ADDON_VERSION ->
            "This extension doesn't work with this version of Ora."
        WebExtension.InstallException.ErrorCodes.ERROR_CORRUPT_FILE,
        WebExtension.InstallException.ErrorCodes.ERROR_INCORRECT_HASH ->
            "The file is damaged, so it can't be installed. Download it again."
        WebExtension.InstallException.ErrorCodes.ERROR_NETWORK_FAILURE ->
            "The download failed. Check your connection and try again."
        WebExtension.InstallException.ErrorCodes.ERROR_FILE_ACCESS ->
            "Ora couldn't read the file."
        WebExtension.InstallException.ErrorCodes.ERROR_BLOCKLISTED,
        WebExtension.InstallException.ErrorCodes.ERROR_SOFT_BLOCKED ->
            "Mozilla has blocked this extension."
        WebExtension.InstallException.ErrorCodes.ERROR_USER_CANCELED -> null
        else -> readableFailure(error?.message, fallback)
    } ?: ""
}

/** True when a download response is an extension to install: the xpinstall type, or an address ending in .xpi. */
fun isExtensionDownload(uri: String, headers: Map<String, String>): Boolean {
    val type = headers.entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }?.value.orEmpty()
    if (type.substringBefore(';').trim().equals("application/x-xpinstall", ignoreCase = true)) return true
    return uri.substringBefore('#').substringBefore('?').endsWith(".xpi", ignoreCase = true)
}

class ExtensionsRepository(runtime: GeckoRuntime) {

    private val controller: WebExtensionController = runtime.webExtensionController
    private val extensionsById = mutableMapOf<String, WebExtension>()

    private val _extensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())
    val extensions: StateFlow<List<ExtensionInfo>> = _extensions.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Addresses with an install in progress. Buttons read this to show "Adding…" and ignore repeat taps. */
    private val _installing = MutableStateFlow<Set<String>>(emptySet())
    val installing: StateFlow<Set<String>> = _installing.asStateFlow()

    private val _pending = MutableStateFlow<PendingInstall?>(null)
    /** The install sheet shows this. Null when nothing waits. */
    val pending: StateFlow<PendingInstall?> = _pending.asStateFlow()

    private var answer: GeckoResult<WebExtension.PermissionPromptResponse>? = null
    /** Where the install now running came from; read when GeckoView asks for the permission prompt. */
    private var currentSource: String = "addons.mozilla.org"
    private var currentFromFile = false

    init {
        // GeckoView holds the install until this answers, so the sheet is the one confirmation.
        controller.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>,
                dataCollection: Array<out String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse> {
                val result = GeckoResult<WebExtension.PermissionPromptResponse>()
                // A second prompt while one waits is declined, so no answer is left hanging.
                answer?.complete(WebExtension.PermissionPromptResponse(false, false, false))
                answer = result
                val meta = extension.metaData
                val amo = currentSource in AMO_HOSTS
                _pending.value = PendingInstall(
                    name = meta?.name?.takeIf { it.isNotBlank() } ?: extension.id,
                    version = meta?.version.orEmpty(),
                    abilities = describeAbilities(permissions.toList(), origins.toList()),
                    source = currentSource,
                    fromAmo = amo,
                    fromFile = currentFromFile,
                    signed = (meta?.signedState ?: 0) >= WebExtension.SignedStateFlags.SIGNED,
                )
                BrowserLog.info("extensions", "Asking to add ${extension.id} from $currentSource")
                return result
            }
        })
        refresh()
    }

    /** Called by the install sheet. [allowPrivate] is the "Allow in private tabs" switch, off unless the user turned it on. */
    fun answerInstall(add: Boolean, allowPrivate: Boolean) {
        val result = answer ?: return
        answer = null
        _pending.value = null
        BrowserLog.info("extensions", if (add) "Add extension chosen (private tabs ${if (allowPrivate) "allowed" else "not allowed"})" else "Install cancelled")
        result.complete(WebExtension.PermissionPromptResponse(add, add && allowPrivate, add))
    }

    /** A page handed over an extension file. It is installed, not downloaded. Logs the site only. */
    fun installFromPage(url: String, pageUrl: String) {
        val host = LogPrivacy.domainOf(url).ifBlank { LogPrivacy.domainOf(pageUrl) }
        BrowserLog.info("extensions", "Extension file offered by $host, installing instead of downloading")
        install(url, source = host.ifBlank { "this site" })
    }

    /** Copies a picked or downloaded .xpi to the app cache, installs from the copy, then deletes it. */
    fun installFromFile(context: Context, uri: Uri) {
        val copy = File(context.cacheDir, "install-${System.nanoTime()}.xpi")
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } }
                ?: throw java.io.IOException("Could not open the file")
        } catch (e: Exception) {
            copy.delete()
            reportError("Reading the extension file", e, "Ora couldn't read that file")
            return
        }
        install("file://${copy.absolutePath}", source = "a file on this device", fromFile = true, cleanup = copy)
    }

    /** The .xpi files Ora saved in Downloads/Ora, newest first, for the "Found in Downloads" card. */
    fun xpiInDownloads(context: Context): List<Pair<String, Uri>> = try {
        val base = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            base,
            arrayOf(android.provider.MediaStore.Downloads._ID, android.provider.MediaStore.Downloads.DISPLAY_NAME),
            "${android.provider.MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${android.provider.MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("Download/Ora/%", "%.xpi"),
            "${android.provider.MediaStore.Downloads.DATE_ADDED} DESC",
        )?.use { c ->
            buildList { while (c.moveToNext()) add(c.getString(1) to android.content.ContentUris.withAppendedId(base, c.getLong(0))) }
        }.orEmpty()
    } catch (e: Exception) {
        BrowserLog.warn("extensions", "Could not look for .xpi files in Downloads", e)
        emptyList()
    }

    /** Reloads the installed list from GeckoView. */
    fun refresh() {
        controller.list().accept(
            { list ->
                val safe = list.orEmpty()
                extensionsById.clear()
                safe.forEach { extensionsById[it.id] = it }
                _extensions.value = safe.map { it.toInfo() }
            },
            { error -> reportError("Loading extensions", error, "Could not load extensions") }
        )
    }

    /** Installs from an https URL or a resource address. A tap while the same address is already installing is ignored. */
    fun install(
        url: String,
        source: String = AMO_HOST,
        fromFile: Boolean = false,
        cleanup: File? = null,
    ) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        if (trimmed in _installing.value) {
            BrowserLog.info("extensions", "Install already running for $trimmed, ignoring the repeat tap")
            return
        }
        _lastError.value = null
        _installing.value = _installing.value + trimmed
        // Only the site is logged, never the whole address.
        val where = if (fromFile) "a file" else LogPrivacy.domainOf(trimmed)
        currentSource = if (fromFile) source else LogPrivacy.domainOf(trimmed).ifBlank { source }
        currentFromFile = fromFile
        BrowserLog.info("extensions", "Installing from $where")
        val method = if (fromFile) WebExtensionController.INSTALLATION_METHOD_FROM_FILE else WebExtensionController.INSTALLATION_METHOD_MANAGER
        controller.install(trimmed, method).accept(
            {
                _installing.value = _installing.value - trimmed
                cleanup?.delete()
                BrowserLog.info("extensions", "Installed from $where")
                InAppNotices.show("Extension added")
                refresh()
            },
            { error ->
                _installing.value = _installing.value - trimmed
                cleanup?.delete()
                val declined = (error as? WebExtension.InstallException)?.code == WebExtension.InstallException.ErrorCodes.ERROR_USER_CANCELED
                if (declined) {
                    BrowserLog.info("extensions", "Install from $where cancelled")
                } else {
                    val plain = plainInstallError(error, "Could not install this extension")
                    BrowserLog.error("extensions", "Installing from $where failed: $plain", error)
                    _lastError.value = plain
                    InAppNotices.show(plain)
                }
            }
        )
    }

    /** True while [url] is being installed, so its button can show that it is busy. */
    fun isInstalling(url: String): Boolean = url.trim() in _installing.value

    fun setEnabled(id: String, enabled: Boolean) {
        val extension = extensionsById[id] ?: return
        val source = WebExtensionController.EnableSource.USER
        val result = if (enabled) controller.enable(extension, source) else controller.disable(extension, source)
        val action = if (enabled) "Enabling ${extension.id}" else "Disabling ${extension.id}"
        result.accept({ refresh() }, { error -> reportError(action, error, "Could not change this extension") })
    }

    /** Lets an extension run in private tabs, or stops it. GeckoView keeps the choice. */
    fun setAllowedInPrivate(id: String, allowed: Boolean) {
        val extension = extensionsById[id] ?: return
        controller.setAllowedInPrivateBrowsing(extension, allowed).accept(
            { BrowserLog.info("extensions", "${if (allowed) "Allowed" else "Stopped"} $id in private tabs"); refresh() },
            { error -> reportError("Changing private access for $id", error, "Could not change private tab access") }
        )
    }

    fun uninstall(id: String) {
        val extension = extensionsById[id] ?: return
        controller.uninstall(extension).accept(
            { BrowserLog.info("extensions", "Removed $id"); refresh() },
            { error -> reportError("Removing $id", error, "Could not remove this extension") }
        )
    }

    fun clearError() {
        _lastError.value = null
    }

    companion object {
        const val AMO_HOST = "addons.mozilla.org"
        private val AMO_HOSTS = setOf(AMO_HOST, "addons.cdn.mozilla.net")
    }

    private fun reportError(action: String, error: Throwable?, fallback: String) {
        BrowserLog.error("extensions", "$action failed", error)
        _lastError.value = readableFailure(error?.message, fallback)
    }

    private fun WebExtension.toInfo(): ExtensionInfo = ExtensionInfo(
        id = id,
        name = metaData?.name?.takeIf { it.isNotBlank() } ?: id,
        version = metaData?.version.orEmpty(),
        description = metaData?.description.orEmpty(),
        enabled = metaData?.enabled ?: false,
        builtIn = isBuiltIn,
        allowedInPrivate = metaData?.allowedInPrivateBrowsing ?: false,
        abilities = describeAbilities(metaData?.requiredPermissions.orEmpty().toList(), metaData?.requiredOrigins.orEmpty().toList()),
        optionsUrl = metaData?.optionsPageUrl?.takeIf { it.isNotBlank() },
    )

    /** Turns permission names into short plain sentences. Unknown ones are shown as named. */
    internal fun describeAbilities(permissions: List<String>, origins: List<String>): List<String> = buildList {
        if (origins.any { it == "<all_urls>" || it.startsWith("*://*/") || it == "*://*/*" }) add("Read and change data on all sites")
        else if (origins.isNotEmpty()) add("Read and change data on ${origins.size} ${if (origins.size == 1) "site" else "sites"}")
        permissions.forEach { permission ->
            add(
                when (permission) {
                    "tabs" -> "See your open tabs"
                    "activeTab" -> "Use the tab you tap it on"
                    "webRequest", "webRequestBlocking" -> "Watch and block page requests"
                    "declarativeNetRequest", "declarativeNetRequestWithHostAccess" -> "Block page requests"
                    "storage", "unlimitedStorage" -> "Keep its own settings"
                    "cookies" -> "Read and change cookies"
                    "notifications" -> "Show notifications"
                    "scripting" -> "Run scripts on pages"
                    "clipboardWrite" -> "Copy to the clipboard"
                    "contextMenus", "menus" -> "Add items to menus"
                    "downloads" -> "Manage downloads"
                    "history" -> "Read your history"
                    "bookmarks" -> "Read and change your bookmarks"
                    "alarms" -> "Run things on a schedule"
                    "privacy" -> "Change privacy settings"
                    "proxy" -> "Control how Ora connects"
                    "nativeMessaging" -> "Talk to other apps on this device"
                    else -> permission
                }
            )
        }
    }.distinct()
}
