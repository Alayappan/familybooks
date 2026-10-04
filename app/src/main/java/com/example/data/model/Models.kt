package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["date"]),
        Index(value = ["paidBy"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val paymentMethod: String, // "Cash", "Card", "UPI / Bank Transfer", "Wallet", "Other"
    val paidBy: String, // Family Member name
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deviceId: String = ""
)

@Entity(
    tableName = "family_invoices",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["fromMember"]),
        Index(value = ["toMember"]),
        Index(value = ["status"])
    ]
)
data class FamilyInvoiceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fromMember: String, // Debtor: person who owes money
    val toMember: String, // Creditor: person who is owed money
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String = "",
    val status: String = "UNPAID", // "UNPAID", "SETTLED"
    val settledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deviceId: String = ""
)

@Entity(
    tableName = "family_members",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["deviceId"])
    ]
)
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String = "", // Direct link to P2P peer device
    val name: String,
    val deviceName: String = "", // Friendly model e.g. "Pixel 8", "Samsung S24"
    val macAddress: String = "", // Internal hardware MAC address to handle dynamic IP changes
    val role: String = "Family Device", // e.g. "Pixel 8", "Samsung S24"
    val avatarColorHex: String = "#0A2540",
    val hostAddress: String = "", // Local Wi-Fi IP (kept internally for socket connection)
    val port: Int = 8976,
    val isSelf: Boolean = false, // True for this device owner
    val lastConnectedAt: Long? = null,
    val connectionStatus: String = "OFFLINE", // "ONLINE", "SYNCED", "OFFLINE"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val peerName: String,
    val peerIp: String,
    val direction: String, // "INCOMING", "OUTGOING", "BIDIRECTIONAL"
    val status: String, // "SUCCESS", "FAILED", "CONFLICT_RESOLVED"
    val message: String,
    val recordsProcessed: Int = 0
)

data class PeerDevice(
    val deviceId: String,
    val deviceName: String, // Friendly model e.g. "Pixel 8"
    val memberName: String,
    val macAddress: String = "", // Kept internally for persistent identification
    val hostAddress: String, // Kept internally for socket connection
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val syncStatus: PeerSyncStatus = PeerSyncStatus.DISCOVERED,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null
)

enum class PeerSyncStatus {
    DISCOVERED,
    CONNECTING,
    SYNCING,
    SYNCED,
    FAILED
}

data class SyncPayload(
    val deviceId: String,
    val memberName: String,
    val deviceName: String = "",
    val macAddress: String = "",
    val appVersion: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val transactions: List<TransactionEntity> = emptyList(),
    val invoices: List<FamilyInvoiceEntity> = emptyList(),
    val members: List<FamilyMemberEntity> = emptyList()
)

data class MemberBalanceSummary(
    val memberName: String,
    val deviceId: String = "",
    val deviceName: String = "",
    val isSelf: Boolean = false,
    val hostAddress: String = "",
    val isOnline: Boolean = false,
    val totalPaid: Double,
    val totalOwedToThem: Double,
    val totalTheyOwe: Double,
    val netBalance: Double // totalOwedToThem - totalTheyOwe
)
