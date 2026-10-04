package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    members: List<FamilyMemberEntity>,
    defaultMember: String,
    existingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        type: String,
        amount: Double,
        date: Long,
        category: String,
        paymentMethod: String,
        paidBy: String,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(existingTransaction?.type ?: "EXPENSE") }
    var amountText by remember { mutableStateOf(existingTransaction?.let { "%.2f".format(it.amount) } ?: "") }
    var category by remember { mutableStateOf(existingTransaction?.category ?: "Groceries") }
    var paidBy by remember { mutableStateOf(existingTransaction?.paidBy ?: defaultMember) }
    var paymentMethod by remember { mutableStateOf(existingTransaction?.paymentMethod ?: "Card") }
    var notes by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val expenseCategories = listOf(
        "Groceries", "Utilities", "Rent", "Dining", "Education",
        "Healthcare", "Transport", "Entertainment", "Shopping", "Other"
    )

    val incomeCategories = listOf(
        "Salary", "Bonus", "Investment", "Freelance", "Gift", "Other"
    )

    val paymentMethods = listOf("Card", "Cash", "UPI / Bank Transfer", "Wallet")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("add_transaction_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingTransaction != null) "Edit Transaction" else "Record Transaction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle: Expense vs Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F4F8))
                        .padding(4.dp)
                ) {
                    val expenseSelected = type == "EXPENSE"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (expenseSelected) ExpenseRed else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                type = "EXPENSE"
                                if (category !in expenseCategories) category = "Groceries"
                            }
                            .padding(vertical = 10.dp)
                            .testTag("type_expense_tab")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (expenseSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Expense",
                                fontWeight = FontWeight.Bold,
                                color = if (expenseSelected) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    val incomeSelected = type == "INCOME"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (incomeSelected) IncomeGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                type = "INCOME"
                                if (category !in incomeCategories) category = "Salary"
                            }
                            .padding(vertical = 10.dp)
                            .testTag("type_income_tab")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (incomeSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Income",
                                fontWeight = FontWeight.Bold,
                                color = if (incomeSelected) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Paid By (Family Member)
                Text(
                    text = if (type == "EXPENSE") "Paid By (Family Member)" else "Received By",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                val memberNames = if (members.isNotEmpty()) members.map { it.name }.distinct() else listOf(defaultMember)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    memberNames.forEach { name ->
                        val isSelected = paidBy.equals(name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { paidBy = name }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) TealAccent else FormatUtils.getMemberAvatarColor(name)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Navy900,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Chips
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                val availableCategories = if (type == "EXPENSE") expenseCategories else incomeCategories
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                if (type == "EXPENSE") ExpenseRedBg else IncomeGreenBg
                            } else Color(0xFFF5F7FA),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (type == "EXPENSE") ExpenseRed else IncomeGreen
                                )
                            } else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) {
                                    if (type == "EXPENSE") ExpenseRed else IncomeGreen
                                } else Color(0xFF4A5568),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method
                Text(
                    text = "Payment Method",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = paymentMethod == method
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFE2EBF5) else Color(0xFFF7FAFC),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, Navy900)
                            } else androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paymentMethod = method }
                        ) {
                            Text(
                                text = method,
                                color = if (isSelected) Navy900 else Color(0xFF4A5568),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    placeholder = { Text("e.g. Monthly supermarket run") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_notes_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Please enter a valid amount greater than $0.00"
                                return@Button
                            }
                            onSave(
                                existingTransaction?.id,
                                type,
                                parsedAmount,
                                existingTransaction?.date ?: System.currentTimeMillis(),
                                category,
                                paymentMethod,
                                paidBy,
                                notes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_transaction_button")
                    ) {
                        Text("Save Entry", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
