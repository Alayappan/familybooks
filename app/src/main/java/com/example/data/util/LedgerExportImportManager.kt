package com.example.data.util

import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.data.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LedgerImportPreview(
    val detectedYears: List<Int>,
    val detectedMonthsByYear: Map<Int, List<Int>>, // Year -> List of 0-based months
    val allTransactions: List<TransactionEntity>,
    val allInvoices: List<FamilyInvoiceEntity>,
    val allProfiles: List<FamilyMemberEntity> = emptyList(),
    val format: String // "JSON" or "CSV"
)

data class ImportResult(
    val transactionsImported: Int,
    val invoicesImported: Int,
    val profilesImported: Int = 0,
    val year: Int?,
    val monthCount: Int,
    val message: String
)

object LedgerExportImportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun matchesYearAndMonths(timestamp: Long, year: Int, months: Set<Int>): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        if (y != year) return false
        if (months.isNotEmpty() && !months.contains(m)) return false
        return true
    }

    fun getAvailableYears(
        transactions: List<TransactionEntity>,
        invoices: List<FamilyInvoiceEntity> = emptyList()
    ): List<Int> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (transactions.map { it.date } + invoices.map { it.date }).map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            cal.get(Calendar.YEAR)
        }.toSet() + currentYear
        return years.sortedDescending()
    }

    fun exportToJson(
        transactions: List<TransactionEntity>,
        invoices: List<FamilyInvoiceEntity>,
        profiles: List<FamilyMemberEntity> = emptyList(),
        year: Int,
        months: Set<Int>, // empty = full year
        currentMember: String
    ): String {
        val filteredTx = transactions.filter { matchesYearAndMonths(it.date, year, months) }
        val filteredInv = invoices.filter { matchesYearAndMonths(it.date, year, months) }

        val scopeDescription = when {
            months.isEmpty() -> "Full Year $year"
            months.size == 1 -> "${monthNames[months.first()]} $year"
            else -> months.sorted().joinToString(", ") { monthNames[it] } + " ($year)"
        }

        val totalExpense = filteredTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalIncome = filteredTx.filter { it.type == "INCOME" }.sumOf { it.amount }

        val root = JSONObject()
        root.put("app", "FamilyBooks")
        root.put("exportVersion", "2.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedBy", currentMember)
        root.put("year", year)

        val monthsArray = JSONArray()
        months.sorted().forEach { monthsArray.put(it) }
        root.put("months", monthsArray)
        root.put("scopeDescription", scopeDescription)

        val summaryObj = JSONObject()
        summaryObj.put("transactionCount", filteredTx.size)
        summaryObj.put("invoiceCount", filteredInv.size)
        summaryObj.put("profileCount", profiles.size)
        summaryObj.put("totalExpense", totalExpense)
        summaryObj.put("totalIncome", totalIncome)
        summaryObj.put("netBalance", totalIncome - totalExpense)
        root.put("summary", summaryObj)

        val profilesArray = JSONArray()
        for (profile in profiles) {
            val obj = JSONObject()
            obj.put("id", profile.id)
            obj.put("name", profile.name)
            obj.put("role", profile.role)
            obj.put("avatarColorHex", profile.avatarColorHex)
            obj.put("isPrimary", profile.isPrimary)
            obj.put("notes", profile.notes)
            obj.put("createdAt", profile.createdAt)
            obj.put("updatedAt", profile.updatedAt)
            profilesArray.put(obj)
        }
        root.put("profiles", profilesArray)

        val txArray = JSONArray()
        for (tx in filteredTx) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("date", tx.date)
            obj.put("formattedDate", dateFormat.format(Date(tx.date)))
            obj.put("category", tx.category)
            obj.put("paymentMethod", tx.paymentMethod)
            obj.put("paidBy", tx.paidBy)
            obj.put("forWhom", tx.forWhom)
            obj.put("fromWhom", tx.fromWhom)
            obj.put("contributedBy", tx.contributedBy)
            obj.put("contributedAmount", tx.contributedAmount)
            obj.put("notes", tx.notes)
            obj.put("createdAt", tx.createdAt)
            obj.put("updatedAt", tx.updatedAt)
            obj.put("isDeleted", tx.isDeleted)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val invArray = JSONArray()
        for (inv in filteredInv) {
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("title", inv.title)
            obj.put("liabilityType", inv.liabilityType)
            obj.put("linkedProfile", inv.linkedProfile)
            obj.put("fromMember", inv.fromMember)
            obj.put("toMember", inv.toMember)
            obj.put("amount", inv.amount)
            obj.put("paidAmount", inv.paidAmount)
            obj.put("date", inv.date)
            if (inv.dueDate != null) obj.put("dueDate", inv.dueDate)
            obj.put("notes", inv.notes)
            obj.put("status", inv.status)
            if (inv.settledAt != null) obj.put("settledAt", inv.settledAt)
            obj.put("createdAt", inv.createdAt)
            obj.put("updatedAt", inv.updatedAt)
            obj.put("isDeleted", inv.isDeleted)
            invArray.put(obj)
        }
        root.put("invoices", invArray)

        return root.toString(2)
    }

    fun exportToCsv(
        transactions: List<TransactionEntity>,
        year: Int,
        months: Set<Int>
    ): String {
        val filteredTx = transactions.filter { matchesYearAndMonths(it.date, year, months) }
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Amount,Category,PaymentMethod,PaidBy,ForWhom,FromWhom,ContributedBy,ContributedAmount,Notes,CreatedAt\n")

        for (tx in filteredTx) {
            val formattedDate = dateFormat.format(Date(tx.date))
            sb.append(
                listOf(
                    tx.id,
                    formattedDate,
                    tx.type,
                    tx.amount.toString(),
                    csvEscape(tx.category),
                    csvEscape(tx.paymentMethod),
                    csvEscape(tx.paidBy),
                    csvEscape(tx.forWhom),
                    csvEscape(tx.fromWhom),
                    csvEscape(tx.contributedBy),
                    tx.contributedAmount.toString(),
                    csvEscape(tx.notes),
                    tx.createdAt.toString()
                ).joinToString(",")
            ).append("\n")
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    fun parseImportContent(content: String): Result<LedgerImportPreview> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Import content is empty"))
        }

        return if (trimmed.startsWith("{")) {
            parseJsonImport(trimmed)
        } else if (trimmed.contains(",") && (trimmed.contains("Date") || trimmed.contains("Amount") || trimmed.contains("EXPENSE") || trimmed.contains("INCOME"))) {
            parseCsvImport(trimmed)
        } else {
            Result.failure(IllegalArgumentException("Unrecognized format. Please provide valid FamilyBooks JSON or CSV export."))
        }
    }

    private fun parseJsonImport(jsonString: String): Result<LedgerImportPreview> {
        return try {
            val root = JSONObject(jsonString)
            val txList = mutableListOf<TransactionEntity>()
            val invList = mutableListOf<FamilyInvoiceEntity>()
            val profileList = mutableListOf<FamilyMemberEntity>()

            val profilesArray = root.optJSONArray("profiles")
            if (profilesArray != null) {
                for (i in 0 until profilesArray.length()) {
                    val obj = profilesArray.getJSONObject(i)
                    val name = obj.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        profileList.add(
                            FamilyMemberEntity(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                name = name,
                                role = obj.optString("role", "Member"),
                                avatarColorHex = obj.optString("avatarColorHex", "#0A2540"),
                                isPrimary = obj.optBoolean("isPrimary", false),
                                notes = obj.optString("notes", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                                isDeleted = false
                            )
                        )
                    }
                }
            }

            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            type = obj.optString("type", "EXPENSE"),
                            amount = obj.optDouble("amount", 0.0),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            category = obj.optString("category", "General"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            paidBy = obj.optString("paidBy", "Family"),
                            forWhom = obj.optString("forWhom", ""),
                            fromWhom = obj.optString("fromWhom", ""),
                            contributedBy = obj.optString("contributedBy", ""),
                            contributedAmount = obj.optDouble("contributedAmount", 0.0),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val invArray = root.optJSONArray("invoices")
            if (invArray != null) {
                for (i in 0 until invArray.length()) {
                    val obj = invArray.getJSONObject(i)
                    invList.add(
                        FamilyInvoiceEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Liability"),
                            liabilityType = obj.optString("liabilityType", LiabilityType.FAMILY_IOU),
                            linkedProfile = obj.optString("linkedProfile", ""),
                            fromMember = obj.optString("fromMember", "Member"),
                            toMember = obj.optString("toMember", "Member"),
                            amount = obj.optDouble("amount", 0.0),
                            paidAmount = obj.optDouble("paidAmount", 0.0),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null,
                            notes = obj.optString("notes", ""),
                            status = obj.optString("status", "UNPAID"),
                            settledAt = if (obj.has("settledAt") && !obj.isNull("settledAt")) obj.optLong("settledAt") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            if (txList.isEmpty() && invList.isEmpty() && profileList.isEmpty()) {
                return Result.failure(IllegalArgumentException("No transactions, liabilities, or profiles found in JSON file"))
            }

            val yearsMap = mutableMapOf<Int, MutableSet<Int>>()
            val cal = Calendar.getInstance()
            for (tx in txList) {
                cal.timeInMillis = tx.date
                yearsMap.getOrPut(cal.get(Calendar.YEAR)) { mutableSetOf() }.add(cal.get(Calendar.MONTH))
            }
            for (inv in invList) {
                cal.timeInMillis = inv.date
                yearsMap.getOrPut(cal.get(Calendar.YEAR)) { mutableSetOf() }.add(cal.get(Calendar.MONTH))
            }

            Result.success(
                LedgerImportPreview(
                    detectedYears = yearsMap.keys.sortedDescending(),
                    detectedMonthsByYear = yearsMap.mapValues { it.value.sorted() },
                    allTransactions = txList,
                    allInvoices = invList,
                    allProfiles = profileList,
                    format = "JSON"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse JSON export: ${e.message}"))
        }
    }

    private fun parseCsvImport(csvContent: String): Result<LedgerImportPreview> {
        return try {
            val lines = csvContent.lines().filter { it.isNotBlank() }
            if (lines.size < 2) {
                return Result.failure(IllegalArgumentException("CSV contains no data rows"))
            }

            val header = lines.first().split(",").map { it.trim().lowercase() }
            val idIndex = header.indexOf("id")
            val dateIndex = header.indexOf("date")
            val typeIndex = header.indexOf("type")
            val amountIndex = header.indexOf("amount")
            val categoryIndex = header.indexOf("category")
            val methodIndex = header.indexOf("paymentmethod")
            val paidByIndex = header.indexOf("paidby")
            val forWhomIndex = header.indexOf("forwhom")
            val fromWhomIndex = header.indexOf("fromwhom")
            val contributedByIndex = header.indexOf("contributedby")
            val contributedAmountIndex = header.indexOf("contributedamount")
            val notesIndex = header.indexOf("notes")

            val txList = mutableListOf<TransactionEntity>()
            for (line in lines.drop(1)) {
                val tokens = parseCsvLine(line)
                if (tokens.isEmpty()) continue

                val id = if (idIndex in tokens.indices) tokens[idIndex].ifBlank { UUID.randomUUID().toString() } else UUID.randomUUID().toString()
                val dateStr = if (dateIndex in tokens.indices) tokens[dateIndex] else ""
                val date = parseDateOrTimestamp(dateStr)
                val type = if (typeIndex in tokens.indices) tokens[typeIndex].uppercase() else "EXPENSE"
                val amount = if (amountIndex in tokens.indices) tokens[amountIndex].toDoubleOrNull() ?: 0.0 else 0.0
                val category = if (categoryIndex in tokens.indices) tokens[categoryIndex] else "General"
                val method = if (methodIndex in tokens.indices) tokens[methodIndex] else "Cash"
                val paidBy = if (paidByIndex in tokens.indices) tokens[paidByIndex] else "Family"
                val forWhom = if (forWhomIndex in tokens.indices) tokens[forWhomIndex] else ""
                val fromWhom = if (fromWhomIndex in tokens.indices) tokens[fromWhomIndex] else ""
                val contributedBy = if (contributedByIndex in tokens.indices) tokens[contributedByIndex] else ""
                val contributedAmount = if (contributedAmountIndex in tokens.indices) tokens[contributedAmountIndex].toDoubleOrNull() ?: 0.0 else 0.0
                val notes = if (notesIndex in tokens.indices) tokens[notesIndex] else ""

                txList.add(
                    TransactionEntity(
                        id = id,
                        type = if (type.contains("INC")) "INCOME" else "EXPENSE",
                        amount = amount,
                        date = date,
                        category = category.ifBlank { "General" },
                        paymentMethod = method.ifBlank { "Cash" },
                        paidBy = paidBy.ifBlank { "Family" },
                        forWhom = forWhom,
                        fromWhom = fromWhom,
                        contributedBy = contributedBy,
                        contributedAmount = contributedAmount,
                        notes = notes,
                        createdAt = date,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            val yearsMap = mutableMapOf<Int, MutableSet<Int>>()
            val cal = Calendar.getInstance()
            for (tx in txList) {
                cal.timeInMillis = tx.date
                yearsMap.getOrPut(cal.get(Calendar.YEAR)) { mutableSetOf() }.add(cal.get(Calendar.MONTH))
            }

            Result.success(
                LedgerImportPreview(
                    detectedYears = yearsMap.keys.sortedDescending(),
                    detectedMonthsByYear = yearsMap.mapValues { it.value.sorted() },
                    allTransactions = txList,
                    allInvoices = emptyList(),
                    format = "CSV"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse CSV: ${e.message}"))
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur.clear()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString().trim())
        return result
    }

    private fun parseDateOrTimestamp(str: String): Long {
        val cleaned = str.trim()
        val num = cleaned.toLongOrNull()
        if (num != null && num > 100000000000L) {
            return num
        }
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        )
        for (f in formats) {
            try {
                val d = f.parse(cleaned)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
