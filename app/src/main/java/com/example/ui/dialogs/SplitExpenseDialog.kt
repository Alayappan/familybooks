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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitExpenseDialog(
    members: List<FamilyMemberEntity>,
    currentMember: String,
    onDismiss: () -> Unit,
    onConfirmSplit: (
        title: String,
        totalAmount: Double,
        paidBy: String,
        participatingMembers: List<String>,
        alsoLogMainExpense: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    val memberNames = remember(members, currentMember) {
        if (members.isNotEmpty()) {
            members.map { it.name }.distinct()
        } else if (currentMember.isNotBlank()) {
            listOf(currentMember)
        } else {
            emptyList()
        }
    }

    var paidBy by remember { mutableStateOf(currentMember.ifBlank { memberNames.firstOrNull() ?: "" }) }
    val selectedMembers = remember {
        mutableStateListOf<String>().apply {
            addAll(memberNames)
        }
    }
    var alsoLogMainExpense by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val totalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val perPersonShare by remember(totalAmount, selectedMembers.size) {
        derivedStateOf {
            if (selectedMembers.isNotEmpty()) totalAmount / selectedMembers.size else 0.0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("split_expense_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Split Family Expense",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Divide shared bills & track each member's share",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (memberNames.size < 2) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add at least 2 family profiles in the Profiles tab to split expenses between members.",
                            fontSize = 12.sp,
                            color = Navy900,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Expense Title / Purpose") },
                    placeholder = { Text("e.g. Sunday Family Dinner, Water Bill") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = {
                        totalAmountText = it
                        errorMessage = null
                    },
                    label = { Text("Total Bill Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                if (memberNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Who Paid the Full Bill Upfront?",
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
                            val isSelected = paidBy.equals(name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        paidBy = name
                                        if (!selectedMembers.contains(name)) {
                                            selectedMembers.add(name)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Split Among Profiles (${selectedMembers.size} selected)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    memberNames.forEach { memberName ->
                        val isChecked = selectedMembers.contains(memberName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) {
                                        if (selectedMembers.size > 1) selectedMembers.remove(memberName)
                                    } else {
                                        selectedMembers.add(memberName)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedMembers.add(memberName)
                                    } else {
                                        if (selectedMembers.size > 1) selectedMembers.remove(memberName)
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(FormatUtils.getMemberAvatarColor(memberName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = memberName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = memberName,
                                fontSize = 14.sp,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                color = Navy900
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Also Log Expense in Ledger",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Text(
                            text = "Records the full expense paid by ${paidBy.ifBlank { "payer" }}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = alsoLogMainExpense,
                        onCheckedChange = { alsoLogMainExpense = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Navy900
                        )
                    )
                }

                if (totalAmount > 0 && selectedMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Each Member's Share:",
                                    fontWeight = FontWeight.Medium,
                                    color = Navy900,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(perPersonShare),
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val oweCount = selectedMembers.count { !it.equals(paidBy, ignoreCase = true) }
                            Text(
                                text = "Will create $oweCount share IOU(s) owed to $paidBy",
                                fontSize = 11.sp,
                                color = Color(0xFF007563)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            if (memberNames.size < 2) {
                                errorMessage = "Add at least 2 profiles first"
                                return@Button
                            }
                            if (title.isBlank()) {
                                errorMessage = "Please enter an expense title"
                                return@Button
                            }
                            if (totalAmount <= 0.0) {
                                errorMessage = "Please enter a valid bill amount"
                                return@Button
                            }
                            if (selectedMembers.size < 2) {
                                errorMessage = "Select at least 2 profiles to split"
                                return@Button
                            }
                            onConfirmSplit(
                                title.trim(),
                                totalAmount,
                                paidBy,
                                selectedMembers.toList(),
                                alsoLogMainExpense
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_split_button")
                    ) {
                        Text("Split & Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
