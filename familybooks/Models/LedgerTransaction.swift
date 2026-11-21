import Foundation
import SwiftData

@Model
final class LedgerTransaction {
    @Attribute(.unique) var id: String
    var type: String
    var amount: Double
    var date: Int64
    var category: String
    var paymentMethod: String
    var paidBy: String
    var forWhom: String
    var fromWhom: String
    var contributedBy: String
    var contributedAmount: Double
    var notes: String
    var createdAt: Int64
    var updatedAt: Int64
    var isDeleted: Bool

    init(
        id: String = UUID().uuidString,
        type: String = LedgerTransaction.typeExpense,
        amount: Double,
        date: Int64 = FormatUtils.nowEpochMs,
        category: String = "Other",
        paymentMethod: String = "Card",
        paidBy: String = "Family",
        forWhom: String = "",
        fromWhom: String = "",
        contributedBy: String = "",
        contributedAmount: Double = 0.0,
        notes: String = "",
        createdAt: Int64 = FormatUtils.nowEpochMs,
        updatedAt: Int64 = FormatUtils.nowEpochMs,
        isDeleted: Bool = false
    ) {
        self.id = id
        self.type = type
        self.amount = amount
        self.date = date
        self.category = category
        self.paymentMethod = paymentMethod
        self.paidBy = paidBy
        self.forWhom = forWhom
        self.fromWhom = fromWhom
        self.contributedBy = contributedBy
        self.contributedAmount = max(0.0, min(amount, contributedAmount))
        self.notes = notes
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.isDeleted = isDeleted
    }

    var netOutOfPocketAmount: Double {
        max(0.0, amount - contributedAmount)
    }

    var isExpense: Bool {
        type.uppercased() == LedgerTransaction.typeExpense
    }

    var isIncome: Bool {
        type.uppercased() == LedgerTransaction.typeIncome
    }

    static let typeExpense = "EXPENSE"
    static let typeIncome = "INCOME"

    static let expenseCategories: [String] = [
        "Groceries",
        "Utilities",
        "Rent",
        "Dining",
        "Education",
        "Healthcare",
        "Transport",
        "Entertainment",
        "Shopping",
        "Other"
    ]

    static let incomeCategories: [String] = [
        "Salary",
        "Bonus",
        "Investment",
        "Freelance",
        "Gift",
        "Refund",
        "Other"
    ]

    static let paymentMethods: [String] = [
        "Card",
        "Cash",
        "UPI / Bank Transfer",
        "Wallet"
    ]

    static let incomeSourcePresets: [String] = [
        "Employer",
        "Freelance / Client",
        "Family Member",
        "Refund",
        "3rd Party"
    ]
}
