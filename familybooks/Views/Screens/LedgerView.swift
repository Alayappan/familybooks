import SwiftUI

struct LedgerView: View {
    let store: LedgerStore
    @Binding var selectedProfileFilter: String?
    let onOpenAddTransaction: () -> Void
    let onEditTransaction: (LedgerTransaction) -> Void
    let onOpenExportImport: () -> Void

    enum TypeFilter: String, CaseIterable, Identifiable {
        case all = "All Types"
        case expenses = "Expenses"
        case income = "Income"

        var id: String { rawValue }
    }

    @State private var searchText: String = ""
    @State private var typeFilter: TypeFilter = .all
    @State private var transactionToDelete: LedgerTransaction?

    private var filteredTransactions: [LedgerTransaction] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        return store.transactions.filter { tx in
            // Type filter
            switch typeFilter {
            case .all: break
            case .expenses:
                if !tx.isExpense { return false }
            case .income:
                if !tx.isIncome { return false }
            }

            // Profile filter (paidBy OR forWhom OR fromWhom OR contributedBy)
            if let profile = selectedProfileFilter, !profile.isEmpty {
                let matchesProfile =
                    AnalyticsCalculator.matchesName(tx.paidBy, profile)
                    || AnalyticsCalculator.forWhomIncludes(tx.forWhom, profileName: profile)
                    || AnalyticsCalculator.matchesName(tx.fromWhom, profile)
                    || AnalyticsCalculator.matchesName(tx.contributedBy, profile)
                if !matchesProfile { return false }
            }

            // Search text filter
            if !query.isEmpty {
                let amountString = String(format: "%.2f", tx.amount)
                let fields = [
                    tx.category,
                    tx.notes,
                    tx.paidBy,
                    tx.forWhom,
                    tx.fromWhom,
                    tx.contributedBy,
                    tx.paymentMethod,
                    amountString
                ]
                let matched = fields.contains { $0.lowercased().contains(query) }
                if !matched { return false }
            }

            return true
        }
    }

    private var totalOut: Double {
        filteredTransactions.filter { $0.isExpense }.reduce(0.0) { $0 + $1.amount }
    }

    private var totalIn: Double {
        filteredTransactions.filter { $0.isIncome }.reduce(0.0) { $0 + $1.amount }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // Search & Filters Header
                VStack(alignment: .leading, spacing: 10) {
                    StyledTextField(
                        label: "",
                        placeholder: "Search category, notes, payer, amount...",
                        text: $searchText,
                        icon: "magnifyingglass"
                    )

                    // Type Filter Chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(TypeFilter.allCases) { filter in
                                SelectableChip(
                                    title: filter.rawValue,
                                    isSelected: typeFilter == filter,
                                    accentColor: filter == .expenses ? BrandColors.expenseRed : (filter == .income ? BrandColors.incomeGreen : BrandColors.navy900)
                                ) {
                                    typeFilter = filter
                                }
                            }
                        }
                    }

                    // Profile Filter Chips
                    if !store.profiles.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                SelectableChip(
                                    title: "All Profiles",
                                    isSelected: selectedProfileFilter == nil,
                                    accentColor: BrandColors.tealDark
                                ) {
                                    selectedProfileFilter = nil
                                }

                                ForEach(store.profiles, id: \.id) { member in
                                    let isSelected = selectedProfileFilter.map { AnalyticsCalculator.matchesName($0, member.name) } ?? false
                                    SelectableChip(
                                        title: member.name,
                                        isSelected: isSelected,
                                        accentColor: Color(hex: member.avatarColorHex)
                                    ) {
                                        selectedProfileFilter = member.name
                                    }
                                }
                            }
                        }
                    }

                    // Summary Strip
                    HStack(spacing: 10) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(filteredTransactions.count) \(filteredTransactions.count == 1 ? "Record" : "Records") Found")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(BrandColors.textPrimary)

                            HStack(spacing: 6) {
                                Text("Out: \(FormatUtils.currency(totalOut))")
                                    .foregroundStyle(BrandColors.expenseRed)
                                Text("|")
                                    .foregroundStyle(BrandColors.textTertiary)
                                Text("In: \(FormatUtils.currency(totalIn))")
                                    .foregroundStyle(BrandColors.incomeGreen)
                            }
                            .font(.caption2.weight(.bold))
                        }

                        Spacer()

                        Button(action: onOpenExportImport) {
                            Label("Export / Import", systemImage: "arrow.up.arrow.down.circle.fill")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(BrandColors.navy900)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(BrandColors.tealLight)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(BrandColors.tealDark.opacity(0.45), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(10)
                    .background(BrandColors.surfaceLight)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(BrandColors.cardBorderLight, lineWidth: 1)
                    )
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 10)
                .background(BrandColors.backgroundLight)

                // Ledger List
                ScrollView {
                    if filteredTransactions.isEmpty {
                        EmptyStateCard(
                            icon: store.transactions.isEmpty ? "list.bullet.rectangle" : "line.3.horizontal.decrease.circle",
                            title: store.transactions.isEmpty ? "No Ledger Entries Yet" : "No Matching Entries",
                            message: store.transactions.isEmpty
                                ? "Tap the + button below to record your first family expense or income, or import a JSON/CSV backup."
                                : "Try clearing your search or switching your type or profile filter.",
                            actionTitle: store.transactions.isEmpty ? "Add First Entry" : "Clear Filters",
                            action: {
                                if store.transactions.isEmpty {
                                    onOpenAddTransaction()
                                } else {
                                    searchText = ""
                                    typeFilter = .all
                                    selectedProfileFilter = nil
                                }
                            }
                        )
                        .padding(16)
                    } else {
                        LazyVStack(spacing: 10) {
                            ForEach(filteredTransactions, id: \.id) { tx in
                                TransactionRowCard(
                                    transaction: tx,
                                    avatarColorHex: store.avatarColorHex(for: tx.paidBy),
                                    showActionButtons: true,
                                    onEdit: { onEditTransaction(tx) },
                                    onDelete: { transactionToDelete = tx }
                                )
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 4)
                        .padding(.bottom, 88)
                    }
                }
            }
            .background(BrandColors.backgroundLight)

            // Floating "+" Button
            Button(action: onOpenAddTransaction) {
                Image(systemName: "plus")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(BrandColors.navy900)
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(BrandColors.tealAccent, lineWidth: 2)
                    )
                    .shadow(color: BrandColors.navy900.opacity(0.28), radius: 8, x: 0, y: 4)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 18)
            .padding(.bottom, 18)
            .accessibilityLabel("Add Ledger Entry")
        }
        .alert(
            "Delete Ledger Entry?",
            isPresented: Binding(
                get: { transactionToDelete != nil },
                set: { if !$0 { transactionToDelete = nil } }
            ),
            presenting: transactionToDelete
        ) { tx in
            Button("Delete", role: .destructive) {
                store.deleteTransaction(tx)
                store.showToast("Entry deleted")
                transactionToDelete = nil
            }
            Button("Cancel", role: .cancel) {
                transactionToDelete = nil
            }
        } message: { tx in
            Text("Remove \(tx.category) (\(FormatUtils.currency(tx.amount))) from your family ledger?")
        }
    }
}
