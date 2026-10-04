package com.example.sync

import android.util.Log
import com.example.data.model.SyncLogEntity
import com.example.data.repository.FamilyBooksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class P2PSyncServer(
    private val repository: FamilyBooksRepository,
    private val scope: CoroutineScope,
    private val getDeviceId: () -> String,
    private val getMemberName: () -> String,
    private val getDeviceName: () -> String = { NetworkUtils.getDeviceModelName() },
    private val getMacAddress: () -> String = { "" }
) {
    private val tag = "P2PSyncServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    var boundPort: Int = 8976
        private set

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(preferredPort: Int = 8976): Int {
        if (isRunning) return boundPort

        try {
            serverSocket = try {
                ServerSocket(preferredPort)
            } catch (e: Exception) {
                Log.w(tag, "Preferred port $preferredPort occupied, binding to dynamic port...")
                ServerSocket(0)
            }
            boundPort = serverSocket?.localPort ?: preferredPort
            isRunning = true

            serverJob = scope.launch(Dispatchers.IO) {
                Log.i(tag, "P2P Server listening on port $boundPort")
                while (isActive && isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClient(socket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(tag, "Accept error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start P2P server: ${e.message}")
            isRunning = false
        }
        return boundPort
    }

    private suspend fun handleClient(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)

                // Read HTTP Request Line
                val requestLine = reader.readLine() ?: return@withContext
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@withContext

                val method = parts[0]
                val path = parts[1]

                // Read Headers
                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                val remoteIp = socket.inetAddress?.hostAddress ?: "Unknown"

                when {
                    method.equals("GET", ignoreCase = true) && path.startsWith("/handshake") -> {
                        val responseJson = JSONObject().apply {
                            put("status", "OK")
                            put("deviceId", getDeviceId())
                            put("memberName", getMemberName())
                            put("deviceName", getDeviceName())
                            put("macAddress", getMacAddress())
                            put("port", boundPort)
                            put("timestamp", System.currentTimeMillis())
                            put("app", "FamilyBooks")
                            put("readyForPairing", true)
                        }.toString()

                        sendHttpResponse(writer, 200, "OK", responseJson)
                    }

                    method.equals("POST", ignoreCase = true) && path.startsWith("/sync") -> {
                        val bodyChars = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val r = reader.read(bodyChars, read, contentLength - read)
                            if (r == -1) break
                            read += r
                        }
                        val requestBody = String(bodyChars, 0, read)

                        val incomingPayload = JsonSerializer.deserializeSyncPayload(requestBody)
                        val mergeResult = repository.mergeIncomingSyncPayload(incomingPayload, remoteIp, socket.port)

                        // Prepare outgoing payload to reply with local state
                        val localPayload = repository.createSyncPayload(
                            deviceId = getDeviceId(),
                            memberName = getMemberName(),
                            deviceName = getDeviceName(),
                            macAddress = getMacAddress(),
                            sinceTimestamp = 0L
                        )
                        val responseBody = JsonSerializer.serializeSyncPayload(localPayload)

                        repository.logSyncEvent(
                            SyncLogEntity(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                peerName = incomingPayload.memberName,
                                peerIp = remoteIp,
                                direction = "INCOMING",
                                status = "SUCCESS",
                                message = "Merged from ${incomingPayload.memberName}: ${mergeResult.transactionsProcessed} txs, ${mergeResult.invoicesProcessed} invoices (${mergeResult.conflictsResolved} conflicts resolved with LWW).",
                                recordsProcessed = mergeResult.transactionsProcessed + mergeResult.invoicesProcessed
                            )
                        )

                        sendHttpResponse(writer, 200, "OK", responseBody)
                    }

                    else -> {
                        sendHttpResponse(writer, 404, "Not Found", """{"error":"Not Found"}""")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Client processing error: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun sendHttpResponse(
        writer: OutputStreamWriter,
        statusCode: Int,
        statusText: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
        writer.flush()
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        serverJob = null
    }
}
