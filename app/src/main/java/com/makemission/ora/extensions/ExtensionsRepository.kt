package com.makemission.ora.extensions

import com.makemission.ora.log.BrowserLog
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

    init {
        // The user typed the address and chose Install, so accept the requested permissions.
        controller.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>,
                dataCollection: Array<out String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse> =
                GeckoResult.fromValue(WebExtension.PermissionPromptResponse(true, true, true))
        })
        refresh()
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
    fun install(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        if (trimmed in _installing.value) {
            BrowserLog.info("extensions", "Install already running for $trimmed, ignoring the repeat tap")
            return
        }
        _lastError.value = null
        _installing.value = _installing.value + trimmed
        BrowserLog.info("extensions", "Installing from $trimmed")
        controller.install(trimmed).accept(
            {
                _installing.value = _installing.value - trimmed
                BrowserLog.info("extensions", "Installed from $trimmed")
                refresh()
            },
            { error ->
                _installing.value = _installing.value - trimmed
                reportError("Installing $trimmed", error, "Could not install this extension")
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
    private fun describeAbilities(permissions: List<String>, origins: List<String>): List<String> = buildList {
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
