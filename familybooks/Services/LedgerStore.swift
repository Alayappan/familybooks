import Foundation
import Observation
import SwiftData

@Observable
final class LedgerStore {
    static let activeProfileNameKey = "active_profile_name"
    static let cashPoolBaselineKey = "cash_pool_baseline"

    private let modelContext: ModelContext
    private let userDefaults: UserDefaults

    var profiles: [FamilyMember] = []
    var transactions: [LedgerTransaction] = []
    var liabilities: [FamilyLiability] = []

    var activeProfileName: String {
        didSet {
            userDefaults.set(activeProfileName, forKey: Self.activeProfileNameKey)
        }
    }

    var cashPoolBaseline: Double {
        didSet {
            userDefaults.set(cashPoolBaseline, forKey: Self.cashPoolBaselineKey)
        }
    }

    var toastMessage: String?

    init(modelContext: ModelContext, userDefaults: UserDefaults = .standard) {
        self.modelContext = modelContext
        self.userDefaults = userDefaults
        self.activeProfileName = userDefaults.string(forKey: Self.activeProfileNameKey) ?? ""
        self.cashPoolBaseline = userDefaults.double(forKey: Self.cashPoolBaselineKey)
        refresh()
    }

    // MARK: - Fetch & Refresh

    func refresh() {
        do {
            let memberDescriptor = FetchDescriptor<FamilyMember>(
                predicate: #Predicate { !$0.isDeleted }
            )
            let fetchedMembers = try modelContext.fetch(memberDescriptor)
            self.profiles = fetchedMembers.sorted { lhs, rhs in
                if lhs.isPrimary != rhs.isPrimary {
                    return lhs.isPrimary && !rhs.isPrimary
                }
                return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
            }

            let txDescriptor = FetchDescriptor<LedgerTransaction>(
                predicate: #Predicate { !$0.isDeleted },
                sortBy: [SortDescriptor(\.date, order: .reverse)]
            )
            self.transactions = try modelContext.fetch(txDescriptor)

            let liabDescriptor = FetchDescriptor<FamilyLiability>(
                predicate: #Predicate { !$0.isDeleted },
                sortBy: [SortDescriptor(\.date, order: .reverse)]
            )
            self.liabilities = try modelContext.fetch(liabDescriptor)
        } catch {
            // Keep current in-memory lists if fetch fails
        }
    }

    func showToast(_ message: String) {
        toastMessage = message
    }

    // MARK: - Computed Summaries

    var activeProfile: FamilyMember? {
        if !activeProfileName.isEmpty,
           let matched = profiles.first(where: { AnalyticsCalculator.matchesName($0.name, activeProfileName) }) {
            return matched
        }
        return profiles.first(where: { $0.isPrimary }) ?? profiles.first
    }

