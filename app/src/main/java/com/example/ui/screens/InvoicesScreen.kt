package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.MemberBalanceSummary
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.PendingOrange
import com.example.ui.theme.PendingOrangeBg
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils

@Composable
fun InvoicesScreen(
    invoices: List<FamilyInvoiceEntity>,
    memberBalances: List<MemberBalanceSummary>,
    currentMember: String,
    onNewInvoiceClick: () -> Unit,
    onSplitExpenseClick: () -> Unit,
    onSettleInvoice: (String) -> Unit,
    onDeleteInvoice: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Settled, 2: Balance Matrix

    val pendingInvoices = invoices.filter { it.status == "UNPAID" }
    val settledInvoices = invoices.filter { it.status == "SETTLED" }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewInvoiceClick,
                containerColor = Navy900,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("invoices_fab_new")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Family Invoice")
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("invoices_screen")
        ) {
            // Header Action Buttons Strip
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNewInvoiceClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy900,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Invoice / IOU", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onSplitExpenseClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Split Expense", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tabs: Pending / Settled / Balances
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Navy900,
                        indicator = { tabPositions ->
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Navy900
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "Pending (${pendingInvoices.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) Navy900 else Color(0xFF6B7280)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Settled (${settledInvoices.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) Navy900 else Color(0xFF6B7280)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    "Member Balances",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 2) Navy900 else Color(0xFF6B7280)
                                )
                            }
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Pending Invoices List
                    if (pendingInvoices.isEmpty()) {
                        EmptyInvoicesState(message = "All family debts & invoices are settled! Good job!")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            items(pendingInvoices, key = { it.id }) { invoice ->
                                InvoiceCardItem(
                                    invoice = invoice,
                                    onSettle = { onSettleInvoice(invoice.id) },
                                    onDelete = { onDeleteInvoice(invoice.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }

                1 -> {
                    // Settled Invoices List
                    if (settledInvoices.isEmpty()) {
                        EmptyInvoicesState(message = "No settled invoices in history yet.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            items(settledInvoices, key = { it.id }) { invoice ->
                                InvoiceCardItem(
                                    invoice = invoice,
                                    onSettle = null,
                                    onDelete = { onDeleteInvoice(invoice.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }

                2 -> {
                    // Member Net Balances Matrix
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Family Member Net Balance Matrix",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Calculated from unpaid family invoices and shared ledger expenses.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        items(memberBalances, key = { it.memberName }) { summary ->
                            MemberBalanceCard(summary = summary)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCardItem(
    invoice: FamilyInvoiceEntity,
    onSettle: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val isPending = invoice.status == "UNPAID"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatDate(invoice.date),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = FormatUtils.formatCurrency(invoice.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPending) ExpenseRed else IncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Debtor to Creditor Visual Flow: [Debtor] owes -> [Creditor]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF7FAFC))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Debtor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FormatUtils.getMemberAvatarColor(invoice.fromMember)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = invoice.fromMember.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = invoice.fromMember, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                        Text(text = "Owes", fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Owes to",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )

                // Creditor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = invoice.toMember, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                        Text(text = "To Receive", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FormatUtils.getMemberAvatarColor(invoice.toMember)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = invoice.toMember.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (invoice.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${invoice.notes}",
                    fontSize = 12.sp,
                    color = Color(0xFF4A5568)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                if (isPending) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PendingOrangeBg
                    ) {
                        Text(
                            text = if (invoice.dueDate != null) "Due: ${FormatUtils.formatDate(invoice.dueDate)}" else "Unsettled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PendingOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Settled ${invoice.settledAt?.let { "• " + FormatUtils.formatDate(it) } ?: ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSettle != null) {
                        Button(
                            onClick = onSettle,
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("settle_button_${invoice.id}")
                        ) {
                            Text("Mark Settled", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MemberBalanceCard(summary: MemberBalanceSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FormatUtils.getMemberAvatarColor(summary.memberName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.memberName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = summary.memberName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }

                // Net Balance Badge
                val isPositive = summary.netBalance >= 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPositive) IncomeGreenBg else ExpenseRedBg
                ) {
                    Text(
                        text = if (isPositive) "Gets back ${FormatUtils.formatCurrency(summary.netBalance)}"
                        else "Owes ${FormatUtils.formatCurrency(kotlin.math.abs(summary.netBalance))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) IncomeGreen else ExpenseRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Spent for Family", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.totalPaid),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                }

                Column {
                    Text(text = "Owed to Them", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.totalOwedToThem),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = IncomeGreen
                    )
                }

                Column {
                    Text(text = "They Owe Others", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.totalTheyOwe),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInvoicesState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color(0xFFBAC7D5),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Navy900,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
