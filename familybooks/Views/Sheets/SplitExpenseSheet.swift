import SwiftUI

struct SplitExpenseSheet: View {
    let store: LedgerStore
    var onRequestAddProfile: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss

    @State private var title: String = ""
    @State private var totalAmountText: String = ""
    @State private var paidBy: String
    @State private var selectedParticipants: Set<String>
    @State private var alsoLogExpense: Bool = true
    @State private var category: String = "Dining"
    @State private var paymentMethod: String = "Card"
    @State private var notes: String = ""
    @State private var validationError: String?

    init(store: LedgerStore, onRequestAddProfile: (() -> Void)? = nil) {
        self.store = store
        self.onRequestAddProfile = onRequestAddProfile
        let names = store.profiles.map { $0.name }
        let defaultPayer = store.activeProfile?.name ?? names.first ?? ""
        _paidBy = State(initialValue: defaultPayer)
        _selectedParticipants = State(initialValue: Set(names))
    }

    private var profileNames: [String] {
        store.profiles.map { $0.name }
    }

    private var orderedParticipants: [String] {
        profileNames.filter { selectedParticipants.contains($0) }
    }

    private var parsedAmount: Double {
        FormatUtils.parseDecimalAmount(totalAmountText) ?? 0.0
    }

    private var splitPreview: (perPersonShare: Double, debtorShares: [(debtor: String, amount: Double)]) {
        AnalyticsCalculator.computeSplitShares(
            totalAmount: parsedAmount,
            participants: orderedParticipants,
            paidBy: paidBy
        )
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if store.profiles.count < 2 {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 10) {
                                Image(systemName: "person.2.slash.fill")
                                    .font(.title3)
                                    .foregroundStyle(BrandColors.pendingOrange)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("At Least 2 Family Profiles Required")
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(BrandColors.textPrimary)
                                    Text("Create at least two family member profiles before splitting a bill.")
                                        .font(.caption)
                                        .foregroundStyle(BrandColors.textSecondary)
                                }
                            }

