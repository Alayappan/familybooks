import SwiftUI

struct DebtsView: View {
    let store: LedgerStore
    let onOpenAddLiability: () -> Void
    let onOpenSplitExpense: () -> Void

    enum LiabilityFilter: String, CaseIterable, Identifiable {
        case all = "All Liabilities"
        case familyIOUs = "Family IOUs"
        case thirdParty = "3rd-Party Debts"

        var id: String { rawValue }
    }

    enum DebtsSubTab: String, CaseIterable, Identifiable {
        case pending = "Pending"
        case settled = "Settled"
        case byProfile = "By Profile"

        var id: String { rawValue }
    }

    @State private var typeFilter: LiabilityFilter = .all
    @State private var subTab: DebtsSubTab = .pending
    @State private var payingLiability: FamilyLiability?
    @State private var paymentAmountText: String = ""
    @State private var paymentError: String?
    @State private var liabilityToDelete: FamilyLiability?

    private var typeFilteredLiabilities: [FamilyLiability] {
        store.liabilities.filter { liab in
            switch typeFilter {
            case .all: return true
            case .familyIOUs: return liab.liabilityType == FamilyLiability.typeFamilyIOU
            case .thirdParty: return liab.isThirdPartyLiability
            }
        }
    }

    private var pendingFiltered: [FamilyLiability] {
        typeFilteredLiabilities.filter { !$0.isSettled }
    }

