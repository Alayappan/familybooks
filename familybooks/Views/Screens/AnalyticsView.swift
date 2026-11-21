import SwiftUI

struct AnalyticsView: View {
    let store: LedgerStore
    @Binding var selectedScopeProfile: String?

    private var analytics: MonthOnMonthAnalytics {
        AnalyticsCalculator.computeMonthOnMonthAnalytics(
            transactions: store.transactions,
            liabilities: store.liabilities,
            scopeProfileName: selectedScopeProfile
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                scopeSelectorCard
                heroMoMCard
                attributionAndLiabilitiesCard
                cashInHandComparisonCard
                categoryMoMCard
                paymentMethodsCard
            }
            .padding(16)
            .padding(.bottom, 20)
        }
        .background(BrandColors.backgroundLight)
    }

    // MARK: - Scope Selector

    private var scopeSelectorCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Analyze Scope (Family or Individual Profile)")
                .font(.caption.weight(.bold))
                .foregroundStyle(BrandColors.textSecondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    SelectableChip(
                        title: "All Family",
                        isSelected: selectedScopeProfile == nil,
                        icon: "house.fill",
                        accentColor: BrandColors.navy900
                    ) {
                        selectedScopeProfile = nil
                    }

                    ForEach(store.profiles, id: \.id) { member in
                        let selected = selectedScopeProfile.map { AnalyticsCalculator.matchesName($0, member.name) } ?? false
                        SelectableChip(
                            title: member.name,
                            isSelected: selected,
                            accentColor: Color(hex: member.avatarColorHex)
                        ) {
                            selectedScopeProfile = member.name
                        }
                    }
                }
            }
        }
        .cardSurface(cornerRadius: 14, padding: 14)
    }

    // MARK: - Hero Navy900 MoM Card

    private var heroMoMCard: some View {
        let data = analytics
        let expenseTitle = data.scopeName.map { "Paid by \($0)" } ?? "Expenses This Month"

        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("MONTH-ON-MONTH COMPARISON")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.tealAccent)
                    .tracking(0.8)
                Spacer()
            }

            Text("\(data.currentMonthLabel) vs \(data.previousMonthLabel)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white.opacity(0.9))

            HStack(spacing: 12) {
                // Expenses MoM
                VStack(alignment: .leading, spacing: 6) {
                    Text(expenseTitle)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(0.8))
                        .lineLimit(1)

                    Text(FormatUtils.currency(data.expensesThisMonth))
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)

                    changeBadge(percent: data.expensePercentChange, invertGoodBad: true)

                    Text("Last mo: \(FormatUtils.currency(data.expensesLastMonth))")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.68))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(BrandColors.navy800)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                // Income MoM
                VStack(alignment: .leading, spacing: 6) {
                    Text("Income This Month")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(0.8))
                        .lineLimit(1)

                    Text(FormatUtils.currency(data.incomeThisMonth))
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)

                    changeBadge(percent: data.incomePercentChange, invertGoodBad: false)

                    Text("Last mo: \(FormatUtils.currency(data.incomeLastMonth))")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.68))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(BrandColors.navy800)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            // Net Savings Strip
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Net Savings This Month")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.75))
                    Text(FormatUtils.signedNetCurrency(data.netSavingsThisMonth))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(data.netSavingsThisMonth >= 0 ? BrandColors.tealAccent : Color(hex: "#FF8A80"))
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Last Month")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.75))
                    Text(FormatUtils.signedNetCurrency(data.netSavingsLastMonth))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white.opacity(0.9))
                }
            }
            .padding(12)
            .background(Color.white.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .padding(18)
        .background(BrandColors.navy900)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    @ViewBuilder
    private func changeBadge(percent: Double?, invertGoodBad: Bool) -> some View {
        if let pct = percent {
            let isPositive = pct > 0.05
            let isNegative = pct < -0.05
            let isGood = invertGoodBad ? isNegative : isPositive
            let color: Color = isGood ? BrandColors.tealAccent : ((isPositive || isNegative) ? Color(hex: "#FF8A80") : .white.opacity(0.75))
            let sign = pct > 0 ? "+" : ""
            Text("\(sign)\(Int(pct.rounded()))% MoM")
                .font(.caption2.weight(.bold))
                .foregroundStyle(color)
        } else {
            Text("0% MoM")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white.opacity(0.65))
        }
    }

    // MARK: - Attribution, Shares & Liabilities Overview Card

    private var attributionAndLiabilitiesCard: some View {
        let data = analytics
        let spentLabel = data.scopeName.map { "Spent For \($0)" } ?? "Total Expenses"

        return VStack(alignment: .leading, spacing: 12) {
            Text("Attribution, Shares & Liabilities Overview")
                .font(.headline)
                .foregroundStyle(BrandColors.textPrimary)

            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                overviewMetricTile(
                    title: spentLabel,
                    value: FormatUtils.currency(data.spentForThisMonth),
                    tint: BrandColors.blueBadge,
                    bg: BrandColors.blueBadgeBg
                )

                overviewMetricTile(
                    title: "Shares Contributed",
                    value: FormatUtils.currency(data.sharesContributedThisMonth),
                    tint: BrandColors.purpleBadge,
                    bg: BrandColors.purpleBadgeBg
                )

                overviewMetricTile(
                    title: "3rd-Party Debt Owed",
                    value: FormatUtils.currency(data.thirdPartyDebtOwed),
                    tint: BrandColors.expenseRed,
                    bg: BrandColors.expenseRedBg
                )

                overviewMetricTile(
                    title: "3rd-Party Receivables",
                    value: FormatUtils.currency(data.thirdPartyReceivables),
                    tint: BrandColors.incomeGreen,
                    bg: BrandColors.incomeGreenBg
                )

                overviewMetricTile(
                    title: "Family IOUs Owed",
                    value: FormatUtils.currency(data.familyIOUsOwed),
                    tint: BrandColors.pendingOrange,
                    bg: BrandColors.pendingOrangeBg
                )

                overviewMetricTile(
                    title: "Family IOUs Receivable",
                    value: FormatUtils.currency(data.familyIOUsReceivable),
                    tint: BrandColors.tealDark,
                    bg: BrandColors.tealLight
                )
            }
        }
        .cardSurface()
    }

    private func overviewMetricTile(title: String, value: String, tint: Color, bg: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(BrandColors.textSecondary)
                .lineLimit(1)
            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(11)
        .background(bg.opacity(0.65))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Cash in Hand Comparison Card

    private var cashInHandComparisonCard: some View {
        let data = analytics
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Cash in Hand & Cash Spend", systemImage: "banknote.fill")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)
                Spacer()
                Text(FormatUtils.currency(store.cashInHandBalance))
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.tealDark)
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Cash Spent This Month")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(BrandColors.textSecondary)
                    Text(FormatUtils.currency(data.cashSpentThisMonth))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(BrandColors.textPrimary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(BrandColors.inputContainerLight)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    Text("Cash Spent Last Month")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(BrandColors.textSecondary)
                    Text(FormatUtils.currency(data.cashSpentLastMonth))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(BrandColors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(BrandColors.inputContainerLight)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
        .cardSurface()
    }

    // MARK: - Category-by-Category MoM Comparison

    private var categoryMoMCard: some View {
        let comparisons = analytics.categoryComparisons
        let maxVal = max(1.0, comparisons.map { max($0.thisMonthAmount, $0.lastMonthAmount) }.max() ?? 1.0)

        return VStack(alignment: .leading, spacing: 12) {
            Text("Category-by-Category MoM Comparison")
                .font(.headline)
                .foregroundStyle(BrandColors.textPrimary)

            if comparisons.isEmpty {
                Text("No expense categories recorded in the current or previous month for this scope.")
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
            } else {
                VStack(spacing: 12) {
                    ForEach(comparisons) { item in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                HStack(spacing: 6) {
                                    Image(systemName: BrandColors.categoryIcon(for: item.category))
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(BrandColors.categoryColor(for: item.category))
                                    Text(item.category)
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(BrandColors.textPrimary)
                                }

                                Spacer()

                                if item.isSaving {
                                    StatusBadgePill(
                                        text: "Saved \(FormatUtils.currency(abs(item.deltaAmount)))",
                                        foreground: BrandColors.incomeGreen,
                                        background: BrandColors.incomeGreenBg
                                    )
                                } else if item.deltaAmount > 0.005 {
                                    StatusBadgePill(
                                        text: "+\(FormatUtils.currency(item.deltaAmount))",
                                        foreground: BrandColors.expenseRed,
                                        background: BrandColors.expenseRedBg
                                    )
                                } else {
                                    StatusBadgePill(
                                        text: "Same",
                                        foreground: BrandColors.textSecondary,
                                        background: BrandColors.inputContainerLight
                                    )
                                }
                            }

                            VStack(spacing: 4) {
                                comparisonBarRow(
                                    label: "This mo",
                                    amount: item.thisMonthAmount,
                                    ratio: item.thisMonthAmount / maxVal,
                                    color: BrandColors.navy900
                                )
                                comparisonBarRow(
                                    label: "Last mo",
                                    amount: item.lastMonthAmount,
                                    ratio: item.lastMonthAmount / maxVal,
                                    color: BrandColors.inputBorderUnfocused
                                )
                            }
                        }
                        .padding(12)
                        .background(BrandColors.inputContainerLight)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
        .cardSurface()
    }

    private func comparisonBarRow(label: String, amount: Double, ratio: Double, color: Color) -> some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(BrandColors.textSecondary)
                .frame(width: 48, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(BrandColors.cardBorderLight.opacity(0.6))
                        .frame(height: 7)
                    if amount > 0 {
                        Capsule()
                            .fill(color)
                            .frame(width: max(6, geo.size.width * min(1.0, ratio)), height: 7)
                    }
                }
            }
            .frame(height: 7)

            Text(FormatUtils.currency(amount))
                .font(.caption2.weight(.bold))
                .foregroundStyle(BrandColors.textPrimary)
                .frame(width: 72, alignment: .trailing)
        }
    }

    // MARK: - Payment Methods Breakdown Card

    private var paymentMethodsCard: some View {
        let methods = analytics.paymentMethods
        return VStack(alignment: .leading, spacing: 12) {
            Text("Payment Methods Breakdown (This Month)")
                .font(.headline)
                .foregroundStyle(BrandColors.textPrimary)

            if methods.isEmpty {
                Text("No spending methods logged this month yet.")
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
            } else {
                VStack(spacing: 10) {
                    ForEach(methods) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(item.method)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(BrandColors.textPrimary)
                                Text("(\(item.count))")
                                    .font(.caption)
                                    .foregroundStyle(BrandColors.textTertiary)
                                Spacer()
                                Text(FormatUtils.currency(item.amount))
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.navy900)
                            }

                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(BrandColors.inputContainerLight)
                                        .frame(height: 8)
                                    Capsule()
                                        .fill(BrandColors.tealDark)
                                        .frame(width: max(8, geo.size.width * item.percentage), height: 8)
                                }
                            }
                            .frame(height: 8)
                        }
                    }
                }
            }
        }
        .cardSurface()
    }
}
