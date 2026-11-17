package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FamilyBooksDatabase
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.data.model.ProfileFinancialSummary
import com.example.data.model.TransactionEntity
import com.example.data.model.remainingAmount
import com.example.data.repository.FamilyBooksRepository
import com.example.data.util.ImportResult
import com.example.data.util.LedgerExportImportManager
import com.example.data.util.LedgerImportPreview
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
        familyMemberDao = database.familyMemberDao()
    )

    // --- Core Data Flows ---
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<FamilyInvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<FamilyMemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profileSummaries: StateFlow<List<ProfileFinancialSummary>> = repository.profileSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Profile Preference ---
    private val _preferredProfileName = MutableStateFlow(
        prefs.getString("active_profile_name", "") ?: ""
    )

    val activeProfileName: StateFlow<String> = combine(
        allMembers,
        _preferredProfileName
    ) { members, preferred ->
        when {
            members.isEmpty() -> ""
            preferred.isNotBlank() && members.any { it.name.equals(preferred, ignoreCase = true) } ->
                members.first { it.name.equals(preferred, ignoreCase = true) }.name
            else -> members.firstOrNull { it.isPrimary }?.name ?: members.first().name
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setActiveProfile(name: String) {
        _preferredProfileName.value = name
        prefs.edit().putString("active_profile_name", name).apply()
    }

    // --- Cash in Hand (Wallet / Cash Pool) Flow ---
    private val _cashPoolBaseline = MutableStateFlow(
        prefs.getFloat("cash_pool_baseline", 0f).toDouble()
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refillCashPool(amount: Double, note: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val owner = activeProfileName.value.ifBlank { "Family" }
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = "INCOME",
                amount = amount,
                date = now,
                category = "Other",
                paymentMethod = "Cash",
                paidBy = owner,
                forWhom = "Shared Family",
                fromWhom = "ATM / Cash Wallet Refill",
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

    private val _selectedAnalyticsProfile = MutableStateFlow("ALL")
    val selectedAnalyticsProfile: StateFlow<String> = _selectedAnalyticsProfile.asStateFlow()

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
                tx.forWhom.contains(query, ignoreCase = true) ||
                tx.fromWhom.contains(query, ignoreCase = true) ||
                tx.contributedBy.contains(query, ignoreCase = true) ||
                tx.paymentMethod.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)

            val matchesType = typeFilter == "ALL" || tx.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = catFilter == "ALL" || tx.category.equals(catFilter, ignoreCase = true)
            val matchesMember = memberFilter == "ALL" ||
                tx.paidBy.equals(memberFilter, ignoreCase = true) ||
                tx.forWhom.equals(memberFilter, ignoreCase = true) ||
                tx.fromWhom.equals(memberFilter, ignoreCase = true) ||
                tx.contributedBy.equals(memberFilter, ignoreCase = true)

            matchesQuery && matchesType && matchesCategory && matchesMember
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Analytics ---
    data class DashboardAnalytics(
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val netCashFlow: Double = 0.0,
        val pendingInvoicesCount: Int = 0,
        val pendingFamilyIousAmount: Double = 0.0,
        val thirdPartyPayableAmount: Double = 0.0,
        val thirdPartyReceivableAmount: Double = 0.0,
        val totalContributionsThisMonth: Double = 0.0,
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
        val totalContributions = monthTxs.sumOf { it.contributedAmount }
        val netCashFlow = totalIncome - totalExpense

        val pendingInvs = invs.filter { it.status != "SETTLED" }
        val familyIousAmt = pendingInvs
            .filter { it.liabilityType == LiabilityType.FAMILY_IOU }
            .sumOf { it.remainingAmount }
        val thirdPartyPayableAmt = pendingInvs
            .filter { it.liabilityType == LiabilityType.THIRD_PARTY_PAYABLE }
            .sumOf { it.remainingAmount }
        val thirdPartyReceivableAmt = pendingInvs
            .filter { it.liabilityType == LiabilityType.THIRD_PARTY_RECEIVABLE }
            .sumOf { it.remainingAmount }

        val categoryMap = mutableMapOf<String, Double>()
        val memberPayerMap = mutableMapOf<String, Double>()

        monthTxs.filter { it.type == "EXPENSE" }.forEach {
            categoryMap[it.category] = (categoryMap[it.category] ?: 0.0) + it.amount
            if (it.paidBy.isNotBlank()) {
                memberPayerMap[it.paidBy] = (memberPayerMap[it.paidBy] ?: 0.0) + it.amount
            }
        }

        val topPayer = memberPayerMap.maxByOrNull { it.value }

        DashboardAnalytics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            pendingInvoicesCount = pendingInvs.size,
            pendingFamilyIousAmount = familyIousAmt,
            thirdPartyPayableAmount = thirdPartyPayableAmt,
            thirdPartyReceivableAmount = thirdPartyReceivableAmt,
            totalContributionsThisMonth = totalContributions,
            categoryBreakdown = categoryMap,
            topPayerName = topPayer?.key ?: "None",
            topPayerAmount = topPayer?.value ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAnalytics())

    // --- Month-on-Month & Per-Profile Detailed Analytics ---
    data class CategoryMoMComparison(
        val category: String,
        val currentMonthAmount: Double,
        val previousMonthAmount: Double,
        val difference: Double,
        val percentageChange: Double
    )

    data class MonthAnalyticsSummary(
        val selectedProfile: String = "ALL",
        val currentMonthLabel: String = "",
        val previousMonthLabel: String = "",
        val currentTotalIncome: Double = 0.0,
        val previousTotalIncome: Double = 0.0,
        val incomeChangePercent: Double = 0.0,
        val currentTotalExpense: Double = 0.0,
        val previousTotalExpense: Double = 0.0,
        val expenseChangePercent: Double = 0.0,
        val expenseForProfileThisMonth: Double = 0.0,
        val contributionsProvidedThisMonth: Double = 0.0,
        val contributionsReceivedThisMonth: Double = 0.0,
        val thirdPartyDebtOwed: Double = 0.0,
        val thirdPartyReceivable: Double = 0.0,
        val familyDebtOwed: Double = 0.0,
        val familyReceivable: Double = 0.0,
        val currentNetSavings: Double = 0.0,
        val previousNetSavings: Double = 0.0,
        val categoryComparisons: List<CategoryMoMComparison> = emptyList(),
        val cashSpentCurrentMonth: Double = 0.0,
        val cashSpentPreviousMonth: Double = 0.0,
        val paymentMethodBreakdown: Map<String, Double> = emptyMap()
    )

    val monthAnalytics: StateFlow<MonthAnalyticsSummary> = combine(
        allTransactions,
        allInvoices,
        selectedAnalyticsProfile
    ) { allTxs, allInvs, profileFilter ->
        val isAll = profileFilter == "ALL"

        val txs = if (isAll) {
            allTxs
        } else {
            allTxs.filter { tx ->
                tx.paidBy.equals(profileFilter, ignoreCase = true) ||
                    tx.forWhom.equals(profileFilter, ignoreCase = true) ||
                    tx.contributedBy.equals(profileFilter, ignoreCase = true)
            }
        }

        val calCurrent = Calendar.getInstance()
        val currentMonth = calCurrent.get(Calendar.MONTH)
        val currentYear = calCurrent.get(Calendar.YEAR)

        val calPrev = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
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

        val curIncome = currentTxs.filter {
            it.type == "INCOME" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val prevIncome = prevTxs.filter {
            it.type == "INCOME" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val incomeChange = if (prevIncome > 0) ((curIncome - prevIncome) / prevIncome) * 100 else 0.0

        val curExpense = currentTxs.filter {
            it.type == "EXPENSE" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val prevExpense = prevTxs.filter {
            it.type == "EXPENSE" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val expenseChange = if (prevExpense > 0) ((curExpense - prevExpense) / prevExpense) * 100 else 0.0

        val expenseForProfile = if (isAll) {
            curExpense
        } else {
            currentTxs.filter {
                it.type == "EXPENSE" && it.forWhom.equals(profileFilter, ignoreCase = true)
            }.sumOf { it.amount }
        }

        val contributionsProvided = currentTxs.filter {
            it.contributedAmount > 0 && (isAll || it.contributedBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.contributedAmount }

        val contributionsReceived = currentTxs.filter {
            it.contributedAmount > 0 && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.contributedAmount }

        val activeInvs = allInvs.filter { it.status != "SETTLED" }
        val thirdPartyDebt = activeInvs.filter {
            it.liabilityType == LiabilityType.THIRD_PARTY_PAYABLE &&
                (isAll || it.linkedProfile.equals(profileFilter, ignoreCase = true) ||
                    (it.linkedProfile.isBlank() && it.fromMember.equals(profileFilter, ignoreCase = true)))
        }.sumOf { it.remainingAmount }

        val thirdPartyRec = activeInvs.filter {
            it.liabilityType == LiabilityType.THIRD_PARTY_RECEIVABLE &&
                (isAll || it.linkedProfile.equals(profileFilter, ignoreCase = true) ||
                    (it.linkedProfile.isBlank() && it.toMember.equals(profileFilter, ignoreCase = true)))
        }.sumOf { it.remainingAmount }

        val familyDebt = activeInvs.filter {
            it.liabilityType == LiabilityType.FAMILY_IOU &&
                (isAll || it.fromMember.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.remainingAmount }

        val familyRec = activeInvs.filter {
            it.liabilityType == LiabilityType.FAMILY_IOU &&
                (isAll || it.toMember.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.remainingAmount }

        val curSavings = curIncome - curExpense
        val prevSavings = prevIncome - prevExpense

        val curCashSpent = currentTxs.filter {
            it.type == "EXPENSE" &&
                it.paymentMethod.equals("Cash", ignoreCase = true) &&
                (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val prevCashSpent = prevTxs.filter {
            it.type == "EXPENSE" &&
                it.paymentMethod.equals("Cash", ignoreCase = true) &&
                (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.sumOf { it.amount }

        val curExpenseForCat = currentTxs.filter {
            it.type == "EXPENSE" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true) || it.forWhom.equals(profileFilter, ignoreCase = true))
        }
        val prevExpenseForCat = prevTxs.filter {
            it.type == "EXPENSE" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true) || it.forWhom.equals(profileFilter, ignoreCase = true))
        }

        val allCategories = (curExpenseForCat.map { it.category } + prevExpenseForCat.map { it.category })
            .filter { it.isNotBlank() }
            .distinct()

        val categoryComparisons = allCategories.map { cat ->
            val curAmt = curExpenseForCat.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
            val prevAmt = prevExpenseForCat.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
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

        val methodMap = mutableMapOf<String, Double>()
        currentTxs.filter {
            it.type == "EXPENSE" && (isAll || it.paidBy.equals(profileFilter, ignoreCase = true))
        }.forEach {
            methodMap[it.paymentMethod] = (methodMap[it.paymentMethod] ?: 0.0) + it.amount
        }

        MonthAnalyticsSummary(
            selectedProfile = profileFilter,
            currentMonthLabel = currentMonthLabel,
            previousMonthLabel = prevMonthLabel,
            currentTotalIncome = curIncome,
            previousTotalIncome = prevIncome,
            incomeChangePercent = incomeChange,
            currentTotalExpense = curExpense,
            previousTotalExpense = prevExpense,
            expenseChangePercent = expenseChange,
            expenseForProfileThisMonth = expenseForProfile,
            contributionsProvidedThisMonth = contributionsProvided,
            contributionsReceivedThisMonth = contributionsReceived,
            thirdPartyDebtOwed = thirdPartyDebt,
            thirdPartyReceivable = thirdPartyRec,
            familyDebtOwed = familyDebt,
            familyReceivable = familyRec,
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

    fun setAnalyticsProfile(profileName: String) {
        _selectedAnalyticsProfile.value = profileName
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
        forWhom: String,
        fromWhom: String,
        contributedBy: String,
        contributedAmount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tx = TransactionEntity(
                id = id ?: UUID.randomUUID().toString(),
                type = type,
                amount = amount,
                date = date,
                category = category,
                paymentMethod = paymentMethod,
                paidBy = paidBy.trim().ifBlank { "Family" },
                forWhom = forWhom.trim(),
                fromWhom = fromWhom.trim(),
                contributedBy = contributedBy.trim(),
                contributedAmount = if (contributedBy.isBlank()) 0.0 else contributedAmount.coerceAtLeast(0.0),
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now
            )
            repository.saveTransaction(tx)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    // --- Liability / Debt / IOU Actions ---
    fun saveInvoice(
        id: String? = null,
        title: String,
        liabilityType: String = LiabilityType.FAMILY_IOU,
        linkedProfile: String = "",
        fromMember: String,
        toMember: String,
        amount: Double,
        date: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val inv = FamilyInvoiceEntity(
                id = id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                liabilityType = liabilityType,
                linkedProfile = linkedProfile.trim(),
                fromMember = fromMember.trim(),
                toMember = toMember.trim(),
                amount = amount,
                paidAmount = 0.0,
                date = date,
                dueDate = dueDate,
                notes = notes.trim(),
                status = "UNPAID",
                createdAt = now,
                updatedAt = now
            )
            repository.saveInvoice(inv)
        }
    }

    fun recordLiabilityPayment(id: String, paymentAmount: Double) {
        viewModelScope.launch {
            repository.recordLiabilityPayment(id, paymentAmount)
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
        participatingMembers: List<String>,
        alsoLogMainExpense: Boolean = false,
        category: String = "Shared",
        paymentMethod: String = "Card"
    ) {
        if (participatingMembers.isEmpty()) return
        val splitAmount = totalAmount / participatingMembers.size
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            if (alsoLogMainExpense) {
                repository.saveTransaction(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        amount = totalAmount,
                        date = now,
                        category = category,
                        paymentMethod = paymentMethod,
                        paidBy = paidBy,
                        forWhom = participatingMembers.joinToString(", "),
                        notes = title.trim(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            for (member in participatingMembers) {
                if (!member.equals(paidBy, ignoreCase = true)) {
                    repository.saveInvoice(
                        FamilyInvoiceEntity(
                            id = UUID.randomUUID().toString(),
                            title = "$title (Split Share)",
                            liabilityType = LiabilityType.FAMILY_IOU,
                            linkedProfile = "",
                            fromMember = member,
                            toMember = paidBy,
                            amount = Math.round(splitAmount * 100.0) / 100.0,
                            date = now,
                            notes = "Split from '$title' paid by $paidBy",
                            status = "UNPAID",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }
        }
    }

    // --- Family Profile CRUD ---
    fun saveProfile(
        id: String? = null,
        name: String,
        role: String,
        avatarColorHex: String = "#0A2540",
        notes: String = "",
        isPrimary: Boolean = false
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val shouldBePrimary = isPrimary || allMembers.value.isEmpty()
            val member = FamilyMemberEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = cleanName,
                role = role.trim().ifEmpty { "Member" },
                avatarColorHex = avatarColorHex,
                isPrimary = shouldBePrimary,
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now
            )
            repository.saveMember(member)
            if (shouldBePrimary || activeProfileName.value.isBlank()) {
                setActiveProfile(cleanName)
            }
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            val deleted = allMembers.value.firstOrNull { it.id == id }
            repository.deleteMember(id)
            if (deleted != null && _preferredProfileName.value.equals(deleted.name, ignoreCase = true)) {
                setActiveProfile("")
            }
            if (deleted != null && _selectedAnalyticsProfile.value.equals(deleted.name, ignoreCase = true)) {
                _selectedAnalyticsProfile.value = "ALL"
            }
            if (deleted != null && _selectedMemberFilter.value.equals(deleted.name, ignoreCase = true)) {
                _selectedMemberFilter.value = "ALL"
            }
        }
    }

    // --- Export / Import Ledger Operations ---
    fun exportLedgerData(
        year: Int,
        months: Set<Int>, // empty = full year
        format: String // "JSON" or "CSV"
    ): String {
        val txs = allTransactions.value
        val invs = allInvoices.value
        val profiles = allMembers.value
        val exporter = activeProfileName.value.ifBlank { "Family" }

        return if (format.equals("CSV", ignoreCase = true)) {
            LedgerExportImportManager.exportToCsv(txs, year, months)
        } else {
            LedgerExportImportManager.exportToJson(txs, invs, profiles, year, months, exporter)
        }
    }

    suspend fun importLedgerData(
        preview: LedgerImportPreview,
        targetYear: Int?,
        targetMonths: Set<Int>
    ): ImportResult {
        val filteredTx = if (targetYear != null) {
            preview.allTransactions.filter {
                LedgerExportImportManager.matchesYearAndMonths(it.date, targetYear, targetMonths)
            }
        } else {
            preview.allTransactions
        }

        val filteredInv = if (targetYear != null) {
            preview.allInvoices.filter {
                LedgerExportImportManager.matchesYearAndMonths(it.date, targetYear, targetMonths)
            }
        } else {
            preview.allInvoices
        }

        return repository.importLedgerRecords(
            transactions = filteredTx,
            invoices = filteredInv,
            profiles = preview.allProfiles,
            year = targetYear,
            months = targetMonths
        )
    }
}
