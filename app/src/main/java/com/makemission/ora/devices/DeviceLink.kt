package com.makemission.ora.devices

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors

/** A device found on the network. [pairing] is true while it shows a pairing code. Unpaired devices appear only in that state. */
data class Peer(val id: String, val name: String, val host: InetAddress, val port: Int, val pairing: Boolean, val serviceName: String)

/** Remembers message ids it has seen and refuses stale timestamps, so a copied message cannot be replayed. */
class ReplayGuard(private val maxAgeMs: Long = 10 * 60_000L, private val capacity: Int = 256) {
    private val seen = object : LinkedHashMap<String, Unit>(capacity, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?) = size > capacity
    }

    /** True the first time a fresh message id is offered; false for a repeat or a message outside the allowed clock difference. */
    @Synchronized
    fun accept(id: String, timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (kotlin.math.abs(now - timestamp) > maxAgeMs) return false
        if (seen.containsKey(id)) return false
        seen[id] = Unit
        return true
    }
}

/**
 * Talks to the person's other devices over the local network, with no account and no server.
 *
 * - Each device advertises itself with Android's network service discovery and listens on a random port.
 * - Pairing happens once: one device shows a six digit code, the other types it. The two then share a random
 *   256-bit secret that seals every later message (AES-GCM), so others on the same Wi-Fi cannot read or forge them.
 * - A message to a device that is asleep or away waits in an outbox and goes when the device is next seen (24 hours at most).
 * - Features register a handler for a message type with [on]; this class knows nothing about tabs or reading lists.
 *
 * Only runs while [start] has been called, which the app does when the person turns Link devices on.
 */
