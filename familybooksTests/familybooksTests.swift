import Foundation
import SwiftData
import Testing
@testable import familybooks

@MainActor
struct FamilyBooksLedgerTests {

    @MainActor
    private final class TestHarness {
        let container: ModelContainer
        let store: LedgerStore
        let defaults: UserDefaults
        let suiteName: String

        init() throws {
            let schema = Schema([
                FamilyMember.self,
                LedgerTransaction.self,
                FamilyLiability.self
            ])
            let uniqueID = UUID().uuidString
            let config = ModelConfiguration(
                "TestStore-\(uniqueID)",
                schema: schema,
                isStoredInMemoryOnly: true,
                cloudKitDatabase: .none
            )
            let container = try ModelContainer(for: schema, configurations: [config])
            let suiteName = "FamilyBooksTests.\(uniqueID)"
            let defaults = UserDefaults(suiteName: suiteName) ?? .standard
            defaults.removePersistentDomain(forName: suiteName)
            self.container = container
            self.defaults = defaults
            self.suiteName = suiteName
            self.store = LedgerStore(modelContext: container.mainContext, userDefaults: defaults)
        }

        func cleanup() {
            defaults.removePersistentDomain(forName: suiteName)
        }
    }

    private func epochMs(year: Int, month: Int, day: Int, hour: Int = 12) -> Int64 {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = day
        comps.hour = hour
        comps.minute = 0
        comps.second = 0
        let date = Calendar.current.date(from: comps) ?? Date()
        return FormatUtils.epochMs(from: date)
    }

    // MARK: - Clean First Launch & Cash-in-Hand Tests

    @Test func cleanFirstLaunchStartsCompletelyEmpty() throws {
        let harness = try TestHarness()
        defer { harness.cleanup() }
        let store = harness.store

        #expect(store.profiles.isEmpty)
        #expect(store.transactions.isEmpty)
        #expect(store.liabilities.isEmpty)
        #expect(store.activeProfileName.isEmpty)
        #expect(store.cashPoolBaseline == 0.0)
        #expect(store.cashInHandBalance == 0.0)
    }

    @Test func cashInHandBalanceCalculationAndBaselineAdjustment() throws {
        let harness = try TestHarness()
        defer { harness.cleanup() }
        let store = harness.store

        store.addProfile(name: "Alex", role: "Self", avatarColorHex: "#0A2540", notes: "", setAsActive: true)

        // Refill $300 cash
        store.refillCashPool(amount: 300.0, note: "ATM Withdrawal")
        #expect(store.cashInHandBalance == 300.0)
        #expect(store.transactions.count == 1)
        #expect(store.transactions.first?.fromWhom == "ATM / Cash Wallet Refill")
        #expect(store.transactions.first?.paymentMethod == "Cash")
        #expect(store.transactions.first?.paidBy == "Alex")

        // Spend $75.50 in Cash -> $224.50 left
        store.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 75.50,
            category: "Groceries",
            paymentMethod: "Cash",
            paidBy: "Alex",
            forWhom: "Shared Family",
            fromWhom: "",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "Farmers market"
        )
        #expect(store.cashInHandBalance == 224.50)

