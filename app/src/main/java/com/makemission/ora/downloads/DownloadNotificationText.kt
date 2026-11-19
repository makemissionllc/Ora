package com.makemission.ora.downloads

/** What a download's system notification can say and offer. Free of Android calls, so the rules can be tested. */
internal object DownloadNotificationText {

    /** The buttons a notification can show. [DownloadActionReceiver] runs each one. */
    enum class Action { PAUSE, RESUME, CANCEL, RETRY, OPEN }

    /** A private download never shows its file name, so nothing from the page reaches the shade. */
    fun title(item: DownloadItem, isPrivate: Boolean): String = if (isPrivate) "Private download" else item.fileName

    /** The line under the title, for the download's current state. */
    fun line(item: DownloadItem): String = when (item.status) {
        DownloadStatus.RUNNING -> running(item)
        DownloadStatus.PAUSED -> "Paused · ${amount(item)}"
        DownloadStatus.DONE -> "Saved · ${DownloadsRepository.sizeLabel(item.bytes)}"
        DownloadStatus.FAILED -> "Failed · Retry to continue"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

    /**
     * The buttons for the state. A finished risky file gets none, so it is opened from Ora's list,
     * where the warning shows first.
     */
    fun actions(item: DownloadItem): List<Action> = when (item.status) {
        DownloadStatus.RUNNING -> if (item.pausable) listOf(Action.PAUSE, Action.CANCEL) else listOf(Action.CANCEL)
        DownloadStatus.PAUSED -> listOf(Action.RESUME, Action.CANCEL)
        DownloadStatus.FAILED -> listOf(Action.RETRY, Action.CANCEL)
        DownloadStatus.DONE -> if (DownloadSafety.isRisky(item.fileName)) emptyList() else listOf(Action.OPEN)
        DownloadStatus.CANCELLED -> emptyList()
    }

    private fun running(item: DownloadItem): String {
        val parts = mutableListOf("Downloading")
        if (item.progress >= 0f) parts += "${(item.progress * 100).toInt()}%"
        if (item.speed > 0) parts += "${DownloadsRepository.sizeLabel(item.speed)}/s"
        return parts.joinToString(" · ")
    }

    private fun amount(item: DownloadItem): String =
        if (item.totalBytes > 0) {
            "${DownloadsRepository.sizeLabel(item.bytes)} of ${DownloadsRepository.sizeLabel(item.totalBytes)}"
        } else {
            DownloadsRepository.sizeLabel(item.bytes)
        }
}
