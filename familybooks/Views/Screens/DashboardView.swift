import SwiftUI

struct DashboardView: View {
    let store: LedgerStore
    let onOpenAddTransaction: (String) -> Void
    let onEditTransaction: (LedgerTransaction) -> Void
    let onOpenAddLiability: () -> Void
    let onOpenSplitExpense: () -> Void
    let onOpenRefillCash: () -> Void
    let onSwitchToLedger: () -> Void
    let onSwitchToAnalytics: () -> Void
    let onSwitchToDebts: () -> Void

    private var monthTotals: (income: Double, expense: Double, net: Double) {
        store.currentMonthTotals()
    }

    private var topCategories: [DashboardCategorySummary] {
        AnalyticsCalculator.dashboardTopCategories(transactions: store.transactions, limit: 5)
    }

    private var recentEntries: [LedgerTransaction] {
        Array(store.transactions.prefix(8))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                heroNetCashFlowCard
                cashInHandCard
                analyticsShortcutBanner
                quickActionTiles

                if !store.pendingLiabilities.isEmpty {
                    activeDebtsSummaryBanner
                }

                monthlyExpenseCategoriesCard
                recentEntriesSection
            }
            .padding(16)
            .padding(.bottom, 20)
        }
        .background(BrandColors.backgroundLight)
    }

    // MARK: - Hero Navy900 Card

    private var heroNetCashFlowCard: some View {
        let totals = monthTotals
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("THIS MONTH'S NET CASH FLOW")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.tealAccent)
                    .tracking(0.8)

                Spacer()

                Text(FormatUtils.monthYearLabel(
                    year: Calendar.current.component(.year, from: Date()),
                    oneBasedMonth: Calendar.current.component(.month, from: Date())
                ))
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white.opacity(0.75))
            }

            Text(FormatUtils.signedNetCurrency(totals.net))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(.white)

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.down.left.circle.fill")
                            .font(.caption)
                            .foregroundStyle(BrandColors.tealAccent)
                        Text("Total Income")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.white.opacity(0.82))
                    }
                    Text(FormatUtils.currency(totals.income))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(BrandColors.navy800)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.up.right.circle.fill")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "#FF8A80"))
                        Text("Total Expenses")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.white.opacity(0.82))
                    }
                    Text(FormatUtils.currency(totals.expense))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(BrandColors.navy800)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(18)
        .background(BrandColors.navy900)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: BrandColors.navy900.opacity(0.16), radius: 10, x: 0, y: 4)
    }

    // MARK: - Cash in Hand Card

    private var cashInHandCard: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(BrandColors.tealLight)
                    .frame(width: 44, height: 44)
                Image(systemName: "banknote.fill")
                    .font(.title3)
                    .foregroundStyle(BrandColors.tealDark)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text("Cash in Hand")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(BrandColors.textSecondary)
                Text(FormatUtils.currency(store.cashInHandBalance))
                    .font(.title3.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)
            }

            Spacer()

            Button(action: onOpenRefillCash) {
                Text("+ Refill / ATM")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.navy900)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(BrandColors.tealLight)
                    .clipShape(Capsule())
                    .overlay(
                        Capsule()
                            .stroke(BrandColors.tealDark.opacity(0.5), lineWidth: 1)
                    )
            }
            .buttonStyle(.plain)
        }
        .cardSurface(cornerRadius: 16, padding: 14)
    }

    // MARK: - Analytics Shortcut Banner

    private var analyticsShortcutBanner: some View {
        Button(action: onSwitchToAnalytics) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(BrandColors.navy900)
                        .frame(width: 38, height: 38)
                    Image(systemName: "chart.bar.xaxis")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(BrandColors.tealAccent)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Profile & Month-on-Month Analytics")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(BrandColors.textPrimary)
                    Text("Compare monthly spending, categories, and per-member breakdown")
                        .font(.caption)
                        .foregroundStyle(BrandColors.textSecondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.tealDark)
            }
            .padding(14)
            .background(BrandColors.tealLight.opacity(0.65))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(BrandColors.tealDark.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - 4 Quick Action Tiles

    private var quickActionTiles: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 10) {
            quickTile(
                title: "+ Expense",
                icon: "minus.circle.fill",
                tint: BrandColors.expenseRed,
                bg: BrandColors.expenseRedBg
            ) {
                onOpenAddTransaction(LedgerTransaction.typeExpense)
            }

            quickTile(
                title: "+ Income",
                icon: "plus.circle.fill",
                tint: BrandColors.incomeGreen,
                bg: BrandColors.incomeGreenBg
            ) {
                onOpenAddTransaction(LedgerTransaction.typeIncome)
            }

            quickTile(
                title: "+ Debt",
                icon: "doc.text.fill",
                tint: BrandColors.pendingOrange,
                bg: BrandColors.pendingOrangeBg
            ) {
                onOpenAddLiability()
            }

            quickTile(
                title: "Split Bill",
                icon: "person.2.fill",
                tint: BrandColors.tealDark,
                bg: BrandColors.tealLight
            ) {
                onOpenSplitExpense()
            }
        }
    }

    private func quickTile(
        title: String,
        icon: String,
        tint: Color,
        bg: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(bg)
                        .frame(width: 38, height: 38)
                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(tint)
                }
                Text(title)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .padding(.horizontal, 4)
            .background(BrandColors.surfaceLight)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(BrandColors.cardBorderLight, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Active Debts Summary Banner

    private var activeDebtsSummaryBanner: some View {
        Button(action: onSwitchToDebts) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.circle.fill")
                            .foregroundStyle(BrandColors.pendingOrange)
                        Text("Active Debts & Liabilities (\(store.pendingLiabilities.count))")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.pendingOrange)
                }

                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Family IOUs")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.textSecondary)
                        Text(FormatUtils.currency(store.totalPendingFamilyIOUs))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.navy900)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("3rd-Party Debt")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.textSecondary)
                        Text(FormatUtils.currency(store.totalPendingThirdPartyPayable))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.expenseRed)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(14)
            .background(BrandColors.pendingOrangeBg)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(BrandColors.pendingOrange.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Monthly Expense Categories Card

    private var monthlyExpenseCategoriesCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Monthly Expense Categories")
                .font(.headline)
                .foregroundStyle(BrandColors.textPrimary)

            if topCategories.isEmpty {
                Text("No expenses recorded this month yet. Tap \"+ Expense\" above to start tracking your family's spending.")
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
                    .padding(.vertical, 6)
            } else {
                VStack(spacing: 12) {
                    ForEach(topCategories) { item in
                        VStack(alignment: .leading, spacing: 5) {
                            HStack {
                                HStack(spacing: 6) {
                                    Image(systemName: BrandColors.categoryIcon(for: item.category))
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(BrandColors.categoryColor(for: item.category))
                                    Text(item.category)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(BrandColors.textPrimary)
                                }

                                Spacer()

                                Text(FormatUtils.currency(item.totalAmount))
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)
                            }

                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(BrandColors.inputContainerLight)
                                        .frame(height: 8)
                                    Capsule()
                                        .fill(BrandColors.categoryColor(for: item.category))
                                        .frame(width: max(8, geo.size.width * item.percentage), height: 8)
                                }
                            }
                            .frame(height: 8)

                            HStack {
                                Text("Top Payer: \(item.topPayer)")
                                    .font(.caption2.weight(.medium))
                                    .foregroundStyle(BrandColors.textSecondary)
                                Spacer()
                                Text("\(Int((item.percentage * 100).rounded()))%")
                                    .font(.caption2.weight(.semibold))
                                    .foregroundStyle(BrandColors.textTertiary)
                            }
                        }
                    }
                }
            }
        }
        .cardSurface()
    }

    // MARK: - Recent Family Ledger Entries

    private var recentEntriesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Family Ledger Entries")
                    .font(.headline)
                    .foregroundStyle(BrandColors.textPrimary)

                Spacer()

                if !store.transactions.isEmpty {
                    Button("View All", action: onSwitchToLedger)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.tealDark)
                }
            }

            if recentEntries.isEmpty {
                EmptyStateCard(
                    icon: "book.pages.fill",
                    title: "Your Family Ledger is Ready",
                    message: "Start fresh with $0.00 cash on hand. Add a family profile or log your first expense or income entry.",
                    actionTitle: "Add First Expense",
                    action: { onOpenAddTransaction(LedgerTransaction.typeExpense) }
                )
            } else {
                VStack(spacing: 10) {
                    ForEach(recentEntries, id: \.id) { tx in
                        Button {
                            onEditTransaction(tx)
                        } label: {
                            TransactionRowCard(
                                transaction: tx,
                                avatarColorHex: store.avatarColorHex(for: tx.paidBy)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}
