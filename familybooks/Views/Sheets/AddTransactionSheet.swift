import SwiftUI

struct AddTransactionSheet: View {
    let store: LedgerStore
    let initialType: String
    let editingTransaction: LedgerTransaction?

    @Environment(\.dismiss) private var dismiss

    @State private var type: String
    @State private var amountText: String
    @State private var date: Date
    @State private var category: String
    @State private var paymentMethod: String
    @State private var paidBy: String
    @State private var forWhom: String
    @State private var fromWhom: String
    @State private var hasShareContribution: Bool
    @State private var contributedBy: String
    @State private var contributedAmountText: String
    @State private var notes: String
    @State private var validationError: String?

    init(
        store: LedgerStore,
        initialType: String = LedgerTransaction.typeExpense,
        editingTransaction: LedgerTransaction? = nil
    ) {
        self.store = store
        self.initialType = initialType
        self.editingTransaction = editingTransaction

        if let tx = editingTransaction {
            _type = State(initialValue: tx.type)
            _amountText = State(initialValue: String(format: "%.2f", tx.amount))
            _date = State(initialValue: FormatUtils.date(fromEpochMs: tx.date))
            _category = State(initialValue: tx.category)
            _paymentMethod = State(initialValue: tx.paymentMethod)
            _paidBy = State(initialValue: tx.paidBy)
            _forWhom = State(initialValue: tx.forWhom)
            _fromWhom = State(initialValue: tx.fromWhom)
            _hasShareContribution = State(initialValue: tx.contributedAmount > 0 || !tx.contributedBy.isEmpty)
            _contributedBy = State(initialValue: tx.contributedBy)
            _contributedAmountText = State(initialValue: tx.contributedAmount > 0 ? String(format: "%.2f", tx.contributedAmount) : "")
            _notes = State(initialValue: tx.notes)
        } else {
            let isInc = initialType == LedgerTransaction.typeIncome
            _type = State(initialValue: isInc ? LedgerTransaction.typeIncome : LedgerTransaction.typeExpense)
            _amountText = State(initialValue: "")
            _date = State(initialValue: Date())
            _category = State(initialValue: isInc ? "Salary" : "Groceries")
            _paymentMethod = State(initialValue: "Card")
            _paidBy = State(initialValue: store.defaultPayerName)
            _forWhom = State(initialValue: "Shared Family")
            _fromWhom = State(initialValue: "Employer")
            _hasShareContribution = State(initialValue: false)
            _contributedBy = State(initialValue: "")
            _contributedAmountText = State(initialValue: "")
            _notes = State(initialValue: "")
        }
    }

    private var isExpense: Bool {
        type == LedgerTransaction.typeExpense
    }

    private var parsedAmount: Double {
        FormatUtils.parseDecimalAmount(amountText) ?? 0.0
    }

    private var parsedShareAmount: Double {
        guard hasShareContribution else { return 0.0 }
        return FormatUtils.parseDecimalAmount(contributedAmountText) ?? 0.0
    }

    private var categories: [String] {
        isExpense ? LedgerTransaction.expenseCategories : LedgerTransaction.incomeCategories
    }

    private var profileNames: [String] {
        store.profiles.map { $0.name }
    }

    private var shareCandidateNames: [String] {
        profileNames.filter { !AnalyticsCalculator.matchesName($0, paidBy) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // Expense / Income Toggle
                    HStack(spacing: 10) {
                        typeToggleButton(
                            title: "Expense",
                            icon: "arrow.up.right.circle.fill",
                            selected: isExpense,
                            activeColor: BrandColors.expenseRed
                        ) {
                            if !isExpense {
                                type = LedgerTransaction.typeExpense
                                if !LedgerTransaction.expenseCategories.contains(category) {
                                    category = "Groceries"
                                }
                            }
                        }

                        typeToggleButton(
                            title: "Income",
                            icon: "arrow.down.left.circle.fill",
                            selected: !isExpense,
                            activeColor: BrandColors.incomeGreen
                        ) {
                            if isExpense {
                                type = LedgerTransaction.typeIncome
                                if !LedgerTransaction.incomeCategories.contains(category) {
                                    category = "Salary"
                                }
                            }
                        }
                    }

                    // Amount & Date Card
                    VStack(alignment: .leading, spacing: 14) {
                        StyledTextField(
                            label: "Amount (USD)",
                            placeholder: "0.00",
                            text: $amountText,
                            keyboardType: .decimalPad,
                            prefix: "$"
                        )

                        VStack(alignment: .leading, spacing: 6) {
                            Text("Date")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)

                            DatePicker(
                                "Entry Date",
                                selection: $date,
                                displayedComponents: [.date]
                            )
                            .datePickerStyle(.compact)
                            .labelsHidden()
                            .tint(BrandColors.navy900)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(BrandColors.inputContainerLight)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(BrandColors.inputBorderUnfocused, lineWidth: 1.1)
                            )
                        }
                    }
                    .cardSurface()

