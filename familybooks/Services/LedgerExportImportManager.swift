import Foundation

enum ExportImportError: LocalizedError {
    case emptyInput
    case invalidJSON(String)
    case invalidCSV(String)

    var errorDescription: String? {
        switch self {
        case .emptyInput:
            return "No backup data was provided."
        case .invalidJSON(let message):
            return "Invalid FamilyBooks JSON backup: \(message)"
        case .invalidCSV(let message):
            return "Invalid FamilyBooks CSV backup: \(message)"
        }
    }
}

enum LedgerExportImportManager {
    static let csvHeader = "ID,Date,Type,Amount,Category,PaymentMethod,PaidBy,ForWhom,FromWhom,ContributedBy,ContributedAmount,Notes,CreatedAt"

    // MARK: - JSON v2.0 Export

    static func exportJSON(
        profiles: [FamilyMember],
        transactions: [LedgerTransaction],
        liabilities: [FamilyLiability],
        scope: ExportScopeFilter,
        exportedBy: String,
        calendar: Calendar = .current
    ) throws -> String {
        let activeProfiles = profiles.filter { !$0.isDeleted }
        let filteredTransactions = transactions
            .filter { !$0.isDeleted && scope.matches(epochMs: $0.date, calendar: calendar) }
            .sorted { $0.date > $1.date }
        let filteredLiabilities = liabilities
            .filter { !$0.isDeleted && scope.matches(epochMs: $0.date, calendar: calendar) }
            .sorted { $0.date > $1.date }

        let totalExpense = filteredTransactions
            .filter { $0.isExpense }
            .reduce(0.0) { $0 + $1.amount }
        let totalIncome = filteredTransactions
            .filter { $0.isIncome }
            .reduce(0.0) { $0 + $1.amount }

        let profileDTOs = activeProfiles.map {
            ExportProfileDTO(
                id: $0.id,
                name: $0.name,
                role: $0.role,
                avatarColorHex: $0.avatarColorHex,
                isPrimary: $0.isPrimary,
                notes: $0.notes,
                createdAt: $0.createdAt,
                updatedAt: $0.updatedAt
            )
        }

        let txDTOs = filteredTransactions.map {
            ExportTransactionDTO(
                id: $0.id,
                type: $0.type,
                amount: $0.amount,
                date: $0.date,
                formattedDate: FormatUtils.isoTimestamp($0.date),
                category: $0.category,
                paymentMethod: $0.paymentMethod,
                paidBy: $0.paidBy,
                forWhom: $0.forWhom,
                fromWhom: $0.fromWhom,
                contributedBy: $0.contributedBy,
                contributedAmount: $0.contributedAmount,
                notes: $0.notes,
                createdAt: $0.createdAt,
                updatedAt: $0.updatedAt,
                isDeleted: $0.isDeleted
            )
        }

        let liabDTOs = filteredLiabilities.map {
            ExportLiabilityDTO(
                id: $0.id,
                title: $0.title,
                liabilityType: $0.liabilityType,
                linkedProfile: $0.linkedProfile,
                fromMember: $0.fromMember,
                toMember: $0.toMember,
                amount: $0.amount,
                paidAmount: $0.paidAmount,
                date: $0.date,
                dueDate: $0.dueDate,
                notes: $0.notes,
                status: $0.status,
                settledAt: $0.settledAt,
                createdAt: $0.createdAt,
                updatedAt: $0.updatedAt,
                isDeleted: $0.isDeleted
            )
        }

        let summary = ExportSummaryDTO(
            transactionCount: txDTOs.count,
            invoiceCount: liabDTOs.count,
            profileCount: profileDTOs.count,
            totalExpense: FormatUtils.roundToTwoDecimals(totalExpense),
            totalIncome: FormatUtils.roundToTwoDecimals(totalIncome),
            netBalance: FormatUtils.roundToTwoDecimals(totalIncome - totalExpense)
        )

        let payload = ExportPayloadV2(
            app: "FamilyBooks",
            exportVersion: "2.0",
            exportedAt: FormatUtils.nowEpochMs,
            exportedBy: exportedBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Family" : exportedBy,
            year: scope.year,
            months: scope.sortedMonths,
            scopeDescription: scope.descriptionText,
            summary: summary,
            profiles: profileDTOs,
            transactions: txDTOs,
            invoices: liabDTOs
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(payload)
        return String(data: data, encoding: .utf8) ?? "{}"
    }

    // MARK: - CSV Export

    static func exportCSV(
        transactions: [LedgerTransaction],
        scope: ExportScopeFilter,
        calendar: Calendar = .current
    ) -> String {
        let filteredTransactions = transactions
            .filter { !$0.isDeleted && scope.matches(epochMs: $0.date, calendar: calendar) }
            .sorted { $0.date > $1.date }

        var lines: [String] = [csvHeader]
        for tx in filteredTransactions {
            let row = [
                csvEscape(tx.id),
                csvEscape(FormatUtils.isoTimestamp(tx.date)),
                csvEscape(tx.type),
                String(format: "%.2f", tx.amount),
                csvEscape(tx.category),
                csvEscape(tx.paymentMethod),
                csvEscape(tx.paidBy),
                csvEscape(tx.forWhom),
                csvEscape(tx.fromWhom),
                csvEscape(tx.contributedBy),
                String(format: "%.2f", tx.contributedAmount),
                csvEscape(tx.notes),
                String(tx.createdAt)
            ].joined(separator: ",")
            lines.append(row)
        }
        return lines.joined(separator: "\n")
    }

    static func csvEscape(_ field: String) -> String {
        let needsQuotes = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")
        if needsQuotes {
            let escaped = field.replacingOccurrences(of: "\"", with: "\"\"")
            return "\"\(escaped)\""
        }
        return field
    }

    // MARK: - Auto-Detect & Parse Import

    static func parseImportText(
        _ rawText: String,
        calendar: Calendar = .current
    ) throws -> ParsedImportPreview {
        let cleaned = rawText
            .replacingOccurrences(of: "\u{FEFF}", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else {
            throw ExportImportError.emptyInput
        }

        if cleaned.hasPrefix("{") {
            return try parseJSON(cleaned, calendar: calendar)
        } else {
            return try parseCSV(cleaned, calendar: calendar)
        }
    }

    private static func parseJSON(
        _ text: String,
        calendar: Calendar
    ) throws -> ParsedImportPreview {
        guard let data = text.data(using: .utf8) else {
            throw ExportImportError.invalidJSON("Could not read UTF-8 text.")
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ExportImportError.invalidJSON("Root object must be a JSON dictionary.")
        }

        let rawProfiles = root["profiles"] as? [[String: Any]] ?? []
        let rawTransactions = root["transactions"] as? [[String: Any]] ?? []
        let rawInvoices = root["invoices"] as? [[String: Any]] ?? []

        if rawProfiles.isEmpty && rawTransactions.isEmpty && rawInvoices.isEmpty {
            let appName = root["app"] as? String
            if appName == nil {
                throw ExportImportError.invalidJSON("Missing FamilyBooks backup fields (profiles, transactions, or invoices).")
            }
        }

        let now = FormatUtils.nowEpochMs

        let profiles: [ExportProfileDTO] = rawProfiles.compactMap { item in
            guard let name = (item["name"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !name.isEmpty else { return nil }
            let id = (item["id"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? UUID().uuidString
            let createdAt = int64Value(item["createdAt"]) ?? now
            let updatedAt = int64Value(item["updatedAt"]) ?? createdAt
            return ExportProfileDTO(
                id: id,
                name: name,
                role: (item["role"] as? String) ?? "Other",
                avatarColorHex: (item["avatarColorHex"] as? String) ?? "#0A2540",
                isPrimary: (item["isPrimary"] as? Bool) ?? false,
                notes: (item["notes"] as? String) ?? "",
                createdAt: createdAt,
                updatedAt: updatedAt
            )
        }

        let transactions: [ExportTransactionDTO] = rawTransactions.compactMap { item in
            let amount = doubleValue(item["amount"]) ?? 0.0
            guard amount > 0 else { return nil }
            let id = (item["id"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? UUID().uuidString
            let formatted = (item["formattedDate"] as? String) ?? ""
            let dateMs = int64Value(item["date"]) ?? FormatUtils.parseIsoTimestamp(formatted) ?? now
            let createdAt = int64Value(item["createdAt"]) ?? dateMs
            let updatedAt = int64Value(item["updatedAt"]) ?? createdAt
            let rawType = ((item["type"] as? String) ?? LedgerTransaction.typeExpense).uppercased()
            let type = rawType == LedgerTransaction.typeIncome ? LedgerTransaction.typeIncome : LedgerTransaction.typeExpense
            let contribAmount = max(0.0, min(amount, doubleValue(item["contributedAmount"]) ?? 0.0))

            return ExportTransactionDTO(
                id: id,
                type: type,
                amount: amount,
                date: dateMs,
                formattedDate: formatted.isEmpty ? FormatUtils.isoTimestamp(dateMs) : formatted,
                category: (item["category"] as? String) ?? "Other",
                paymentMethod: (item["paymentMethod"] as? String) ?? "Card",
                paidBy: (item["paidBy"] as? String) ?? "Family",
                forWhom: (item["forWhom"] as? String) ?? "",
                fromWhom: (item["fromWhom"] as? String) ?? "",
                contributedBy: (item["contributedBy"] as? String) ?? "",
                contributedAmount: contribAmount,
                notes: (item["notes"] as? String) ?? "",
                createdAt: createdAt,
                updatedAt: updatedAt,
                isDeleted: (item["isDeleted"] as? Bool) ?? false
            )
        }

        let liabilities: [ExportLiabilityDTO] = rawInvoices.compactMap { item in
            let amount = doubleValue(item["amount"]) ?? 0.0
            guard amount > 0 else { return nil }
            let id = (item["id"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? UUID().uuidString
            let dateMs = int64Value(item["date"]) ?? now
            let createdAt = int64Value(item["createdAt"]) ?? dateMs
            let updatedAt = int64Value(item["updatedAt"]) ?? createdAt
            let paidAmount = max(0.0, doubleValue(item["paidAmount"]) ?? 0.0)
            let status = ((item["status"] as? String) ?? FamilyLiability.statusUnpaid).uppercased()

            return ExportLiabilityDTO(
                id: id,
                title: (item["title"] as? String) ?? "Liability",
                liabilityType: (item["liabilityType"] as? String) ?? FamilyLiability.typeFamilyIOU,
                linkedProfile: (item["linkedProfile"] as? String) ?? "",
                fromMember: (item["fromMember"] as? String) ?? "",
                toMember: (item["toMember"] as? String) ?? "",
                amount: amount,
                paidAmount: paidAmount,
                date: dateMs,
                dueDate: int64Value(item["dueDate"]),
                notes: (item["notes"] as? String) ?? "",
                status: status,
                settledAt: int64Value(item["settledAt"]),
                createdAt: createdAt,
                updatedAt: updatedAt,
                isDeleted: (item["isDeleted"] as? Bool) ?? false
            )
        }

        return buildPreview(
            format: .json,
            profiles: profiles,
            transactions: transactions,
            liabilities: liabilities,
            calendar: calendar
        )
    }

    private static func parseCSV(
        _ text: String,
        calendar: Calendar
    ) throws -> ParsedImportPreview {
        let rows = parseRFC4180Rows(text)
        guard rows.count >= 2 else {
            throw ExportImportError.invalidCSV("CSV must contain a header row and at least one transaction row.")
        }

        let header = rows[0].map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
        func index(of names: [String]) -> Int? {
            for name in names {
                if let idx = header.firstIndex(of: name.lowercased()) {
                    return idx
                }
            }
            return nil
        }

        guard let dateIdx = index(of: ["date"]),
              let typeIdx = index(of: ["type"]),
              let amountIdx = index(of: ["amount"]) else {
            throw ExportImportError.invalidCSV("Required CSV columns (Date, Type, Amount) were not found.")
        }

        let idIdx = index(of: ["id"])
        let catIdx = index(of: ["category"])
        let methodIdx = index(of: ["paymentmethod", "payment_method", "payment method"])
        let paidByIdx = index(of: ["paidby", "paid_by", "paid by"])
        let forWhomIdx = index(of: ["forwhom", "for_whom", "for whom"])
        let fromWhomIdx = index(of: ["fromwhom", "from_whom", "from whom"])
        let contribByIdx = index(of: ["contributedby", "contributed_by", "contributed by"])
        let contribAmtIdx = index(of: ["contributedamount", "contributed_amount", "contributed amount"])
        let notesIdx = index(of: ["notes"])
        let createdAtIdx = index(of: ["createdat", "created_at", "created at"])

        let now = FormatUtils.nowEpochMs
        var transactions: [ExportTransactionDTO] = []

        for rowIndex in 1..<rows.count {
            let cols = rows[rowIndex]
            if cols.allSatisfy({ $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) {
                continue
            }

            func value(at idx: Int?) -> String {
                guard let idx, idx < cols.count else { return "" }
                return cols[idx].trimmingCharacters(in: .whitespacesAndNewlines)
            }

            guard let amount = Double(value(at: amountIdx)), amount > 0 else {
                continue
            }
            let rawDate = value(at: dateIdx)
            let dateMs = FormatUtils.parseIsoTimestamp(rawDate) ?? now
            let rawType = value(at: typeIdx).uppercased()
            let type = rawType == LedgerTransaction.typeIncome ? LedgerTransaction.typeIncome : LedgerTransaction.typeExpense
            let rawId = value(at: idIdx)
            let id = rawId.isEmpty ? UUID().uuidString : rawId
            let contribAmount = max(0.0, min(amount, Double(value(at: contribAmtIdx)) ?? 0.0))
            let createdAt = Int64(value(at: createdAtIdx)) ?? dateMs

            transactions.append(
                ExportTransactionDTO(
                    id: id,
                    type: type,
                    amount: amount,
                    date: dateMs,
                    formattedDate: rawDate.isEmpty ? FormatUtils.isoTimestamp(dateMs) : rawDate,
                    category: value(at: catIdx).isEmpty ? "Other" : value(at: catIdx),
                    paymentMethod: value(at: methodIdx).isEmpty ? "Card" : value(at: methodIdx),
                    paidBy: value(at: paidByIdx).isEmpty ? "Family" : value(at: paidByIdx),
                    forWhom: value(at: forWhomIdx),
                    fromWhom: value(at: fromWhomIdx),
                    contributedBy: value(at: contribByIdx),
                    contributedAmount: contribAmount,
                    notes: value(at: notesIdx),
                    createdAt: createdAt,
                    updatedAt: createdAt,
                    isDeleted: false
                )
            )
        }

        if transactions.isEmpty {
            throw ExportImportError.invalidCSV("No valid transaction rows could be parsed.")
        }

        return buildPreview(
            format: .csv,
            profiles: [],
            transactions: transactions,
            liabilities: [],
            calendar: calendar
        )
    }

    /// RFC-4180 compliant CSV parser supporting quoted fields, escaped quotes (""), and embedded newlines.
    static func parseRFC4180Rows(_ text: String) -> [[String]] {
        var rows: [[String]] = []
        var currentRow: [String] = []
        var currentField = ""
        var inQuotes = false

        let chars = Array(text)
        var i = 0
        while i < chars.count {
            let ch = chars[i]
            if inQuotes {
                if ch == "\"" {
                    if i + 1 < chars.count && chars[i + 1] == "\"" {
                        currentField.append("\"")
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                if ch == "\"" {
                    inQuotes = true
                } else if ch == "," {
                    currentRow.append(currentField)
                    currentField = ""
                } else if ch == "\r" {
                    if i + 1 < chars.count && chars[i + 1] == "\n" {
                        i += 1
                    }
                    currentRow.append(currentField)
                    rows.append(currentRow)
                    currentRow = []
                    currentField = ""
                } else if ch == "\n" {
                    currentRow.append(currentField)
                    rows.append(currentRow)
                    currentRow = []
                    currentField = ""
                } else {
                    currentField.append(ch)
                }
            }
            i += 1
        }

        if !currentField.isEmpty || !currentRow.isEmpty {
            currentRow.append(currentField)
            rows.append(currentRow)
        }

        return rows
    }

    static func filterPreview(
        _ preview: ParsedImportPreview,
        scope: ExportScopeFilter?,
        calendar: Calendar = .current
    ) -> (profiles: [ExportProfileDTO], transactions: [ExportTransactionDTO], liabilities: [ExportLiabilityDTO]) {
        guard let scope else {
            return (preview.profiles, preview.transactions, preview.liabilities)
        }
        let filteredTx = preview.transactions.filter { scope.matches(epochMs: $0.date, calendar: calendar) }
        let filteredLiab = preview.liabilities.filter { scope.matches(epochMs: $0.date, calendar: calendar) }
        return (preview.profiles, filteredTx, filteredLiab)
    }

    private static func buildPreview(
        format: ExportFormat,
        profiles: [ExportProfileDTO],
        transactions: [ExportTransactionDTO],
        liabilities: [ExportLiabilityDTO],
        calendar: Calendar
    ) -> ParsedImportPreview {
        var monthsByYear: [Int: Set<Int>] = [:]
        for tx in transactions where !tx.isDeleted {
            let ym = FormatUtils.yearAndZeroBasedMonth(fromEpochMs: tx.date, calendar: calendar)
            monthsByYear[ym.year, default: []].insert(ym.zeroBasedMonth)
        }
        for liab in liabilities where !liab.isDeleted {
            let ym = FormatUtils.yearAndZeroBasedMonth(fromEpochMs: liab.date, calendar: calendar)
            monthsByYear[ym.year, default: []].insert(ym.zeroBasedMonth)
        }
        let years = monthsByYear.keys.sorted(by: >)
        return ParsedImportPreview(
            format: format,
            profiles: profiles,
            transactions: transactions,
            liabilities: liabilities,
            detectedYears: years,
            detectedMonthsByYear: monthsByYear
        )
    }

    private static func int64Value(_ value: Any?) -> Int64? {
        if let v = value as? Int64 { return v }
        if let v = value as? Int { return Int64(v) }
        if let v = value as? Double { return Int64(v) }
        if let s = value as? String { return Int64(s) }
        return nil
    }

    private static func doubleValue(_ value: Any?) -> Double? {
        if let v = value as? Double { return v }
        if let v = value as? Int { return Double(v) }
        if let v = value as? Int64 { return Double(v) }
        if let s = value as? String { return Double(s) }
        return nil
    }
}
