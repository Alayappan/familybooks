package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.TransactionEntity
import com.example.data.util.ImportResult
import com.example.data.util.LedgerExportImportManager
import com.example.data.util.LedgerImportPreview
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils
import java.util.Calendar

enum class ScopeType {
    FULL_YEAR,
    PARTICULAR_MONTH,
    MULTIPLE_MONTHS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportImportLedgerDialog(
    transactions: List<TransactionEntity>,
    invoices: List<FamilyInvoiceEntity>,
    currentMember: String,
    onDismiss: () -> Unit,
    onExport: (year: Int, months: Set<Int>, format: String) -> String,
    onImport: (preview: LedgerImportPreview, targetYear: Int?, targetMonths: Set<Int>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val availableYears = remember(transactions) {
        LedgerExportImportManager.getAvailableYears(transactions)
    }

    // --- Export States ---
    var exportYear by remember { mutableIntStateOf(availableYears.firstOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var exportScopeType by remember { mutableStateOf(ScopeType.FULL_YEAR) }
    var exportSingleMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var exportSelectedMonths by remember { mutableStateOf(setOf(Calendar.getInstance().get(Calendar.MONTH))) }
    var exportFormat by remember { mutableStateOf("JSON") } // "JSON" or "CSV"
    var exportFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // --- Import States ---
    var importRawText by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<LedgerImportPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    var importScopeType by remember { mutableStateOf(ScopeType.FULL_YEAR) }
    var importSelectedYear by remember { mutableStateOf<Int?>(null) }
    var importSingleMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var importSelectedMonths by remember { mutableStateOf(setOf<Int>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("export_import_ledger_dialog"),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ledger Export & Import",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Yearly and monthly ledger backup & restore",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs (Export vs Import)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF0F4F8),
                    contentColor = Navy900,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Navy900,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = Navy900,
                        unselectedContentColor = Color.Gray
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = Navy900,
                        unselectedContentColor = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Body content based on tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // ==================== EXPORT TAB ====================

                        // 1. Year Selection
                        Text(
                            text = "1. Select Year",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableYears.forEach { yr ->
                                val isSelected = exportYear == yr
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { exportYear = yr }
                                ) {
                                    Text(
                                        text = "$yr",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else Navy900,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Scope Selection (Full Year / Particular Month / Months of Year)
                        Text(
                            text = "2. Export Range",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                ScopeType.FULL_YEAR to "Full Year",
                                ScopeType.PARTICULAR_MONTH to "Particular Month",
                                ScopeType.MULTIPLE_MONTHS to "Months of Year"
                            ).forEach { (st, label) ->
                                val isSelected = exportScopeType == st
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TealAccent else Color(0xFFF0F4F8),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { exportScopeType = st }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Month Selectors based on Scope
                        when (exportScopeType) {
                            ScopeType.FULL_YEAR -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0F5FA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "All 12 months of $exportYear will be included in the export.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4A5568),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            ScopeType.PARTICULAR_MONTH -> {
                                Text(
                                    text = "Select Month:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LedgerExportImportManager.monthNames.forEachIndexed { idx, name ->
                                        val isSelected = exportSingleMonth == idx
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { exportSingleMonth = idx }
                                        ) {
                                            Text(
                                                text = name.take(3),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Navy900,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            ScopeType.MULTIPLE_MONTHS -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Select Months of $exportYear (${exportSelectedMonths.size} selected):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy900
                                    )
                                    TextButton(onClick = {
                                        exportSelectedMonths = if (exportSelectedMonths.size == 12) emptySet() else (0..11).toSet()
                                    }) {
                                        Text(
                                            text = if (exportSelectedMonths.size == 12) "Clear All" else "Select All",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900
                                        )
                                    }
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LedgerExportImportManager.monthNames.forEachIndexed { idx, name ->
                                        val isChecked = exportSelectedMonths.contains(idx)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChecked) Navy900 else Color(0xFFF0F4F8),
                                            border = if (isChecked) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    exportSelectedMonths = if (isChecked) {
                                                        exportSelectedMonths - idx
                                                    } else {
                                                        exportSelectedMonths + idx
                                                    }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                if (isChecked) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = name.take(3),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isChecked) Color.White else Navy900
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Format Choice
                        Text(
                            text = "3. Export Format",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "JSON" to "JSON Backup (Full Data & Invoices)",
                                "CSV" to "CSV (Excel / Spreadsheet)"
                            ).forEach { (fmt, desc) ->
                                val isSelected = exportFormat == fmt
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFF0F5FA) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Navy900 else CardBorderLight
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { exportFormat = fmt }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = fmt, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text(text = desc, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Summary of records to export
                        val resolvedExportMonths = when (exportScopeType) {
                            ScopeType.FULL_YEAR -> emptySet()
                            ScopeType.PARTICULAR_MONTH -> setOf(exportSingleMonth)
                            ScopeType.MULTIPLE_MONTHS -> exportSelectedMonths
                        }

                        val matchingTx = transactions.filter {
                            LedgerExportImportManager.matchesYearAndMonths(it.date, exportYear, resolvedExportMonths)
                        }
                        val matchingInv = invoices.filter {
                            LedgerExportImportManager.matchesYearAndMonths(it.date, exportYear, resolvedExportMonths)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Ready to Export:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${matchingTx.size} Transactions, ${matchingInv.size} Invoices in range",
                                    fontSize = 13.sp,
                                    color = Navy900
                                )
                                Text(
                                    text = "Total: ${FormatUtils.formatCurrency(matchingTx.filter { it.type == "EXPENSE" }.sumOf { it.amount })} Expenses | ${FormatUtils.formatCurrency(matchingTx.filter { it.type == "INCOME" }.sumOf { it.amount })} Income",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4A5568)
                                )
                            }
                        }

                        if (exportFeedbackMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = exportFeedbackMessage ?: "",
                                color = IncomeGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Export Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val exportedString = onExport(exportYear, resolvedExportMonths, exportFormat)
                                    clipboardManager.setText(AnnotatedString(exportedString))
                                    exportFeedbackMessage = "Copied ${exportFormat} export (${matchingTx.size} records) to clipboard!"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Navy900),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val exportedString = onExport(exportYear, resolvedExportMonths, exportFormat)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportedString)
                                        putExtra(Intent.EXTRA_TITLE, "FamilyBooks_${exportYear}_ledger.${exportFormat.lowercase()}")
                                        type = if (exportFormat == "CSV") "text/csv" else "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share FamilyBooks Ledger ($exportYear)")
                                    context.startActivity(shareIntent)
                                    exportFeedbackMessage = "Shared ${exportFormat} export file!"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Navy900,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share / Save File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // ==================== IMPORT TAB ====================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Exported JSON or CSV:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )

                            Button(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) {
                                        importRawText = clip
                                        val parseRes = LedgerExportImportManager.parseImportContent(clip)
                                        if (parseRes.isSuccess) {
                                            val prev = parseRes.getOrNull()
                                            importPreview = prev
                                            importError = null
                                            importSelectedYear = prev?.detectedYears?.firstOrNull()
                                            importSelectedMonths = prev?.detectedMonthsByYear?.get(importSelectedYear)?.toSet() ?: emptySet()
                                        } else {
                                            importPreview = null
                                            importError = parseRes.exceptionOrNull()?.message
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F4F8),
                                    contentColor = Navy900
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste from Clipboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = importRawText,
                            onValueChange = {
                                importRawText = it
                                if (it.isNotBlank()) {
                                    val parseRes = LedgerExportImportManager.parseImportContent(it)
                                    if (parseRes.isSuccess) {
                                        val prev = parseRes.getOrNull()
                                        importPreview = prev
                                        importError = null
                                        importSelectedYear = prev?.detectedYears?.firstOrNull()
                                        importSelectedMonths = prev?.detectedMonthsByYear?.get(importSelectedYear)?.toSet() ?: emptySet()
                                    } else {
                                        importPreview = null
                                        importError = parseRes.exceptionOrNull()?.message
                                    }
                                } else {
                                    importPreview = null
                                    importError = null
                                }
                            },
                            placeholder = { Text("Paste FamilyBooks JSON or CSV export content here...") },
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )

                        if (importError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = importError ?: "",
                                color = ExpenseRed,
                                fontSize = 12.sp
                            )
                        }

                        // Preview of detected data
                        importPreview?.let { preview ->
                            Spacer(modifier = Modifier.height(14.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = IncomeGreenBg),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Valid ${preview.format} Ledger Detected",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = IncomeGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${preview.allTransactions.size} Entries, ${preview.allInvoices.size} Liabilities" +
                                            if (preview.allProfiles.isNotEmpty()) ", ${preview.allProfiles.size} Profiles" else "",
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "Years present: ${preview.detectedYears.joinToString(", ")}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4A5568)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Year & Month Filter for Import
                            Text(
                                text = "Import Scope (Filter to full year or particular months):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Year selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                preview.detectedYears.forEach { yr ->
                                    val isSelected = importSelectedYear == yr
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                importSelectedYear = yr
                                                importSelectedMonths = preview.detectedMonthsByYear[yr]?.toSet() ?: emptySet()
                                            }
                                    ) {
                                        Text(
                                            text = "$yr",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Navy900,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Scope Choice for Import
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    ScopeType.FULL_YEAR to "Full Year",
                                    ScopeType.PARTICULAR_MONTH to "Particular Month",
                                    ScopeType.MULTIPLE_MONTHS to "Selected Months"
                                ).forEach { (st, label) ->
                                    val isSelected = importScopeType == st
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) TealAccent else Color(0xFFF0F4F8),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { importScopeType = st }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Month pills for selected year in preview
                            importSelectedYear?.let { yr ->
                                val availableMonthsInFile = preview.detectedMonthsByYear[yr] ?: emptyList()

                                when (importScopeType) {
                                    ScopeType.FULL_YEAR -> {
                                        Text(
                                            text = "All records from year $yr will be imported.",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    ScopeType.PARTICULAR_MONTH -> {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            availableMonthsInFile.forEach { mIdx ->
                                                val isSelected = importSingleMonth == mIdx
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) Navy900 else Color(0xFFF0F4F8),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { importSingleMonth = mIdx }
                                                ) {
                                                    Text(
                                                        text = LedgerExportImportManager.monthNames[mIdx],
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else Navy900,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ScopeType.MULTIPLE_MONTHS -> {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            availableMonthsInFile.forEach { mIdx ->
                                                val isChecked = importSelectedMonths.contains(mIdx)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isChecked) Navy900 else Color(0xFFF0F4F8),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            importSelectedMonths = if (isChecked) {
                                                                importSelectedMonths - mIdx
                                                            } else {
                                                                importSelectedMonths + mIdx
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = LedgerExportImportManager.monthNames[mIdx],
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isChecked) Color.White else Navy900,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Calculate count to be imported
                            val resolvedImportMonths = when (importScopeType) {
                                ScopeType.FULL_YEAR -> emptySet()
                                ScopeType.PARTICULAR_MONTH -> setOf(importSingleMonth)
                                ScopeType.MULTIPLE_MONTHS -> importSelectedMonths
                            }

                            val willImportTx = if (importSelectedYear != null) {
                                preview.allTransactions.filter {
                                    LedgerExportImportManager.matchesYearAndMonths(it.date, importSelectedYear!!, resolvedImportMonths)
                                }
                            } else preview.allTransactions

                            val willImportInv = if (importSelectedYear != null) {
                                preview.allInvoices.filter {
                                    LedgerExportImportManager.matchesYearAndMonths(it.date, importSelectedYear!!, resolvedImportMonths)
                                }
                            } else preview.allInvoices

                            Button(
                                onClick = {
                                    onImport(preview, importSelectedYear, resolvedImportMonths)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Navy900,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Confirm & Merge ${willImportTx.size} Transactions",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
