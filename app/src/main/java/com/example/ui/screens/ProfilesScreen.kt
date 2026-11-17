package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.ProfileFinancialSummary
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.util.FormatUtils

@Composable
fun ProfilesScreen(
    profiles: List<FamilyMemberEntity>,
    profileSummaries: List<ProfileFinancialSummary>,
    activeProfileName: String,
    onAddProfileClick: () -> Unit,
    onEditProfileClick: (FamilyMemberEntity) -> Unit,
    onDeleteProfileClick: (FamilyMemberEntity) -> Unit,
    onSetActiveProfile: (String) -> Unit,
    onAnalyzeProfileClick: (String) -> Unit,
    onViewProfileLedgerClick: (String) -> Unit,
    onExportImportClick: () -> Unit
) {
    var profileToDelete by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    val profileById = remember(profiles) { profiles.associateBy { it.id } }

    val totalThirdPartyDebt = profileSummaries.sumOf { it.thirdPartyTheyOwe }
    val totalThirdPartyReceivable = profileSummaries.sumOf { it.thirdPartyOwedToThem }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProfileClick,
                containerColor = Navy900,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("profiles_fab_add")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Family Profile")
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("profiles_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Hero Family Profiles Summary Card
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FAMILY PROFILES & LIABILITIES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = TealAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${profiles.size} Saved Family Profile${if (profiles.size == 1) "" else "s"}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = onAddProfileClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealAccent,
                                    contentColor = Navy900
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("add_profile_header_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "3rd-Party Debt Owed",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalThirdPartyDebt),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalThirdPartyDebt > 0) Color(0xFFFF8A80) else Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "3rd-Party Receivables",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalThirdPartyReceivable),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalThirdPartyReceivable > 0) TealAccent else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Individual Profile Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Text(
                    text = "Tap a profile to set it active, or inspect its income, expenses, shares, and liabilities.",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (profileSummaries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFFBAC7D5),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No family profiles yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create individual profiles (e.g., Mom, Dad, Alex) to analyze spending for whom, income from whom, shared contributions, and third-party debts.",
                                fontSize = 12.sp,
                                color = TextSecondaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onAddProfileClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add First Family Profile", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(profileSummaries, key = { it.profileName }) { summary ->
                    val entity = profileById[summary.profileId]
                    val isActive = summary.profileName.equals(activeProfileName, ignoreCase = true)

                    ProfileDetailCard(
                        summary = summary,
                        isActive = isActive,
                        onSetActive = { onSetActiveProfile(summary.profileName) },
                        onEdit = { entity?.let(onEditProfileClick) },
                        onDelete = { entity?.let { profileToDelete = it } },
                        onAnalyze = { onAnalyzeProfileClick(summary.profileName) },
                        onViewLedger = { onViewProfileLedgerClick(summary.profileName) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Offline Backup Card
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImportExport,
                                    contentDescription = null,
                                    tint = Navy900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Offline Backup (Export / Import)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                                Text(
                                    text = "Export or restore full year or selected months (JSON/CSV)",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onExportImportClick,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    profileToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            containerColor = Color.White,
            titleContentColor = Navy900,
            textContentColor = TextSecondaryLight,
            title = { Text("Remove Profile?", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Text(
                    "Remove '${member.name}' from family profiles? Existing ledger entries and liabilities will keep their historical labels.",
                    fontSize = 13.sp,
                    color = TextSecondaryLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfileClick(member)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel", color = TextSecondaryLight, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailCard(
    summary: ProfileFinancialSummary,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onAnalyze: () -> Unit,
    onViewLedger: () -> Unit
) {
    val avatarColor = FormatUtils.parseAvatarColor(summary.avatarColorHex, summary.profileName)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) TealAccent else CardBorderLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (summary.isExplicitProfile) onSetActive() }
            .testTag("profile_card_${summary.profileName}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.profileName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = summary.profileName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealLight
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF007563),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Active",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF007563)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = summary.role,
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                if (summary.isExplicitProfile) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEdit != null) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Navy900, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F4F8))
            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("Income", FormatUtils.formatCurrency(summary.totalIncome), IncomeGreen, Modifier.weight(1f))
                MetricColumn("Paid Out", FormatUtils.formatCurrency(summary.totalExpensePaid), Navy900, Modifier.weight(1f))
                MetricColumn("Spent For", FormatUtils.formatCurrency(summary.totalExpenseForThem), ExpenseRed, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("Shares Given", FormatUtils.formatCurrency(summary.contributionsProvided), Color(0xFF007563), Modifier.weight(1f))
                MetricColumn("Family Debt", FormatUtils.formatCurrency(summary.familyTheyOwe), ExpenseRed, Modifier.weight(1f))
                MetricColumn("3rd-Party Debt", FormatUtils.formatCurrency(summary.thirdPartyTheyOwe), ExpenseRed, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net Liability Standing Banner
            val netPositive = summary.netLiabilityStanding >= 0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (netPositive) IncomeGreenBg else ExpenseRedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Liability Standing (Family + 3rd Party)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy900
                    )
                    Text(
                        text = if (netPositive) "+${FormatUtils.formatCurrency(summary.netLiabilityStanding)}"
                        else "-${FormatUtils.formatCurrency(kotlin.math.abs(summary.netLiabilityStanding))}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netPositive) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Drill-down Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAnalyze,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewLedger,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Entries", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, color = TextSecondaryLight)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
