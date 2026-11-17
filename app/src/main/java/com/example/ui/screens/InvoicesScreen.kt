package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.LiabilityType
import com.example.data.model.ProfileFinancialSummary
import com.example.data.model.isThirdPartyLiability
import com.example.data.model.remainingAmount
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
    profileSummaries: List<ProfileFinancialSummary>,
    onNewInvoiceClick: (String) -> Unit,
    onSplitExpenseClick: () -> Unit,
    onRecordPayment: (id: String, paymentAmount: Double) -> Unit,
    onSettleInvoice: (String) -> Unit,
    onDeleteInvoice: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Settled, 2: By Profile
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, FAMILY, THIRD_PARTY
    var paymentTarget by remember { mutableStateOf<FamilyInvoiceEntity?>(null) }

    val filteredByType = remember(invoices, typeFilter) {
        when (typeFilter) {
            "FAMILY" -> invoices.filter { it.liabilityType == LiabilityType.FAMILY_IOU }
            "THIRD_PARTY" -> invoices.filter { it.isThirdPartyLiability }
            else -> invoices
        }
    }

    val pendingInvoices = filteredByType.filter { it.status != "SETTLED" }
    val settledInvoices = filteredByType.filter { it.status == "SETTLED" }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNewInvoiceClick(LiabilityType.FAMILY_IOU) },
                containerColor = Navy900,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("invoices_fab_new")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt or IOU")
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
                            onClick = { onNewInvoiceClick(LiabilityType.THIRD_PARTY_PAYABLE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy900,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Debt / IOU", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onSplitExpenseClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Split Expense", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Liability Type Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All Liabilities",
                            "FAMILY" to "Family IOUs",
                            "THIRD_PARTY" to "3rd-Party Debts"
                        ).forEach { (key, label) ->
                            val selected = typeFilter == key
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) TealAccent else Color(0xFFF0F4F8),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { typeFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = Navy900,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                    "By Profile",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 2) Navy900 else Color(0xFF6B7280)
                                )
                            }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (pendingInvoices.isEmpty()) {
                        EmptyInvoicesState(message = "No pending debts, shares, or third-party liabilities.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            items(pendingInvoices, key = { it.id }) { invoice ->
                                InvoiceCardItem(
                                    invoice = invoice,
                                    onPayPartial = { paymentTarget = invoice },
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
                    if (settledInvoices.isEmpty()) {
                        EmptyInvoicesState(message = "No settled debts or liabilities yet.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            items(settledInvoices, key = { it.id }) { invoice ->
                                InvoiceCardItem(
                                    invoice = invoice,
                                    onPayPartial = null,
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
                    if (profileSummaries.isEmpty()) {
                        EmptyInvoicesState(message = "Add family profiles or liabilities to view per-profile standing.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Liabilities by Individual Profile",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Combines intra-family shares and external third-party debts/receivables per profile.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(profileSummaries, key = { it.profileName }) { summary ->
                                ProfileLiabilitySummaryCard(summary = summary)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    paymentTarget?.let { target ->
        RecordPaymentDialog(
            invoice = target,
            onDismiss = { paymentTarget = null },
            onConfirmPayment = { amt ->
                onRecordPayment(target.id, amt)
                paymentTarget = null
            }
        )
    }
}

@Composable
private fun RecordPaymentDialog(
    invoice: FamilyInvoiceEntity,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    val remaining = invoice.remainingAmount
    var amountText by remember { mutableStateOf("%.2f".format(remaining)) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column {
                Text(
                    text = "${invoice.title} • Remaining: ${FormatUtils.formatCurrency(remaining)}",
                    fontSize = 13.sp,
                    color = Color(0xFF4A5568)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("Payment Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = ExpenseRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid positive amount"
                        return@Button
                    }
                    onConfirmPayment(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Apply Payment", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun InvoiceCardItem(
    invoice: FamilyInvoiceEntity,
    onPayPartial: (() -> Unit)?,
    onSettle: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val isPending = invoice.status != "SETTLED"
    val remaining = invoice.remainingAmount

    val (badgeLabel, badgeBg, badgeFg) = when (invoice.liabilityType) {
        LiabilityType.THIRD_PARTY_PAYABLE -> Triple("3rd-Party Debt", ExpenseRedBg, ExpenseRed)
        LiabilityType.THIRD_PARTY_RECEIVABLE -> Triple("3rd-Party Owed", IncomeGreenBg, IncomeGreen)
        else -> Triple("Family IOU", TealLight, Navy900)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                            Text(
                                text = badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (invoice.linkedProfile.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF0F4F8)) {
                                Text(
                                    text = "Profile: ${invoice.linkedProfile}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy900,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = invoice.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Navy900
                    )
                    Text(
                        text = FormatUtils.formatDate(invoice.date),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(if (isPending) remaining else invoice.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isPending) ExpenseRed else IncomeGreen
                    )
                    if (invoice.paidAmount > 0 && isPending) {
                        Text(
                            text = "of ${FormatUtils.formatCurrency(invoice.amount)}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (invoice.paidAmount > 0 && isPending) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = (invoice.paidAmount / invoice.amount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = IncomeGreen,
                    trackColor = Color(0xFFF0F4F8)
                )
                Text(
                    text = "Paid ${FormatUtils.formatCurrency(invoice.paidAmount)} (${(progress * 100).toInt()}%)",
                    fontSize = 10.sp,
                    color = IncomeGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF7FAFC))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = invoice.toMember, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                        Text(text = "Creditor", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    if (onPayPartial != null) {
                        OutlinedButton(
                            onClick = onPayPartial,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (onSettle != null) {
                        Button(
                            onClick = onSettle,
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("settle_button_${invoice.id}")
                        ) {
                            Text("Settle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLiabilitySummaryCard(summary: ProfileFinancialSummary) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(FormatUtils.parseAvatarColor(summary.avatarColorHex, summary.profileName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.profileName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.profileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(text = summary.role, fontSize = 11.sp, color = Color.Gray)
                    }
                }

                val isPositive = summary.netLiabilityStanding >= 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPositive) IncomeGreenBg else ExpenseRedBg
                ) {
                    Text(
                        text = if (isPositive) "Net +${FormatUtils.formatCurrency(summary.netLiabilityStanding)}"
                        else "Net -${FormatUtils.formatCurrency(kotlin.math.abs(summary.netLiabilityStanding))}",
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Family Owed", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.familyOwedToThem),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IncomeGreen
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Owes Family", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.familyTheyOwe),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ExpenseRed
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "3rd-Party Debt", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.thirdPartyTheyOwe),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ExpenseRed
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "3rd-Party Owed", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = FormatUtils.formatCurrency(summary.thirdPartyOwedToThem),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IncomeGreen
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
