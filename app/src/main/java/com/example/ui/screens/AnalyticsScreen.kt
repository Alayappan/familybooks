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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils
import com.example.viewmodel.FamilyBooksViewModel

@Composable
fun AnalyticsScreen(
    monthAnalytics: FamilyBooksViewModel.MonthAnalyticsSummary,
    profiles: List<FamilyMemberEntity>,
    selectedProfile: String,
    onSelectProfile: (String) -> Unit,
    cashInHandBalance: Double
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("analytics_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Profile Selector Bar for Per-Profile Analysis
            Text(
                text = "Analyze Scope (Family or Individual Profile)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf("ALL") + profiles.map { it.name }.distinct()
                options.forEach { profileKey ->
                    val isSelected = selectedProfile.equals(profileKey, ignoreCase = true)
                    val label = if (profileKey == "ALL") "All Family" else profileKey
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Navy900 else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Navy900 else CardBorderLight
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectProfile(profileKey) }
                            .testTag("analytics_profile_chip_$profileKey")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            if (profileKey != "ALL") {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) TealAccent else FormatUtils.getMemberAvatarColor(profileKey)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Navy900
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month on Month Comparison Header Card
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
                        Column {
                            Text(
                                text = if (selectedProfile == "ALL") "FAMILY MONTH-ON-MONTH"
                                else "${selectedProfile.uppercase()} • PROFILE ANALYSIS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TealAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${monthAnalytics.currentMonthLabel} vs ${monthAnalytics.previousMonthLabel}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TealAccent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    tint = TealAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MoM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent
                                )
                            }
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
                                text = if (selectedProfile == "ALL") "Expenses This Month" else "Paid by $selectedProfile",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.currentTotalExpense),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val reduced = monthAnalytics.expenseChangePercent < 0
                                Icon(
                                    imageVector = if (reduced) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (reduced) TealAccent else ExpenseRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${FormatUtils.formatPercentage(monthAnalytics.expenseChangePercent)} vs last mo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (reduced) TealAccent else ExpenseRed
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedProfile == "ALL") "Income This Month" else "Income ($selectedProfile)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.currentTotalIncome),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val increased = monthAnalytics.incomeChangePercent >= 0
                                Icon(
                                    imageVector = if (increased) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (increased) TealAccent else ExpenseRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${FormatUtils.formatPercentage(monthAnalytics.incomeChangePercent)} vs last mo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (increased) TealAccent else ExpenseRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Net Savings: ${FormatUtils.formatCurrency(monthAnalytics.currentNetSavings)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Last mo: ${FormatUtils.formatCurrency(monthAnalytics.previousNetSavings)}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attribution, Contributions & Liabilities Card (for selected profile or family)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedProfile == "ALL") "Shares & Liabilities Overview"
                        else "$selectedProfile • Attribution & Liabilities",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedProfile == "ALL") "Total Expenses" else "Spent For $selectedProfile",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.expenseForProfileThisMonth),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shares Contributed",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.contributionsProvidedThisMonth),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007563)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF0F4F8))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "3rd-Party Debt Owed", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.thirdPartyDebtOwed),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "3rd-Party Receivables", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.thirdPartyReceivable),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Family IOUs Owed", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.familyDebtOwed),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Family IOUs Receivable", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = FormatUtils.formatCurrency(monthAnalytics.familyReceivable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cash in Hand Pool Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Navy900,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cash in Hand (Wallet Pool)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Cash Spent: ${FormatUtils.formatCurrency(monthAnalytics.cashSpentCurrentMonth)} (Last: ${FormatUtils.formatCurrency(monthAnalytics.cashSpentPreviousMonth)})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = FormatUtils.formatCurrency(cashInHandBalance),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cashInHandBalance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Category-by-Category Comparison",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
            Text(
                text = "This month vs last month" + if (selectedProfile != "ALL") " for $selectedProfile" else "",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (monthAnalytics.categoryComparisons.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No category expense history available",
                            fontWeight = FontWeight.Bold,
                            color = Navy900,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log transactions to see automatic category and month-on-month comparisons.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(monthAnalytics.categoryComparisons, key = { it.category }) { item ->
                CategoryComparisonCard(item = item)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (monthAnalytics.paymentMethodBreakdown.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Payment Methods Breakdown (This Month)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        monthAnalytics.paymentMethodBreakdown.entries.sortedByDescending { it.value }.forEach { (method, amt) ->
                            val total = monthAnalytics.currentTotalExpense.coerceAtLeast(1.0)
                            val pct = (amt / total).toFloat()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = method, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                                Text(
                                    text = "${FormatUtils.formatCurrency(amt)} (${(pct * 100).toInt()}%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Navy900,
                                trackColor = Color(0xFFF0F4F8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun CategoryComparisonCard(item: FamilyBooksViewModel.CategoryMoMComparison) {
    val catColor = FormatUtils.getCategoryColor(item.category)
    val maxAmt = maxOf(item.currentMonthAmount, item.previousMonthAmount).coerceAtLeast(1.0)
    val curProgress = (item.currentMonthAmount / maxAmt).toFloat()
    val prevProgress = (item.previousMonthAmount / maxAmt).toFloat()

    val isReduced = item.difference < 0
    val isSame = item.difference == 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FormatUtils.getCategoryIcon(item.category),
                            contentDescription = item.category,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = item.category, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy900)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isReduced -> IncomeGreenBg
                        isSame -> Color(0xFFF0F4F8)
                        else -> ExpenseRedBg
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isReduced -> Icons.Default.TrendingDown
                                isSame -> Icons.Default.CompareArrows
                                else -> Icons.Default.TrendingUp
                            },
                            contentDescription = null,
                            tint = when {
                                isReduced -> IncomeGreen
                                isSame -> Color.Gray
                                else -> ExpenseRed
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isSame -> "Same as last mo"
                                isReduced -> "Saved ${FormatUtils.formatCurrency(kotlin.math.abs(item.difference))} (${FormatUtils.formatPercentage(item.percentageChange)})"
                                else -> "+${FormatUtils.formatCurrency(item.difference)} (${FormatUtils.formatPercentage(item.percentageChange)})"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isReduced -> IncomeGreen
                                isSame -> Color(0xFF4A5568)
                                else -> ExpenseRed
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "This Month", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy900)
                    Text(text = FormatUtils.formatCurrency(item.currentMonthAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { curProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = catColor,
                    trackColor = Color(0xFFF0F4F8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Last Month", fontSize = 12.sp, color = Color.Gray)
                    Text(text = FormatUtils.formatCurrency(item.previousMonthAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { prevProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFBAC7D5),
                    trackColor = Color(0xFFF0F4F8)
                )
            }
        }
    }
}