        // Spend $120.00 on Card -> Cash in hand unaffected
        store.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 120.0,
            category: "Utilities",
            paymentMethod: "Card",
            paidBy: "Alex",
            forWhom: "Shared Family",
            fromWhom: "",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "Electric bill"
        )
        #expect(store.cashInHandBalance == 224.50)

        // Adjust baseline so actual cash counted is $250.00 without adding a transaction
        let txCountBefore = store.transactions.count
        store.setCashPoolBaseline(newTotal: 250.0)
        #expect(store.cashInHandBalance == 250.0)
        #expect(store.cashPoolBaseline == 25.50)
        #expect(store.transactions.count == txCountBefore)
    }

    // MARK: - Per-Profile Aggregation Tests

    @Test func perProfileFinancialAggregationAndNetLiabilityStanding() throws {
        let harness = try TestHarness()
        defer { harness.cleanup() }
        let store = harness.store

        store.addProfile(name: "Alex", role: "Self", avatarColorHex: "#0A2540", notes: "", setAsActive: true)
        store.addProfile(name: "Sam", role: "Spouse", avatarColorHex: "#00A88D", notes: "", setAsActive: false)

        // Alex receives $4000 salary
        store.addTransaction(
            type: LedgerTransaction.typeIncome,
            amount: 4000.0,
            category: "Salary",
            paymentMethod: "UPI / Bank Transfer",
            paidBy: "Alex",
            forWhom: "",
            fromWhom: "Employer",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "Monthly pay"
        )

        // Alex pays $200 groceries for Sam, with Sam contributing $50 share
        store.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 200.0,
            category: "Groceries",
            paymentMethod: "Card",
            paidBy: "Alex",
            forWhom: "Sam",
            fromWhom: "",
            contributedBy: "Sam",
            contributedAmount: 50.0,
            notes: "Weekly market"
        )

        // Sam pays $100 dining for Alex
        store.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 100.0,
            category: "Dining",
            paymentMethod: "Card",
            paidBy: "Sam",
            forWhom: "Alex",
            fromWhom: "",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "Birthday lunch"
        )

        // Family IOU: Sam owes Alex $80
        store.addLiability(
            title: "Flight Tickets",
            liabilityType: FamilyLiability.typeFamilyIOU,
            linkedProfile: "",
            fromMember: "Sam",
            toMember: "Alex",
            amount: 80.0
        )

        // 3rd-party Payable: Alex owes Bank $300
        store.addLiability(
            title: "Credit Card Balance",
            liabilityType: FamilyLiability.typeThirdPartyPayable,
            linkedProfile: "Alex",
            fromMember: "Alex",
            toMember: "Bank / Loan",
            amount: 300.0
        )

        // 3rd-party Receivable: Client owes Alex $150
        store.addLiability(
            title: "Consulting Refund",
            liabilityType: FamilyLiability.typeThirdPartyReceivable,
            linkedProfile: "Alex",
            fromMember: "Client Co",
            toMember: "Alex",
            amount: 150.0
        )

        let summaries = store.profileFinancialSummaries
        let alex = try #require(summaries.first(where: { $0.name == "Alex" }))
        let sam = try #require(summaries.first(where: { $0.name == "Sam" }))

        // Verify Alex
        #expect(alex.totalIncome == 4000.0)
        #expect(alex.totalPaidOut == 200.0)
        #expect(alex.totalSpentFor == 100.0)
        #expect(alex.totalSharesContributed == 0.0)
        #expect(alex.totalSharesReceived == 50.0)
        #expect(alex.familyOwedToThem == 80.0)
        #expect(alex.familyTheyOwe == 0.0)
        #expect(alex.thirdPartyTheyOwe == 300.0)
        #expect(alex.thirdPartyOwedToThem == 150.0)
        // Net standing = (80 + 150) - (0 + 300) = -70
        #expect(alex.netLiabilityStanding == -70.0)

        // Verify Sam
        #expect(sam.totalIncome == 0.0)
        #expect(sam.totalPaidOut == 100.0)
        #expect(sam.totalSpentFor == 200.0)
        #expect(sam.totalSharesContributed == 50.0)
        #expect(sam.familyTheyOwe == 80.0)
        #expect(sam.familyOwedToThem == 0.0)
        #expect(sam.netLiabilityStanding == -80.0)
    }

    // MARK: - Partial Liability Payment Transitions

    @Test func partialLiabilityPaymentsTransitionUnpaidToPartialToSettled() throws {
        let harness = try TestHarness()
        defer { harness.cleanup() }
        let store = harness.store

        let liab = store.addLiability(
            title: "Clinic Bill",
            liabilityType: FamilyLiability.typeThirdPartyPayable,
            linkedProfile: "Alex",
            fromMember: "Alex",
            toMember: "Clinic / Hospital",
            amount: 100.0
        )

        #expect(liab.status == FamilyLiability.statusUnpaid)
        #expect(liab.paidAmount == 0.0)
        #expect(liab.remainingAmount == 100.0)
        #expect(liab.settledAt == nil)

        // First partial payment: $40
        store.recordPartialPayment(liability: liab, paymentAmount: 40.0)
        #expect(liab.status == FamilyLiability.statusPartial)
        #expect(liab.paidAmount == 40.0)
        #expect(liab.remainingAmount == 60.0)
        #expect(liab.settledAt == nil)

        // Second partial payment: $59.997 (within $0.005 settles the liability)
        store.recordPartialPayment(liability: liab, paymentAmount: 59.997)
        #expect(liab.status == FamilyLiability.statusSettled)
        #expect(liab.paidAmount == 100.0)
        #expect(liab.remainingAmount == 0.0)
        #expect(liab.settledAt != nil)
    }

    // MARK: - Split Expense Calculation & Rounding

    @Test func splitExpenseCalculationRoundingAndIOUCreation() throws {
        let harness = try TestHarness()
        defer { harness.cleanup() }
        let store = harness.store

        store.addProfile(name: "Alex", role: "Self", avatarColorHex: "#0A2540", notes: "", setAsActive: true)
        store.addProfile(name: "Sam", role: "Spouse", avatarColorHex: "#00A88D", notes: "", setAsActive: false)
        store.addProfile(name: "Pat", role: "Sibling", avatarColorHex: "#1E5B94", notes: "", setAsActive: false)

        let result = store.saveSplitExpense(
            title: "Family Dinner",
            totalAmount: 100.0,
            paidBy: "Alex",
            participants: ["Alex", "Sam", "Pat"],
            alsoLogExpense: true,
            category: "Dining",
            paymentMethod: "Card",
            notes: "Anniversary"
        )

        let loggedTx = try #require(result.transaction)
        #expect(loggedTx.amount == 100.0)
        #expect(loggedTx.paidBy == "Alex")
        #expect(loggedTx.forWhom == "Alex, Sam, Pat")

        // Should create 2 FAMILY_IOU entries (Sam -> Alex and Pat -> Alex) rounded to $33.33 each
        #expect(result.createdLiabilities.count == 2)
        for iou in result.createdLiabilities {
            #expect(iou.liabilityType == FamilyLiability.typeFamilyIOU)
            #expect(iou.toMember == "Alex")
            #expect(iou.amount == 33.33)
            #expect(iou.status == FamilyLiability.statusUnpaid)
        }
        let debtors = Set(result.createdLiabilities.map { $0.fromMember })
        #expect(debtors == Set(["Sam", "Pat"]))

        // Verify alsoLogExpense == false skips ledger transaction
        let iouOnlyResult = store.saveSplitExpense(
            title: "Streaming Plan",
            totalAmount: 25.0,
            paidBy: "Sam",
            participants: ["Alex", "Sam"],
            alsoLogExpense: false
        )
        #expect(iouOnlyResult.transaction == nil)
        #expect(iouOnlyResult.createdLiabilities.count == 1)
        #expect(iouOnlyResult.createdLiabilities.first?.amount == 12.50)
        #expect(iouOnlyResult.createdLiabilities.first?.fromMember == "Alex")
        #expect(iouOnlyResult.createdLiabilities.first?.toMember == "Sam")
    }

    // MARK: - JSON v2.0 & CSV Export / Import Round-Trip Filtering

    @Test func jsonV2ExportImportRoundTripWithScopeFiltering() throws {
        let sourceHarness = try TestHarness()
        defer { sourceHarness.cleanup() }
        let sourceStore = sourceHarness.store

        sourceStore.addProfile(name: "Alex", role: "Self", avatarColorHex: "#0A2540", notes: "Primary", setAsActive: true)
        sourceStore.addProfile(name: "Sam", role: "Spouse", avatarColorHex: "#00A88D", notes: "", setAsActive: false)

        // Transactions in Jan 2026, Mar 2026, and Nov 2025
        sourceStore.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 120.0,
            date: epochMs(year: 2026, month: 1, day: 15),
            category: "Groceries",
            paymentMethod: "Card",
            paidBy: "Alex",
            forWhom: "Shared Family",
            fromWhom: "",
            contributedBy: "Sam",
            contributedAmount: 40.0,
            notes: "Jan groceries"
        )
        sourceStore.addTransaction(
            type: LedgerTransaction.typeIncome,
            amount: 2500.0,
            date: epochMs(year: 2026, month: 3, day: 10),
            category: "Salary",
            paymentMethod: "UPI / Bank Transfer",
            paidBy: "Alex",
            forWhom: "",
            fromWhom: "Employer",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "March salary"
        )
        sourceStore.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 90.0,
            date: epochMs(year: 2025, month: 11, day: 20),
            category: "Transport",
            paymentMethod: "Cash",
            paidBy: "Sam",
            forWhom: "Sam",
            fromWhom: "",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "2025 fuel"
        )

        sourceStore.addLiability(
            title: "March Repair",
            liabilityType: FamilyLiability.typeThirdPartyPayable,
            linkedProfile: "Alex",
            fromMember: "Alex",
            toMember: "Vendor",
            amount: 200.0,
            date: epochMs(year: 2026, month: 3, day: 12)
        )

        // 1) Full Year 2026 Export -> expects 2 transactions, 1 liability, months == []
        let fullYearScope = ExportScopeFilter(mode: .fullYear, year: 2026, selectedZeroBasedMonths: [])
        let fullYearJSON = try LedgerExportImportManager.exportJSON(
            profiles: sourceStore.profiles,
            transactions: sourceStore.transactions,
            liabilities: sourceStore.liabilities,
            scope: fullYearScope,
            exportedBy: "Alex"
        )
        let decodedFull = try JSONDecoder().decode(ExportPayloadV2.self, from: Data(fullYearJSON.utf8))
        #expect(decodedFull.app == "FamilyBooks")
        #expect(decodedFull.exportVersion == "2.0")
        #expect(decodedFull.year == 2026)
        #expect(decodedFull.months.isEmpty)
        #expect(decodedFull.summary.transactionCount == 2)
        #expect(decodedFull.summary.invoiceCount == 1)
        #expect(decodedFull.summary.profileCount == 2)

        // 2) Particular Month (March 2026 -> 0-based month 2)
        let marchScope = ExportScopeFilter(mode: .particularMonth, year: 2026, selectedZeroBasedMonths: [2])
        let marchJSON = try LedgerExportImportManager.exportJSON(
            profiles: sourceStore.profiles,
            transactions: sourceStore.transactions,
            liabilities: sourceStore.liabilities,
            scope: marchScope,
            exportedBy: "Alex"
        )
        let decodedMarch = try JSONDecoder().decode(ExportPayloadV2.self, from: Data(marchJSON.utf8))
        #expect(decodedMarch.months == [2])
        #expect(decodedMarch.transactions.count == 1)
        #expect(decodedMarch.transactions.first?.category == "Salary")
        #expect(decodedMarch.invoices.count == 1)

        // 3) Multiple Months (Jan & Mar 2026 -> 0-based [0, 2])
        let multiScope = ExportScopeFilter(mode: .multipleMonths, year: 2026, selectedZeroBasedMonths: [0, 2])
        let multiJSON = try LedgerExportImportManager.exportJSON(
            profiles: sourceStore.profiles,
            transactions: sourceStore.transactions,
            liabilities: sourceStore.liabilities,
            scope: multiScope,
            exportedBy: "Alex"
        )
        let decodedMulti = try JSONDecoder().decode(ExportPayloadV2.self, from: Data(multiJSON.utf8))
        #expect(decodedMulti.months == [0, 2])
        #expect(decodedMulti.transactions.count == 2)

        // Import into a clean destination store and verify merge & duplicate profile skipping
        let destHarness = try TestHarness()
        defer { destHarness.cleanup() }
        let destStore = destHarness.store

        // Pre-add "alex" (different case) to test case-insensitive duplicate profile skip
        destStore.addProfile(name: "alex", role: "Self", avatarColorHex: "#0A2540", notes: "", setAsActive: true)

        let preview = try LedgerExportImportManager.parseImportText(fullYearJSON)
        #expect(preview.format == .json)
        let mergeResult = destStore.mergeImport(preview: preview, scope: nil)
        #expect(mergeResult.profilesAdded == 1) // Sam added
        #expect(mergeResult.profilesSkipped == 1) // Alex skipped (case-insensitive match)
        #expect(mergeResult.transactionsAdded == 2)
        #expect(mergeResult.liabilitiesAdded == 1)
        #expect(destStore.transactions.count == 2)
        #expect(destStore.liabilities.count == 1)
    }

    @Test func csvExportImportRoundTripWithRFC4180QuotingAndScopeFilter() throws {
        let sourceHarness = try TestHarness()
        defer { sourceHarness.cleanup() }
        let sourceStore = sourceHarness.store

        sourceStore.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 54.75,
            date: epochMs(year: 2026, month: 2, day: 14),
            category: "Dining",
            paymentMethod: "Card",
            paidBy: "Alex",
            forWhom: "Alex, Sam",
            fromWhom: "",
            contributedBy: "Sam",
            contributedAmount: 20.0,
            notes: "Dinner at \"Bistro, East\"\nGreat dessert"
        )
        sourceStore.addTransaction(
            type: LedgerTransaction.typeExpense,
            amount: 88.00,
            date: epochMs(year: 2026, month: 5, day: 3),
            category: "Groceries",
            paymentMethod: "Cash",
            paidBy: "Sam",
            forWhom: "Shared Family",
            fromWhom: "",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: "May groceries"
        )

        // Export Full Year 2026 CSV
        let fullCSV = LedgerExportImportManager.exportCSV(
            transactions: sourceStore.transactions,
            scope: ExportScopeFilter(mode: .fullYear, year: 2026, selectedZeroBasedMonths: [])
        )
        #expect(fullCSV.hasPrefix(LedgerExportImportManager.csvHeader))

        // Parse CSV back and verify RFC-4180 unquoting & embedded newline preservation
        let parsed = try LedgerExportImportManager.parseImportText(fullCSV)
        #expect(parsed.format == .csv)
        #expect(parsed.transactions.count == 2)

        let diningRow = try #require(parsed.transactions.first(where: { $0.category == "Dining" }))
        #expect(diningRow.amount == 54.75)
        #expect(diningRow.forWhom == "Alex, Sam")
        #expect(diningRow.contributedBy == "Sam")
        #expect(diningRow.contributedAmount == 20.0)
        #expect(diningRow.notes == "Dinner at \"Bistro, East\"\nGreat dessert")

        // Filter import to Particular Month (February 2026 -> 0-based month 1)
        let destHarness = try TestHarness()
        defer { destHarness.cleanup() }
        let destStore = destHarness.store

        let febScope = ExportScopeFilter(mode: .particularMonth, year: 2026, selectedZeroBasedMonths: [1])
        let merge = destStore.mergeImport(preview: parsed, scope: febScope)
        #expect(merge.transactionsAdded == 1)
        #expect(destStore.transactions.count == 1)
        #expect(destStore.transactions.first?.category == "Dining")
    }
}
