package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FamilyBooksDatabase
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.MemberBalanceSummary
import com.example.data.model.PeerDevice
import com.example.data.model.SyncLogEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FamilyBooksRepository
import com.example.sync.FamilyBooksSyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class FamilyBooksViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("familybooks_prefs", Context.MODE_PRIVATE)

    private val database = FamilyBooksDatabase.getDatabase(application, viewModelScope)
    private val repository = FamilyBooksRepository(
        transactionDao = database.transactionDao(),
        invoiceDao = database.invoiceDao(),
        familyMemberDao = database.familyMemberDao(),
        syncLogDao = database.syncLogDao()
    )

    val syncEngine = FamilyBooksSyncEngine(
        context = application,
        repository = repository,
        scope = viewModelScope
    )

    init {
        syncEngine.start()
    }

    // --- Core Data Flows ---
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<FamilyInvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<FamilyMemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberBalances: StateFlow<List<MemberBalanceSummary>> = repository.memberBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cash in Hand (Wallet / Cash Pool) Flow ---
    private val _cashPoolBaseline = MutableStateFlow(
        prefs.getFloat("cash_pool_baseline", 250f).toDouble()
    )
    val cashPoolBaseline: StateFlow<Double> = _cashPoolBaseline.asStateFlow()

    val cashInHandBalance: StateFlow<Double> = combine(
        allTransactions,
        cashPoolBaseline
    ) { txs, baseline ->
        val cashIncomes = txs.filter { it.type == "INCOME" && it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.amount }
        val cashExpenses = txs.filter { it.type == "EXPENSE" && it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.amount }
        baseline + cashIncomes - cashExpenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250.0)

    val cashSpentThisMonth: StateFlow<Double> = allTransactions.combine(_cashPoolBaseline) { txs, _ ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        txs.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear &&
                    it.type == "EXPENSE" &&
                    it.paymentMethod.equals("Cash", ignoreCase = true)
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refillCashPool(amount: Double, note: String) {
        viewModelScope.launch {
            // Log as Cash refill transaction
            val now = System.currentTimeMillis()
            val currentMember = syncEngine.currentMemberName.value
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = "INCOME",
                amount = amount,
                date = now,
                category = "Other",
                paymentMethod = "Cash",
                paidBy = currentMember,
                notes = if (note.isBlank()) "ATM Cash Refill" else note,
                createdAt = now,
                updatedAt = now
            )
            repository.saveTransaction(tx)
        }
    }

    fun setCashPoolBaseline(newTotal: Double) {
        val current = cashInHandBalance.value
        val adjustment = newTotal - current
        val newBaseline = _cashPoolBaseline.value + adjustment
        _cashPoolBaseline.value = newBaseline
        prefs.edit().putFloat("cash_pool_baseline", newBaseline.toFloat()).apply()
    }

    // --- Search & Filter States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL")
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedMemberFilter = MutableStateFlow("ALL")
    val selectedMemberFilter: StateFlow<String> = _selectedMemberFilter.asStateFlow()

    private val _selectedInvoiceStatusFilter = MutableStateFlow("ALL")
    val selectedInvoiceStatusFilter: StateFlow<String> = _selectedInvoiceStatusFilter.asStateFlow()

    // --- Filtered Transactions Flow ---
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedMemberFilter
    ) { txs, query, typeFilter, catFilter, memberFilter ->
        txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.paidBy.contains(query, ignoreCase = true) ||
                    tx.paymentMethod.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            val matchesType = typeFilter == "ALL" || tx.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = catFilter == "ALL" || tx.category.equals(catFilter, ignoreCase = true)
            val matchesMember = memberFilter == "ALL" || tx.paidBy.equals(memberFilter, ignoreCase = true)

            matchesQuery && matchesType && matchesCategory && matchesMember
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Invoices Flow ---
    val filteredInvoices: StateFlow<List<FamilyInvoiceEntity>> = combine(
        allInvoices,
        searchQuery,
        selectedInvoiceStatusFilter
    ) { invs, query, statusFilter ->
        invs.filter { inv ->
            val matchesQuery = query.isBlank() ||
                    inv.title.contains(query, ignoreCase = true) ||
                    inv.fromMember.contains(query, ignoreCase = true) ||
                    inv.toMember.contains(query, ignoreCase = true) ||
                    inv.notes.contains(query, ignoreCase = true) ||
                    inv.amount.toString().contains(query)

            val matchesStatus = statusFilter == "ALL" || inv.status.equals(statusFilter, ignoreCase = true)

            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Analytics ---
    data class DashboardAnalytics(
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val netCashFlow: Double = 0.0,
        val pendingInvoicesCount: Int = 0,
        val pendingInvoicesAmount: Double = 0.0,
        val categoryBreakdown: Map<String, Double> = emptyMap(),
        val topPayerName: String = "None",
        val topPayerAmount: Double = 0.0
    )

    val analytics: StateFlow<DashboardAnalytics> = combine(
        allTransactions,
        allInvoices
    ) { txs, invs ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthTxs = txs.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val totalIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense

        val pendingInvs = invs.filter { it.status == "UNPAID" }
        val pendingCount = pendingInvs.size
        val pendingAmount = pendingInvs.sumOf { it.amount }

        val categoryMap = mutableMapOf<String, Double>()
        val memberPayerMap = mutableMapOf<String, Double>()

        monthTxs.filter { it.type == "EXPENSE" }.forEach {
            categoryMap[it.category] = (categoryMap[it.category] ?: 0.0) + it.amount
            memberPayerMap[it.paidBy] = (memberPayerMap[it.paidBy] ?: 0.0) + it.amount
        }

        val topPayer = memberPayerMap.maxByOrNull { it.value }

        DashboardAnalytics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            pendingInvoicesCount = pendingCount,
            pendingInvoicesAmount = pendingAmount,
            categoryBreakdown = categoryMap,
            topPayerName = topPayer?.key ?: "None",
            topPayerAmount = topPayer?.value ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAnalytics())

    // --- Month-on-Month Detailed Category & Income/Expense Analytics ---
    data class CategoryMoMComparison(
        val category: String,
        val currentMonthAmount: Double,
        val previousMonthAmount: Double,
        val difference: Double, // current - previous
        val percentageChange: Double // ((current - previous) / previous) * 100
    )

    data class MonthAnalyticsSummary(
        val currentMonthLabel: String = "",
        val previousMonthLabel: String = "",
        val currentTotalIncome: Double = 0.0,
        val previousTotalIncome: Double = 0.0,
        val incomeChangePercent: Double = 0.0,
        val currentTotalExpense: Double = 0.0,
        val previousTotalExpense: Double = 0.0,
        val expenseChangePercent: Double = 0.0,
        val currentNetSavings: Double = 0.0,
        val previousNetSavings: Double = 0.0,
        val categoryComparisons: List<CategoryMoMComparison> = emptyList(),
        val cashSpentCurrentMonth: Double = 0.0,
        val cashSpentPreviousMonth: Double = 0.0,
        val paymentMethodBreakdown: Map<String, Double> = emptyMap()
    )

    val monthAnalytics: StateFlow<MonthAnalyticsSummary> = allTransactions.combine(allInvoices) { txs, _ ->
        val calCurrent = Calendar.getInstance()
        val currentMonth = calCurrent.get(Calendar.MONTH)
        val currentYear = calCurrent.get(Calendar.YEAR)

        val calPrev = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonth = calPrev.get(Calendar.MONTH)
        val prevYear = calPrev.get(Calendar.YEAR)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthLabel = monthFormat.format(calCurrent.time)
        val prevMonthLabel = monthFormat.format(calPrev.time)

        val currentTxs = txs.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val prevTxs = txs.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }

        val curIncome = currentTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val prevIncome = prevTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val incomeChange = if (prevIncome > 0) ((curIncome - prevIncome) / prevIncome) * 100 else 0.0

        val curExpense = currentTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val prevExpense = prevTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val expenseChange = if (prevExpense > 0) ((curExpense - prevExpense) / prevExpense) * 100 else 0.0

        val curSavings = curIncome - curExpense
        val prevSavings = prevIncome - prevExpense

        val curCashSpent = currentTxs.filter { it.type == "EXPENSE" && it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.amount }
        val prevCashSpent = prevTxs.filter { it.type == "EXPENSE" && it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.amount }

        // Category-by-category comparison: compare similar expenses
        val allCategories = (currentTxs.map { it.category } + prevTxs.map { it.category })
            .filter { it.isNotBlank() && it != "Salary" && it != "Bonus" && it != "Income" }
            .distinct()

        val categoryComparisons = allCategories.map { cat ->
            val curAmt = currentTxs.filter { it.category.equals(cat, ignoreCase = true) && it.type == "EXPENSE" }
                .sumOf { it.amount }
            val prevAmt = prevTxs.filter { it.category.equals(cat, ignoreCase = true) && it.type == "EXPENSE" }
                .sumOf { it.amount }
            val diff = curAmt - prevAmt
            val pct = if (prevAmt > 0) ((curAmt - prevAmt) / prevAmt) * 100 else if (curAmt > 0) 100.0 else 0.0

            CategoryMoMComparison(
                category = cat,
                currentMonthAmount = curAmt,
                previousMonthAmount = prevAmt,
                difference = diff,
                percentageChange = pct
            )
        }.sortedByDescending { it.currentMonthAmount }

        // Payment method breakdown this month
        val methodMap = mutableMapOf<String, Double>()
        currentTxs.filter { it.type == "EXPENSE" }.forEach {
            methodMap[it.paymentMethod] = (methodMap[it.paymentMethod] ?: 0.0) + it.amount
        }

        MonthAnalyticsSummary(
            currentMonthLabel = currentMonthLabel,
            previousMonthLabel = prevMonthLabel,
            currentTotalIncome = curIncome,
            previousTotalIncome = prevIncome,
            incomeChangePercent = incomeChange,
            currentTotalExpense = curExpense,
            previousTotalExpense = prevExpense,
            expenseChangePercent = expenseChange,
            currentNetSavings = curSavings,
            previousNetSavings = prevSavings,
            categoryComparisons = categoryComparisons,
            cashSpentCurrentMonth = curCashSpent,
            cashSpentPreviousMonth = prevCashSpent,
            paymentMethodBreakdown = methodMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthAnalyticsSummary())

    // --- Search & Filter Setters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setMemberFilter(member: String) {
        _selectedMemberFilter.value = member
    }

    fun setInvoiceStatusFilter(status: String) {
        _selectedInvoiceStatusFilter.value = status
    }

    // --- Transaction Actions ---
    fun saveTransaction(
        id: String? = null,
        type: String,
        amount: Double,
        date: Long,
        category: String,
        paymentMethod: String,
        paidBy: String,
        notes: String
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = id ?: UUID.randomUUID().toString(),
                type = type,
                amount = amount,
                date = date,
                category = category,
                paymentMethod = paymentMethod,
                paidBy = paidBy,
                notes = notes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveTransaction(tx)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    // --- Invoice Actions ---
    fun saveInvoice(
        id: String? = null,
        title: String,
        fromMember: String,
        toMember: String,
        amount: Double,
        date: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val inv = FamilyInvoiceEntity(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                fromMember = fromMember,
                toMember = toMember,
                amount = amount,
                date = date,
                dueDate = dueDate,
                notes = notes,
                status = "UNPAID",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveInvoice(inv)
        }
    }

    fun settleInvoice(id: String) {
        viewModelScope.launch {
            repository.settleInvoice(id)
        }
    }

    fun deleteInvoice(id: String) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    fun splitExpenseAcrossMembers(
        title: String,
        totalAmount: Double,
        paidBy: String,
        participatingMembers: List<String>
    ) {
        if (participatingMembers.isEmpty()) return
        val splitAmount = totalAmount / participatingMembers.size
        viewModelScope.launch {
            for (member in participatingMembers) {
                if (!member.equals(paidBy, ignoreCase = true)) {
                    saveInvoice(
                        title = "$title (Split Share)",
                        fromMember = member,
                        toMember = paidBy,
                        amount = Math.round(splitAmount * 100.0) / 100.0,
                        notes = "Auto-split from '$title' paid by $paidBy"
                    )
                }
            }
        }
    }

    // --- Member Actions ---
    fun addFamilyMember(name: String, role: String, avatarColorHex: String = "#0A2540") {
        viewModelScope.launch {
            repository.saveMember(
                FamilyMemberEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    role = role.trim(),
                    avatarColorHex = avatarColorHex,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun switchCurrentMember(name: String) {
        syncEngine.setCurrentMember(name)
    }

    val currentDeviceName: StateFlow<String> = syncEngine.currentDeviceName

    fun updateProfile(name: String, deviceName: String) {
        syncEngine.updateProfile(name, deviceName)
    }

    // --- Sync & Pairing Actions ---
    val lastSyncSummary: StateFlow<com.example.sync.SyncRunSummary?> = syncEngine.lastSyncSummary
    val isScanningNetwork: StateFlow<Boolean> = syncEngine.isScanningNetwork
    val scannedPairingDevices: StateFlow<List<PeerDevice>> = syncEngine.scannedPairingDevices

    fun scanLocalNetworkForPairing() {
        syncEngine.scanLocalNetworkForPairing()
    }

    fun dismissSyncSummary() {
        syncEngine.dismissSyncSummary()
    }

    fun syncNow() {
        viewModelScope.launch {
            syncEngine.syncWithAllPeers()
        }
    }

    fun syncWithPeer(peer: PeerDevice) {
        viewModelScope.launch {
            syncEngine.syncWithPeer(peer)
        }
    }

    fun syncMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            syncEngine.syncMember(member)
        }
    }

    fun pairDiscoveredDevice(peer: PeerDevice) {
        syncEngine.pairDiscoveredDevice(peer)
    }

    fun removePairedDevice(memberId: String, deviceId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            syncEngine.removePeer(deviceId)
        }
    }

    fun connectDirectPeer(ip: String, port: Int, memberName: String) {
        syncEngine.connectDirectPeer(ip, port, memberName)
    }

    fun pairFamilyDevice(name: String, deviceName: String, sampleExpense: Double = 0.0) {
        syncEngine.pairFamilyDevice(name, deviceName, sampleExpense)
    }

    fun pairSimulatedFamilyDevice(name: String, role: String, sampleExpense: Double = 50.0) {
        syncEngine.pairSimulatedFamilyDevice(name, role, sampleExpense)
    }

    // --- Export / Import Ledger Operations ---

    fun exportLedgerData(
        year: Int,
        months: Set<Int>, // empty = full year
        format: String // "JSON" or "CSV"
    ): String {
        val txs = allTransactions.value
        val invs = allInvoices.value
        val currentMember = syncEngine.currentMemberName.value

        return if (format.equals("CSV", ignoreCase = true)) {
            com.example.data.util.LedgerExportImportManager.exportToCsv(txs, year, months)
        } else {
            com.example.data.util.LedgerExportImportManager.exportToJson(txs, invs, year, months, currentMember)
        }
    }

    suspend fun importLedgerData(
        preview: com.example.data.util.LedgerImportPreview,
        targetYear: Int?,
        targetMonths: Set<Int>
    ): com.example.data.util.ImportResult {
        val filteredTx = if (targetYear != null) {
            preview.allTransactions.filter {
                com.example.data.util.LedgerExportImportManager.matchesYearAndMonths(it.date, targetYear, targetMonths)
            }
        } else {
            preview.allTransactions
        }

        val filteredInv = if (targetYear != null) {
            preview.allInvoices.filter {
                com.example.data.util.LedgerExportImportManager.matchesYearAndMonths(it.date, targetYear, targetMonths)
            }
        } else {
            preview.allInvoices
        }

        return repository.importLedgerRecords(filteredTx, filteredInv, targetYear, targetMonths)
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.stop()
    }
}
