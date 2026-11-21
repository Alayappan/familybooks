import SwiftUI

struct AddLiabilitySheet: View {
    let store: LedgerStore

    @Environment(\.dismiss) private var dismiss

    enum DuePreset: String, CaseIterable, Identifiable {
        case none = "No Due"
        case days7 = "7d"
        case days14 = "14d"
        case days30 = "30d"
        case days90 = "90d"

        var id: String { rawValue }

        var daysOffset: Int? {
            switch self {
            case .none: return nil
            case .days7: return 7
            case .days14: return 14
            case .days30: return 30
            case .days90: return 90
            }
        }
    }

    @State private var liabilityType: String = FamilyLiability.typeFamilyIOU
    @State private var title: String = ""
    @State private var amountText: String = ""
    @State private var debtorName: String
    @State private var creditorName: String
    @State private var duePreset: DuePreset = .none
    @State private var notes: String = ""
    @State private var validationError: String?

    init(store: LedgerStore) {
        self.store = store
        let defaultName = store.activeProfile?.name ?? store.profiles.first?.name ?? ""
        let secondName = store.profiles.count > 1 ? store.profiles[1].name : ""
        _debtorName = State(initialValue: defaultName)
        _creditorName = State(initialValue: secondName)
    }

    private var profileNames: [String] {
        store.profiles.map { $0.name }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // 3-way Type Selector
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Liability Type")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        HStack(spacing: 8) {
                            typeButton("Family IOU", value: FamilyLiability.typeFamilyIOU, accent: BrandColors.navy900)
                            typeButton("3rd-Party Debt", value: FamilyLiability.typeThirdPartyPayable, accent: BrandColors.expenseRed)
                            typeButton("3rd-Party Owed", value: FamilyLiability.typeThirdPartyReceivable, accent: BrandColors.incomeGreen)
                        }
                    }
                    .cardSurface()

                    // Title & Amount
                    VStack(alignment: .leading, spacing: 14) {
                        StyledTextField(
                            label: "Title",
                            placeholder: titlePlaceholder,
                            text: $title
                        )

                        StyledTextField(
                            label: "Total Amount (USD)",
                            placeholder: "0.00",
                            text: $amountText,
                            keyboardType: .decimalPad,
                            prefix: "$"
                        )
                    }
                    .cardSurface()

                    // Dynamic Party Fields
                    VStack(alignment: .leading, spacing: 16) {
                        if liabilityType == FamilyLiability.typeThirdPartyPayable {
                            // Debtor = Family Profile (sets linkedProfile & fromMember)
                            partyProfileSection(
                                header: "Family Member Who Owes (Debtor)",
                                fieldLabel: "Debtor Profile",
                                placeholder: "Select or enter family profile",
                                selection: $debtorName
                            )

                            Divider().overlay(BrandColors.cardBorderLight)

                            // External Creditor
                            VStack(alignment: .leading, spacing: 10) {
                                Text("External Creditor (Owed To)")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)

                                FlowLayout(spacing: 8) {
                                    ForEach(FamilyLiability.thirdPartyCreditorPresets, id: \.self) { preset in
                                        SelectableChip(
                                            title: preset,
                                            isSelected: AnalyticsCalculator.matchesName(creditorName, preset),
                                            accentColor: BrandColors.expenseRed
                                        ) {
                                            creditorName = preset
                                        }
                                    }
                                }

                                StyledTextField(
                                    label: "Creditor Name",
                                    placeholder: "e.g., Chase Card, Landlord, Auto Loan",
                                    text: $creditorName
                                )
                            }
                        } else if liabilityType == FamilyLiability.typeThirdPartyReceivable {
                            // Creditor = Family Profile (sets linkedProfile & toMember)
                            partyProfileSection(
                                header: "Family Member Who Is Owed (Creditor)",
                                fieldLabel: "Creditor Profile",
                                placeholder: "Select or enter family profile",
                                selection: $creditorName
                            )

                            Divider().overlay(BrandColors.cardBorderLight)

                            // External Debtor
                            StyledTextField(
                                label: "External Debtor (Who Owes)",
                                placeholder: "e.g., Friend, Tenant, Client, Insurance",
                                text: $debtorName
                            )
                        } else {
                            // FAMILY_IOU
                            partyProfileSection(
                                header: "Debtor (Who Owes)",
                                fieldLabel: "From Member",
                                placeholder: "Select or enter family member",
                                selection: $debtorName
                            )

                            Divider().overlay(BrandColors.cardBorderLight)

                            partyProfileSection(
                                header: "Creditor (Owed To)",
                                fieldLabel: "To Member",
                                placeholder: "Select or enter family member",
                                selection: $creditorName
                            )
                        }
                    }
                    .cardSurface()

                    // Due Date Presets
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Due Date")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        FlowLayout(spacing: 8) {
                            ForEach(DuePreset.allCases) { preset in
                                SelectableChip(
                                    title: preset.rawValue,
                                    isSelected: duePreset == preset,
                                    accentColor: BrandColors.navy900
                                ) {
                                    duePreset = preset
                                }
                            }
                        }
                    }
                    .cardSurface()

                    // Notes
                    VStack(alignment: .leading, spacing: 10) {
                        StyledTextField(
                            label: "Notes (Optional)",
                            placeholder: "Account number, context, or repayment plan",
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

                    HStack(spacing: 12) {
                        Button("Cancel") { dismiss() }
                            .buttonStyle(SecondaryButtonStyle())

                        Button("Save Liability") { saveLiability() }
                            .buttonStyle(PrimaryButtonStyle())
                    }
                    .padding(.bottom, 16)
                }
                .padding(16)
            }
            .navigationTitle("Add Debt / IOU")
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

    private var titlePlaceholder: String {
        switch liabilityType {
        case FamilyLiability.typeThirdPartyPayable:
            return "e.g., Car Repair Bill, Credit Card Balance"
        case FamilyLiability.typeThirdPartyReceivable:
            return "e.g., Security Deposit Refund, Freelance Invoice"
        default:
            return "e.g., Shared Groceries, Flight Tickets"
        }
    }

    private func typeButton(_ label: String, value: String, accent: Color) -> some View {
        let selected = liabilityType == value
        return Button {
            liabilityType = value
            validationError = nil
            if value == FamilyLiability.typeThirdPartyPayable {
                if debtorName.isEmpty {
                    debtorName = store.activeProfile?.name ?? store.profiles.first?.name ?? ""
                }
                if profileNames.contains(where: { AnalyticsCalculator.matchesName($0, creditorName) }) {
                    creditorName = ""
                }
            } else if value == FamilyLiability.typeThirdPartyReceivable {
                if creditorName.isEmpty {
                    creditorName = store.activeProfile?.name ?? store.profiles.first?.name ?? ""
                }
                if profileNames.contains(where: { AnalyticsCalculator.matchesName($0, debtorName) }) {
                    debtorName = ""
                }
            }
        } label: {
            Text(label)
                .font(.caption.weight(.bold))
                .multilineTextAlignment(.center)
                .foregroundStyle(selected ? .white : BrandColors.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .padding(.horizontal, 6)
                .background(selected ? accent : BrandColors.inputContainerLight)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(selected ? accent : BrandColors.inputBorderUnfocused, lineWidth: 1.1)
                )
        }
        .buttonStyle(.plain)
    }

    private func partyProfileSection(
        header: String,
        fieldLabel: String,
        placeholder: String,
        selection: Binding<String>
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(header)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(BrandColors.textPrimary)

            if !profileNames.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(profileNames, id: \.self) { name in
                        SelectableChip(
                            title: name,
                            isSelected: AnalyticsCalculator.matchesName(selection.wrappedValue, name)
                        ) {
                            selection.wrappedValue = name
                        }
                    }
                }
            }

            StyledTextField(
                label: fieldLabel,
                placeholder: placeholder,
                text: selection
            )
        }
    }

    private func saveLiability() {
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanTitle.isEmpty else {
            validationError = "Please enter a title for this debt or IOU."
            return
        }
        guard let amount = FormatUtils.parseDecimalAmount(amountText), amount > 0 else {
            validationError = "Please enter a valid amount greater than $0.00."
            return
        }

        let cleanDebtor = debtorName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanCreditor = creditorName.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanDebtor.isEmpty, !cleanCreditor.isEmpty else {
            validationError = "Please specify both the debtor and creditor."
            return
        }

        if liabilityType == FamilyLiability.typeFamilyIOU && AnalyticsCalculator.matchesName(cleanDebtor, cleanCreditor) {
            validationError = "For a Family IOU, the debtor and creditor must be different members."
            return
        }

        let linkedProfile: String
        switch liabilityType {
        case FamilyLiability.typeThirdPartyPayable:
            linkedProfile = cleanDebtor
        case FamilyLiability.typeThirdPartyReceivable:
            linkedProfile = cleanCreditor
        default:
            linkedProfile = ""
        }

        let now = Date()
        let dueDateMs: Int64?
        if let days = duePreset.daysOffset,
           let futureDate = Calendar.current.date(byAdding: .day, value: days, to: now) {
            dueDateMs = FormatUtils.epochMs(from: futureDate)
        } else {
            dueDateMs = nil
        }

        store.addLiability(
            title: cleanTitle,
            liabilityType: liabilityType,
            linkedProfile: linkedProfile,
            fromMember: cleanDebtor,
            toMember: cleanCreditor,
            amount: amount,
            date: FormatUtils.epochMs(from: now),
            dueDate: dueDateMs,
            notes: notes
        )
        store.showToast("Liability recorded")
        dismiss()
    }
}
