import Foundation

enum AnalyticsCalculator {
    private static let genericExclusions: Set<String> = [
        "",
        "family",
        "shared family",
        "employer",
        "freelance / client",
        "family member",
        "refund",
        "3rd party",
        "atm / cash wallet refill",
        "bank / loan",
        "credit card",
        "landlord",
        "friend",
        "clinic / hospital",
        "vendor"
    ]

    static func matchesName(_ lhs: String, _ rhs: String) -> Bool {
        let a = lhs.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let b = rhs.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !a.isEmpty, !b.isEmpty else { return false }
        return a == b
    }

    static func beneficiaryShareAmount(forWhom: String, totalAmount: Double, profileName: String) -> Double {
        let trimmed = forWhom.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return 0.0 }
        if matchesName(trimmed, profileName) {
            return totalAmount
        }
        let parts = trimmed
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard parts.count > 1 else { return 0.0 }
        let matchedCount = parts.filter { matchesName($0, profileName) }.count
        guard matchedCount > 0 else { return 0.0 }
        return FormatUtils.roundToTwoDecimals((totalAmount / Double(parts.count)) * Double(matchedCount))
    }

    static func forWhomIncludes(_ forWhom: String, profileName: String) -> Bool {
        beneficiaryShareAmount(forWhom: forWhom, totalAmount: 1.0, profileName: profileName) > 0
    }

    static func cashInHandBalance(baseline: Double, transactions: [LedgerTransaction]) -> Double {
        let active = transactions.filter { !$0.isDeleted && $0.paymentMethod.caseInsensitiveCompare("Cash") == .orderedSame }
        var total = baseline
        for tx in active {
            if tx.isIncome {
                total += tx.amount
            } else if tx.isExpense {
                total -= tx.amount
            }
        }
        return FormatUtils.roundToTwoDecimals(total)
    }

    static func computeSplitShares(
        totalAmount: Double,
        participants: [String],
        paidBy: String
    ) -> (perPersonShare: Double, debtorShares: [(debtor: String, amount: Double)]) {
        let cleanedParticipants = participants
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard cleanedParticipants.count >= 2, totalAmount > 0 else {
            return (0.0, [])
        }
        let perPerson = FormatUtils.roundToTwoDecimals(totalAmount / Double(cleanedParticipants.count))
        let debtors = cleanedParticipants
            .filter { !matchesName($0, paidBy) }
            .map { ($0, perPerson) }
        return (perPerson, debtors)
    }

    static func profileFinancialSummaries(
        members: [FamilyMember],
        transactions: [LedgerTransaction],
        liabilities: [FamilyLiability]
    ) -> [ProfileFinancialSummary] {
        let activeMembers = members
            .filter { !$0.isDeleted }
            .sorted { lhs, rhs in
                if lhs.isPrimary != rhs.isPrimary {
                    return lhs.isPrimary && !rhs.isPrimary
                }
                return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
            }

        let activeTx = transactions.filter { !$0.isDeleted }
        let activeLiab = liabilities.filter { !$0.isDeleted }

        var seenLower = Set<String>()
        var orderedEntries: [(member: FamilyMember?, name: String)] = []

        for member in activeMembers {
            let key = member.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            guard !key.isEmpty, !seenLower.contains(key) else { continue }
            seenLower.insert(key)
            orderedEntries.append((member, member.name.trimmingCharacters(in: .whitespacesAndNewlines)))
        }

        // Gather inferred contributor names from transactions and IOUs
        var inferredNames: [String] = []
        func considerInferred(_ raw: String) {
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            let key = trimmed.lowercased()
            guard !trimmed.isEmpty, !genericExclusions.contains(key), !seenLower.contains(key) else {
                return
            }
            seenLower.insert(key)
            inferredNames.append(trimmed)
        }

        for tx in activeTx {
            considerInferred(tx.paidBy)
            if !tx.contributedBy.isEmpty {
                considerInferred(tx.contributedBy)
            }
        }
        for liab in activeLiab {
            if liab.liabilityType == FamilyLiability.typeFamilyIOU {
                considerInferred(liab.fromMember)
                considerInferred(liab.toMember)
            } else if !liab.linkedProfile.isEmpty {
                considerInferred(liab.linkedProfile)
            } else if liab.liabilityType == FamilyLiability.typeThirdPartyPayable {
                considerInferred(liab.fromMember)
            } else if liab.liabilityType == FamilyLiability.typeThirdPartyReceivable {
                considerInferred(liab.toMember)
            }
        }

        for name in inferredNames.sorted(by: { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }) {
            orderedEntries.append((nil, name))
        }

        return orderedEntries.map { entry in
            let profileName = entry.name
            var totalIncome = 0.0
            var totalPaidOut = 0.0
            var totalSpentFor = 0.0
            var totalSharesContributed = 0.0
            var totalSharesReceived = 0.0

            for tx in activeTx {
                if tx.isIncome {
                    if matchesName(tx.paidBy, profileName) {
                        totalIncome += tx.amount
                    }
                } else if tx.isExpense {
                    if matchesName(tx.paidBy, profileName) {
                        totalPaidOut += tx.amount
                        if tx.contributedAmount > 0 {
                            totalSharesReceived += tx.contributedAmount
                        }
                    }
                    totalSpentFor += beneficiaryShareAmount(
                        forWhom: tx.forWhom,
                        totalAmount: tx.amount,
                        profileName: profileName
                    )
                }

                if tx.contributedAmount > 0 && matchesName(tx.contributedBy, profileName) {
                    totalSharesContributed += tx.contributedAmount
                }
            }

            let standing = liabilityBreakdown(for: profileName, liabilities: activeLiab)

            return ProfileFinancialSummary(
                memberID: entry.member?.id,
                name: profileName,
                role: entry.member?.role ?? "Inferred",
                avatarColorHex: entry.member?.avatarColorHex ?? "#1E5B94",
                isPrimary: entry.member?.isPrimary ?? false,
                isSavedProfile: entry.member != nil,
                notes: entry.member?.notes ?? "",
                totalIncome: FormatUtils.roundToTwoDecimals(totalIncome),
                totalPaidOut: FormatUtils.roundToTwoDecimals(totalPaidOut),
                totalSpentFor: FormatUtils.roundToTwoDecimals(totalSpentFor),
                totalSharesContributed: FormatUtils.roundToTwoDecimals(totalSharesContributed),
                totalSharesReceived: FormatUtils.roundToTwoDecimals(totalSharesReceived),
                familyTheyOwe: standing.familyTheyOwe,
                familyOwedToThem: standing.familyOwedToThem,
                thirdPartyTheyOwe: standing.thirdPartyTheyOwe,
                thirdPartyOwedToThem: standing.thirdPartyOwedToThem
            )
        }
    }

    static func liabilityBreakdown(
        for profileName: String,
        liabilities: [FamilyLiability]
    ) -> (familyTheyOwe: Double, familyOwedToThem: Double, thirdPartyTheyOwe: Double, thirdPartyOwedToThem: Double) {
        var familyTheyOwe = 0.0
        var familyOwedToThem = 0.0
        var thirdPartyTheyOwe = 0.0
        var thirdPartyOwedToThem = 0.0

        for liab in liabilities where !liab.isDeleted && !liab.isSettled {
            let rem = liab.remainingAmount
            guard rem > 0 else { continue }

            switch liab.liabilityType {
            case FamilyLiability.typeFamilyIOU:
                if matchesName(liab.fromMember, profileName) {
                    familyTheyOwe += rem
                }
                if matchesName(liab.toMember, profileName) {
                    familyOwedToThem += rem
                }
            case FamilyLiability.typeThirdPartyPayable:
                let responsible = liab.linkedProfile.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? liab.fromMember
                    : liab.linkedProfile
                if matchesName(responsible, profileName) {
                    thirdPartyTheyOwe += rem
                }
            case FamilyLiability.typeThirdPartyReceivable:
                let recipient = liab.linkedProfile.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? liab.toMember
                    : liab.linkedProfile
                if matchesName(recipient, profileName) {
                    thirdPartyOwedToThem += rem
                }
            default:
                if matchesName(liab.fromMember, profileName) {
                    familyTheyOwe += rem
                }
                if matchesName(liab.toMember, profileName) {
                    familyOwedToThem += rem
                }
            }
        }

        return (
            FormatUtils.roundToTwoDecimals(familyTheyOwe),
            FormatUtils.roundToTwoDecimals(familyOwedToThem),
            FormatUtils.roundToTwoDecimals(thirdPartyTheyOwe),
            FormatUtils.roundToTwoDecimals(thirdPartyOwedToThem)
        )
    }

    static func dashboardTopCategories(
        transactions: [LedgerTransaction],
        referenceDate: Date = Date(),
        calendar: Calendar = .current,
        limit: Int = 5
    ) -> [DashboardCategorySummary] {
        let comps = calendar.dateComponents([.year, .month], from: referenceDate)
        let year = comps.year ?? 2026
        let month = comps.month ?? 1

        let monthExpenses = transactions.filter {
            !$0.isDeleted && $0.isExpense && FormatUtils.isEpochMs($0.date, inYear: year, oneBasedMonth: month, calendar: calendar)
        }

        let totalExpense = monthExpenses.reduce(0.0) { $0 + $1.amount }
        guard totalExpense > 0 else { return [] }

        var byCategory: [String: (total: Double, payerTotals: [String: Double])] = [:]
        for tx in monthExpenses {
            let cat = tx.category.isEmpty ? "Other" : tx.category
            let payer = tx.paidBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Family" : tx.paidBy
            var entry = byCategory[cat] ?? (0.0, [:])
            entry.total += tx.amount
            entry.payerTotals[payer, default: 0.0] += tx.amount
            byCategory[cat] = entry
        }

        return byCategory
            .map { cat, entry in
                let topPayer = entry.payerTotals.max(by: { $0.value < $1.value })?.key ?? "Family"
                return DashboardCategorySummary(
                    category: cat,
                    totalAmount: FormatUtils.roundToTwoDecimals(entry.total),
                    percentage: min(1.0, max(0.0, entry.total / totalExpense)),
                    topPayer: topPayer
                )
            }
            .sorted { $0.totalAmount > $1.totalAmount }
            .prefix(limit)
            .map { $0 }
    }

    static func computeMonthOnMonthAnalytics(
        transactions: [LedgerTransaction],
        liabilities: [FamilyLiability],
        scopeProfileName: String?,
        referenceDate: Date = Date(),
        calendar: Calendar = .current
    ) -> MonthOnMonthAnalytics {
        let comps = calendar.dateComponents([.year, .month], from: referenceDate)
        let currentYear = comps.year ?? 2026
        let currentMonth = comps.month ?? 1
        let prev = FormatUtils.previousYearAndOneBasedMonth(year: currentYear, oneBasedMonth: currentMonth)

        let currentLabel = FormatUtils.monthYearLabel(year: currentYear, oneBasedMonth: currentMonth)
        let previousLabel = FormatUtils.monthYearLabel(year: prev.year, oneBasedMonth: prev.month)

        let activeTx = transactions.filter { !$0.isDeleted }
        let activeLiab = liabilities.filter { !$0.isDeleted && !$0.isSettled }

        let thisMonthTx = activeTx.filter {
            FormatUtils.isEpochMs($0.date, inYear: currentYear, oneBasedMonth: currentMonth, calendar: calendar)
        }
        let lastMonthTx = activeTx.filter {
            FormatUtils.isEpochMs($0.date, inYear: prev.year, oneBasedMonth: prev.month, calendar: calendar)
        }

        var expensesThisMonth = 0.0
        var expensesLastMonth = 0.0
        var incomeThisMonth = 0.0
        var incomeLastMonth = 0.0
        var spentForThisMonth = 0.0
        var sharesContributedThisMonth = 0.0
        var cashSpentThisMonth = 0.0
        var cashSpentLastMonth = 0.0

        var thisMonthCategories: [String: Double] = [:]
        var lastMonthCategories: [String: Double] = [:]
        var paymentTotals: [String: (amount: Double, count: Int)] = [:]

        if let profile = scopeProfileName, !profile.isEmpty {
            for tx in thisMonthTx {
                if tx.isExpense {
                    if matchesName(tx.paidBy, profile) {
                        expensesThisMonth += tx.amount
                        if tx.paymentMethod.caseInsensitiveCompare("Cash") == .orderedSame {
                            cashSpentThisMonth += tx.amount
                        }
                    }
                    let forShare = beneficiaryShareAmount(forWhom: tx.forWhom, totalAmount: tx.amount, profileName: profile)
                    spentForThisMonth += forShare

                    if matchesName(tx.paidBy, profile) || forShare > 0 {
                        let cat = tx.category.isEmpty ? "Other" : tx.category
                        thisMonthCategories[cat, default: 0.0] += tx.amount
                        let method = tx.paymentMethod.isEmpty ? "Card" : tx.paymentMethod
                        let existing = paymentTotals[method] ?? (0.0, 0)
                        paymentTotals[method] = (existing.amount + tx.amount, existing.count + 1)
                    }
                } else if tx.isIncome {
                    if matchesName(tx.paidBy, profile) {
                        incomeThisMonth += tx.amount
                    }
                }

                if tx.contributedAmount > 0 && matchesName(tx.contributedBy, profile) {
                    sharesContributedThisMonth += tx.contributedAmount
                }
            }

            for tx in lastMonthTx {
                if tx.isExpense {
                    if matchesName(tx.paidBy, profile) {
                        expensesLastMonth += tx.amount
                        if tx.paymentMethod.caseInsensitiveCompare("Cash") == .orderedSame {
                            cashSpentLastMonth += tx.amount
                        }
                    }
                    if matchesName(tx.paidBy, profile) || forWhomIncludes(tx.forWhom, profileName: profile) {
                        let cat = tx.category.isEmpty ? "Other" : tx.category
                        lastMonthCategories[cat, default: 0.0] += tx.amount
                    }
                } else if tx.isIncome {
                    if matchesName(tx.paidBy, profile) {
                        incomeLastMonth += tx.amount
                    }
                }
            }

            let liabStanding = liabilityBreakdown(for: profile, liabilities: activeLiab)

            return buildAnalyticsResult(
                scopeName: profile,
                currentLabel: currentLabel,
                previousLabel: previousLabel,
                expensesThisMonth: expensesThisMonth,
                expensesLastMonth: expensesLastMonth,
                incomeThisMonth: incomeThisMonth,
                incomeLastMonth: incomeLastMonth,
                spentForThisMonth: spentForThisMonth,
                sharesContributedThisMonth: sharesContributedThisMonth,
                thirdPartyDebtOwed: liabStanding.thirdPartyTheyOwe,
                thirdPartyReceivables: liabStanding.thirdPartyOwedToThem,
                familyIOUsOwed: liabStanding.familyTheyOwe,
                familyIOUsReceivable: liabStanding.familyOwedToThem,
                cashSpentThisMonth: cashSpentThisMonth,
                cashSpentLastMonth: cashSpentLastMonth,
                thisMonthCategories: thisMonthCategories,
                lastMonthCategories: lastMonthCategories,
                paymentTotals: paymentTotals
            )
        } else {
            for tx in thisMonthTx {
                if tx.isExpense {
                    expensesThisMonth += tx.amount
                    spentForThisMonth += tx.amount
                    if tx.paymentMethod.caseInsensitiveCompare("Cash") == .orderedSame {
                        cashSpentThisMonth += tx.amount
                    }
                    let cat = tx.category.isEmpty ? "Other" : tx.category
                    thisMonthCategories[cat, default: 0.0] += tx.amount
                    let method = tx.paymentMethod.isEmpty ? "Card" : tx.paymentMethod
                    let existing = paymentTotals[method] ?? (0.0, 0)
                    paymentTotals[method] = (existing.amount + tx.amount, existing.count + 1)
                } else if tx.isIncome {
                    incomeThisMonth += tx.amount
                }
                if tx.contributedAmount > 0 {
                    sharesContributedThisMonth += tx.contributedAmount
                }
            }

            for tx in lastMonthTx {
                if tx.isExpense {
                    expensesLastMonth += tx.amount
                    if tx.paymentMethod.caseInsensitiveCompare("Cash") == .orderedSame {
                        cashSpentLastMonth += tx.amount
                    }
                    let cat = tx.category.isEmpty ? "Other" : tx.category
                    lastMonthCategories[cat, default: 0.0] += tx.amount
                } else if tx.isIncome {
                    incomeLastMonth += tx.amount
                }
            }

            var thirdPartyDebtOwed = 0.0
            var thirdPartyReceivables = 0.0
            var familyIOUsTotal = 0.0

            for liab in activeLiab {
                switch liab.liabilityType {
                case FamilyLiability.typeThirdPartyPayable:
                    thirdPartyDebtOwed += liab.remainingAmount
                case FamilyLiability.typeThirdPartyReceivable:
                    thirdPartyReceivables += liab.remainingAmount
                default:
                    familyIOUsTotal += liab.remainingAmount
                }
            }

            return buildAnalyticsResult(
                scopeName: nil,
                currentLabel: currentLabel,
                previousLabel: previousLabel,
                expensesThisMonth: expensesThisMonth,
                expensesLastMonth: expensesLastMonth,
                incomeThisMonth: incomeThisMonth,
                incomeLastMonth: incomeLastMonth,
                spentForThisMonth: spentForThisMonth,
                sharesContributedThisMonth: sharesContributedThisMonth,
                thirdPartyDebtOwed: thirdPartyDebtOwed,
                thirdPartyReceivables: thirdPartyReceivables,
                familyIOUsOwed: familyIOUsTotal,
                familyIOUsReceivable: familyIOUsTotal,
                cashSpentThisMonth: cashSpentThisMonth,
                cashSpentLastMonth: cashSpentLastMonth,
                thisMonthCategories: thisMonthCategories,
                lastMonthCategories: lastMonthCategories,
                paymentTotals: paymentTotals
            )
        }
    }

    private static func buildAnalyticsResult(
        scopeName: String?,
        currentLabel: String,
        previousLabel: String,
        expensesThisMonth: Double,
        expensesLastMonth: Double,
        incomeThisMonth: Double,
        incomeLastMonth: Double,
        spentForThisMonth: Double,
        sharesContributedThisMonth: Double,
        thirdPartyDebtOwed: Double,
        thirdPartyReceivables: Double,
        familyIOUsOwed: Double,
        familyIOUsReceivable: Double,
        cashSpentThisMonth: Double,
        cashSpentLastMonth: Double,
        thisMonthCategories: [String: Double],
        lastMonthCategories: [String: Double],
        paymentTotals: [String: (amount: Double, count: Int)]
    ) -> MonthOnMonthAnalytics {
        let allCategories = Set(thisMonthCategories.keys).union(lastMonthCategories.keys)
        let categoryComparisons = allCategories
            .map { cat in
                CategoryMoMComparison(
                    category: cat,
                    thisMonthAmount: FormatUtils.roundToTwoDecimals(thisMonthCategories[cat] ?? 0.0),
                    lastMonthAmount: FormatUtils.roundToTwoDecimals(lastMonthCategories[cat] ?? 0.0)
                )
            }
            .sorted { max($0.thisMonthAmount, $0.lastMonthAmount) > max($1.thisMonthAmount, $1.lastMonthAmount) }

        let totalPaymentSpend = paymentTotals.values.reduce(0.0) { $0 + $1.amount }
        let paymentMethods = paymentTotals
            .map { method, data in
                PaymentMethodBreakdown(
                    method: method,
                    amount: FormatUtils.roundToTwoDecimals(data.amount),
                    percentage: totalPaymentSpend > 0 ? min(1.0, data.amount / totalPaymentSpend) : 0.0,
                    count: data.count
                )
            }
            .sorted { $0.amount > $1.amount }

        return MonthOnMonthAnalytics(
            scopeName: scopeName,
            currentMonthLabel: currentLabel,
            previousMonthLabel: previousLabel,
            expensesThisMonth: FormatUtils.roundToTwoDecimals(expensesThisMonth),
            expensesLastMonth: FormatUtils.roundToTwoDecimals(expensesLastMonth),
            incomeThisMonth: FormatUtils.roundToTwoDecimals(incomeThisMonth),
            incomeLastMonth: FormatUtils.roundToTwoDecimals(incomeLastMonth),
            spentForThisMonth: FormatUtils.roundToTwoDecimals(spentForThisMonth),
            sharesContributedThisMonth: FormatUtils.roundToTwoDecimals(sharesContributedThisMonth),
            thirdPartyDebtOwed: FormatUtils.roundToTwoDecimals(thirdPartyDebtOwed),
            thirdPartyReceivables: FormatUtils.roundToTwoDecimals(thirdPartyReceivables),
            familyIOUsOwed: FormatUtils.roundToTwoDecimals(familyIOUsOwed),
            familyIOUsReceivable: FormatUtils.roundToTwoDecimals(familyIOUsReceivable),
            cashSpentThisMonth: FormatUtils.roundToTwoDecimals(cashSpentThisMonth),
            cashSpentLastMonth: FormatUtils.roundToTwoDecimals(cashSpentLastMonth),
            categoryComparisons: categoryComparisons,
            paymentMethods: paymentMethods
        )
    }
}
