package com.example.data.repository

import com.example.data.local.FamilyMemberDao
import com.example.data.local.InvoiceDao
import com.example.data.local.SyncLogDao
import com.example.data.local.TransactionDao
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.MemberBalanceSummary
import com.example.data.model.PeerDevice
import com.example.data.model.SyncLogEntity
import com.example.data.model.SyncPayload
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class SyncMergeResult(
    val transactionsProcessed: Int = 0,
    val invoicesProcessed: Int = 0,
    val membersProcessed: Int = 0,
    val conflictsResolved: Int = 0,
    val connectedMemberName: String = ""
)

class FamilyBooksRepository(
    private val transactionDao: TransactionDao,
    private val invoiceDao: InvoiceDao,
    private val familyMemberDao: FamilyMemberDao,
    private val syncLogDao: SyncLogDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllActiveTransactions()
    val allInvoices: Flow<List<FamilyInvoiceEntity>> = invoiceDao.getAllActiveInvoices()
    val allMembers: Flow<List<FamilyMemberEntity>> = familyMemberDao.getAllActiveMembers()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentLogs()

    suspend fun getAllMembersSnapshot(): List<FamilyMemberEntity> {
        return familyMemberDao.getAllActiveMembersSnapshot()
    }

    // Real-time calculation of balances across all stored family members
    val memberBalances: Flow<List<MemberBalanceSummary>> = combine(
        allTransactions,
        allInvoices,
        allMembers
    ) { transactions, invoices, members ->
        // Get all members from database, plus any referenced in logs
        val memberMap = members.associateBy { it.name.lowercase() }
        val allNames = (members.map { it.name } +
                transactions.map { it.paidBy } +
                invoices.map { it.fromMember } +
                invoices.map { it.toMember })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        allNames.map { name ->
            val memberEntity = memberMap[name.lowercase()]
            val totalPaidForExpenses = transactions
                .filter { it.paidBy.equals(name, ignoreCase = true) && it.type == "EXPENSE" }
                .sumOf { it.amount }

            // Unpaid invoices where others owe this member
            val totalOwedToThem = invoices
                .filter { it.toMember.equals(name, ignoreCase = true) && it.status == "UNPAID" }
                .sumOf { it.amount }

            // Unpaid invoices where this member owes others
            val totalTheyOwe = invoices
                .filter { it.fromMember.equals(name, ignoreCase = true) && it.status == "UNPAID" }
                .sumOf { it.amount }

            MemberBalanceSummary(
                memberName = name,
                deviceId = memberEntity?.deviceId ?: "",
                deviceName = memberEntity?.deviceName ?: memberEntity?.role ?: "",
                isSelf = memberEntity?.isSelf ?: false,
                hostAddress = memberEntity?.hostAddress ?: "",
                isOnline = memberEntity?.connectionStatus == "ONLINE" || memberEntity?.connectionStatus == "SYNCED",
                totalPaid = totalPaidForExpenses,
                totalOwedToThem = totalOwedToThem,
                totalTheyOwe = totalTheyOwe,
                netBalance = totalOwedToThem - totalTheyOwe
            )
        }
    }

    // --- Transaction CRUD ---
    suspend fun saveTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        val toSave = transaction.copy(
            updatedAt = now,
            createdAt = if (transaction.createdAt <= 0) now else transaction.createdAt
        )
        transactionDao.insertOrUpdate(toSave)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }

    // --- Invoice CRUD ---
    suspend fun saveInvoice(invoice: FamilyInvoiceEntity) {
        val now = System.currentTimeMillis()
        val toSave = invoice.copy(
            updatedAt = now,
            createdAt = if (invoice.createdAt <= 0) now else invoice.createdAt
        )
        invoiceDao.insertOrUpdate(toSave)
    }

    suspend fun settleInvoice(id: String) {
        val now = System.currentTimeMillis()
        invoiceDao.markAsSettled(id = id, settledAt = now, updatedAt = now)
    }

    suspend fun deleteInvoice(id: String) {
        invoiceDao.softDelete(id, System.currentTimeMillis())
    }

    // --- Family Member CRUD & P2P Device Registration ---
    suspend fun saveMember(member: FamilyMemberEntity) {
        val now = System.currentTimeMillis()
        val toSave = member.copy(
            updatedAt = now,
            createdAt = if (member.createdAt <= 0) now else member.createdAt
        )
        familyMemberDao.insertOrUpdate(toSave)
    }

    suspend fun deleteMember(id: String) {
        familyMemberDao.softDelete(id, System.currentTimeMillis())
    }

    /**
     * When connecting with another device, store his profile and make him a family member.
     * Uses deviceName and persistent MAC/device identifier internally to handle dynamic IP changes.
     */
    suspend fun registerOrUpdatePeerAsFamilyMember(
        deviceId: String,
        name: String,
        deviceName: String = "",
        macAddress: String = "",
        hostAddress: String,
        port: Int = 8976,
        role: String = "Family Device",
        status: String = "ONLINE"
    ): FamilyMemberEntity {
        val now = System.currentTimeMillis()
        val existingByMac = if (macAddress.isNotEmpty()) familyMemberDao.getMemberByMacAddress(macAddress) else null
        val existingByDevice = if (deviceId.isNotEmpty()) familyMemberDao.getMemberByDeviceId(deviceId) else null
        val existingByName = familyMemberDao.getMemberByName(name)

        val target = existingByMac ?: existingByDevice ?: existingByName
        val friendlyDeviceName = if (deviceName.isNotBlank()) deviceName else (target?.deviceName?.ifEmpty { role } ?: role)
        val memberToSave = if (target != null) {
            target.copy(
                deviceId = if (deviceId.isNotEmpty()) deviceId else target.deviceId,
                name = name,
                deviceName = friendlyDeviceName,
                macAddress = if (macAddress.isNotEmpty()) macAddress else target.macAddress,
                hostAddress = if (hostAddress.isNotEmpty()) hostAddress else target.hostAddress,
                port = if (port > 0) port else target.port,
                connectionStatus = status,
                lastConnectedAt = now,
                updatedAt = now
            )
        } else {
            FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId.ifEmpty { "peer_" + UUID.randomUUID().toString().take(8) },
                name = name,
                deviceName = friendlyDeviceName,
                macAddress = macAddress,
                role = role,
                avatarColorHex = "#0A2540",
                hostAddress = hostAddress,
                port = port,
                isSelf = false,
                connectionStatus = status,
                lastConnectedAt = now,
                createdAt = now,
                updatedAt = now
            )
        }
        familyMemberDao.insertOrUpdate(memberToSave)
        return memberToSave
    }

    // --- Sync Log ---
    suspend fun logSyncEvent(log: SyncLogEntity) {
        syncLogDao.insertLog(log)
    }

    // --- P2P Decentralized Sync & LWW Conflict Resolution Engine ---

    /**
     * Prepares outgoing payload containing all entities (or those modified since `sinceTimestamp`).
     */
    suspend fun createSyncPayload(
        deviceId: String,
        memberName: String,
        deviceName: String = "",
        macAddress: String = "",
        sinceTimestamp: Long = 0L
    ): SyncPayload {
        val txs = if (sinceTimestamp <= 0L) {
            transactionDao.getAllActiveTransactionsSnapshot()
        } else {
            transactionDao.getTransactionsModifiedSince(sinceTimestamp)
        }

        val invs = if (sinceTimestamp <= 0L) {
            invoiceDao.getAllActiveInvoicesSnapshot()
        } else {
            invoiceDao.getInvoicesModifiedSince(sinceTimestamp)
        }

        val mems = if (sinceTimestamp <= 0L) {
            familyMemberDao.getAllActiveMembersSnapshot()
        } else {
            familyMemberDao.getMembersModifiedSince(sinceTimestamp)
        }

        return SyncPayload(
            deviceId = deviceId,
            memberName = memberName,
            deviceName = deviceName,
            macAddress = macAddress,
            timestamp = System.currentTimeMillis(),
            transactions = txs,
            invoices = invs,
            members = mems
        )
    }

    /**
     * Merges incoming peer payload using Timestamp-Based / Last-Write-Wins (LWW) conflict resolution,
     * and automatically registers the sender device as a verified Family Member.
     */
    suspend fun mergeIncomingSyncPayload(
        payload: SyncPayload,
        remoteIp: String = "",
        remotePort: Int = 8976
    ): SyncMergeResult {
        var txCount = 0
        var invCount = 0
        var memCount = 0
        var conflictCount = 0

        // 1. AUTOMATICALLY STORE SENDER AS FAMILY MEMBER
        if (payload.memberName.isNotBlank()) {
            val peerDeviceName = payload.deviceName.ifEmpty { "Family Device" }
            registerOrUpdatePeerAsFamilyMember(
                deviceId = payload.deviceId,
                name = payload.memberName,
                deviceName = peerDeviceName,
                macAddress = payload.macAddress,
                hostAddress = remoteIp,
                port = remotePort,
                role = peerDeviceName,
                status = "SYNCED"
            )
            memCount++
        }

        // 2. Merge Transactions with LWW
        for (remoteTx in payload.transactions) {
            val localTx = transactionDao.getTransactionById(remoteTx.id)
            if (localTx == null) {
                transactionDao.insertOrUpdate(remoteTx)
                txCount++
            } else {
                if (remoteTx.updatedAt > localTx.updatedAt) {
                    transactionDao.insertOrUpdate(remoteTx)
                    txCount++
                    conflictCount++
                } else if (remoteTx.updatedAt < localTx.updatedAt) {
                    conflictCount++
                }
            }
        }

        // 3. Merge Invoices with LWW
        for (remoteInv in payload.invoices) {
            val localInv = invoiceDao.getInvoiceById(remoteInv.id)
            if (localInv == null) {
                invoiceDao.insertOrUpdate(remoteInv)
                invCount++
            } else {
                if (remoteInv.updatedAt > localInv.updatedAt) {
                    invoiceDao.insertOrUpdate(remoteInv)
                    invCount++
                    conflictCount++
                } else if (remoteInv.updatedAt < localInv.updatedAt) {
                    conflictCount++
                }
            }
        }

        // 4. Merge Family Members with LWW (do not overwrite isSelf = true)
        for (remoteMem in payload.members) {
            if (remoteMem.isSelf) {
                // Sender sent their own profile - register them as remote family member!
                registerOrUpdatePeerAsFamilyMember(
                    deviceId = remoteMem.deviceId,
                    name = remoteMem.name,
                    deviceName = remoteMem.deviceName.ifEmpty { remoteMem.role },
                    macAddress = remoteMem.macAddress,
                    hostAddress = if (remoteMem.hostAddress.isNotEmpty()) remoteMem.hostAddress else remoteIp,
                    port = if (remoteMem.port > 0) remoteMem.port else remotePort,
                    role = remoteMem.role,
                    status = "SYNCED"
                )
            } else {
                val localMem = familyMemberDao.getMemberById(remoteMem.id)
                    ?: (if (remoteMem.deviceId.isNotEmpty()) familyMemberDao.getMemberByDeviceId(remoteMem.deviceId) else null)
                    ?: familyMemberDao.getMemberByName(remoteMem.name)

                if (localMem == null) {
                    familyMemberDao.insertOrUpdate(remoteMem.copy(isSelf = false))
                    memCount++
                } else if (!localMem.isSelf && remoteMem.updatedAt > localMem.updatedAt) {
                    familyMemberDao.insertOrUpdate(remoteMem.copy(isSelf = false))
                    memCount++
                    conflictCount++
                }
            }
        }

        return SyncMergeResult(
            transactionsProcessed = txCount,
            invoicesProcessed = invCount,
            membersProcessed = memCount,
            conflictsResolved = conflictCount,
            connectedMemberName = payload.memberName
        )
    }

    // --- Bulk Import Ledger Records (for Year/Months selection) ---
    suspend fun importLedgerRecords(
        transactions: List<TransactionEntity>,
        invoices: List<FamilyInvoiceEntity>,
        year: Int?,
        months: Set<Int>
    ): com.example.data.util.ImportResult {
        var txCount = 0
        var invCount = 0
        val now = System.currentTimeMillis()

        for (tx in transactions) {
            val existing = transactionDao.getTransactionById(tx.id)
            if (existing == null || tx.updatedAt >= existing.updatedAt) {
                transactionDao.insertOrUpdate(tx.copy(updatedAt = now))
                txCount++
            }
        }

        for (inv in invoices) {
            val existing = invoiceDao.getInvoiceById(inv.id)
            if (existing == null || inv.updatedAt >= existing.updatedAt) {
                invoiceDao.insertOrUpdate(inv.copy(updatedAt = now))
                invCount++
            }
        }

        val scopeMsg = if (year != null) {
            if (months.isEmpty()) "for Full Year $year" else "for $year (${months.size} months)"
        } else "from file"

        return com.example.data.util.ImportResult(
            transactionsImported = txCount,
            invoicesImported = invCount,
            year = year,
            monthCount = months.size,
            message = "Successfully imported $txCount transactions and $invCount invoices $scopeMsg"
        )
    }
}