                            if let onRequestAddProfile {
                                Button("Add Family Profile") {
                                    dismiss()
                                    onRequestAddProfile()
                                }
                                .buttonStyle(PrimaryButtonStyle(backgroundColor: BrandColors.pendingOrange))
                            }
                        }
                        .padding(14)
                        .background(BrandColors.pendingOrangeBg)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(BrandColors.pendingOrange.opacity(0.4), lineWidth: 1)
                        )
                    }

                    // Bill Details
                    VStack(alignment: .leading, spacing: 14) {
                        StyledTextField(
                            label: "Expense Title",
                            placeholder: "e.g., Family Dinner, Monthly Utilities",
                            text: $title
                        )

                        StyledTextField(
                            label: "Total Bill Amount (USD)",
                            placeholder: "0.00",
                            text: $totalAmountText,
                            keyboardType: .decimalPad,
                            prefix: "$"
                        )
                    }
                    .cardSurface()

                    // Who Paid Upfront
                    if !profileNames.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Who Paid the Full Bill Upfront?")
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
                            }
                        }
                        .cardSurface()

                        // Participants Checklist
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Split Among Members (Min 2)")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)
                                Spacer()
                                Text("\(orderedParticipants.count) selected")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(BrandColors.tealDark)
                            }

                            VStack(spacing: 8) {
                                ForEach(store.profiles, id: \.id) { member in
                                    let isSelected = selectedParticipants.contains(member.name)
                                    Button {
                                        if isSelected {
                                            selectedParticipants.remove(member.name)
                                        } else {
                                            selectedParticipants.insert(member.name)
                                        }
                                    } label: {
                                        HStack(spacing: 12) {
                                            Image(systemName: isSelected ? "checkmark.square.fill" : "square")
                                                .font(.title3)
                                                .foregroundStyle(isSelected ? BrandColors.tealDark : BrandColors.inputBorderUnfocused)

                                            ProfileAvatarCircle(name: member.name, colorHex: member.avatarColorHex, size: 30)

                                            VStack(alignment: .leading, spacing: 1) {
                                                Text(member.name)
                                                    .font(.subheadline.weight(.semibold))
                                                    .foregroundStyle(BrandColors.textPrimary)
                                                Text(member.role)
                                                    .font(.caption2)
                                                    .foregroundStyle(BrandColors.textTertiary)
                                            }

                                            Spacer()

                                            if isSelected && splitPreview.perPersonShare > 0 {
                                                Text(FormatUtils.currency(splitPreview.perPersonShare))
                                                    .font(.caption.weight(.bold))
                                                    .foregroundStyle(BrandColors.navy900)
                                            }
                                        }
                                        .padding(10)
                                        .background(isSelected ? BrandColors.tealLight.opacity(0.55) : BrandColors.inputContainerLight)
                                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                                .stroke(isSelected ? BrandColors.tealDark : BrandColors.cardBorderLight, lineWidth: 1)
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .cardSurface()
                    }

                    // Also Log Expense Toggle
                    VStack(alignment: .leading, spacing: 12) {
                        Toggle(isOn: $alsoLogExpense) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Also Log Expense in Ledger")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)
                                Text("Records the full upfront expense paid by \(paidBy.isEmpty ? "payer" : paidBy)")
                                    .font(.caption)
                                    .foregroundStyle(BrandColors.textSecondary)
                            }
                        }
                        .tint(BrandColors.tealDark)

                        if alsoLogExpense {
                            Divider().overlay(BrandColors.cardBorderLight)

                            Text("Category")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)

                            FlowLayout(spacing: 8) {
                                ForEach(LedgerTransaction.expenseCategories, id: \.self) { cat in
                                    SelectableChip(
                                        title: cat,
                                        isSelected: category == cat,
                                        accentColor: BrandColors.categoryColor(for: cat)
                                    ) {
                                        category = cat
                                    }
                                }
                            }

                            Text("Payment Method")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)

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

                        StyledTextField(
                            label: "Notes (Optional)",
                            placeholder: "Optional bill notes",
                            text: $notes
                        )
                    }
                    .cardSurface()

                    // Live Each Member's Share Summary
                    if parsedAmount > 0 && orderedParticipants.count >= 2 {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text("Each Member's Share")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(.white)
                                Spacer()
                                Text(FormatUtils.currency(splitPreview.perPersonShare))
                                    .font(.title3.weight(.bold))
                                    .foregroundStyle(BrandColors.tealAccent)
                            }

                            Text("\(splitPreview.debtorShares.count) Family IOU\(splitPreview.debtorShares.count == 1 ? "" : "s") will be created:")
                                .font(.caption)
                                .foregroundStyle(.white.opacity(0.8))

                            ForEach(splitPreview.debtorShares, id: \.debtor) { share in
                                HStack {
                                    Text("\(share.debtor) owes \(paidBy)")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(.white)
                                    Spacer()
                                    Text(FormatUtils.currency(share.amount))
                                        .font(.caption.weight(.bold))
                                        .foregroundStyle(BrandColors.tealAccent)
                                }
                            }
                        }
                        .padding(16)
                        .background(BrandColors.navy900)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }

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

                    HStack(spacing: 12) {
                        Button("Cancel") { dismiss() }
                            .buttonStyle(SecondaryButtonStyle())

                        Button("Create Split") {
                            saveSplit()
                        }
                        .buttonStyle(PrimaryButtonStyle(isDisabled: store.profiles.count < 2))
                        .disabled(store.profiles.count < 2)
                    }
                    .padding(.bottom, 16)
                }
                .padding(16)
            }
            .navigationTitle("Split Family Expense")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(BrandColors.navy900)
                }
            }
        }
        .familyBooksSheetStyle()
    }

    private func saveSplit() {
        guard store.profiles.count >= 2 else {
            validationError = "Add at least two family profiles before splitting an expense."
            return
        }
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanTitle.isEmpty else {
            validationError = "Please enter an expense title."
            return
        }
        guard let amount = FormatUtils.parseDecimalAmount(totalAmountText), amount > 0 else {
            validationError = "Please enter a valid bill amount greater than $0.00."
            return
        }
        guard orderedParticipants.count >= 2 else {
            validationError = "Please select at least 2 participating family members."
            return
        }
        guard !paidBy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            validationError = "Please select who paid the bill upfront."
            return
        }

        let result = store.saveSplitExpense(
            title: cleanTitle,
            totalAmount: amount,
            paidBy: paidBy,
            participants: orderedParticipants,
            alsoLogExpense: alsoLogExpense,
            category: category,
            paymentMethod: paymentMethod,
            notes: notes
        )
        store.showToast("Split saved (\(result.createdLiabilities.count) IOU\(result.createdLiabilities.count == 1 ? "" : "s"))")
        dismiss()
    }
}
