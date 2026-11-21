import Foundation
import SwiftData

@Model
final class FamilyLiability {
    @Attribute(.unique) var id: String
    var title: String
    var liabilityType: String
    var linkedProfile: String
    var fromMember: String
    var toMember: String
    var amount: Double
    var paidAmount: Double
    var date: Int64
    var dueDate: Int64?
    var notes: String
    var status: String
    var settledAt: Int64?
    var createdAt: Int64
    var updatedAt: Int64
    var isDeleted: Bool

    init(
        id: String = UUID().uuidString,
        title: String,
        liabilityType: String = FamilyLiability.typeFamilyIOU,
        linkedProfile: String = "",
        fromMember: String,
        toMember: String,
        amount: Double,
        paidAmount: Double = 0.0,
        date: Int64 = FormatUtils.nowEpochMs,
        dueDate: Int64? = nil,
        notes: String = "",
        status: String = FamilyLiability.statusUnpaid,
        settledAt: Int64? = nil,
        createdAt: Int64 = FormatUtils.nowEpochMs,
        updatedAt: Int64 = FormatUtils.nowEpochMs,
        isDeleted: Bool = false
    ) {
        self.id = id
        self.title = title
        self.liabilityType = liabilityType
        self.linkedProfile = linkedProfile
        self.fromMember = fromMember
        self.toMember = toMember
        self.amount = amount
        self.paidAmount = paidAmount
        self.date = date
        self.dueDate = dueDate
        self.notes = notes
        self.status = status
        self.settledAt = settledAt
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.isDeleted = isDeleted
    }

    var remainingAmount: Double {
        status == FamilyLiability.statusSettled ? 0.0 : max(0.0, amount - paidAmount)
    }

    var isThirdPartyLiability: Bool {
        liabilityType == FamilyLiability.typeThirdPartyPayable
            || liabilityType == FamilyLiability.typeThirdPartyReceivable
    }

    var isSettled: Bool {
        status == FamilyLiability.statusSettled
    }

    var isPartial: Bool {
        status == FamilyLiability.statusPartial
    }

    var repaymentProgress: Double {
        guard amount > 0 else { return isSettled ? 1.0 : 0.0 }
        if isSettled { return 1.0 }
        return min(1.0, max(0.0, paidAmount / amount))
    }

    var typeDisplayLabel: String {
        switch liabilityType {
        case FamilyLiability.typeThirdPartyPayable:
            return "3rd-Party Debt"
        case FamilyLiability.typeThirdPartyReceivable:
            return "3rd-Party Owed"
        default:
            return "Family IOU"
        }
    }

    static let typeFamilyIOU = "FAMILY_IOU"
    static let typeThirdPartyPayable = "THIRD_PARTY_PAYABLE"
    static let typeThirdPartyReceivable = "THIRD_PARTY_RECEIVABLE"

    static let statusUnpaid = "UNPAID"
    static let statusPartial = "PARTIAL"
    static let statusSettled = "SETTLED"

    static let thirdPartyCreditorPresets: [String] = [
        "Bank / Loan",
        "Credit Card",
        "Landlord",
        "Friend",
        "Clinic / Hospital",
        "Vendor"
    ]
}
