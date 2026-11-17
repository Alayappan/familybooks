package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.familyOutlinedTextFieldColors
import com.example.ui.util.FormatUtils

@Composable
fun RefillCashDialog(
    currentCashInHand: Double,
    onDismiss: () -> Unit,
    onAddCash: (amount: Double, note: String) -> Unit,
    onSetBalance: (newBalance: Double) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0: Add Cash (ATM), 1: Set Baseline Balance
    var amountText by remember { mutableStateOf("100.00") }
    var note by remember { mutableStateOf("ATM Cash Withdrawal") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickAmounts = listOf(50.0, 100.0, 200.0, 500.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("refill_cash_dialog"),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cash in Hand (Wallet Pool)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Cash In Hand Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = TealLight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Cash Available:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Navy900
                        )
                        Text(
                            text = FormatUtils.formatCurrency(currentCashInHand),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentCashInHand >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mode == 0) Navy900 else Color(0xFFF0F4F8),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                mode = 0
                                note = "ATM Cash Withdrawal"
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "+ Refill Cash",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == 0) Color.White else Navy900
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mode == 1) Navy900 else Color(0xFFF0F4F8),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                mode = 1
                                amountText = "%.2f".format(currentCashInHand.coerceAtLeast(0.0))
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Set Baseline Balance",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == 1) Color.White else Navy900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Pills
                if (mode == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.forEach { amt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0F4F8),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { amountText = "%.2f".format(amt) }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+$${amt.toInt()}",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(if (mode == 0) "Cash Amount to Add ($)" else "New Cash in Hand Total ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = familyOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Source / Note") },
                        placeholder = { Text("e.g. Bank ATM, Cash gift") },
                        singleLine = true,
                        colors = familyOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondaryLight, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = amountText.toDoubleOrNull()
                            if (parsed == null || parsed < 0.0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }
                            if (mode == 0) {
                                if (parsed <= 0.0) {
                                    errorMessage = "Amount to add must be greater than $0"
                                    return@Button
                                }
                                onAddCash(parsed, note.trim().ifEmpty { "ATM Refill" })
                            } else {
                                onSetBalance(parsed)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (mode == 0) "Add to Wallet Pool" else "Update Cash Balance",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
