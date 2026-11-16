package com.makemission.ora.extensions

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
)

/**
 * Single source of truth for installed extensions. Wraps GeckoView's
 * WebExtensionController so screens only see [ExtensionInfo] and a StateFlow.
 */
class ExtensionsRepository(runtime: GeckoRuntime) {

    private val controller: WebExtensionController = runtime.webExtensionController
    private val extensionsById = mutableMapOf<String, WebExtension>()

    private val _extensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())
    val extensions: StateFlow<List<ExtensionInfo>> = _extensions.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

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
            { error -> _lastError.value = error?.message ?: "Could not load extensions" }
        )
    }

    /** Installs from an https URL or a resource address. */
    fun install(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        _lastError.value = null
        controller.install(trimmed).accept(
            { refresh() },
            { error -> _lastError.value = error?.message ?: "Could not install this extension" }
        )
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val extension = extensionsById[id] ?: return
        val source = WebExtensionController.EnableSource.USER
        val result = if (enabled) controller.enable(extension, source) else controller.disable(extension, source)
        result.accept({ refresh() }, { error -> _lastError.value = error?.message })
    }

    fun uninstall(id: String) {
        val extension = extensionsById[id] ?: return
        controller.uninstall(extension).accept({ refresh() }, { error -> _lastError.value = error?.message })
    }

    fun clearError() {
        _lastError.value = null
    }

    private fun WebExtension.toInfo(): ExtensionInfo = ExtensionInfo(
        id = id,
        name = metaData?.name?.takeIf { it.isNotBlank() } ?: id,
        version = metaData?.version.orEmpty(),
        description = metaData?.description.orEmpty(),
        enabled = metaData?.enabled ?: false,
        builtIn = isBuiltIn,
    )
}
