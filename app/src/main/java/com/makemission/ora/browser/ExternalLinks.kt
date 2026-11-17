package com.makemission.ora.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Web links that other apps hand to Ora, for example when Ora is the default browser.
 * [MainActivity] offers a link; [BrowserScreen] opens it in a new tab and consumes it.
 */
object ExternalLinks {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(url: String) {
        _pending.value = url
    }

    fun consume() {
        _pending.value = null
    }
}
