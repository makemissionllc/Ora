package com.makemission.ora.torrents

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A torrent something offered to Ora: a magnet link, or the bytes of a .torrent file. The browser screen asks before adding it. */
sealed class TorrentOffer {
    data class Magnet(val link: MagnetLink) : TorrentOffer()
    data class File(val name: String, val bytes: ByteArray) : TorrentOffer()
}

/** Where taps on magnet links, downloaded .torrent files and links from other apps wait for the confirmation card. */
object TorrentOffers {
    private val _pending = MutableStateFlow<TorrentOffer?>(null)
    val pending: StateFlow<TorrentOffer?> = _pending.asStateFlow()

    fun offer(offer: TorrentOffer) { _pending.value = offer }
    fun consume() { _pending.value = null }
}
