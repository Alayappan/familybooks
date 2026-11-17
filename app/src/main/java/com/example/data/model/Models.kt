package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object LiabilityType {
    const val FAMILY_IOU = "FAMILY_IOU"
    const val THIRD_PARTY_PAYABLE = "THIRD_PARTY_PAYABLE"       // Family member owes external third party
    const val THIRD_PARTY_RECEIVABLE = "THIRD_PARTY_RECEIVABLE" // External third party owes family member
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["date"]),
        Index(value = ["paidBy"]),
        Index(value = ["forWhom"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val paymentMethod: String, // "Card", "Cash", "UPI / Bank Transfer", "Wallet", "Other"
    val paidBy: String, // Expense: payer profile | Income: receiver profile
    val forWhom: String = "", // Expense: beneficiary (member profile / "Shared Family" / person)
    val fromWhom: String = "", // Income: source (Employer / member / 3rd party) | Expense: optional merchant
    val contributedBy: String = "", // Other member or 3rd party who covered a share
    val contributedAmount: Double = 0.0, // Amount of share contributed by contributedBy
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

val TransactionEntity.netOutOfPocketAmount: Double
    get() = (amount - contributedAmount).coerceAtLeast(0.0)

@Entity(
    tableName = "family_invoices",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["fromMember"]),
        Index(value = ["toMember"]),
        Index(value = ["linkedProfile"]),
        Index(value = ["liabilityType"]),
        Index(value = ["status"])
    ]
)
data class FamilyInvoiceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val liabilityType: String = LiabilityType.FAMILY_IOU,
    val linkedProfile: String = "", // Primary family profile tied to a third-party debt/receivable
    val fromMember: String, // Debtor: person/party who owes money
    val toMember: String, // Creditor: person/party who is owed money
    val amount: Double,
    val paidAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String = "",
    val status: String = "UNPAID", // "UNPAID", "PARTIAL", "SETTLED"
    val settledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

val FamilyInvoiceEntity.remainingAmount: Double
    get() = if (status == "SETTLED") 0.0 else (amount - paidAmount).coerceAtLeast(0.0)

val FamilyInvoiceEntity.isThirdPartyLiability: Boolean
    get() = liabilityType == LiabilityType.THIRD_PARTY_PAYABLE || liabilityType == LiabilityType.THIRD_PARTY_RECEIVABLE

@Entity(
    tableName = "family_members",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["name"])
    ]
)
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String = "Member", // e.g. "Self", "Spouse", "Parent", "Child", "Other"
    val avatarColorHex: String = "#0A2540",
    val isPrimary: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class ProfileFinancialSummary(
    val profileId: String = "",
    val profileName: String,
    val role: String = "Member",
    val avatarColorHex: String = "#0A2540",
    val isPrimary: Boolean = false,
    val isExplicitProfile: Boolean = true,
    val totalIncome: Double = 0.0,
    val totalExpensePaid: Double = 0.0,
    val totalExpenseForThem: Double = 0.0,
    val contributionsProvided: Double = 0.0,
    val contributionsReceived: Double = 0.0,
    val familyOwedToThem: Double = 0.0,
    val familyTheyOwe: Double = 0.0,
    val thirdPartyOwedToThem: Double = 0.0,
    val thirdPartyTheyOwe: Double = 0.0,
    val netLiabilityStanding: Double = 0.0 // (familyOwedToThem + thirdPartyOwedToThem) - (familyTheyOwe + thirdPartyTheyOwe)
)