    private var settledFiltered: [FamilyLiability] {
        typeFilteredLiabilities.filter { $0.isSettled }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                // Header Actions
                HStack(spacing: 10) {
                    Button(action: onOpenAddLiability) {
                        Label("Add Debt / IOU", systemImage: "plus.circle.fill")
                    }
                    .buttonStyle(PrimaryButtonStyle())

                    Button(action: onOpenSplitExpense) {
                        Label("Split Expense", systemImage: "person.2.fill")
                    }
                    .buttonStyle(SecondaryButtonStyle())
                }

                // Liability Type Filter Chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(LiabilityFilter.allCases) { filter in
                            SelectableChip(
                                title: filter.rawValue,
                                isSelected: typeFilter == filter,
                                accentColor: filter == .thirdParty ? BrandColors.expenseRed : BrandColors.navy900
                            ) {
                                typeFilter = filter
                            }
                        }
                    }
                }

                // 3 Sub-tabs Segmented Picker
                HStack(spacing: 6) {
                    subTabButton(.pending, label: "Pending (\(pendingFiltered.count))")
                    subTabButton(.settled, label: "Settled (\(settledFiltered.count))")
                    subTabButton(.byProfile, label: "By Profile")
                }
                .padding(4)
                .background(BrandColors.surfaceLight)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(BrandColors.cardBorderLight, lineWidth: 1)
                )

                // Sub-tab Content
                switch subTab {
                case .pending:
                    if pendingFiltered.isEmpty {
                        EmptyStateCard(
                            icon: "checkmark.seal.fill",
                            title: "No Pending Debts or IOUs",
                            message: "Track family IOUs, split bills, or 3rd-party debts & receivables here. Tap \"Add Debt / IOU\" to create one.",
                            actionTitle: "Add Debt / IOU",
                            action: onOpenAddLiability
                        )
                    } else {
                        LazyVStack(spacing: 12) {
                            ForEach(pendingFiltered, id: \.id) { liab in
                                liabilityCard(liab)
                            }
                        }
                    }

                case .settled:
                    if settledFiltered.isEmpty {
                        EmptyStateCard(
                            icon: "archivebox.fill",
                            title: "No Settled Records Yet",
                            message: "Once a pending debt or family IOU is paid in full or marked settled, it will appear here."
                        )
                    } else {
                        LazyVStack(spacing: 12) {
                            ForEach(settledFiltered, id: \.id) { liab in
                                liabilityCard(liab)
                            }
                        }
                    }

                case .byProfile:
                    byProfileSection
                }
            }
            .padding(16)
            .padding(.bottom, 20)
        }
        .background(BrandColors.backgroundLight)
        .sheet(item: $payingLiability) { liab in
            partialPaymentSheet(for: liab)
                .presentationDetents([.medium])
        }
        .alert(
            "Delete Liability?",
            isPresented: Binding(
                get: { liabilityToDelete != nil },
                set: { if !$0 { liabilityToDelete = nil } }
            ),
            presenting: liabilityToDelete
        ) { liab in
            Button("Delete", role: .destructive) {
                store.deleteLiability(liab)
                store.showToast("Liability deleted")
                liabilityToDelete = nil
            }
            Button("Cancel", role: .cancel) {
                liabilityToDelete = nil
            }
        } message: { liab in
            Text("Delete \"\(liab.title)\" (\(FormatUtils.currency(liab.amount)))?")
        }
    }

    private func subTabButton(_ tab: DebtsSubTab, label: String) -> some View {
        let selected = subTab == tab
        return Button {
            subTab = tab
        } label: {
            Text(label)
                .font(.caption.weight(.bold))
                .foregroundStyle(selected ? .white : BrandColors.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background(selected ? BrandColors.navy900 : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Liability Card

    private func liabilityCard(_ liab: FamilyLiability) -> some View {
        let badgeColors: (fg: Color, bg: Color) = {
            switch liab.liabilityType {
            case FamilyLiability.typeThirdPartyPayable:
                return (BrandColors.expenseRed, BrandColors.expenseRedBg)
            case FamilyLiability.typeThirdPartyReceivable:
                return (BrandColors.incomeGreen, BrandColors.incomeGreenBg)
            default:
                return (BrandColors.navy900, BrandColors.tealLight)
            }
        }()

        return VStack(alignment: .leading, spacing: 12) {
            // Top badges
            HStack(spacing: 6) {
                StatusBadgePill(
                    text: liab.typeDisplayLabel,
                    foreground: badgeColors.fg,
                    background: badgeColors.bg
                )

                if !liab.linkedProfile.isEmpty {
                    StatusBadgePill(
                        text: "Profile: \(liab.linkedProfile)",
                        foreground: BrandColors.blueBadge,
                        background: BrandColors.blueBadgeBg
                    )
                }

                Spacer()

                if liab.isSettled {
                    let settledDateText = liab.settledAt.map { FormatUtils.shortDate($0) } ?? FormatUtils.shortDate(liab.updatedAt)
                    StatusBadgePill(
                        text: "Settled • \(settledDateText)",
                        foreground: BrandColors.incomeGreen,
                        background: BrandColors.incomeGreenBg,
                        icon: "checkmark.circle.fill"
                    )
                } else if let due = liab.dueDate {
                    StatusBadgePill(
                        text: "Due \(FormatUtils.shortDate(due))",
                        foreground: BrandColors.pendingOrange,
                        background: BrandColors.pendingOrangeBg,
                        icon: "calendar"
                    )
                }
            }

            // Title, Date & Amounts
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(liab.title)
                        .font(.headline)
                        .foregroundStyle(BrandColors.textPrimary)
                    Text("Created \(FormatUtils.shortDate(liab.date))")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(BrandColors.textTertiary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text(FormatUtils.currency(liab.isSettled ? liab.amount : liab.remainingAmount))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(liab.isSettled ? BrandColors.incomeGreen : BrandColors.textPrimary)

                    if !liab.isSettled && liab.paidAmount > 0.005 {
                        Text("of \(FormatUtils.currency(liab.amount))")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.textSecondary)
                    }
                }
            }

            // Partial Repayment Progress Bar
            if !liab.isSettled && liab.paidAmount > 0.005 {
                VStack(alignment: .leading, spacing: 4) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(BrandColors.inputContainerLight)
                                .frame(height: 8)
                            Capsule()
                                .fill(BrandColors.incomeGreen)
                                .frame(width: max(8, geo.size.width * liab.repaymentProgress), height: 8)
                        }
                    }
                    .frame(height: 8)

                    HStack {
                        Text("Paid \(FormatUtils.currency(liab.paidAmount))")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.incomeGreen)
                        Spacer()
                        Text("\(Int((liab.repaymentProgress * 100).rounded()))% repaid")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.textSecondary)
                    }
                }
            }

            // Debtor -> Creditor Pill Row
            HStack(spacing: 8) {
                HStack(spacing: 6) {
                    ProfileAvatarCircle(
                        name: liab.fromMember,
                        colorHex: store.avatarColorHex(for: liab.fromMember),
                        size: 24
                    )
                    Text(liab.fromMember)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.textPrimary)
                        .lineLimit(1)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(BrandColors.inputContainerLight)
                .clipShape(Capsule())

                Image(systemName: "arrow.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(BrandColors.textSecondary)

                HStack(spacing: 6) {
                    ProfileAvatarCircle(
                        name: liab.toMember,
                        colorHex: store.avatarColorHex(for: liab.toMember),
                        size: 24
                    )
                    Text(liab.toMember)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.textPrimary)
                        .lineLimit(1)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(BrandColors.inputContainerLight)
                .clipShape(Capsule())

                Spacer(minLength: 0)
            }

            if !liab.notes.isEmpty {
                Text(liab.notes)
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
            }

            Divider().overlay(BrandColors.cardBorderLight)

            // Action Buttons
            HStack(spacing: 8) {
                if !liab.isSettled {
                    Button {
                        paymentError = nil
                        paymentAmountText = String(format: "%.2f", liab.remainingAmount)
                        payingLiability = liab
                    } label: {
                        Label("Pay", systemImage: "dollarsign.circle.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(BrandColors.navy900)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)

                    Button {
                        store.settleLiability(liab)
                        store.showToast("Marked \"\(liab.title)\" settled")
                    } label: {
                        Label("Settle", systemImage: "checkmark.circle.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(BrandColors.incomeGreen)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(BrandColors.incomeGreenBg)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }

                Spacer()

                Button {
                    liabilityToDelete = liab
                } label: {
                    Image(systemName: "trash")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.expenseRed)
                        .padding(8)
                        .background(BrandColors.expenseRedBg)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Delete \(liab.title)")
            }
        }
        .cardSurface(cornerRadius: 16, padding: 14)
    }

    // MARK: - By Profile Sub-tab

    private var byProfileSection: some View {
        let summaries = store.profileFinancialSummaries
        return Group {
            if summaries.isEmpty {
                EmptyStateCard(
                    icon: "person.2.fill",
                    title: "No Profiles to Compare Yet",
                    message: "Add family profiles or record liabilities to see each member's net standing."
                )
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(summaries) { summary in
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 10) {
                                ProfileAvatarCircle(name: summary.name, colorHex: summary.avatarColorHex, size: 38)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(summary.name)
                                        .font(.headline)
                                        .foregroundStyle(BrandColors.textPrimary)
                                    Text(summary.role)
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(BrandColors.textSecondary)
                                }

                                Spacer()

                                VStack(alignment: .trailing, spacing: 2) {
                                    Text("Net Standing")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(BrandColors.textSecondary)
                                    Text(FormatUtils.signedNetCurrency(summary.netLiabilityStanding))
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(summary.netLiabilityStanding >= 0 ? BrandColors.incomeGreen : BrandColors.expenseRed)
                                }
                            }

                            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 4), spacing: 6) {
                                standingMetric(title: "Family Owed", amount: summary.familyOwedToThem, color: BrandColors.incomeGreen)
                                standingMetric(title: "Owes Family", amount: summary.familyTheyOwe, color: BrandColors.pendingOrange)
                                standingMetric(title: "3rd-Party Debt", amount: summary.thirdPartyTheyOwe, color: BrandColors.expenseRed)
                                standingMetric(title: "3rd-Party Owed", amount: summary.thirdPartyOwedToThem, color: BrandColors.tealDark)
                            }
                        }
                        .cardSurface(cornerRadius: 16, padding: 14)
                    }
                }
            }
        }
    }

    private func standingMetric(title: String, amount: Double, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(BrandColors.textSecondary)
                .lineLimit(2)
                .minimumScaleFactor(0.85)
            Text(FormatUtils.currency(amount))
                .font(.caption.weight(.bold))
                .foregroundStyle(color)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(BrandColors.inputContainerLight)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    // MARK: - Record Partial Payment Sheet

    private func partialPaymentSheet(for liab: FamilyLiability) -> some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(liab.title)
                        .font(.headline)
                        .foregroundStyle(BrandColors.textPrimary)
                    Text("\(liab.fromMember) → \(liab.toMember)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BrandColors.textSecondary)
                    Text("Remaining Balance: \(FormatUtils.currency(liab.remainingAmount)) of \(FormatUtils.currency(liab.amount))")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.tealDark)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardSurface(cornerRadius: 14, padding: 14)

                StyledTextField(
                    label: "Payment Amount (USD)",
                    placeholder: "0.00",
                    text: $paymentAmountText,
                    keyboardType: .decimalPad,
                    prefix: "$",
                    errorMessage: paymentError
                )

                HStack(spacing: 12) {
                    Button("Cancel") {
                        payingLiability = nil
                    }
                    .buttonStyle(SecondaryButtonStyle())

                    Button("Record Payment") {
                        guard let amount = FormatUtils.parseDecimalAmount(paymentAmountText), amount > 0 else {
                            paymentError = "Enter a valid payment amount greater than $0.00."
                            return
                        }
                        store.recordPartialPayment(liability: liab, paymentAmount: amount)
                        store.showToast(liab.isSettled ? "Debt settled in full" : "Partial payment recorded")
                        payingLiability = nil
                    }
                    .buttonStyle(PrimaryButtonStyle(backgroundColor: BrandColors.tealDark))
                }

                Spacer()
            }
            .padding(16)
            .navigationTitle("Record Payment")
            .navigationBarTitleDisplayMode(.inline)
        }
        .familyBooksSheetStyle()
    }
}
