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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Payments
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TransactionEntity
import com.example.ui.components.TopAppBarAndSyncBanner
import com.example.ui.dialogs.AddInvoiceDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.DirectIpDialog
import com.example.ui.dialogs.EditProfileDialog
import com.example.ui.dialogs.ExportImportLedgerDialog
import com.example.ui.dialogs.PairDeviceDialog
import com.example.ui.dialogs.RefillCashDialog
import com.example.ui.dialogs.SplitExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.SyncHubScreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.viewmodel.FamilyBooksViewModel
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    LEDGER("Ledger"),
    ANALYTICS("Analytics"),
    INVOICES("Invoices"),
    SYNC_HUB("P2P Sync")
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
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddInvoiceDialog by remember { mutableStateOf(false) }
    var showSplitExpenseDialog by remember { mutableStateOf(false) }
    var showDirectIpDialog by remember { mutableStateOf(false) }
    var showPairDeviceDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showRefillCashDialog by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    // Core Data States
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val memberBalances by viewModel.memberBalances.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val monthAnalytics by viewModel.monthAnalytics.collectAsStateWithLifecycle()
    val cashInHandBalance by viewModel.cashInHandBalance.collectAsStateWithLifecycle()

    // Search and Filter States
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedMemberFilter by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()

    // P2P Sync Engine States
    val currentMember by viewModel.syncEngine.currentMemberName.collectAsStateWithLifecycle()
    val currentDeviceName by viewModel.currentDeviceName.collectAsStateWithLifecycle()
    val localIp by viewModel.syncEngine.localIp.collectAsStateWithLifecycle()
    val localPort by viewModel.syncEngine.localPort.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncEngine.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.syncEngine.syncStatusMessage.collectAsStateWithLifecycle()
    val lastSyncedTime by viewModel.syncEngine.lastSyncedTime.collectAsStateWithLifecycle()
    val peersMap by viewModel.syncEngine.discoveredPeers.collectAsStateWithLifecycle()
    val peers = peersMap.values.toList()
    val lastSyncSummary by viewModel.lastSyncSummary.collectAsStateWithLifecycle()
    val isScanningNetwork by viewModel.isScanningNetwork.collectAsStateWithLifecycle()
    val scannedPairingDevices by viewModel.scannedPairingDevices.collectAsStateWithLifecycle()

    // Back handling: pop to Dashboard if in another tab
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBarAndSyncBanner(
                currentMember = currentMember,
                currentDeviceName = currentDeviceName,
                statusMessage = statusMessage,
                isSyncing = isSyncing,
                discoveredPeerCount = peers.size,
                onEditProfileClick = { showEditProfileDialog = true },
                onSyncClick = {
                    viewModel.syncNow()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Initiating P2P synchronization...")
                    }
                },
                onBannerClick = { currentTab = AppTab.SYNC_HUB }
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
                // Dashboard
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

                // Ledger
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

                // Analytics
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

                // Invoices
                NavigationBarItem(
                    selected = currentTab == AppTab.INVOICES,
                    onClick = { currentTab = AppTab.INVOICES },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.INVOICES) Icons.Filled.Payments else Icons.Outlined.Payments,
                            contentDescription = "Invoices"
                        )
                    },
                    label = { Text("Invoices", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_invoices")
                )

                // P2P Sync Hub
                NavigationBarItem(
                    selected = currentTab == AppTab.SYNC_HUB,
                    onClick = { currentTab = AppTab.SYNC_HUB },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.SYNC_HUB) Icons.Filled.Hub else Icons.Outlined.Hub,
                            contentDescription = "Sync Hub"
                        )
                    },
                    label = { Text("P2P Sync", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Navy900,
                        selectedTextColor = TealAccent,
                        indicatorColor = TealAccent,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_sync_hub")
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
                    pendingInvoices = invoices.filter { it.status == "UNPAID" },
                    onAddExpenseClick = {
                        editingTransaction = null
                        showAddTransactionDialog = true
                    },
                    onAddIncomeClick = {
                        editingTransaction = null
                        showAddTransactionDialog = true
                    },
                    onNewInvoiceClick = { showAddInvoiceDialog = true },
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
                    selectedCategoryFilter = selectedCategoryFilter,
                    selectedMemberFilter = selectedMemberFilter,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onTypeFilterChange = { viewModel.setTypeFilter(it) },
                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                    onMemberFilterChange = { viewModel.setMemberFilter(it) },
                    onAddTransactionClick = {
                        editingTransaction = null
                        showAddTransactionDialog = true
                    },
                    onEditTransactionClick = { tx ->
                        editingTransaction = tx
                        showAddTransactionDialog = true
                    },
                    onDeleteTransactionClick = { id ->
                        viewModel.deleteTransaction(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Transaction deleted")
                        }
                    },
                    onExportImportClick = { showExportImportDialog = true }
                )

                AppTab.ANALYTICS -> AnalyticsScreen(
                    monthAnalytics = monthAnalytics,
                    cashInHandBalance = cashInHandBalance
                )

                AppTab.INVOICES -> InvoicesScreen(
                    invoices = invoices,
                    memberBalances = memberBalances,
                    currentMember = currentMember,
                    onNewInvoiceClick = { showAddInvoiceDialog = true },
                    onSplitExpenseClick = { showSplitExpenseDialog = true },
                    onSettleInvoice = { id ->
                        viewModel.settleInvoice(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Invoice marked as settled!")
                        }
                    },
                    onDeleteInvoice = { id ->
                        viewModel.deleteInvoice(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Invoice deleted")
                        }
                    }
                )

                AppTab.SYNC_HUB -> SyncHubScreen(
                    currentMember = currentMember,
                    currentDeviceName = currentDeviceName,
                    localIp = localIp,
                    localPort = localPort,
                    isSyncing = isSyncing,
                    statusMessage = statusMessage,
                    lastSyncedTime = lastSyncedTime,
                    members = members,
                    peers = peers,
                    syncLogs = syncLogs,
                    lastSyncSummary = lastSyncSummary,
                    onSyncAllClick = {
                        viewModel.syncNow()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Synchronizing across all available family devices...")
                        }
                    },
                    onSyncPeerClick = { peer ->
                        viewModel.syncWithPeer(peer)
                        coroutineScope.launch {
                            val dev = peer.deviceName.ifBlank { "device" }
                            snackbarHostState.showSnackbar("Syncing with ${peer.memberName}'s $dev...")
                        }
                    },
                    onSyncMemberClick = { member ->
                        viewModel.syncMember(member)
                        coroutineScope.launch {
                            val dev = member.deviceName.ifBlank { "device" }
                            snackbarHostState.showSnackbar("Syncing with ${member.name}'s $dev...")
                        }
                    },
                    onRemoveMemberClick = { member ->
                        viewModel.removePairedDevice(member.id, member.deviceId)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Removed ${member.name}'s paired device")
                        }
                    },
                    onDirectIpClick = { showDirectIpDialog = true },
                    onPairSimulatedClick = {
                        viewModel.scanLocalNetworkForPairing()
                        showPairDeviceDialog = true
                    },
                    onEditProfileClick = { showEditProfileDialog = true },
                    onDismissSyncSummary = { viewModel.dismissSyncSummary() },
                    onExportImportClick = { showExportImportDialog = true }
                )
            }
        }
    }

    // --- Dialogs ---

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            members = members,
            defaultMember = currentMember,
            existingTransaction = editingTransaction,
            onDismiss = {
                showAddTransactionDialog = false
                editingTransaction = null
            },
            onSave = { id, type, amount, date, category, paymentMethod, paidBy, notes ->
                viewModel.saveTransaction(id, type, amount, date, category, paymentMethod, paidBy, notes)
                showAddTransactionDialog = false
                editingTransaction = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Transaction logged successfully")
                }
            }
        )
    }

    if (showAddInvoiceDialog) {
        AddInvoiceDialog(
            members = members,
            currentMember = currentMember,
            onDismiss = { showAddInvoiceDialog = false },
            onSave = { title, fromMember, toMember, amount, dueDate, notes ->
                viewModel.saveInvoice(
                    title = title,
                    fromMember = fromMember,
                    toMember = toMember,
                    amount = amount,
                    dueDate = dueDate,
                    notes = notes
                )
                showAddInvoiceDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Family invoice created")
                }
            }
        )
    }

    if (showSplitExpenseDialog) {
        SplitExpenseDialog(
            members = members,
            currentMember = currentMember,
            onDismiss = { showSplitExpenseDialog = false },
            onConfirmSplit = { title, totalAmount, paidBy, participatingMembers ->
                viewModel.splitExpenseAcrossMembers(title, totalAmount, paidBy, participatingMembers)
                showSplitExpenseDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Bill split among ${participatingMembers.size} members")
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

    if (showDirectIpDialog) {
        DirectIpDialog(
            onDismiss = { showDirectIpDialog = false },
            onConnect = { ip, port, name ->
                viewModel.connectDirectPeer(ip, port, name)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Connecting to $ip:$port and pairing as Family Member...")
                }
            }
        )
    }

    if (showPairDeviceDialog) {
        PairDeviceDialog(
            currentMember = currentMember,
            currentDeviceName = currentDeviceName,
            discoveredDevices = scannedPairingDevices,
            isScanning = isScanningNetwork,
            onDismiss = { showPairDeviceDialog = false },
            onScanAgain = { viewModel.scanLocalNetworkForPairing() },
            onPairFoundDevice = { peer ->
                viewModel.pairDiscoveredDevice(peer)
                coroutineScope.launch {
                    val dev = peer.deviceName.ifBlank { "device" }
                    snackbarHostState.showSnackbar("Paired with ${peer.memberName}'s $dev!")
                }
            },
            onPairManualOrTest = { name, role, sampleExpense ->
                viewModel.pairFamilyDevice(name, role, sampleExpense)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Paired '$name' ($role) as Family Member!")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = currentMember,
            currentDeviceName = currentDeviceName,
            onDismiss = { showEditProfileDialog = false },
            onSaveProfile = { newName, newDeviceName ->
                viewModel.updateProfile(newName, newDeviceName)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Profile updated: $newName ($newDeviceName)")
                }
            }
        )
    }

    if (showExportImportDialog) {
        ExportImportLedgerDialog(
            transactions = transactions,
            invoices = invoices,
            currentMember = currentMember,
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
