package com.makemission.ora.downloads

/** The one-line totals under the Downloads list. Free of Android calls, so it can be tested. */
internal object DownloadStats {

    /** "4 saved · 1.2 GB · 1 failed", or null when the list has nothing to total. */
    fun footer(items: List<DownloadItem>): String? {
        val done = items.filter { it.status == DownloadStatus.DONE }
        val failed = items.count { it.status == DownloadStatus.FAILED }
        if (done.isEmpty() && failed == 0) return null
        val parts = mutableListOf<String>()
        if (done.isNotEmpty()) {
            parts += "${done.size} saved"
            parts += DownloadsRepository.sizeLabel(done.sumOf { it.bytes })
        }
        if (failed > 0) parts += "$failed failed"
        return parts.joinToString(" · ")
    }
}
