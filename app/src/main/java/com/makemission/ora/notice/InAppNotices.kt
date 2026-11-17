package com.makemission.ora.notice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Short messages shown inside the app, as a snackbar at the bottom of the window, in place of system toasts.
 * Any screen or the browser delegate can call [show]; the browser screen collects [messages] and shows them.
 */
object InAppNotices {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Messages waiting to be shown, oldest first. */
    val messages: SharedFlow<String> = events.asSharedFlow()

    /** Queues [message] for the snackbar. Never blocks; a burst beyond the buffer is dropped. */
    fun show(message: String) {
        events.tryEmit(message)
    }
}
