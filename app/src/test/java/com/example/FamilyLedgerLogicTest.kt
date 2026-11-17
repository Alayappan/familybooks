package com.example

import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LiabilityType
import com.example.data.model.TransactionEntity
import com.example.data.repository.FamilyBooksRepository
import com.example.data.util.LedgerExportImportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FamilyLedgerLogicTest {

    @Test
    fun `empty inputs produce zero profiles and zero liabilities`() {
        val summaries = FamilyBooksRepository.computeProfileSummaries(
            transactions = emptyList(),
            invoices = emptyList(),
            members = emptyList()
        )
        assertTrue(summaries.isEmpty())
    }

    @Test
    fun `profile summary calculates attribution shares and third party debts accurately`() {
        val mom = FamilyMemberEntity(id = "m1", name = "Mom", role = "Parent", isPrimary = true)
        val alex = FamilyMemberEntity(id = "m2", name = "Alex", role = "Child")

        val txs = listOf(
            // Mom earns salary from Employer
            TransactionEntity(
                id = "t1",
                type = "INCOME",
                amount = 4000.0,
                category = "Salary",
                paymentMethod = "UPI / Bank Transfer",
                paidBy = "Mom",
                fromWhom = "Acme Corp"
            ),
            // Mom pays $300 school tuition FOR Alex, where Alex chipped in $50
            TransactionEntity(
                id = "t2",
                type = "EXPENSE",
                amount = 300.0,
                category = "Education",
                paymentMethod = "Card",
                paidBy = "Mom",
                forWhom = "Alex",
                contributedBy = "Alex",
                contributedAmount = 50.0
            )
        )

        val invoices = listOf(
            // Family IOU: Alex owes Mom $100 ($25 already paid)
            FamilyInvoiceEntity(
                id = "i1",
                title = "Phone bill share",
                liabilityType = LiabilityType.FAMILY_IOU,
                fromMember = "Alex",
                toMember = "Mom",
                amount = 100.0,
                paidAmount = 25.0,
                status = "PARTIAL"
            ),
            // 3rd-Party Payable: Mom owes City Clinic $500 ($100 paid)
            FamilyInvoiceEntity(
                id = "i2",
                title = "Dental bill",
                liabilityType = LiabilityType.THIRD_PARTY_PAYABLE,
                linkedProfile = "Mom",
                fromMember = "Mom",
                toMember = "City Clinic",
                amount = 500.0,
                paidAmount = 100.0,
                status = "PARTIAL"
            ),
            // 3rd-Party Receivable: Friend Sam owes Alex $60
            FamilyInvoiceEntity(
                id = "i3",
                title = "Concert ticket",
                liabilityType = LiabilityType.THIRD_PARTY_RECEIVABLE,
                linkedProfile = "Alex",
                fromMember = "Sam",
                toMember = "Alex",
                amount = 60.0,
                status = "UNPAID"
            )
        )

        val summaries = FamilyBooksRepository.computeProfileSummaries(txs, invoices, listOf(mom, alex))
        val momSummary = summaries.first { it.profileName == "Mom" }
        val alexSummary = summaries.first { it.profileName == "Alex" }

        assertEquals(4000.0, momSummary.totalIncome, 0.01)
        assertEquals(300.0, momSummary.totalExpensePaid, 0.01)
        assertEquals(50.0, momSummary.contributionsReceived, 0.01)
        assertEquals(75.0, momSummary.familyOwedToThem, 0.01)
        assertEquals(400.0, momSummary.thirdPartyTheyOwe, 0.01)
        assertEquals(-325.0, momSummary.netLiabilityStanding, 0.01)

        assertEquals(300.0, alexSummary.totalExpenseForThem, 0.01)
        assertEquals(50.0, alexSummary.contributionsProvided, 0.01)
        assertEquals(75.0, alexSummary.familyTheyOwe, 0.01)
        assertEquals(60.0, alexSummary.thirdPartyOwedToThem, 0.01)
        assertEquals(-15.0, alexSummary.netLiabilityStanding, 0.01)
    }

    @Test
    fun `json export and import preserves attribution shares and third party liabilities`() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        val profile = FamilyMemberEntity(id = "p1", name = "Mom", role = "Parent", isPrimary = true)
        val tx = TransactionEntity(
            id = "tx1",
            type = "EXPENSE",
            amount = 200.0,
            date = cal.timeInMillis,
            category = "Groceries",
            paymentMethod = "Cash",
            paidBy = "Mom",
            forWhom = "Shared Family",
            contributedBy = "Dad",
            contributedAmount = 80.0
        )
        val inv = FamilyInvoiceEntity(
            id = "inv1",
            title = "Car Loan",
            liabilityType = LiabilityType.THIRD_PARTY_PAYABLE,
            linkedProfile = "Mom",
            fromMember = "Mom",
            toMember = "Credit Union",
            amount = 1200.0,
            paidAmount = 300.0,
            date = cal.timeInMillis,
            status = "PARTIAL"
        )

        val json = LedgerExportImportManager.exportToJson(
            transactions = listOf(tx),
            invoices = listOf(inv),
            profiles = listOf(profile),
            year = year,
            months = setOf(month),
            currentMember = "Mom"
        )

        val parsed = LedgerExportImportManager.parseImportContent(json).getOrThrow()
        assertEquals(1, parsed.allProfiles.size)
        assertEquals(1, parsed.allTransactions.size)
        assertEquals(1, parsed.allInvoices.size)

        val importedTx = parsed.allTransactions.first()
        assertEquals("Shared Family", importedTx.forWhom)
        assertEquals("Dad", importedTx.contributedBy)
        assertEquals(80.0, importedTx.contributedAmount, 0.01)

        val importedInv = parsed.allInvoices.first()
        assertEquals(LiabilityType.THIRD_PARTY_PAYABLE, importedInv.liabilityType)
        assertEquals("Mom", importedInv.linkedProfile)
        assertEquals(300.0, importedInv.paidAmount, 0.01)
    }
}
