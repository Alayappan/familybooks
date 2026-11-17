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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.PendingOrange
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.familyOutlinedTextFieldColors
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddInvoiceDialog(
    members: List<FamilyMemberEntity>,
    currentMember: String,
    initialLiabilityType: String = LiabilityType.FAMILY_IOU,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        liabilityType: String,
        linkedProfile: String,
        fromMember: String,
        toMember: String,
        amount: Double,
        dueDate: Long?,
        notes: String
    ) -> Unit
) {
    val profileNames = remember(members) { members.map { it.name }.distinct() }
    val defaultOwner = currentMember.ifBlank { profileNames.firstOrNull() ?: "" }

    var liabilityType by remember { mutableStateOf(initialLiabilityType) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var linkedProfile by remember { mutableStateOf(defaultOwner) }
    var fromMember by remember {
        mutableStateOf(
            when (initialLiabilityType) {
                LiabilityType.THIRD_PARTY_PAYABLE -> defaultOwner
                LiabilityType.THIRD_PARTY_RECEIVABLE -> ""
                else -> profileNames.firstOrNull { !it.equals(defaultOwner, ignoreCase = true) } ?: ""
            }
        )
    }
    var toMember by remember {
        mutableStateOf(
            when (initialLiabilityType) {
                LiabilityType.THIRD_PARTY_PAYABLE -> ""
                LiabilityType.THIRD_PARTY_RECEIVABLE -> defaultOwner
                else -> defaultOwner
            }
        )
    }
    var notes by remember { mutableStateOf("") }
    var dueDaysOption by remember { mutableIntStateOf(30) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val thirdPartyPresets = listOf("Bank / Loan", "Credit Card", "Landlord", "Friend", "Clinic / Hospital", "Vendor")

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
                            text = "Add Debt / Liability",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Track family IOUs & third-party debts per profile",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryLight)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Liability Type Selector
                Text(
                    text = "Liability Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(LiabilityType.FAMILY_IOU, "Family IOU", Navy900),
                        Triple(LiabilityType.THIRD_PARTY_PAYABLE, "3rd-Party Debt", ExpenseRed),
                        Triple(LiabilityType.THIRD_PARTY_RECEIVABLE, "3rd-Party Owed", IncomeGreen)
                    ).forEach { (typeKey, label, activeColor) ->
                        val isSelected = liabilityType == typeKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) activeColor else Color(0xFFF0F4F8),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    liabilityType = typeKey
                                    errorMessage = null
                                    when (typeKey) {
                                        LiabilityType.FAMILY_IOU -> {
                                            toMember = defaultOwner
                                            fromMember = profileNames.firstOrNull { !it.equals(defaultOwner, ignoreCase = true) } ?: ""
                                        }
                                        LiabilityType.THIRD_PARTY_PAYABLE -> {
                                            linkedProfile = defaultOwner
                                            fromMember = defaultOwner
                                            toMember = ""
                                        }
                                        LiabilityType.THIRD_PARTY_RECEIVABLE -> {
                                            linkedProfile = defaultOwner
                                            fromMember = ""
                                            toMember = defaultOwner
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Navy900,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Title / Purpose") },
                    placeholder = {
                        Text(
                            when (liabilityType) {
                                LiabilityType.THIRD_PARTY_PAYABLE -> "e.g. Car Loan EMI, Medical Bill, Rent Due"
                                LiabilityType.THIRD_PARTY_RECEIVABLE -> "e.g. Loan to Friend, Security Deposit"
                                else -> "e.g. Electricity share, Groceries split"
                            }
                        )
                    },
                    singleLine = true,
                    colors = familyOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Total Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    colors = familyOutlinedTextFieldColors(),
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

                Spacer(modifier = Modifier.height(14.dp))

                when (liabilityType) {
                    LiabilityType.THIRD_PARTY_PAYABLE -> {
                        // Which family profile owes this 3rd-party debt?
                        Text(
                            text = "Family Profile Responsible (Debtor)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileChipSelector(
                            names = (profileNames + "Family").distinct(),
                            selectedName = linkedProfile,
                            activeBg = Color(0xFFFFEBEE),
                            activeBorder = ExpenseRed,
                            activeText = ExpenseRed,
                            onSelect = {
                                linkedProfile = it
                                fromMember = it
                            }
                        )
                        if (profileNames.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = linkedProfile,
                                onValueChange = {
                                    linkedProfile = it
                                    fromMember = it
                                },
                                label = { Text("Family Profile Name") },
                                placeholder = { Text("e.g. Mom, Dad, Family") },
                                singleLine = true,
                                colors = familyOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "External Third Party Owed (Creditor / Lender)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            thirdPartyPresets.forEach { preset ->
                                val selected = toMember.equals(preset, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) Navy900 else Color(0xFFF0F4F8),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { toMember = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        color = if (selected) Color.White else Navy900,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = toMember,
                            onValueChange = {
                                toMember = it
                                errorMessage = null
                            },
                            label = { Text("3rd-Party Creditor Name") },
                            placeholder = { Text("e.g. Chase Bank, Landlord Mike, City Clinic") },
                            singleLine = true,
                            colors = familyOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LiabilityType.THIRD_PARTY_RECEIVABLE -> {
                        Text(
                            text = "Family Profile To Receive (Creditor)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileChipSelector(
                            names = (profileNames + "Family").distinct(),
                            selectedName = linkedProfile,
                            activeBg = Color(0xFFE8F5E9),
                            activeBorder = IncomeGreen,
                            activeText = IncomeGreen,
                            onSelect = {
                                linkedProfile = it
                                toMember = it
                            }
                        )
                        if (profileNames.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = linkedProfile,
                                onValueChange = {
                                    linkedProfile = it
                                    toMember = it
                                },
                                label = { Text("Family Profile Name") },
                                placeholder = { Text("e.g. Mom, Dad, Family") },
                                singleLine = true,
                                colors = familyOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "External Third Party Who Owes Us (Debtor)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fromMember,
                            onValueChange = {
                                fromMember = it
                                errorMessage = null
                            },
                            label = { Text("3rd-Party Debtor Name") },
                            placeholder = { Text("e.g. Friend John, Employer Reimbursement") },
                            singleLine = true,
                            colors = familyOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        // FAMILY_IOU
                        Text(
                            text = "Who Owes Money? (Debtor)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (profileNames.isNotEmpty()) {
                            ProfileChipSelector(
                                names = profileNames,
                                selectedName = fromMember,
                                activeBg = Color(0xFFFFEBEE),
                                activeBorder = ExpenseRed,
                                activeText = ExpenseRed,
                                onSelect = { fromMember = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (profileNames.size <= 1) {
                            OutlinedTextField(
                                value = fromMember,
                                onValueChange = {
                                    fromMember = it
                                    errorMessage = null
                                },
                                label = { Text("Debtor Name") },
                                placeholder = { Text("e.g. Alex, Jordan") },
                                singleLine = true,
                                colors = familyOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Who Is Owed? (Creditor)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (profileNames.isNotEmpty()) {
                            ProfileChipSelector(
                                names = profileNames,
                                selectedName = toMember,
                                activeBg = Color(0xFFE8F5E9),
                                activeBorder = IncomeGreen,
                                activeText = IncomeGreen,
                                onSelect = { toMember = it }
                            )
                        }
                        if (profileNames.size <= 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = toMember,
                                onValueChange = {
                                    toMember = it
                                    errorMessage = null
                                },
                                label = { Text("Creditor Name") },
                                placeholder = { Text("e.g. Mom, Dad") },
                                singleLine = true,
                                colors = familyOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Due Date
                Text(
                    text = "Due Date",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "No Due",
                        7 to "7d",
                        14 to "14d",
                        30 to "30d",
                        90 to "90d"
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Installment terms, reference, or account note") },
                    colors = familyOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorMessage = "Please enter a title"
                                return@Button
                            }
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }
                            if (fromMember.isBlank() || toMember.isBlank()) {
                                errorMessage = "Please specify both debtor and creditor"
                                return@Button
                            }
                            if (fromMember.equals(toMember, ignoreCase = true)) {
                                errorMessage = "Debtor and Creditor cannot be the same"
                                return@Button
                            }

                            val dueDateMs = if (dueDaysOption > 0) {
                                System.currentTimeMillis() + (dueDaysOption * 24L * 60L * 60L * 1000L)
                            } else null

                            onSave(
                                title.trim(),
                                liabilityType,
                                if (liabilityType == LiabilityType.FAMILY_IOU) "" else linkedProfile.trim(),
                                fromMember.trim(),
                                toMember.trim(),
                                parsedAmount,
                                dueDateMs,
                                notes.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_invoice_button")
                    ) {
                        Text(
                            text = if (liabilityType == LiabilityType.FAMILY_IOU) "Create IOU" else "Save Liability",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileChipSelector(
    names: List<String>,
    selectedName: String,
    activeBg: Color,
    activeBorder: Color,
    activeText: Color,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        names.forEach { name ->
            val isSelected = selectedName.equals(name, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) activeBg else Color(0xFFF0F4F8),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, activeBorder) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(name) }
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
                        color = if (isSelected) activeText else Navy900,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
