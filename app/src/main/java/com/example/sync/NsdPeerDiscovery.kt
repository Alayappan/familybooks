package com.example.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.PeerDevice
import com.example.data.model.PeerSyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

class NsdPeerDiscovery(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getDeviceId: () -> String,
    private val getMemberName: () -> String,
    private val getDeviceName: () -> String = { NetworkUtils.getDeviceModelName() },
    private val getMacAddress: () -> String = { "" },
    private val onPeerDiscovered: (PeerDevice) -> Unit
) {
    private val tag = "NsdPeerDiscovery"
    private val serviceType = "_familybooks._tcp."
    private var registeredServiceName: String? = null

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredPeers = MutableStateFlow<Map<String, PeerDevice>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, PeerDevice>> = _discoveredPeers.asStateFlow()

    private val _discoveryActive = MutableStateFlow(false)
    val discoveryActive: StateFlow<Boolean> = _discoveryActive.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // Sequential resolve channel to prevent "FAILURE_ALREADY_ACTIVE" in NsdManager
    private val resolveQueue = Channel<NsdServiceInfo>(Channel.UNLIMITED)
    private var resolveWorkerJob: Job? = null

    init {
        startResolveWorker()
    }

    private fun startResolveWorker() {
        resolveWorkerJob = scope.launch(Dispatchers.IO) {
            for (serviceInfo in resolveQueue) {
                if (!isActive) break
                resolveServiceSequentially(serviceInfo)
            }
        }
    }

    fun start(port: Int) {
        acquireMulticastLock()
        registerService(port)
        startDiscovery()
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("FamilyBooks_mDNS_Lock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.w(tag, "MulticastLock error: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun registerService(port: Int) {
        val nsd = nsdManager ?: return
        val currentDeviceId = getDeviceId()
        val currentMember = getMemberName().replace(" ", "-")
        val currentDevName = getDeviceName().replace(" ", "-").replace("_", "-")
        val currentMac = getMacAddress().replace(":", "").ifEmpty { "000000000000" }
        val proposedName = "FB_${currentDeviceId}_${currentMember}_${currentDevName}_${currentMac}"

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = proposedName
            serviceType = this@NsdPeerDiscovery.serviceType
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registeredServiceName = serviceInfo.serviceName
                Log.i(tag, "Service registered as: ${serviceInfo.serviceName} on port ${serviceInfo.port}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(tag, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service unregistration failed: $errorCode")
            }
        }

        try {
            nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Error registering NSD service: ${e.message}")
        }
    }

    private fun startDiscovery() {
        val nsd = nsdManager ?: return
        if (_discoveryActive.value) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery failed: $errorCode")
                _discoveryActive.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Stop discovery failed: $errorCode")
                _discoveryActive.value = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.i(tag, "NSD Service discovery started for $serviceType")
                _discoveryActive.value = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(tag, "NSD Service discovery stopped")
                _discoveryActive.value = false
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "Service found: ${service.serviceName}")
                val registered = registeredServiceName
                // Skip if this is our own announced service
                if (registered != null && service.serviceName.equals(registered, ignoreCase = true)) {
                    return
                }
                if (service.serviceName.startsWith("FB_")) {
                    resolveQueue.trySend(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(tag, "Service lost: ${service.serviceName}")
                // Remove lost peer
                _discoveredPeers.update { map ->
                    map.filterValues { it.deviceName != service.serviceName }
                }
            }
        }

        try {
            nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery: ${e.message}")
        }
    }

    private fun resolveServiceSequentially(serviceInfo: NsdServiceInfo) {
        val nsd = nsdManager ?: return
        val lock = Object()
        var resolved = false

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(tag, "Resolve failed for ${service.serviceName}: code $errorCode")
                synchronized(lock) {
                    resolved = true
                    lock.notifyAll()
                }
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host: InetAddress? = resolvedInfo.host
                val hostAddress = host?.hostAddress ?: ""
                val port = resolvedInfo.port
                val name = resolvedInfo.serviceName

                Log.i(tag, "Resolved peer: $name at $hostAddress:$port")

                if (hostAddress.isNotEmpty() && port > 0) {
                    // Extract member and device ID from name "FB_<deviceId>_<memberName>_<deviceName>_<mac>"
                    val tokens = name.split("_")
                    val devId = tokens.getOrNull(1) ?: name
                    val member = tokens.getOrNull(2)?.replace("-", " ") ?: "Family Member"
                    val devModel = tokens.getOrNull(3)?.replace("-", " ") ?: "Family Device"
                    val rawMac = tokens.getOrNull(4) ?: ""
                    val mac = if (rawMac.length == 12 && rawMac != "000000000000") {
                        rawMac.chunked(2).joinToString(":")
                    } else ""

                    val peer = PeerDevice(
                        deviceId = devId,
                        deviceName = devModel,
                        memberName = member,
                        macAddress = mac,
                        hostAddress = hostAddress,
                        port = port,
                        lastSeen = System.currentTimeMillis(),
                        syncStatus = PeerSyncStatus.DISCOVERED
                    )

                    _discoveredPeers.update { current ->
                        current + (peer.deviceId to peer)
                    }

                    onPeerDiscovered(peer)
                }

                synchronized(lock) {
                    resolved = true
                    lock.notifyAll()
                }
            }
        }

        try {
            nsd.resolveService(serviceInfo, resolveListener)
            synchronized(lock) {
                if (!resolved) {
                    lock.wait(3000) // 3 second timeout per resolve
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during resolve: ${e.message}")
        }
    }

    fun addManualPeer(
        host: String,
        port: Int,
        memberName: String = "Direct Peer",
        deviceModelName: String = "Family Device"
    ): PeerDevice {
        val devId = "manual_${host.replace(".", "_")}_$port"
        val peer = PeerDevice(
            deviceId = devId,
            deviceName = deviceModelName.ifEmpty { "Family Device" },
            memberName = memberName,
            hostAddress = host,
            port = port,
            lastSeen = System.currentTimeMillis(),
            syncStatus = PeerSyncStatus.DISCOVERED
        )
        _discoveredPeers.update { it + (peer.deviceId to peer) }
        return peer
    }

    fun removePeer(deviceId: String) {
        _discoveredPeers.update { it - deviceId }
    }

    fun updatePeerStatus(deviceId: String, status: PeerSyncStatus, lastError: String? = null) {
        _discoveredPeers.update { map ->
            val peer = map[deviceId] ?: return@update map
            map + (deviceId to peer.copy(
                syncStatus = status,
                lastSyncedAt = if (status == PeerSyncStatus.SYNCED) System.currentTimeMillis() else peer.lastSyncedAt,
                lastError = lastError
            ))
        }
    }

    fun stop() {
        val nsd = nsdManager
        if (nsd != null) {
            try {
                discoveryListener?.let { nsd.stopServiceDiscovery(it) }
            } catch (_: Exception) {}

            try {
                registrationListener?.let { nsd.unregisterService(it) }
            } catch (_: Exception) {}
        }
        discoveryListener = null
        registrationListener = null
        _discoveryActive.value = false
        resolveWorkerJob?.cancel()
        releaseMulticastLock()
    }
}
