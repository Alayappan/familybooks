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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.util.FormatUtils

@Composable
fun LedgerScreen(
    transactions: List<TransactionEntity>,
    members: List<FamilyMemberEntity>,
    searchQuery: String,
    selectedTypeFilter: String,
    selectedCategoryFilter: String,
    selectedMemberFilter: String,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onMemberFilterChange: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (TransactionEntity) -> Unit,
    onDeleteTransactionClick: (String) -> Unit,
    onExportImportClick: () -> Unit = {}
) {
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = Navy900,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("ledger_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("ledger_screen")
        ) {
            // Search Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search transactions, notes, paid by...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CardBorderLight,
                            focusedBorderColor = Navy900
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Filter Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type Filters
                        listOf("ALL" to "All Types", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (typeKey, label) ->
                            val isSelected = selectedTypeFilter == typeKey
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onTypeFilterChange(typeKey) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Navy900,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Member Filter Chips
                        val allMemberNames = listOf("ALL") + members.map { it.name }.distinct()
                        allMemberNames.forEach { memberName ->
                            val isSelected = selectedMemberFilter == memberName
                            val label = if (memberName == "ALL") "All Members" else memberName
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) TealAccent else Color(0xFFF0F4F8),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onMemberFilterChange(memberName) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (memberName != "ALL") {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(FormatUtils.getMemberAvatarColor(memberName))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Navy900 else Navy900
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary Header Strip with Export/Import Action
            val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${transactions.size} Records Found",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Out: ${FormatUtils.formatCurrency(totalExpense)}  |  In: ${FormatUtils.formatCurrency(totalIncome)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }

                OutlinedButton(
                    onClick = onExportImportClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("ledger_export_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ImportExport,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Navy900
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export / Import",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }
            }

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching transactions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search filters or log a new entry using the '+' button below.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        LedgerItemCard(
                            transaction = tx,
                            onEdit = { onEditTransactionClick(tx) },
                            onDelete = { onDeleteTransactionClick(tx.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerItemCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            FormatUtils.getCategoryColor(transaction.category).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FormatUtils.getCategoryIcon(transaction.category),
                        contentDescription = null,
                        tint = FormatUtils.getCategoryColor(transaction.category),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0F4F8)
                        ) {
                            Text(
                                text = transaction.paidBy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (transaction.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.notes,
                            fontSize = 12.sp,
                            color = Color(0xFF4A5568)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${FormatUtils.formatDate(transaction.date)} • ${transaction.paymentMethod}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Amount
                Text(
                    text = (if (isExpense) "-" else "+") + FormatUtils.formatCurrency(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isExpense) ExpenseRed else IncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Strip (Edit / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
