package com.example.data.repository

import com.example.data.local.FamilyMemberDao
import com.example.data.local.InvoiceDao
import com.example.data.local.TransactionDao
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.data.model.ProfileFinancialSummary
import com.example.data.model.TransactionEntity
import com.example.data.model.remainingAmount
import com.example.data.util.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class FamilyBooksRepository(
    private val transactionDao: TransactionDao,
    private val invoiceDao: InvoiceDao,
    private val familyMemberDao: FamilyMemberDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllActiveTransactions()
    val allInvoices: Flow<List<FamilyInvoiceEntity>> = invoiceDao.getAllActiveInvoices()
    val allMembers: Flow<List<FamilyMemberEntity>> = familyMemberDao.getAllActiveMembers()

    /**
     * Real-time per-profile financial & liability summary combining transactions,
     * share contributions, family IOUs, and third-party debts/receivables.
     */
    val profileSummaries: Flow<List<ProfileFinancialSummary>> = combine(
        allTransactions,
        allInvoices,
        allMembers
    ) { transactions, invoices, members ->
        computeProfileSummaries(transactions, invoices, members)
    }

    // --- Transaction CRUD ---
    suspend fun saveTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        val toSave = transaction.copy(
            updatedAt = now,
            createdAt = if (transaction.createdAt <= 0L) now else transaction.createdAt
        )
        transactionDao.insertOrUpdate(toSave)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }

    // --- Liability / Debt CRUD ---
    suspend fun saveInvoice(invoice: FamilyInvoiceEntity) {
        val now = System.currentTimeMillis()
        val computedStatus = when {
            invoice.paidAmount >= invoice.amount - 0.005 && invoice.amount > 0 -> "SETTLED"
            invoice.paidAmount > 0.005 -> "PARTIAL"
            else -> invoice.status.ifBlank { "UNPAID" }
        }
        val toSave = invoice.copy(
            status = computedStatus,
            settledAt = if (computedStatus == "SETTLED") (invoice.settledAt ?: now) else null,
            updatedAt = now,
            createdAt = if (invoice.createdAt <= 0L) now else invoice.createdAt
        )
        invoiceDao.insertOrUpdate(toSave)
    }

    suspend fun recordLiabilityPayment(id: String, paymentAmount: Double) {
        if (paymentAmount <= 0.0) return
        val existing = invoiceDao.getInvoiceById(id) ?: return
        val now = System.currentTimeMillis()
        val newPaid = (existing.paidAmount + paymentAmount).coerceAtMost(existing.amount)
        val isFullyPaid = newPaid >= existing.amount - 0.005
        val updated = existing.copy(
            paidAmount = newPaid,
            status = if (isFullyPaid) "SETTLED" else "PARTIAL",
            settledAt = if (isFullyPaid) now else null,
            updatedAt = now
        )
        invoiceDao.insertOrUpdate(updated)
    }

    suspend fun settleInvoice(id: String) {
        val now = System.currentTimeMillis()
        invoiceDao.markAsSettled(id = id, settledAt = now, updatedAt = now)
    }

    suspend fun deleteInvoice(id: String) {
        invoiceDao.softDelete(id, System.currentTimeMillis())
    }

    // --- Family Profile CRUD ---
    suspend fun saveMember(member: FamilyMemberEntity) {
        val now = System.currentTimeMillis()
        if (member.isPrimary) {
            familyMemberDao.clearPrimaryFlags()
        }
        val toSave = member.copy(
            name = member.name.trim(),
            role = member.role.trim().ifEmpty { "Member" },
            updatedAt = now,
            createdAt = if (member.createdAt <= 0L) now else member.createdAt
        )
        familyMemberDao.insertOrUpdate(toSave)
    }

    suspend fun setPrimaryMember(id: String) {
        val target = familyMemberDao.getMemberById(id) ?: return
        familyMemberDao.clearPrimaryFlags()
        familyMemberDao.insertOrUpdate(
            target.copy(isPrimary = true, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteMember(id: String) {
        familyMemberDao.softDelete(id, System.currentTimeMillis())
    }

    // --- Bulk Import Ledger Records ---
    suspend fun importLedgerRecords(
        transactions: List<TransactionEntity>,
        invoices: List<FamilyInvoiceEntity>,
        profiles: List<FamilyMemberEntity>,
        year: Int?,
        months: Set<Int>
    ): ImportResult {
        var txCount = 0
        var invCount = 0
        var profCount = 0
        val now = System.currentTimeMillis()

        for (prof in profiles) {
            val existingById = familyMemberDao.getMemberById(prof.id)
            val existingByName = familyMemberDao.getMemberByName(prof.name)
            if (existingById == null && existingByName == null) {
                familyMemberDao.insertOrUpdate(
                    prof.copy(
                        id = prof.id.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = now
                    )
                )
                profCount++
            }
        }

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

        return ImportResult(
            transactionsImported = txCount,
            invoicesImported = invCount,
            profilesImported = profCount,
            year = year,
            monthCount = months.size,
            message = "Imported $txCount entries, $invCount liabilities" +
                (if (profCount > 0) ", $profCount profiles" else "") + " $scopeMsg"
        )
    }

    companion object {
        private val genericBuckets = setOf("", "family", "shared", "shared family", "none", "unassigned", "third party")

        fun computeProfileSummaries(
            transactions: List<TransactionEntity>,
            invoices: List<FamilyInvoiceEntity>,
            members: List<FamilyMemberEntity>
        ): List<ProfileFinancialSummary> {
            val explicitByLowerName = members.associateBy { it.name.trim().lowercase() }

            // Start with explicit profiles, plus any non-generic family member names referenced in transactions/family IOUs
            val referencedNames = buildSet {
                members.forEach { add(it.name.trim()) }
                transactions.forEach { tx ->
                    if (tx.paidBy.trim().lowercase() !in genericBuckets) add(tx.paidBy.trim())
                    if (tx.forWhom.trim().lowercase() !in genericBuckets &&
                        explicitByLowerName.containsKey(tx.forWhom.trim().lowercase())
                    ) {
                        add(tx.forWhom.trim())
                    }
                    if (tx.contributedBy.trim().lowercase() !in genericBuckets &&
                        explicitByLowerName.containsKey(tx.contributedBy.trim().lowercase())
                    ) {
                        add(tx.contributedBy.trim())
                    }
                }
                invoices.forEach { inv ->
                    if (inv.linkedProfile.trim().lowercase() !in genericBuckets) {
                        add(inv.linkedProfile.trim())
                    }
                    if (inv.liabilityType == LiabilityType.FAMILY_IOU) {
                        if (inv.fromMember.trim().lowercase() !in genericBuckets) add(inv.fromMember.trim())
                        if (inv.toMember.trim().lowercase() !in genericBuckets) add(inv.toMember.trim())
                    }
                }
            }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }

            return referencedNames.map { name ->
                val memberEntity = explicitByLowerName[name.lowercase()]

                val totalIncome = transactions
                    .filter { it.type == "INCOME" && it.paidBy.equals(name, ignoreCase = true) }
                    .sumOf { it.amount }

                val totalExpensePaid = transactions
                    .filter { it.type == "EXPENSE" && it.paidBy.equals(name, ignoreCase = true) }
                    .sumOf { it.amount }

                val totalExpenseForThem = transactions
                    .filter { it.type == "EXPENSE" && it.forWhom.equals(name, ignoreCase = true) }
                    .sumOf { it.amount }

                val contributionsProvided = transactions
                    .filter { it.contributedBy.equals(name, ignoreCase = true) && it.contributedAmount > 0 }
                    .sumOf { it.contributedAmount }

                val contributionsReceived = transactions
                    .filter { it.paidBy.equals(name, ignoreCase = true) && it.contributedAmount > 0 }
                    .sumOf { it.contributedAmount }

                val activeInvoices = invoices.filter { it.status != "SETTLED" }

                val familyOwedToThem = activeInvoices
                    .filter {
                        it.liabilityType == LiabilityType.FAMILY_IOU &&
                            it.toMember.equals(name, ignoreCase = true)
                    }
                    .sumOf { it.remainingAmount }

                val familyTheyOwe = activeInvoices
                    .filter {
                        it.liabilityType == LiabilityType.FAMILY_IOU &&
                            it.fromMember.equals(name, ignoreCase = true)
                    }
                    .sumOf { it.remainingAmount }

                val thirdPartyOwedToThem = activeInvoices
                    .filter { inv ->
                        inv.liabilityType == LiabilityType.THIRD_PARTY_RECEIVABLE &&
                            (inv.linkedProfile.equals(name, ignoreCase = true) ||
                                (inv.linkedProfile.isBlank() && inv.toMember.equals(name, ignoreCase = true)))
                    }
                    .sumOf { inv -> inv.remainingAmount }

                val thirdPartyTheyOwe = activeInvoices
                    .filter { inv ->
                        inv.liabilityType == LiabilityType.THIRD_PARTY_PAYABLE &&
                            (inv.linkedProfile.equals(name, ignoreCase = true) ||
                                (inv.linkedProfile.isBlank() && inv.fromMember.equals(name, ignoreCase = true)))
                    }
                    .sumOf { inv -> inv.remainingAmount }

                val netStanding = (familyOwedToThem + thirdPartyOwedToThem) - (familyTheyOwe + thirdPartyTheyOwe)

                ProfileFinancialSummary(
                    profileId = memberEntity?.id ?: "",
                    profileName = memberEntity?.name ?: name,
                    role = memberEntity?.role ?: "Contributor",
                    avatarColorHex = memberEntity?.avatarColorHex ?: "#0A2540",
                    isPrimary = memberEntity?.isPrimary ?: false,
                    isExplicitProfile = memberEntity != null,
                    totalIncome = totalIncome,
                    totalExpensePaid = totalExpensePaid,
                    totalExpenseForThem = totalExpenseForThem,
                    contributionsProvided = contributionsProvided,
                    contributionsReceived = contributionsReceived,
                    familyOwedToThem = familyOwedToThem,
                    familyTheyOwe = familyTheyOwe,
                    thirdPartyOwedToThem = thirdPartyOwedToThem,
                    thirdPartyTheyOwe = thirdPartyTheyOwe,
                    netLiabilityStanding = netStanding
                )
            }.sortedWith(
                compareByDescending<ProfileFinancialSummary> { it.isExplicitProfile }
                    .thenByDescending { it.isPrimary }
                    .thenBy { it.profileName.lowercase() }
            )
        }
    }
}
