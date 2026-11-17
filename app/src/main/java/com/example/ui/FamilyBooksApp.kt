package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.data.model.TransactionEntity
import com.example.ui.components.TopAppBarHeader
import com.example.ui.dialogs.AddEditProfileDialog
import com.example.ui.dialogs.AddInvoiceDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.ExportImportLedgerDialog
import com.example.ui.dialogs.RefillCashDialog
import com.example.ui.dialogs.SplitExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.viewmodel.FamilyBooksViewModel
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    LEDGER("Ledger"),
    ANALYTICS("Analytics"),
    INVOICES("Debts"),
    PROFILES("Profiles")
}

@Composable
fun FamilyBooksApp(
    viewModel: FamilyBooksViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Dialog States
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var initialTxType by remember { mutableStateOf("EXPENSE") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    var showAddInvoiceDialog by remember { mutableStateOf(false) }
    var initialLiabilityType by remember { mutableStateOf(LiabilityType.FAMILY_IOU) }

    var showSplitExpenseDialog by remember { mutableStateOf(false) }
    var showAddEditProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var showRefillCashDialog by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    // Core Data States
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val profileSummaries by viewModel.profileSummaries.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val monthAnalytics by viewModel.monthAnalytics.collectAsStateWithLifecycle()
    val cashInHandBalance by viewModel.cashInHandBalance.collectAsStateWithLifecycle()
    val activeProfileName by viewModel.activeProfileName.collectAsStateWithLifecycle()

    // Search and Filter States
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedMemberFilter by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val selectedAnalyticsProfile by viewModel.selectedAnalyticsProfile.collectAsStateWithLifecycle()

    val activeProfileEntity = remember(members, activeProfileName) {
        members.firstOrNull { it.name.equals(activeProfileName, ignoreCase = true) }
    }

    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBarHeader(
                activeProfile = activeProfileEntity,
                profileCount = members.size,
                onProfileIconClick = {
                    if (members.isEmpty()) {
                        editingProfile = null
                        showAddEditProfileDialog = true
                    } else {
                        currentTab = AppTab.PROFILES
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { currentTab = AppTab.DASHBOARD },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.LEDGER,
                    onClick = { currentTab = AppTab.LEDGER },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.LEDGER) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Ledger"
                        )
                    },
                    label = { Text("Ledger", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_ledger")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ANALYTICS,
                    onClick = { currentTab = AppTab.ANALYTICS },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.ANALYTICS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_analytics")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.INVOICES,
                    onClick = { currentTab = AppTab.INVOICES },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.INVOICES) Icons.Filled.Payments else Icons.Outlined.Payments,
                            contentDescription = "Debts & Shares"
                        )
                    },
                    label = { Text("Debts", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_invoices")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.PROFILES,
                    onClick = { currentTab = AppTab.PROFILES },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.PROFILES) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Profiles"
                        )
                    },
                    label = { Text("Profiles", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_profiles")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F7FA)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    analytics = analytics,
                    cashInHandBalance = cashInHandBalance,
                    recentTransactions = transactions,
                    onAddExpenseClick = {
                        editingTransaction = null
                        initialTxType = "EXPENSE"
                        showAddTransactionDialog = true
                    },
                    onAddIncomeClick = {
                        editingTransaction = null
                        initialTxType = "INCOME"
                        showAddTransactionDialog = true
                    },
                    onNewInvoiceClick = {
                        initialLiabilityType = LiabilityType.THIRD_PARTY_PAYABLE
                        showAddInvoiceDialog = true
                    },
                    onSplitExpenseClick = { showSplitExpenseDialog = true },
                    onRefillCashClick = { showRefillCashDialog = true },
                    onViewAnalyticsClick = { currentTab = AppTab.ANALYTICS },
                    onViewAllTransactionsClick = { currentTab = AppTab.LEDGER },
                    onViewInvoicesClick = { currentTab = AppTab.INVOICES },
                    onTransactionClick = { tx ->
                        editingTransaction = tx
                        showAddTransactionDialog = true
                    }
                )

                AppTab.LEDGER -> LedgerScreen(
                    transactions = filteredTransactions,
                    members = members,
                    searchQuery = searchQuery,
                    selectedTypeFilter = selectedTypeFilter,
                    selectedMemberFilter = selectedMemberFilter,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onTypeFilterChange = { viewModel.setTypeFilter(it) },
                    onMemberFilterChange = { viewModel.setMemberFilter(it) },
                    onAddTransactionClick = {
                        editingTransaction = null
                        initialTxType = "EXPENSE"
                        showAddTransactionDialog = true
                    },
                    onEditTransactionClick = { tx ->
                        editingTransaction = tx
                        showAddTransactionDialog = true
                    },
                    onDeleteTransactionClick = { id ->
                        viewModel.deleteTransaction(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Entry deleted")
                        }
                    },
                    onExportImportClick = { showExportImportDialog = true }
                )

                AppTab.ANALYTICS -> AnalyticsScreen(
                    monthAnalytics = monthAnalytics,
                    profiles = members,
                    selectedProfile = selectedAnalyticsProfile,
                    onSelectProfile = { viewModel.setAnalyticsProfile(it) },
                    cashInHandBalance = cashInHandBalance
                )

                AppTab.INVOICES -> InvoicesScreen(
                    invoices = invoices,
                    profileSummaries = profileSummaries,
                    onNewInvoiceClick = { defaultType ->
                        initialLiabilityType = defaultType
                        showAddInvoiceDialog = true
                    },
                    onSplitExpenseClick = { showSplitExpenseDialog = true },
                    onRecordPayment = { id, amt ->
                        viewModel.recordLiabilityPayment(id, amt)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Payment applied")
                        }
                    },
                    onSettleInvoice = { id ->
                        viewModel.settleInvoice(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Marked as settled")
                        }
                    },
                    onDeleteInvoice = { id ->
                        viewModel.deleteInvoice(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Liability deleted")
                        }
                    }
                )

                AppTab.PROFILES -> ProfilesScreen(
                    profiles = members,
                    profileSummaries = profileSummaries,
                    activeProfileName = activeProfileName,
                    onAddProfileClick = {
                        editingProfile = null
                        showAddEditProfileDialog = true
                    },
                    onEditProfileClick = { member ->
                        editingProfile = member
                        showAddEditProfileDialog = true
                    },
                    onDeleteProfileClick = { member ->
                        viewModel.deleteProfile(member.id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Removed profile '${member.name}'")
                        }
                    },
                    onSetActiveProfile = { name ->
                        viewModel.setActiveProfile(name)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Active profile: $name")
                        }
                    },
                    onAnalyzeProfileClick = { name ->
                        viewModel.setAnalyticsProfile(name)
                        currentTab = AppTab.ANALYTICS
                    },
                    onViewProfileLedgerClick = { name ->
                        viewModel.setMemberFilter(name)
                        currentTab = AppTab.LEDGER
                    },
                    onExportImportClick = { showExportImportDialog = true }
                )
            }
        }
    }

    // --- Dialogs ---
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            members = members,
            defaultMember = activeProfileName,
            initialType = initialTxType,
            existingTransaction = editingTransaction,
            onDismiss = {
                showAddTransactionDialog = false
                editingTransaction = null
            },
            onSave = { id, type, amount, date, category, paymentMethod, paidBy, forWhom, fromWhom, contributedBy, contributedAmount, notes ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    amount = amount,
                    date = date,
                    category = category,
                    paymentMethod = paymentMethod,
                    paidBy = paidBy,
                    forWhom = forWhom,
                    fromWhom = fromWhom,
                    contributedBy = contributedBy,
                    contributedAmount = contributedAmount,
                    notes = notes
                )
                showAddTransactionDialog = false
                editingTransaction = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Entry saved to offline ledger")
                }
            }
        )
    }

    if (showAddInvoiceDialog) {
        AddInvoiceDialog(
            members = members,
            currentMember = activeProfileName,
            initialLiabilityType = initialLiabilityType,
            onDismiss = { showAddInvoiceDialog = false },
            onSave = { title, liabilityType, linkedProfile, fromMember, toMember, amount, dueDate, notes ->
                viewModel.saveInvoice(
                    title = title,
                    liabilityType = liabilityType,
                    linkedProfile = linkedProfile,
                    fromMember = fromMember,
                    toMember = toMember,
                    amount = amount,
                    dueDate = dueDate,
                    notes = notes
                )
                showAddInvoiceDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Debt / liability recorded")
                }
            }
        )
    }

    if (showSplitExpenseDialog) {
        SplitExpenseDialog(
            members = members,
            currentMember = activeProfileName,
            onDismiss = { showSplitExpenseDialog = false },
            onConfirmSplit = { title, totalAmount, paidBy, participatingMembers, alsoLogMainExpense ->
                viewModel.splitExpenseAcrossMembers(
                    title = title,
                    totalAmount = totalAmount,
                    paidBy = paidBy,
                    participatingMembers = participatingMembers,
                    alsoLogMainExpense = alsoLogMainExpense
                )
                showSplitExpenseDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Bill split among ${participatingMembers.size} profiles")
                }
            }
        )
    }

    if (showRefillCashDialog) {
        RefillCashDialog(
            currentCashInHand = cashInHandBalance,
            onDismiss = { showRefillCashDialog = false },
            onAddCash = { amount, note ->
                viewModel.refillCashPool(amount, note)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added $amount to Cash in Hand pool")
                }
            },
            onSetBalance = { newBalance ->
                viewModel.setCashPoolBaseline(newBalance)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Cash in Hand baseline updated to $newBalance")
                }
            }
        )
    }

    if (showAddEditProfileDialog) {
        AddEditProfileDialog(
            existingProfile = editingProfile,
            onDismiss = {
                showAddEditProfileDialog = false
                editingProfile = null
            },
            onSaveProfile = { id, name, role, colorHex, notes, isPrimary ->
                viewModel.saveProfile(
                    id = id,
                    name = name,
                    role = role,
                    avatarColorHex = colorHex,
                    notes = notes,
                    isPrimary = isPrimary
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Saved profile '$name'")
                }
            }
        )
    }

    if (showExportImportDialog) {
        ExportImportLedgerDialog(
            transactions = transactions,
            invoices = invoices,
            currentMember = activeProfileName.ifBlank { "Family" },
            onDismiss = { showExportImportDialog = false },
            onExport = { year, months, format ->
                viewModel.exportLedgerData(year, months, format)
            },
            onImport = { preview, targetYear, targetMonths ->
                coroutineScope.launch {
                    val result = viewModel.importLedgerData(preview, targetYear, targetMonths)
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        )
    }
}
