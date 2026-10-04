package com.example.sync

import android.util.Log
import com.example.data.model.PeerDevice
import com.example.data.model.SyncLogEntity
import com.example.data.repository.FamilyBooksRepository
import com.example.data.repository.SyncMergeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ProbeProfileResult(
    val isSuccess: Boolean,
    val deviceId: String = "",
    val memberName: String = "",
    val deviceName: String = "",
    val macAddress: String = "",
    val hostAddress: String = "",
    val port: Int = 8976,
    val isAppOpen: Boolean = false,
    val errorMessage: String? = null
)

class P2PClient(
    private val repository: FamilyBooksRepository,
    private val getDeviceId: () -> String,
    private val getMemberName: () -> String,
    private val getDeviceName: () -> String = { NetworkUtils.getDeviceModelName() },
    private val getMacAddress: () -> String = { "" }
) {
    private val tag = "P2PClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(peerIp: String, peerPort: Int): Boolean = withContext(Dispatchers.IO) {
        probeProfile(peerIp, peerPort).isSuccess
    }

    suspend fun probeProfile(peerIp: String, peerPort: Int): ProbeProfileResult = withContext(Dispatchers.IO) {
        try {
            val url = "http://$peerIp:$peerPort/handshake"
            val request = Request.Builder().url(url).get().build()
            probeClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    if (json.optString("app") == "FamilyBooks") {
                        val devId = json.optString("deviceId", "peer_$peerIp")
                        val member = json.optString("memberName", "Family Member")
                        val devName = json.optString("deviceName", "Family Device")
                        val mac = json.optString("macAddress", "")
                        val port = json.optInt("port", peerPort)
                        return@withContext ProbeProfileResult(
                            isSuccess = true,
                            deviceId = devId,
                            memberName = member,
                            deviceName = devName,
                            macAddress = mac,
                            hostAddress = peerIp,
                            port = port,
                            isAppOpen = true
                        )
                    }
                }
                ProbeProfileResult(
                    isSuccess = false,
                    hostAddress = peerIp,
                    port = peerPort,
                    errorMessage = "Not a Family Books service"
                )
            }
        } catch (e: ConnectException) {
            ProbeProfileResult(
                isSuccess = false,
                hostAddress = peerIp,
                port = peerPort,
                errorMessage = "App is not open or port unreachable"
            )
        } catch (e: SocketTimeoutException) {
            ProbeProfileResult(
                isSuccess = false,
                hostAddress = peerIp,
                port = peerPort,
                errorMessage = "Connection timed out"
            )
        } catch (e: Exception) {
            ProbeProfileResult(
                isSuccess = false,
                hostAddress = peerIp,
                port = peerPort,
                errorMessage = e.message ?: "Unreachable"
            )
        }
    }

    suspend fun syncWithPeer(peer: PeerDevice): Result<SyncMergeResult> = withContext(Dispatchers.IO) {
        val remoteUrl = "http://${peer.hostAddress}:${peer.port}/sync"
        try {
            // 1. Create local snapshot to push to peer
            val localPayload = repository.createSyncPayload(
                deviceId = getDeviceId(),
                memberName = getMemberName(),
                deviceName = getDeviceName(),
                macAddress = getMacAddress(),
                sinceTimestamp = 0L
            )
            val jsonBody = JsonSerializer.serializeSyncPayload(localPayload)

            val request = Request.Builder()
                .url(remoteUrl)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    repository.logSyncEvent(
                        SyncLogEntity(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            peerName = peer.memberName,
                            peerIp = peer.hostAddress,
                            direction = "OUTGOING",
                            status = "FAILED",
                            message = "Sync failed to ${peer.memberName}: $errorMsg"
                        )
                    )
                    return@withContext Result.failure(Exception("Sync failed ($errorMsg)"))
                }

                val responseString = response.body?.string() ?: ""
                val peerPayload = JsonSerializer.deserializeSyncPayload(responseString)

                // 2. Merge peer's state into our local Room database with Last-Write-Wins and register as Family Member
                val mergeResult = repository.mergeIncomingSyncPayload(peerPayload, peer.hostAddress, peer.port)

                repository.logSyncEvent(
                    SyncLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        peerName = peer.memberName,
                        peerIp = peer.hostAddress,
                        direction = "OUTGOING",
                        status = "SUCCESS",
                        message = "Synced with ${peer.memberName} (${peer.hostAddress}:${peer.port}): Received & merged ${mergeResult.transactionsProcessed} txs, ${mergeResult.invoicesProcessed} invoices (${mergeResult.conflictsResolved} conflicts resolved). Sent ${localPayload.transactions.size} txs.",
                        recordsProcessed = mergeResult.transactionsProcessed + mergeResult.invoicesProcessed
                    )
                )

                Result.success(mergeResult)
            }
        } catch (e: Exception) {
            val userFriendlyError = when (e) {
                is ConnectException -> "Connection refused: Family Books app is not open on ${peer.memberName}'s device"
                is SocketTimeoutException -> "Timed out: Device is unreachable on Wi-Fi"
                else -> "Connection error: ${e.message ?: "Failed to connect"}"
            }
            Log.e(tag, "Sync exception to $remoteUrl: $userFriendlyError")
            repository.logSyncEvent(
                SyncLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    peerName = peer.memberName.ifEmpty { peer.hostAddress },
                    peerIp = peer.hostAddress,
                    direction = "OUTGOING",
                    status = "FAILED",
                    message = "Sync failed with ${peer.memberName}: $userFriendlyError"
                )
            )
            Result.failure(Exception(userFriendlyError, e))
        }
    }
}