class DeviceLink private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(NsdManager::class.java)
    val store = PairedDevices(appContext)
    private val pool = Executors.newCachedThreadPool { Thread(it, "ora-link").apply { isDaemon = true } }
    private val replay = ReplayGuard()
    private val handlers = HashMap<String, (PairedDevice, JSONObject) -> JSONObject?>()

    private var name = PairedDevices.defaultName()
    private var server: ServerSocket? = null
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    @Volatile private var running = false
    private var serviceName = ""

    private val _peers = MutableStateFlow<Map<String, Peer>>(emptyMap())
    /** Every Ora device seen on this network right now, paired or not, by device id. */
    val peers: StateFlow<Map<String, Peer>> = _peers.asStateFlow()

    private val _pairingCode = MutableStateFlow<String?>(null)
    /** The code this device is showing while it waits to be paired, or null. */
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()
    private var pairFailures = 0

    private val _appeared = MutableSharedFlow<String>(extraBufferCapacity = 16)
    /** Emits a paired device's id each time it comes into view, so features can catch up (reading list sync, outbox). */
    val appeared: SharedFlow<String> = _appeared.asSharedFlow()

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    val deviceId: String get() = store.deviceId

    /** Registers a handler for messages of [type]. It returns the reply body, or null for a plain "ok". Runs off the main thread. */
    fun on(type: String, handler: (from: PairedDevice, body: JSONObject) -> JSONObject?) {
        handlers[type] = handler
    }

    /** Paired devices that are on the network right now. */
    fun isOnline(id: String): Boolean = _peers.value.containsKey(id)

    // --- start and stop -------------------------------------------------------------------------------------------

    @Synchronized
    fun start(deviceName: String) {
        name = deviceName.ifBlank { PairedDevices.defaultName() }
        if (running) { advertise(); return }
        try {
            val socket = ServerSocket(0)
            server = socket
            running = true
            pool.execute { acceptLoop(socket) }
            advertise()
            discover()
            BrowserLog.info("devices", "Device link on as \"$name\", listening on port ${socket.localPort}")
        } catch (e: Exception) {
            running = false
            BrowserLog.error("devices", "Device link could not start", e)
        }
    }

    @Synchronized
    fun stop() {
        if (!running) return
        running = false
        _pairingCode.value = null
        runCatching { server?.close() }
        server = null
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discovery = null
        resolveQueue.clear()
        resolving = false
        _peers.value = emptyMap()
        BrowserLog.info("devices", "Device link off")
    }

    // --- advertising and discovery --------------------------------------------------------------------------------

    private fun advertise() {
        val socket = server ?: return
        registration?.let { runCatching { nsd.unregisterService(it) } }
        val info = NsdServiceInfo().apply {
            serviceName = "Ora-" + store.deviceId.take(8)
            serviceType = SERVICE_TYPE
            port = socket.localPort
            setAttribute("id", store.deviceId)
            setAttribute("name", name)
            setAttribute("pair", if (_pairingCode.value != null) "1" else "0")
        }
        serviceName = info.serviceName
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) { serviceName = info.serviceName }
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) { BrowserLog.warn("devices", "Could not announce this device on the network (code $code)") }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
        }
        registration = listener
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { BrowserLog.warn("devices", "Could not announce this device on the network", it) }
    }

    private fun discover() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) { BrowserLog.info("devices", "Looking for other devices on the network") }
            override fun onStartDiscoveryFailed(type: String, code: Int) { BrowserLog.warn("devices", "Could not look for other devices (code $code)") }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (info.serviceName == serviceName || !info.serviceName.startsWith("Ora-")) return
                BrowserLog.info("devices", "Saw ${info.serviceName} on the network")
                synchronized(resolveQueue) { resolveQueue.addLast(info) }
                resolveNext()
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                _peers.value = _peers.value.filterValues { it.serviceName != info.serviceName }
            }
        }
        discovery = listener
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { BrowserLog.warn("devices", "Could not look for other devices", it) }
    }

    /**
     * Reads the announcement of every device already seen again. A device that starts or stops showing a pairing code
     * re-announces itself, but Android only tells us about NEW services, so without this the code never shows up on
     * a device that saw the other one earlier. Called while the pairing card is open.
     */
    fun refreshPeers() {
        val known = _peers.value.values.map { it.serviceName }
        if (known.isEmpty()) return
        synchronized(resolveQueue) {
            known.forEach { name ->
                if (resolveQueue.none { it.serviceName == name }) resolveQueue.addLast(NsdServiceInfo().apply { serviceName = name; serviceType = SERVICE_TYPE })
            }
        }
        resolveNext()
    }

    @Suppress("DEPRECATION")
    private fun resolveNext() {
        val next = synchronized(resolveQueue) {
            if (resolving) return
            resolveQueue.removeFirstOrNull()?.also { resolving = true } ?: return
        }
        val done = { synchronized(resolveQueue) { resolving = false }; resolveNext() }
        runCatching {
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, code: Int) { BrowserLog.warn("devices", "Could not reach ${info.serviceName} (code $code)"); done() }
                override fun onServiceResolved(info: NsdServiceInfo) {
                    try {
                        val attrs = info.attributes
                        val id = attrs["id"]?.toString(Charsets.UTF_8) ?: return
                        if (id == store.deviceId) return
                        val peerName = attrs["name"]?.toString(Charsets.UTF_8)?.take(40) ?: "Ora device"
                        val pairing = attrs["pair"]?.toString(Charsets.UTF_8) == "1"
                        val host = info.host ?: return
                        val known = _peers.value.containsKey(id)
                        // Re-reads while the pairing card is open come back every few seconds; log only a change.
                        val before = _peers.value[id]
                        if (before == null || before.pairing != pairing || before.port != info.port) {
                            BrowserLog.info("devices", "Found \"$peerName\" at ${host.hostAddress}:${info.port}${if (pairing) ", showing a pairing code" else ""}")
                        }
                        _peers.value = _peers.value + (id to Peer(id, peerName, host, info.port, pairing, info.serviceName))
                        val paired = store.find(id)
                        if (paired != null) {
                            if (paired.name != peerName) store.add(paired.copy(name = peerName))
                            if (!known) {
                                BrowserLog.info("devices", "\"$peerName\" is on the network")
                                _appeared.tryEmit(id)
                                pool.execute { flushQueue(id) }
                            }
                        }
                    } finally {
                        done()
                    }
                }
            })
        }.onFailure { done() }
    }

    // --- pairing --------------------------------------------------------------------------------------------------

    /** Starts showing a pairing code. Other devices on this network can then pair with this one until [endPairing]. */
    fun beginPairing(): String {
        val code = DeviceLinkCrypto.newPairingCode()
        pairFailures = 0
        _pairingCode.value = code
        advertise()
        BrowserLog.info("devices", "Pairing started: waiting for a device to type the code")
        return code
    }

    fun endPairing() {
        if (_pairingCode.value == null) return
        _pairingCode.value = null
        advertise()
        BrowserLog.info("devices", "Pairing ended")
    }

    /** Pairs with a device that is showing a code. Returns null on success or a sentence saying what went wrong. Blocking. */
    fun pairWith(peer: Peer, code: String): String? {
        return try {
            val codeKey = DeviceLinkCrypto.keyFromCode(code)
            val hello = JSONObject().put("id", store.deviceId).put("name", name).put("nonce", UUID.randomUUID().toString())
            val reply = exchange(peer, "pair", DeviceLinkCrypto.seal(codeKey, hello.toString()))
            val text = reply?.takeIf { it != REJECTED }?.let { DeviceLinkCrypto.open(codeKey, it) }
            if (text == null) {
                BrowserLog.warn("devices", "Pairing with \"${peer.name}\" was refused: wrong code")
                return "That code was not right. Check the number on the other device."
            }
            val json = JSONObject(text)
            store.add(PairedDevice(json.getString("id"), json.optString("name", peer.name), json.getString("secret"), System.currentTimeMillis()))
            BrowserLog.info("devices", "Paired with \"${peer.name}\"")
            null
        } catch (e: Exception) {
            BrowserLog.warn("devices", "Pairing with \"${peer.name}\" failed", e)
            "Could not reach that device. Make sure both are on the same Wi-Fi."
        }
    }

    fun forget(id: String) {
        val name = store.find(id)?.name ?: return
        store.forget(id)
        BrowserLog.info("devices", "Forgot \"$name\"")
    }

    // --- sending --------------------------------------------------------------------------------------------------

    enum class SendResult { DELIVERED, QUEUED, FAILED }

    /**
     * Sends [type] with [body] to a paired device. If it cannot be reached right now the message waits in the
     * outbox and goes when the device is next seen. Blocking: call it off the main thread.
     */
    fun send(deviceId: String, type: String, body: JSONObject): SendResult {
        val device = store.find(deviceId) ?: return SendResult.FAILED
        val id = UUID.randomUUID().toString()
        val queued = QueuedMessage(deviceId, id, type, body.toString(), System.currentTimeMillis() + QUEUE_LIFETIME_MS)
        return if (deliver(device, queued) != null) {
            store.touch(deviceId)
            SendResult.DELIVERED
        } else {
            store.enqueue(queued)
            BrowserLog.info("devices", "\"${device.name}\" is not reachable; the $type message waits in the outbox")
            SendResult.QUEUED
        }
    }

    /** Asks a device something and returns its reply, or null when it cannot be reached. Never queued. Blocking. */
    fun ask(deviceId: String, type: String, body: JSONObject): JSONObject? {
        val device = store.find(deviceId) ?: return null
        return deliver(device, QueuedMessage(deviceId, UUID.randomUUID().toString(), type, body.toString(), 0L))
    }

    private fun flushQueue(id: String) {
        store.dropExpired()
        val device = store.find(id) ?: return
        val waiting = store.queue.value.filter { it.to == id }
        if (waiting.isEmpty()) return
        var sent = 0
        for (message in waiting) {
            if (deliver(device, message) == null) break
            store.dequeue(message.id)
            sent++
        }
        if (sent > 0) {
            store.touch(id)
            BrowserLog.info("devices", "Delivered $sent waiting ${if (sent == 1) "message" else "messages"} to \"${device.name}\"")
        }
    }

    /** One sealed round trip. Returns the reply body, or null when the device did not answer or refused. */
    private fun deliver(device: PairedDevice, message: QueuedMessage): JSONObject? {
        val peer = _peers.value[device.id] ?: return null
        return try {
            val key = DeviceLinkCrypto.decode(device.secret)
            val envelope = JSONObject().put("id", message.id).put("ts", System.currentTimeMillis()).put("type", message.type).put("body", JSONObject(message.body))
            val reply = exchange(peer, store.deviceId, DeviceLinkCrypto.seal(key, envelope.toString())) ?: return null
            if (reply == REJECTED) return null
            val text = DeviceLinkCrypto.open(key, reply) ?: return null
            JSONObject(text)
        } catch (e: Exception) {
            BrowserLog.warn("devices", "Could not reach \"${device.name}\"", e)
            _peers.value = _peers.value - device.id
            null
        }
    }

    private fun exchange(peer: Peer, from: String, sealed: String): String? {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(peer.host, peer.port), 3000)
            socket.soTimeout = 8000
            socket.getOutputStream().apply { write("$from\t$sealed\n".toByteArray()); flush() }
            return readLine(socket.getInputStream())
        }
    }

    // --- receiving ------------------------------------------------------------------------------------------------

    private fun acceptLoop(socket: ServerSocket) {
        while (running) {
            val client = try { socket.accept() } catch (e: Exception) { break }
            pool.execute { client.use { handle(it) } }
        }
    }

    private fun handle(client: Socket) {
        try {
            client.soTimeout = 8000
            val line = readLine(client.getInputStream()) ?: return
            val tab = line.indexOf('\t')
            if (tab <= 0) return
            val from = line.substring(0, tab)
            val sealed = line.substring(tab + 1)
            val reply = if (from == "pair") handlePair(sealed) else handleMessage(from, sealed)
            client.getOutputStream().apply { write((reply + "\n").toByteArray()); flush() }
        } catch (e: Exception) {
            BrowserLog.warn("devices", "A message from another device could not be handled", e)
        }
    }

    private fun handlePair(sealed: String): String {
        val code = _pairingCode.value ?: return REJECTED
        val codeKey = DeviceLinkCrypto.keyFromCode(code)
        val text = DeviceLinkCrypto.open(codeKey, sealed)
        if (text == null) {
            // A wrong guess. Five wrong guesses close pairing, so a six digit code cannot be brute forced over Wi-Fi.
            if (++pairFailures >= MAX_PAIR_FAILURES) {
                BrowserLog.warn("devices", "Pairing closed after $MAX_PAIR_FAILURES wrong codes")
                endPairing()
            }
            return REJECTED
        }
        val hello = JSONObject(text)
        val secret = DeviceLinkCrypto.newSecret()
        val id = hello.getString("id")
        val peerName = hello.optString("name", "Ora device").take(40)
        store.add(PairedDevice(id, peerName, DeviceLinkCrypto.encode(secret), System.currentTimeMillis()))
        BrowserLog.info("devices", "Paired with \"$peerName\"")
        _pairingCode.value = null
        advertise()
        return DeviceLinkCrypto.seal(codeKey, JSONObject().put("id", store.deviceId).put("name", name).put("secret", DeviceLinkCrypto.encode(secret)).toString())
    }

    private fun handleMessage(from: String, sealed: String): String {
        val device = store.find(from) ?: return REJECTED
        val key = DeviceLinkCrypto.decode(device.secret)
        val text = DeviceLinkCrypto.open(key, sealed) ?: return REJECTED
        val envelope = JSONObject(text)
        if (!replay.accept(envelope.getString("id"), envelope.getLong("ts"))) return REJECTED
        val type = envelope.getString("type")
        val handler = handlers[type]
        val reply = try { handler?.invoke(device, envelope.getJSONObject("body")) } catch (e: Exception) {
            BrowserLog.error("devices", "Handling a $type message from \"${device.name}\" failed", e)
            null
        }
        return DeviceLinkCrypto.seal(key, (reply ?: JSONObject().put("ok", handler != null)).toString())
    }

    /** Reads up to one line, never more than [MAX_LINE] bytes, so a stranger cannot make the app buffer without limit. */
    private fun readLine(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (out.size() == 0) null else out.toString(Charsets.UTF_8.name())
            if (b == '\n'.code) return out.toString(Charsets.UTF_8.name())
            out.write(b)
            if (out.size() > MAX_LINE) return null
        }
    }

    companion object {
        private const val SERVICE_TYPE = "_oralink._tcp."
        private const val REJECTED = "!"
        private const val MAX_LINE = 512 * 1024
        private const val MAX_PAIR_FAILURES = 5
        private const val QUEUE_LIFETIME_MS = 24 * 60 * 60_000L

        @Volatile private var instance: DeviceLink? = null

        /** The one link for the whole app, so every screen and feature shares one connection state. */
        fun get(context: Context): DeviceLink =
            instance ?: synchronized(this) { instance ?: DeviceLink(context).also { instance = it } }
    }
}