    var defaultPayerName: String {
        if let active = activeProfile {
            return active.name
        }
        if !activeProfileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return activeProfileName
        }
        return "Family"
    }

    var cashInHandBalance: Double {
        AnalyticsCalculator.cashInHandBalance(baseline: cashPoolBaseline, transactions: transactions)
    }

    var pendingLiabilities: [FamilyLiability] {
        liabilities.filter { !$0.isDeleted && !$0.isSettled }
    }

    var settledLiabilities: [FamilyLiability] {
        liabilities.filter { !$0.isDeleted && $0.isSettled }
    }

    var totalPendingFamilyIOUs: Double {
        FormatUtils.roundToTwoDecimals(
            pendingLiabilities
                .filter { $0.liabilityType == FamilyLiability.typeFamilyIOU }
                .reduce(0.0) { $0 + $1.remainingAmount }
        )
    }

    var totalPendingThirdPartyPayable: Double {
        FormatUtils.roundToTwoDecimals(
            pendingLiabilities
                .filter { $0.liabilityType == FamilyLiability.typeThirdPartyPayable }
                .reduce(0.0) { $0 + $1.remainingAmount }
        )
    }

    var totalPendingThirdPartyReceivable: Double {
        FormatUtils.roundToTwoDecimals(
            pendingLiabilities
                .filter { $0.liabilityType == FamilyLiability.typeThirdPartyReceivable }
                .reduce(0.0) { $0 + $1.remainingAmount }
        )
    }

    var profileFinancialSummaries: [ProfileFinancialSummary] {
        AnalyticsCalculator.profileFinancialSummaries(
            members: profiles,
            transactions: transactions,
            liabilities: liabilities
        )
    }

    func currentMonthTotals(referenceDate: Date = Date(), calendar: Calendar = .current) -> (income: Double, expense: Double, net: Double) {
        let comps = calendar.dateComponents([.year, .month], from: referenceDate)
        let year = comps.year ?? 2026
        let month = comps.month ?? 1
        var income = 0.0
        var expense = 0.0
        for tx in transactions where !tx.isDeleted {
            if FormatUtils.isEpochMs(tx.date, inYear: year, oneBasedMonth: month, calendar: calendar) {
                if tx.isIncome {
                    income += tx.amount
                } else if tx.isExpense {
                    expense += tx.amount
                }
            }
        }
        let roundedIncome = FormatUtils.roundToTwoDecimals(income)
        let roundedExpense = FormatUtils.roundToTwoDecimals(expense)
        return (roundedIncome, roundedExpense, FormatUtils.roundToTwoDecimals(roundedIncome - roundedExpense))
    }

    func avatarColorHex(for profileName: String) -> String {
        if let member = profiles.first(where: { AnalyticsCalculator.matchesName($0.name, profileName) }) {
            return member.avatarColorHex
        }
        let palette = BrandColors.avatarPresetHexes
        let hash = abs(profileName.lowercased().hashValue)
        return palette[hash % palette.count]
    }

    // MARK: - Cash Pool Operations

    func setCashPoolBaseline(newTotal: Double) {
        let currentBalance = cashInHandBalance
        let delta = newTotal - currentBalance
        cashPoolBaseline = FormatUtils.roundToTwoDecimals(cashPoolBaseline + delta)
    }

    func refillCashPool(amount: Double, note: String, date: Int64 = FormatUtils.nowEpochMs) {
        guard amount > 0 else { return }
        let cleanNote = note.trimmingCharacters(in: .whitespacesAndNewlines)
        let payer = defaultPayerName
        addTransaction(
            type: LedgerTransaction.typeIncome,
            amount: FormatUtils.roundToTwoDecimals(amount),
            date: date,
            category: "Other",
            paymentMethod: "Cash",
            paidBy: payer,
            forWhom: "",
            fromWhom: "ATM / Cash Wallet Refill",
            contributedBy: "",
            contributedAmount: 0.0,
            notes: cleanNote.isEmpty ? "Cash wallet refill" : cleanNote
        )
    }

    // MARK: - Profile CRUD

    @discardableResult
    func addProfile(
        name: String,
        role: String,
        avatarColorHex: String,
        notes: String,
        setAsActive: Bool
    ) -> FamilyMember {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let now = FormatUtils.nowEpochMs
        let isFirst = profiles.isEmpty
        let shouldBePrimary = isFirst || setAsActive

        if shouldBePrimary {
            for member in profiles where member.isPrimary {
                member.isPrimary = false
                member.updatedAt = now
            }
        }

        let member = FamilyMember(
            name: trimmedName,
            role: role,
            avatarColorHex: avatarColorHex,
            isPrimary: shouldBePrimary,
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
            createdAt: now,
            updatedAt: now,
            isDeleted: false
        )
        modelContext.insert(member)
        saveContext()
        if shouldBePrimary {
            activeProfileName = trimmedName
        }
        refresh()
        return member
    }

    func updateProfile(
        _ member: FamilyMember,
        name: String,
        role: String,
        avatarColorHex: String,
        notes: String,
        setAsActive: Bool
    ) {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let oldName = member.name
        let now = FormatUtils.nowEpochMs

        if setAsActive {
            for other in profiles where other.id != member.id && other.isPrimary {
                other.isPrimary = false
                other.updatedAt = now
            }
            member.isPrimary = true
            activeProfileName = trimmedName
        } else if AnalyticsCalculator.matchesName(activeProfileName, oldName) {
            activeProfileName = trimmedName
        }

        member.name = trimmedName
        member.role = role
        member.avatarColorHex = avatarColorHex
        member.notes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        member.updatedAt = now
        saveContext()
        refresh()
    }

    func setActiveProfile(_ name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let now = FormatUtils.nowEpochMs
        for member in profiles {
            let match = AnalyticsCalculator.matchesName(member.name, trimmed)
            if member.isPrimary != match {
                member.isPrimary = match
                member.updatedAt = now
            }
        }
        activeProfileName = trimmed
        saveContext()
        refresh()
    }

    func deleteProfile(_ member: FamilyMember) {
        let deletedName = member.name
        member.isDeleted = true
        member.isPrimary = false
        member.updatedAt = FormatUtils.nowEpochMs
        saveContext()
        refresh()

        if AnalyticsCalculator.matchesName(activeProfileName, deletedName) {
            if let fallback = profiles.first {
                fallback.isPrimary = true
                activeProfileName = fallback.name
                saveContext()
                refresh()
            } else {
                activeProfileName = ""
            }
        }
    }

    // MARK: - Transaction CRUD

    @discardableResult
    func addTransaction(
        type: String,
        amount: Double,
        date: Int64 = FormatUtils.nowEpochMs,
        category: String,
        paymentMethod: String,
        paidBy: String,
        forWhom: String,
        fromWhom: String,
        contributedBy: String,
        contributedAmount: Double,
        notes: String
    ) -> LedgerTransaction {
        let now = FormatUtils.nowEpochMs
        let clampedShare = max(0.0, min(amount, contributedAmount))
        let tx = LedgerTransaction(
            type: type,
            amount: FormatUtils.roundToTwoDecimals(amount),
            date: date,
            category: category,
            paymentMethod: paymentMethod,
            paidBy: paidBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Family" : paidBy.trimmingCharacters(in: .whitespacesAndNewlines),
            forWhom: forWhom.trimmingCharacters(in: .whitespacesAndNewlines),
            fromWhom: fromWhom.trimmingCharacters(in: .whitespacesAndNewlines),
            contributedBy: contributedBy.trimmingCharacters(in: .whitespacesAndNewlines),
            contributedAmount: FormatUtils.roundToTwoDecimals(clampedShare),
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
            createdAt: now,
            updatedAt: now,
            isDeleted: false
        )
        modelContext.insert(tx)
        saveContext()
        refresh()
        return tx
    }

    func updateTransaction(
        _ transaction: LedgerTransaction,
        type: String,
        amount: Double,
        date: Int64,
        category: String,
        paymentMethod: String,
        paidBy: String,
        forWhom: String,
        fromWhom: String,
        contributedBy: String,
        contributedAmount: Double,
        notes: String
    ) {
        let clampedShare = max(0.0, min(amount, contributedAmount))
        transaction.type = type
        transaction.amount = FormatUtils.roundToTwoDecimals(amount)
        transaction.date = date
        transaction.category = category
        transaction.paymentMethod = paymentMethod
        transaction.paidBy = paidBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Family" : paidBy.trimmingCharacters(in: .whitespacesAndNewlines)
        transaction.forWhom = forWhom.trimmingCharacters(in: .whitespacesAndNewlines)
        transaction.fromWhom = fromWhom.trimmingCharacters(in: .whitespacesAndNewlines)
        transaction.contributedBy = contributedBy.trimmingCharacters(in: .whitespacesAndNewlines)
        transaction.contributedAmount = FormatUtils.roundToTwoDecimals(clampedShare)
        transaction.notes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        transaction.updatedAt = FormatUtils.nowEpochMs
        saveContext()
        refresh()
    }

    func deleteTransaction(_ transaction: LedgerTransaction) {
        transaction.isDeleted = true
        transaction.updatedAt = FormatUtils.nowEpochMs
        saveContext()
        refresh()
    }

    // MARK: - Liability CRUD & Partial Payments

    @discardableResult
    func addLiability(
        title: String,
        liabilityType: String,
        linkedProfile: String,
        fromMember: String,
        toMember: String,
        amount: Double,
        date: Int64 = FormatUtils.nowEpochMs,
        dueDate: Int64? = nil,
        notes: String = ""
    ) -> FamilyLiability {
        let now = FormatUtils.nowEpochMs
        let liability = FamilyLiability(
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            liabilityType: liabilityType,
            linkedProfile: linkedProfile.trimmingCharacters(in: .whitespacesAndNewlines),
            fromMember: fromMember.trimmingCharacters(in: .whitespacesAndNewlines),
            toMember: toMember.trimmingCharacters(in: .whitespacesAndNewlines),
            amount: FormatUtils.roundToTwoDecimals(amount),
            paidAmount: 0.0,
            date: date,
            dueDate: dueDate,
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
            status: FamilyLiability.statusUnpaid,
            settledAt: nil,
            createdAt: now,
            updatedAt: now,
            isDeleted: false
        )
        modelContext.insert(liability)
        saveContext()
        refresh()
        return liability
    }

    func recordPartialPayment(liability: FamilyLiability, paymentAmount: Double) {
        guard paymentAmount > 0, !liability.isSettled else { return }
        let now = FormatUtils.nowEpochMs
        let newPaid = min(liability.amount, FormatUtils.roundToTwoDecimals(liability.paidAmount + paymentAmount))
        liability.paidAmount = newPaid
        let remaining = max(0.0, liability.amount - newPaid)

        if remaining <= 0.005 {
            liability.paidAmount = liability.amount
            liability.status = FamilyLiability.statusSettled
            liability.settledAt = now
        } else {
            liability.status = FamilyLiability.statusPartial
        }
        liability.updatedAt = now
        saveContext()
        refresh()
    }

    func settleLiability(_ liability: FamilyLiability) {
        let now = FormatUtils.nowEpochMs
        liability.paidAmount = liability.amount
        liability.status = FamilyLiability.statusSettled
        liability.settledAt = now
        liability.updatedAt = now
        saveContext()
        refresh()
    }

    func deleteLiability(_ liability: FamilyLiability) {
        liability.isDeleted = true
        liability.updatedAt = FormatUtils.nowEpochMs
        saveContext()
        refresh()
    }

    // MARK: - Split Family Expense

    @discardableResult
    func saveSplitExpense(
        title: String,
        totalAmount: Double,
        paidBy: String,
        participants: [String],
        alsoLogExpense: Bool,
        category: String = "Other",
        paymentMethod: String = "Card",
        notes: String = "",
        date: Int64 = FormatUtils.nowEpochMs
    ) -> (transaction: LedgerTransaction?, createdLiabilities: [FamilyLiability]) {
        let split = AnalyticsCalculator.computeSplitShares(
            totalAmount: totalAmount,
            participants: participants,
            paidBy: paidBy
        )
        guard split.perPersonShare > 0 else {
            return (nil, [])
        }

        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let participantList = participants.joined(separator: ", ")

        var loggedTx: LedgerTransaction? = nil
        if alsoLogExpense {
            let noteBody = notes.trimmingCharacters(in: .whitespacesAndNewlines)
            let combinedNotes = noteBody.isEmpty
                ? "Split bill: \(cleanTitle) (\(participants.count) members @ \(FormatUtils.currency(split.perPersonShare)) each)"
                : "\(cleanTitle) • \(noteBody)"

            loggedTx = addTransaction(
                type: LedgerTransaction.typeExpense,
                amount: totalAmount,
                date: date,
                category: category,
                paymentMethod: paymentMethod,
                paidBy: paidBy,
                forWhom: participantList,
                fromWhom: "",
                contributedBy: "",
                contributedAmount: 0.0,
                notes: combinedNotes
            )
        }

        var created: [FamilyLiability] = []
        for entry in split.debtorShares {
            let liab = addLiability(
                title: "\(cleanTitle) (Split Share)",
                liabilityType: FamilyLiability.typeFamilyIOU,
                linkedProfile: "",
                fromMember: entry.debtor,
                toMember: paidBy,
                amount: entry.amount,
                date: date,
                dueDate: nil,
                notes: "Split of \(FormatUtils.currency(totalAmount)) among \(participantList)"
            )
            created.append(liab)
        }

        return (loggedTx, created)
    }

    // MARK: - Backup Import Merge

    @discardableResult
    func mergeImport(
        preview: ParsedImportPreview,
        scope: ExportScopeFilter?,
        calendar: Calendar = .current
    ) -> ImportMergeResult {
        let filtered = LedgerExportImportManager.filterPreview(preview, scope: scope, calendar: calendar)

        let allMembers = (try? modelContext.fetch(FetchDescriptor<FamilyMember>())) ?? []
        let allTransactions = (try? modelContext.fetch(FetchDescriptor<LedgerTransaction>())) ?? []
        let allLiabilities = (try? modelContext.fetch(FetchDescriptor<FamilyLiability>())) ?? []

        var memberById: [String: FamilyMember] = Dictionary(uniqueKeysWithValues: allMembers.map { ($0.id, $0) })
        var activeMemberNamesLower = Set(
            allMembers
                .filter { !$0.isDeleted }
                .map { $0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
        )

        var profilesAdded = 0
        var profilesSkipped = 0

        for dto in filtered.profiles {
            let lowerName = dto.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if memberById[dto.id] != nil || activeMemberNamesLower.contains(lowerName) {
                profilesSkipped += 1
                continue
            }
            let shouldBePrimary = dto.isPrimary && activeMemberNamesLower.isEmpty
            let member = FamilyMember(
                id: dto.id,
                name: dto.name,
                role: dto.role,
                avatarColorHex: dto.avatarColorHex,
                isPrimary: shouldBePrimary,
                notes: dto.notes,
                createdAt: dto.createdAt,
                updatedAt: dto.updatedAt,
                isDeleted: false
            )
            modelContext.insert(member)
            memberById[member.id] = member
            activeMemberNamesLower.insert(lowerName)
            if shouldBePrimary && activeProfileName.isEmpty {
                activeProfileName = member.name
            }
            profilesAdded += 1
        }

        var txById: [String: LedgerTransaction] = Dictionary(uniqueKeysWithValues: allTransactions.map { ($0.id, $0) })
        var transactionsAdded = 0
        var transactionsUpdated = 0

        for dto in filtered.transactions {
            if let existing = txById[dto.id] {
                if dto.updatedAt > existing.updatedAt {
                    existing.type = dto.type
                    existing.amount = dto.amount
                    existing.date = dto.date
                    existing.category = dto.category
                    existing.paymentMethod = dto.paymentMethod
                    existing.paidBy = dto.paidBy
                    existing.forWhom = dto.forWhom
                    existing.fromWhom = dto.fromWhom
                    existing.contributedBy = dto.contributedBy
                    existing.contributedAmount = dto.contributedAmount
                    existing.notes = dto.notes
                    existing.updatedAt = dto.updatedAt
                    existing.isDeleted = dto.isDeleted
                    transactionsUpdated += 1
                }
            } else {
                let tx = LedgerTransaction(
                    id: dto.id,
                    type: dto.type,
                    amount: dto.amount,
                    date: dto.date,
                    category: dto.category,
                    paymentMethod: dto.paymentMethod,
                    paidBy: dto.paidBy,
                    forWhom: dto.forWhom,
                    fromWhom: dto.fromWhom,
                    contributedBy: dto.contributedBy,
                    contributedAmount: dto.contributedAmount,
                    notes: dto.notes,
                    createdAt: dto.createdAt,
                    updatedAt: dto.updatedAt,
                    isDeleted: dto.isDeleted
                )
                modelContext.insert(tx)
                txById[tx.id] = tx
                transactionsAdded += 1
            }
        }

        var liabById: [String: FamilyLiability] = Dictionary(uniqueKeysWithValues: allLiabilities.map { ($0.id, $0) })
        var liabilitiesAdded = 0
        var liabilitiesUpdated = 0

        for dto in filtered.liabilities {
            if let existing = liabById[dto.id] {
                if dto.updatedAt > existing.updatedAt {
                    existing.title = dto.title
                    existing.liabilityType = dto.liabilityType
                    existing.linkedProfile = dto.linkedProfile
                    existing.fromMember = dto.fromMember
                    existing.toMember = dto.toMember
                    existing.amount = dto.amount
                    existing.paidAmount = dto.paidAmount
                    existing.date = dto.date
                    existing.dueDate = dto.dueDate
                    existing.notes = dto.notes
                    existing.status = dto.status
                    existing.settledAt = dto.settledAt
                    existing.updatedAt = dto.updatedAt
                    existing.isDeleted = dto.isDeleted
                    liabilitiesUpdated += 1
                }
            } else {
                let liab = FamilyLiability(
                    id: dto.id,
                    title: dto.title,
                    liabilityType: dto.liabilityType,
                    linkedProfile: dto.linkedProfile,
                    fromMember: dto.fromMember,
                    toMember: dto.toMember,
                    amount: dto.amount,
                    paidAmount: dto.paidAmount,
                    date: dto.date,
                    dueDate: dto.dueDate,
                    notes: dto.notes,
                    status: dto.status,
                    settledAt: dto.settledAt,
                    createdAt: dto.createdAt,
                    updatedAt: dto.updatedAt,
                    isDeleted: dto.isDeleted
                )
                modelContext.insert(liab)
                liabById[liab.id] = liab
                liabilitiesAdded += 1
            }
        }

        saveContext()
        refresh()

        return ImportMergeResult(
            profilesAdded: profilesAdded,
            profilesSkipped: profilesSkipped,
            transactionsAdded: transactionsAdded,
            transactionsUpdated: transactionsUpdated,
            liabilitiesAdded: liabilitiesAdded,
            liabilitiesUpdated: liabilitiesUpdated
        )
    }

    private func saveContext() {
        do {
            try modelContext.save()
        } catch {
            // Persist errors silently handled offline
        }
    }
}