                    // Paid By / Received By Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text(isExpense ? "Paid By" : "Received By")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        FlowLayout(spacing: 8) {
                            ForEach(profileNames, id: \.self) { name in
                                SelectableChip(
                                    title: name,
                                    isSelected: AnalyticsCalculator.matchesName(paidBy, name)
                                ) {
                                    paidBy = name
                                }
                            }
                            SelectableChip(
                                title: "Family",
                                isSelected: AnalyticsCalculator.matchesName(paidBy, "Family")
                            ) {
                                paidBy = "Family"
                            }
                        }

                        StyledTextField(
                            label: isExpense ? "Payer Name" : "Receiver Name",
                            placeholder: "Enter name or select above",
                            text: $paidBy
                        )
                    }
                    .cardSurface()

                    // Attribution Card (For Whom / From Whom)
                    VStack(alignment: .leading, spacing: 12) {
                        if isExpense {
                            Text("Expense For Whom (Beneficiary)")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(BrandColors.textPrimary)

                            FlowLayout(spacing: 8) {
                                SelectableChip(
                                    title: "Shared Family",
                                    isSelected: AnalyticsCalculator.matchesName(forWhom, "Shared Family"),
                                    accentColor: BrandColors.blueBadge
                                ) {
                                    forWhom = "Shared Family"
                                }
                                ForEach(profileNames, id: \.self) { name in
                                    SelectableChip(
                                        title: name,
                                        isSelected: AnalyticsCalculator.matchesName(forWhom, name),
                                        accentColor: BrandColors.blueBadge
                                    ) {
                                        forWhom = name
                                    }
                                }
                            }

                            StyledTextField(
                                label: "Beneficiary",
                                placeholder: "Shared Family, member name, or custom",
                                text: $forWhom
                            )
                        } else {
                            Text("Income From Whom (Source)")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(BrandColors.textPrimary)

                            FlowLayout(spacing: 8) {
                                ForEach(LedgerTransaction.incomeSourcePresets, id: \.self) { preset in
                                    SelectableChip(
                                        title: preset,
                                        isSelected: AnalyticsCalculator.matchesName(fromWhom, preset),
                                        accentColor: BrandColors.incomeGreen
                                    ) {
                                        fromWhom = preset
                                    }
                                }
                                ForEach(profileNames, id: \.self) { name in
                                    SelectableChip(
                                        title: name,
                                        isSelected: AnalyticsCalculator.matchesName(fromWhom, name),
                                        accentColor: BrandColors.incomeGreen
                                    ) {
                                        fromWhom = name
                                    }
                                }
                            }

                            StyledTextField(
                                label: "Income Source",
                                placeholder: "Employer, client, member, or custom",
                                text: $fromWhom
                            )
                        }
                    }
                    .cardSurface()

                    // Share / Contribution by Other Toggle Card
                    VStack(alignment: .leading, spacing: 12) {
                        Toggle(isOn: $hasShareContribution) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Share / Contribution by Other")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)
                                Text("Track when another person contributed part of this entry")
                                    .font(.caption)
                                    .foregroundStyle(BrandColors.textSecondary)
                            }
                        }
                        .tint(BrandColors.tealDark)

                        if hasShareContribution {
                            Divider()
                                .overlay(BrandColors.cardBorderLight)

                            if !shareCandidateNames.isEmpty {
                                FlowLayout(spacing: 8) {
                                    ForEach(shareCandidateNames, id: \.self) { name in
                                        SelectableChip(
                                            title: name,
                                            isSelected: AnalyticsCalculator.matchesName(contributedBy, name),
                                            accentColor: BrandColors.purpleBadge
                                        ) {
                                            contributedBy = name
                                        }
                                    }
                                }
                            }

                            StyledTextField(
                                label: "Contributed By",
                                placeholder: "Who contributed a share?",
                                text: $contributedBy
                            )

                            StyledTextField(
                                label: "Share Amount",
                                placeholder: "0.00",
                                text: $contributedAmountText,
                                keyboardType: .decimalPad,
                                prefix: "$"
                            )

                            if parsedAmount > 0 {
                                let netOwner = max(0.0, parsedAmount - min(parsedAmount, parsedShareAmount))
                                let ownerLabel = paidBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Owner" : paidBy
                                HStack(spacing: 8) {
                                    Image(systemName: "person.2.fill")
                                        .foregroundStyle(BrandColors.tealDark)
                                    Text("Net by \(ownerLabel): \(FormatUtils.currency(netOwner)) • Share: \(FormatUtils.currency(min(parsedAmount, parsedShareAmount)))")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(BrandColors.navy900)
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(BrandColors.tealLight)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }
                        }
                    }
                    .cardSurface()

                    // Category Selection
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Category")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        FlowLayout(spacing: 8) {
                            ForEach(categories, id: \.self) { cat in
                                SelectableChip(
                                    title: cat,
                                    isSelected: category == cat,
                                    icon: BrandColors.categoryIcon(for: cat, isIncome: !isExpense),
                                    accentColor: BrandColors.categoryColor(for: cat, isIncome: !isExpense)
                                ) {
                                    category = cat
                                }
                            }
                        }
                    }
                    .cardSurface()

                    // Payment Method Selection
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Payment Method")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        FlowLayout(spacing: 8) {
                            ForEach(LedgerTransaction.paymentMethods, id: \.self) { method in
                                SelectableChip(
                                    title: method,
                                    isSelected: paymentMethod == method
                                ) {
                                    paymentMethod = method
                                }
                            }
                        }
                    }
                    .cardSurface()

                    // Notes
                    VStack(alignment: .leading, spacing: 12) {
                        StyledTextField(
                            label: "Notes (Optional)",
                            placeholder: "Add receipt details, store name, or context",
                            text: $notes
                        )
                    }
                    .cardSurface()

                    if let validationError {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(BrandColors.expenseRed)
                            Text(validationError)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.expenseRed)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(BrandColors.expenseRedBg)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }

                    // Action Buttons
                    HStack(spacing: 12) {
                        Button("Cancel") {
                            dismiss()
                        }
                        .buttonStyle(SecondaryButtonStyle())

                        Button(editingTransaction == nil ? "Save Entry" : "Update Entry") {
                            saveEntry()
                        }
                        .buttonStyle(PrimaryButtonStyle(backgroundColor: isExpense ? BrandColors.navy900 : BrandColors.incomeGreen))
                    }
                    .padding(.bottom, 16)
                }
                .padding(16)
            }
            .navigationTitle(editingTransaction == nil ? "Add Ledger Entry" : "Edit Ledger Entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundStyle(BrandColors.navy900)
                }
            }
        }
        .familyBooksSheetStyle()
    }

    private func typeToggleButton(
        title: String,
        icon: String,
        selected: Bool,
        activeColor: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.subheadline.weight(.semibold))
                Text(title)
                    .font(.subheadline.weight(.bold))
            }
            .foregroundStyle(selected ? .white : BrandColors.textPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(selected ? activeColor : BrandColors.surfaceLight)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(selected ? activeColor : BrandColors.inputBorderUnfocused, lineWidth: 1.2)
            )
        }
        .buttonStyle(.plain)
    }

    private func saveEntry() {
        guard let amount = FormatUtils.parseDecimalAmount(amountText), amount > 0 else {
            validationError = "Please enter a valid amount greater than $0.00."
            return
        }

        let cleanPaidBy = paidBy.trimmingCharacters(in: .whitespacesAndNewlines)
        let finalPaidBy = cleanPaidBy.isEmpty ? "Family" : cleanPaidBy

        var finalContribBy = ""
        var finalContribAmount = 0.0

        if hasShareContribution {
            let share = FormatUtils.parseDecimalAmount(contributedAmountText) ?? 0.0
            if share < 0 || share > amount {
                validationError = "Share amount must be between $0.00 and \(FormatUtils.currency(amount))."
                return
            }
            if share > 0 {
                let cleanContrib = contributedBy.trimmingCharacters(in: .whitespacesAndNewlines)
                if cleanContrib.isEmpty {
                    validationError = "Please specify who contributed the share amount."
                    return
                }
                finalContribBy = cleanContrib
                finalContribAmount = share
            }
        }

        let dateMs = FormatUtils.epochMs(from: date)
        let finalForWhom = isExpense ? (forWhom.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Shared Family" : forWhom) : ""
        let finalFromWhom = isExpense ? "" : fromWhom

        if let existing = editingTransaction {
            store.updateTransaction(
                existing,
                type: type,
                amount: amount,
                date: dateMs,
                category: category,
                paymentMethod: paymentMethod,
                paidBy: finalPaidBy,
                forWhom: finalForWhom,
                fromWhom: finalFromWhom,
                contributedBy: finalContribBy,
                contributedAmount: finalContribAmount,
                notes: notes
            )
            store.showToast("Ledger entry updated")
        } else {
            store.addTransaction(
                type: type,
                amount: amount,
                date: dateMs,
                category: category,
                paymentMethod: paymentMethod,
                paidBy: finalPaidBy,
                forWhom: finalForWhom,
                fromWhom: finalFromWhom,
                contributedBy: finalContribBy,
                contributedAmount: finalContribAmount,
                notes: notes
            )
            store.showToast(isExpense ? "Expense added" : "Income added")
        }
        dismiss()
    }
}
