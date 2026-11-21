import Foundation

struct ProfileFinancialSummary: Identifiable, Sendable {
    var id: String { name.lowercased() }
    let memberID: String?
    let name: String
    let role: String
    let avatarColorHex: String
    let isPrimary: Bool
    let isSavedProfile: Bool
    let notes: String

    let totalIncome: Double
    let totalPaidOut: Double
    let totalSpentFor: Double
    let totalSharesContributed: Double
    let totalSharesReceived: Double
    let familyTheyOwe: Double
    let familyOwedToThem: Double
    let thirdPartyTheyOwe: Double
    let thirdPartyOwedToThem: Double

    var netLiabilityStanding: Double {
        (familyOwedToThem + thirdPartyOwedToThem) - (familyTheyOwe + thirdPartyTheyOwe)
    }

    var netCashFlow: Double {
        totalIncome - totalPaidOut
    }
}

struct DashboardCategorySummary: Identifiable, Sendable {
    var id: String { category }
    let category: String
    let totalAmount: Double
    let percentage: Double
    let topPayer: String
}

struct CategoryMoMComparison: Identifiable, Sendable {
    var id: String { category }
    let category: String
    let thisMonthAmount: Double
    let lastMonthAmount: Double

    var deltaAmount: Double {
        thisMonthAmount - lastMonthAmount
    }

    var isSaving: Bool {
        deltaAmount < -0.005
    }
}

struct PaymentMethodBreakdown: Identifiable, Sendable {
    var id: String { method }
    let method: String
    let amount: Double
    let percentage: Double
    let count: Int
}

struct MonthOnMonthAnalytics: Sendable {
    let scopeName: String? // nil means "All Family"
    let currentMonthLabel: String
    let previousMonthLabel: String

    let expensesThisMonth: Double
    let expensesLastMonth: Double
    let incomeThisMonth: Double
    let incomeLastMonth: Double

    var netSavingsThisMonth: Double { incomeThisMonth - expensesThisMonth }
    var netSavingsLastMonth: Double { incomeLastMonth - expensesLastMonth }

    var expensePercentChange: Double? {
        FormatUtils.percentageChange(current: expensesThisMonth, previous: expensesLastMonth)
    }

    var incomePercentChange: Double? {
        FormatUtils.percentageChange(current: incomeThisMonth, previous: incomeLastMonth)
    }

    // Attribution, Shares & Liabilities Overview
    let spentForThisMonth: Double
    let sharesContributedThisMonth: Double
    let thirdPartyDebtOwed: Double
    let thirdPartyReceivables: Double
    let familyIOUsOwed: Double
    let familyIOUsReceivable: Double

    // Cash spent comparison
    let cashSpentThisMonth: Double
    let cashSpentLastMonth: Double

    let categoryComparisons: [CategoryMoMComparison]
    let paymentMethods: [PaymentMethodBreakdown]
}

// MARK: - Cross-Platform Export / Import DTOs (JSON v2.0)

nonisolated enum ExportScopeMode: String, CaseIterable, Identifiable, Sendable {
    case fullYear = "Full Year"
    case particularMonth = "Particular Month"
    case multipleMonths = "Months of Year"

    var id: String { rawValue }
}

nonisolated enum ExportFormat: String, CaseIterable, Identifiable, Sendable {
    case json = "JSON"
    case csv = "CSV"

    var id: String { rawValue }
}

nonisolated struct ExportScopeFilter: Sendable {
    let mode: ExportScopeMode
    let year: Int
    let selectedZeroBasedMonths: Set<Int> // 0...11; empty when fullYear

    var sortedMonths: [Int] {
        switch mode {
        case .fullYear:
            return []
        case .particularMonth, .multipleMonths:
            return selectedZeroBasedMonths.sorted()
        }
    }

    var descriptionText: String {
        switch mode {
        case .fullYear:
            return "Full Year \(year)"
        case .particularMonth:
            if let m = sortedMonths.first, m >= 0, m < 12 {
                return "\(FormatUtils.monthNames[m]) \(year)"
            }
            return "Year \(year)"
        case .multipleMonths:
            let names = sortedMonths.compactMap { m in
                (0..<12).contains(m) ? FormatUtils.shortMonthNames[m] : nil
            }
            if names.isEmpty {
                return "Full Year \(year)"
            }
            return "\(names.joined(separator: ", ")) \(year)"
        }
    }

    func matches(epochMs: Int64, calendar: Calendar = .current) -> Bool {
        let ym = FormatUtils.yearAndZeroBasedMonth(fromEpochMs: epochMs, calendar: calendar)
        guard ym.year == year else { return false }
        switch mode {
        case .fullYear:
            return true
        case .particularMonth, .multipleMonths:
            if selectedZeroBasedMonths.isEmpty {
                return true
            }
            return selectedZeroBasedMonths.contains(ym.zeroBasedMonth)
        }
    }
}

nonisolated struct ExportSummaryDTO: Codable, Sendable {
    let transactionCount: Int
    let invoiceCount: Int
    let profileCount: Int
    let totalExpense: Double
    let totalIncome: Double
    let netBalance: Double
}

nonisolated struct ExportProfileDTO: Codable, Sendable {
    let id: String
    let name: String
    let role: String
    let avatarColorHex: String
    let isPrimary: Bool
    let notes: String
    let createdAt: Int64
    let updatedAt: Int64
}

nonisolated struct ExportTransactionDTO: Codable, Sendable {
    let id: String
    let type: String
    let amount: Double
    let date: Int64
    let formattedDate: String
    let category: String
    let paymentMethod: String
    let paidBy: String
    let forWhom: String
    let fromWhom: String
    let contributedBy: String
    let contributedAmount: Double
    let notes: String
    let createdAt: Int64
    let updatedAt: Int64
    let isDeleted: Bool
}

nonisolated struct ExportLiabilityDTO: Codable, Sendable {
    let id: String
    let title: String
    let liabilityType: String
    let linkedProfile: String
    let fromMember: String
    let toMember: String
    let amount: Double
    let paidAmount: Double
    let date: Int64
    let dueDate: Int64?
    let notes: String
    let status: String
    let settledAt: Int64?
    let createdAt: Int64
    let updatedAt: Int64
    let isDeleted: Bool
}

nonisolated struct ExportPayloadV2: Codable, Sendable {
    let app: String
    let exportVersion: String
    let exportedAt: Int64
    let exportedBy: String
    let year: Int
    let months: [Int]
    let scopeDescription: String
    let summary: ExportSummaryDTO
    let profiles: [ExportProfileDTO]
    let transactions: [ExportTransactionDTO]
    let invoices: [ExportLiabilityDTO]
}

nonisolated struct ParsedImportPreview: Sendable {
    let format: ExportFormat
    let profiles: [ExportProfileDTO]
    let transactions: [ExportTransactionDTO]
    let liabilities: [ExportLiabilityDTO]
    let detectedYears: [Int]
    let detectedMonthsByYear: [Int: Set<Int>] // year -> 0-based months
}

nonisolated struct ImportMergeResult: Sendable {
    let profilesAdded: Int
    let profilesSkipped: Int
    let transactionsAdded: Int
    let transactionsUpdated: Int
    let liabilitiesAdded: Int
    let liabilitiesUpdated: Int
}
