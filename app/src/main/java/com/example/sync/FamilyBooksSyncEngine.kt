package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.PeerDevice
import com.example.data.model.PeerSyncStatus
import com.example.data.repository.FamilyBooksRepository
import com.example.data.repository.SyncMergeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceSyncResult(
    val memberName: String,
    val deviceName: String,
    val isSuccess: Boolean,
    val recordsProcessed: Int = 0,
    val statusMessage: String
)

data class SyncRunSummary(
    val total: Int,
    val successCount: Int,
    val failedCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceResults: List<DeviceSyncResult> = emptyList()
)

class FamilyBooksSyncEngine(
    private val context: Context,
    private val repository: FamilyBooksRepository,
    private val scope: CoroutineScope
) {
    private val tag = "FamilyBooksSyncEngine"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("familybooks_prefs", Context.MODE_PRIVATE)

    private val _currentMemberName = MutableStateFlow(
        prefs.getString("current_member_name", "Mom") ?: "Mom"
    )
    val currentMemberName: StateFlow<String> = _currentMemberName.asStateFlow()

    private val _currentDeviceName = MutableStateFlow(
        prefs.getString("current_device_name", NetworkUtils.getDeviceModelName()) ?: NetworkUtils.getDeviceModelName()
    )
    val currentDeviceName: StateFlow<String> = _currentDeviceName.asStateFlow()

    private val deviceId = NetworkUtils.getDeviceId(context)
    val macAddress: String = NetworkUtils.getDeviceMacAddress(context)

    private val _localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _localPort = MutableStateFlow(8976)
    val localPort: StateFlow<Int> = _localPort.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Ready • Offline-First")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow<Long?>(
        if (prefs.getLong("last_synced_time", 0L) > 0) prefs.getLong("last_synced_time", 0L) else null
    )
    val lastSyncedTime: StateFlow<Long?> = _lastSyncedTime.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncRunSummary?>(null)
    val lastSyncSummary: StateFlow<SyncRunSummary?> = _lastSyncSummary.asStateFlow()

    private val _isScanningNetwork = MutableStateFlow(false)
    val isScanningNetwork: StateFlow<Boolean> = _isScanningNetwork.asStateFlow()

    private val _scannedPairingDevices = MutableStateFlow<List<PeerDevice>>(emptyList())
    val scannedPairingDevices: StateFlow<List<PeerDevice>> = _scannedPairingDevices.asStateFlow()

    private val server = P2PSyncServer(
        repository = repository,
        scope = scope,
        getDeviceId = { deviceId },
        getMemberName = { _currentMemberName.value },
        getDeviceName = { _currentDeviceName.value },
        getMacAddress = { macAddress }
    )

    private val client = P2PClient(
        repository = repository,
        getDeviceId = { deviceId },
        getMemberName = { _currentMemberName.value },
        getDeviceName = { _currentDeviceName.value },
        getMacAddress = { macAddress }
    )

    private val discovery = NsdPeerDiscovery(
        context = context,
        scope = scope,
        getDeviceId = { deviceId },
        getMemberName = { _currentMemberName.value },
        getDeviceName = { _currentDeviceName.value },
        getMacAddress = { macAddress },
        onPeerDiscovered = { peer ->
            Log.i(tag, "Auto-discovered peer: ${peer.memberName} (${peer.deviceName}) at ${peer.hostAddress}:${peer.port}. Storing as Family Member & syncing...")
            scope.launch {
                repository.registerOrUpdatePeerAsFamilyMember(
                    deviceId = peer.deviceId,
                    name = peer.memberName,
                    deviceName = peer.deviceName,
                    macAddress = peer.macAddress,
                    hostAddress = peer.hostAddress,
                    port = peer.port,
                    role = peer.deviceName.ifBlank { "Family Device" },
                    status = "ONLINE"
                )
                delay(800)
                syncWithPeer(peer)
            }
        }
    )

    val discoveredPeers: StateFlow<Map<String, PeerDevice>> = discovery.discoveredPeers

    fun setCurrentMember(name: String) {
        updateProfile(name, _currentDeviceName.value)
    }

    fun updateProfile(name: String, deviceName: String) {
        _currentMemberName.value = name
        _currentDeviceName.value = deviceName
        prefs.edit()
            .putString("current_member_name", name)
            .putString("current_device_name", deviceName)
            .apply()
        scope.launch {
            // Also update self member in repository with device name and internal MAC
            repository.saveMember(
                com.example.data.model.FamilyMemberEntity(
                    id = "self_member_profile",
                    deviceId = deviceId,
                    name = name,
                    deviceName = deviceName,
                    macAddress = macAddress,
                    role = "This Device (Primary)",
                    avatarColorHex = "#00D4B2",
                    hostAddress = _localIp.value,
                    port = _localPort.value,
                    isSelf = true,
                    connectionStatus = "ONLINE",
                    lastConnectedAt = System.currentTimeMillis()
                )
            )
        }
        // Restart discovery advertisement with updated member & device name
        restartServices()
    }

    fun start() {
        val port = server.start(8976)
        _localPort.value = port
        _localIp.value = NetworkUtils.getLocalIpAddress()
        discovery.start(port)
        _syncStatusMessage.value = "Listening on port $port • Scanning Wi-Fi peers"
    }

    private fun restartServices() {
        discovery.stop()
        server.stop()
        start()
    }

    suspend fun syncWithPeer(peer: PeerDevice): Result<SyncMergeResult> {
        _isSyncing.value = true
        _syncStatusMessage.value = "Syncing with ${peer.memberName}..."
        discovery.updatePeerStatus(peer.deviceId, PeerSyncStatus.SYNCING)

        val result = client.syncWithPeer(peer)
        if (result.isSuccess) {
            val merge = result.getOrNull()
            val now = System.currentTimeMillis()
            _lastSyncedTime.value = now
            prefs.edit().putLong("last_synced_time", now).apply()
            discovery.updatePeerStatus(peer.deviceId, PeerSyncStatus.SYNCED)
            val txCount = (merge?.transactionsProcessed ?: 0) + (merge?.invoicesProcessed ?: 0)
            _syncStatusMessage.value = "Synced with ${peer.memberName}: $txCount record(s) merged"
        } else {
            val err = result.exceptionOrNull()?.message ?: "App closed or offline"
            discovery.updatePeerStatus(peer.deviceId, PeerSyncStatus.FAILED, err)
            _syncStatusMessage.value = "Sync failed with ${peer.memberName}: $err"
        }

        _isSyncing.value = false
        return result
    }

    suspend fun syncWithAllPeers() {
        _isSyncing.value = true
        _syncStatusMessage.value = "Scanning & syncing with family devices on Wi-Fi..."

        val allMembers: List<FamilyMemberEntity> = repository.getAllMembersSnapshot()
        val pairedMembers: List<FamilyMemberEntity> = allMembers.filter { !it.isSelf }
        val discovered: Map<String, PeerDevice> = discovery.discoveredPeers.value

        if (pairedMembers.isEmpty() && discovered.isEmpty()) {
            _syncStatusMessage.value = "No paired family devices found. Tap '+ Pair Device' to connect."
            _isSyncing.value = false
            return
        }

        val results = mutableListOf<DeviceSyncResult>()

        // 1. First sync with all formally paired family members
        for (member in pairedMembers) {
            val friendlyDev = if (member.deviceName.isNotBlank()) member.deviceName else member.role

            // Find active discovered peer by deviceId or MAC or Name to get latest hostAddress
            val activePeer = discovered[member.deviceId]
                ?: discovered.values.find { (it.macAddress.isNotEmpty() && it.macAddress == member.macAddress) || it.memberName.equals(member.name, ignoreCase = true) }

            val targetIp = if (activePeer != null && activePeer.hostAddress.isNotBlank()) {
                activePeer.hostAddress
            } else {
                member.hostAddress
            }
            val targetPort = if (activePeer != null && activePeer.port > 0) {
                activePeer.port
            } else if (member.port > 0) {
                member.port
            } else {
                8976
            }

            if (targetIp.isBlank()) {
                val errMsg = "Unavailable: Device address unknown (app not open on Wi-Fi)"
                results.add(
                    DeviceSyncResult(
                        memberName = member.name,
                        deviceName = friendlyDev,
                        isSuccess = false,
                        recordsProcessed = 0,
                        statusMessage = errMsg
                    )
                )
                repository.saveMember(member.copy(connectionStatus = "OFFLINE"))
                continue
            }

            val peerToSync = PeerDevice(
                deviceId = member.deviceId,
                deviceName = friendlyDev,
                memberName = member.name,
                macAddress = member.macAddress,
                hostAddress = targetIp,
                port = targetPort,
                lastSeen = System.currentTimeMillis(),
                syncStatus = PeerSyncStatus.SYNCING
            )

            val syncRes = syncWithPeer(peerToSync)
            if (syncRes.isSuccess) {
                val merge = syncRes.getOrNull()
                val count = (merge?.transactionsProcessed ?: 0) + (merge?.invoicesProcessed ?: 0)
                results.add(
                    DeviceSyncResult(
                        memberName = member.name,
                        deviceName = friendlyDev,
                        isSuccess = true,
                        recordsProcessed = count,
                        statusMessage = "Synced successfully ($count records updated)"
                    )
                )
                repository.saveMember(
                    member.copy(
                        connectionStatus = "SYNCED",
                        lastConnectedAt = System.currentTimeMillis(),
                        hostAddress = targetIp,
                        port = targetPort
                    )
                )
            } else {
                val err = syncRes.exceptionOrNull()?.message ?: "App closed or device offline"
                results.add(
                    DeviceSyncResult(
                        memberName = member.name,
                        deviceName = friendlyDev,
                        isSuccess = false,
                        recordsProcessed = 0,
                        statusMessage = err
                    )
                )
                repository.saveMember(member.copy(connectionStatus = "OFFLINE"))
            }
        }

        // 2. Also sync with any discovered peers not yet in paired members
        val pairedDeviceIds = pairedMembers.map { it.deviceId }.toSet()
        val extraPeers = discovered.values.filter { it.deviceId !in pairedDeviceIds && it.deviceId != deviceId }
        for (extraPeer in extraPeers) {
            val syncRes = syncWithPeer(extraPeer)
            if (syncRes.isSuccess) {
                val merge = syncRes.getOrNull()
                val count = (merge?.transactionsProcessed ?: 0) + (merge?.invoicesProcessed ?: 0)
                results.add(
                    DeviceSyncResult(
                        memberName = extraPeer.memberName,
                        deviceName = extraPeer.deviceName,
                        isSuccess = true,
                        recordsProcessed = count,
                        statusMessage = "Synced successfully ($count records)"
                    )
                )
            } else {
                val err = syncRes.exceptionOrNull()?.message ?: "App closed or device offline"
                results.add(
                    DeviceSyncResult(
                        memberName = extraPeer.memberName,
                        deviceName = extraPeer.deviceName,
                        isSuccess = false,
                        recordsProcessed = 0,
                        statusMessage = err
                    )
                )
            }
        }

        val total = results.size
        val successCount = results.count { it.isSuccess }
        val failedCount = results.count { !it.isSuccess }

        _lastSyncSummary.value = SyncRunSummary(
            total = total,
            successCount = successCount,
            failedCount = failedCount,
            timestamp = System.currentTimeMillis(),
            deviceResults = results
        )

        when {
            successCount == total && total > 0 -> {
                _syncStatusMessage.value = "All $total devices synced successfully • All devices online"
            }
            successCount > 0 && failedCount > 0 -> {
                _syncStatusMessage.value = "Partial sync: $successCount of $total online ($failedCount unavailable - apps may be closed)"
            }
            successCount == 0 && total > 0 -> {
                _syncStatusMessage.value = "Sync failed: 0 of $total devices available. Ensure Family Books is open on family devices."
            }
        }

        _isSyncing.value = false
    }

    suspend fun syncMember(member: FamilyMemberEntity): Result<SyncMergeResult> {
        val discovered = discovery.discoveredPeers.value
        val activePeer = discovered[member.deviceId]
            ?: discovered.values.find { (it.macAddress.isNotEmpty() && it.macAddress == member.macAddress) || it.memberName.equals(member.name, ignoreCase = true) }
        val targetIp = activePeer?.hostAddress?.ifBlank { null } ?: member.hostAddress
        val targetPort = activePeer?.port ?: if (member.port > 0) member.port else 8976

        if (targetIp.isBlank()) {
            val err = "No Wi-Fi address for ${member.name}. Open Family Books on their device to connect."
            _syncStatusMessage.value = err
            repository.saveMember(member.copy(connectionStatus = "OFFLINE"))
            return Result.failure(Exception(err))
        }

        val peerToSync = PeerDevice(
            deviceId = member.deviceId,
            deviceName = member.deviceName.ifBlank { member.role },
            memberName = member.name,
            macAddress = member.macAddress,
            hostAddress = targetIp,
            port = targetPort,
            lastSeen = System.currentTimeMillis(),
            syncStatus = PeerSyncStatus.SYNCING
        )

        val res = syncWithPeer(peerToSync)
        if (res.isSuccess) {
            repository.saveMember(
                member.copy(
                    connectionStatus = "SYNCED",
                    lastConnectedAt = System.currentTimeMillis(),
                    hostAddress = targetIp,
                    port = targetPort
                )
            )
        } else {
            repository.saveMember(member.copy(connectionStatus = "OFFLINE"))
        }
        return res
    }

    fun scanLocalNetworkForPairing() {
        if (_isScanningNetwork.value) return
        scope.launch(Dispatchers.IO) {
            _isScanningNetwork.value = true
            _syncStatusMessage.value = "Scanning local Wi-Fi for active Family Books devices..."

            // Trigger fresh NSD discovery
            discovery.start(_localPort.value)

            val currentFound = mutableListOf<PeerDevice>()
            // Copy existing discovered peers from NSD
            discovery.discoveredPeers.value.values.forEach {
                if (it.deviceId != deviceId && !currentFound.any { ex -> ex.deviceId == it.deviceId }) {
                    currentFound.add(it)
                }
            }
            _scannedPairingDevices.value = currentFound.toList()

            // Subnet IP probe for live open apps on port 8976
            val myIp = _localIp.value
            if (myIp.contains(".") && !myIp.startsWith("127.")) {
                val prefix = myIp.substringBeforeLast(".") + "."
                val myLastOctet = myIp.substringAfterLast(".").toIntOrNull() ?: -1

                // Probe candidate IP range
                val ipsToScan = (1..254).filter { it != myLastOctet }.map { "$prefix$it" }
                for (chunk in ipsToScan.chunked(30)) {
                    chunk.map { ip ->
                        async {
                            val probe = client.probeProfile(ip, 8976)
                            if (probe.isSuccess && probe.deviceId != deviceId && probe.isAppOpen) {
                                val peer = PeerDevice(
                                    deviceId = probe.deviceId,
                                    deviceName = probe.deviceName.ifBlank { "Family Device" },
                                    memberName = probe.memberName,
                                    macAddress = probe.macAddress,
                                    hostAddress = ip,
                                    port = probe.port,
                                    lastSeen = System.currentTimeMillis(),
                                    syncStatus = PeerSyncStatus.DISCOVERED
                                )
                                synchronized(currentFound) {
                                    if (!currentFound.any { it.deviceId == peer.deviceId }) {
                                        currentFound.add(peer)
                                        discovery.addManualPeer(ip, probe.port, peer.memberName, peer.deviceName)
                                    }
                                }
                                _scannedPairingDevices.value = currentFound.toList()
                            }
                        }
                    }.awaitAll()
                }
            }

            delay(1000)
            discovery.discoveredPeers.value.values.forEach {
                if (it.deviceId != deviceId && !currentFound.any { ex -> ex.deviceId == it.deviceId }) {
                    currentFound.add(it)
                }
            }
            _scannedPairingDevices.value = currentFound.toList()
            _isScanningNetwork.value = false
            _syncStatusMessage.value = if (currentFound.isEmpty()) {
                "Scan complete: No active devices found. Ensure app is open on other device."
            } else {
                "Found ${currentFound.size} device(s) ready for pairing"
            }
        }
    }

    fun dismissSyncSummary() {
        _lastSyncSummary.value = null
    }

    fun removePeer(deviceId: String) {
        discovery.removePeer(deviceId)
        _scannedPairingDevices.value = _scannedPairingDevices.value.filter { it.deviceId != deviceId }
    }

    fun pairDiscoveredDevice(peer: PeerDevice) {
        scope.launch {
            val friendlyDevice = peer.deviceName.ifBlank { "Family Device" }
            repository.registerOrUpdatePeerAsFamilyMember(
                deviceId = peer.deviceId,
                name = peer.memberName,
                deviceName = friendlyDevice,
                macAddress = peer.macAddress,
                hostAddress = peer.hostAddress,
                port = peer.port,
                role = friendlyDevice,
                status = "ONLINE"
            )
            _syncStatusMessage.value = "Paired with ${peer.memberName} ($friendlyDevice)"
            syncWithPeer(peer)
        }
    }

    fun connectDirectPeer(ip: String, port: Int, memberName: String) {
        val peer = discovery.addManualPeer(ip, port, memberName)
        scope.launch {
            repository.registerOrUpdatePeerAsFamilyMember(
                deviceId = peer.deviceId,
                name = memberName,
                hostAddress = ip,
                port = port,
                role = "Direct Wi-Fi Device",
                status = "CONNECTING"
            )
            syncWithPeer(peer)
        }
    }

    fun pairFamilyDevice(name: String, deviceName: String, sampleExpense: Double = 0.0) {
        scope.launch {
            val devId = "family_dev_" + name.lowercase().replace(" ", "_")
            val ip = "192.168.1." + (10 + (kotlin.math.abs(name.hashCode()) % 200))
            val internalMac = NetworkUtils.generateDeterministicMac(devId + deviceName)
            val friendlyDevice = if (deviceName.isNotBlank()) deviceName else "Family Device"
            repository.registerOrUpdatePeerAsFamilyMember(
                deviceId = devId,
                name = name,
                deviceName = friendlyDevice,
                macAddress = internalMac,
                hostAddress = ip,
                port = 8976,
                role = friendlyDevice,
                status = "SYNCED"
            )
            val peer = discovery.addManualPeer(ip, 8976, name, friendlyDevice)
            if (sampleExpense > 0) {
                repository.saveTransaction(
                    com.example.data.model.TransactionEntity(
                        type = "EXPENSE",
                        amount = sampleExpense,
                        category = "Groceries",
                        paymentMethod = "Cash",
                        paidBy = name,
                        notes = "Initial shared expense from $friendlyDevice ($name)",
                        deviceId = devId
                    )
                )
            }
            _syncStatusMessage.value = "Paired family device '$name' ($friendlyDevice)"
        }
    }

    fun pairSimulatedFamilyDevice(name: String, role: String, sampleExpense: Double = 50.0) {
        pairFamilyDevice(name, role, sampleExpense)
    }

    fun stop() {
        discovery.stop()
        server.stop()
    }
}
