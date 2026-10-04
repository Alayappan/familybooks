package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddInvoiceDialog(
    members: List<FamilyMemberEntity>,
    currentMember: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        fromMember: String,
        toMember: String,
        amount: Double,
        dueDate: Long?,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val memberNames = if (members.isNotEmpty()) {
        members.map { it.name }.distinct()
    } else {
        listOf(currentMember)
    }

    var toMember by remember { mutableStateOf(currentMember) } // Default: current member is owed
    var fromMember by remember {
        mutableStateOf(memberNames.firstOrNull { !it.equals(currentMember, ignoreCase = true) } ?: "Dad")
    }
    var notes by remember { mutableStateOf("") }
    var dueDaysOption by remember { mutableStateOf(7) } // 7, 14, 30, or 0 (no due date)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("add_invoice_dialog"),
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
                    Column {
                        Text(
                            text = "New Family Invoice",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Record informal debts & splits between members",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Invoice Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Invoice Title / Purpose") },
                    placeholder = { Text("e.g. Electricity bill, Weekend groceries split") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount Owed ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_amount_input")
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

                // Debtor: Who owes money? (From Member)
                Text(
                    text = "Who Owes Money? (Debtor)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    memberNames.forEach { name ->
                        val isSelected = fromMember.equals(name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFFFEBEE) else Color(0xFFF0F4F8),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ExpenseRed) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { fromMember = name }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(FormatUtils.getMemberAvatarColor(name))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) ExpenseRed else Navy900,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Creditor: Who is owed money? (To Member)
                Text(
                    text = "Who Paid / Is Owed? (Creditor)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    memberNames.forEach { name ->
                        val isSelected = toMember.equals(name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF0F4F8),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E7D32)) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { toMember = name }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(FormatUtils.getMemberAvatarColor(name))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFF2E7D32) else Navy900,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Due Date Options
                Text(
                    text = "Payment Due Date",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0 to "No Due",
                        7 to "7 Days",
                        14 to "14 Days",
                        30 to "30 Days"
                    ).forEach { (days, label) ->
                        val isSelected = dueDaysOption == days
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Navy900 else Color(0xFFF5F7FA),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dueDaysOption = days }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSelected) Color.White else Color(0xFF4A5568),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Details or reference") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (title.isBlank()) {
                                errorMessage = "Please enter an invoice title"
                                return@Button
                            }
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }
                            if (fromMember.equals(toMember, ignoreCase = true)) {
                                errorMessage = "Debtor and Creditor cannot be the same person"
                                return@Button
                            }

                            val dueDateMs = if (dueDaysOption > 0) {
                                System.currentTimeMillis() + (dueDaysOption * 24L * 60L * 60L * 1000L)
                            } else null

                            onSave(
                                title.trim(),
                                fromMember,
                                toMember,
                                parsedAmount,
                                dueDateMs,
                                notes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_invoice_button")
                    ) {
                        Text("Create Invoice", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
