import SwiftUI

struct RefillCashSheet: View {
    let store: LedgerStore

    @Environment(\.dismiss) private var dismiss

    enum CashActionMode: String, CaseIterable, Identifiable {
        case refill = "+ Refill Cash"
        case setBaseline = "Set Baseline Balance"

        var id: String { rawValue }
    }

    @State private var mode: CashActionMode = .refill
    @State private var amountText: String = ""
    @State private var noteText: String = ""
    @State private var validationError: String?

    private let refillPresets: [Double] = [50, 100, 200, 500]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Current Cash Available Hero
                    VStack(alignment: .leading, spacing: 6) {
                        Text("CURRENT CASH AVAILABLE")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(BrandColors.tealAccent)
                            .tracking(0.8)

                        Text(FormatUtils.currency(store.cashInHandBalance))
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundStyle(.white)

                        Text("Baseline \(FormatUtils.currency(store.cashPoolBaseline)) + Cash Income − Cash Expenses")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.78))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(18)
                    .background(BrandColors.navy900)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                    // Mode Toggle
                    HStack(spacing: 10) {
                        ForEach(CashActionMode.allCases) { item in
                            let selected = mode == item
                            Button {
                                mode = item
                                validationError = nil
                                if item == .setBaseline && amountText.isEmpty {
                                    amountText = String(format: "%.2f", max(0, store.cashInHandBalance))
                                } else if item == .refill {
                                    amountText = ""
                                }
                            } label: {
                                Text(item.rawValue)
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(selected ? .white : BrandColors.textPrimary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 11)
                                    .background(selected ? BrandColors.navy900 : BrandColors.surfaceLight)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(selected ? BrandColors.navy900 : BrandColors.inputBorderUnfocused, lineWidth: 1.1)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // Action Form
                    VStack(alignment: .leading, spacing: 14) {
                        if mode == .refill {
                            Text("Quick ATM / Wallet Presets")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)

                            HStack(spacing: 8) {
                                ForEach(refillPresets, id: \.self) { preset in
                                    Button("+\(Int(preset))") {
                                        let current = FormatUtils.parseDecimalAmount(amountText) ?? 0.0
                                        amountText = String(format: "%.2f", current + preset)
                                    }
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(BrandColors.tealDark)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 9)
                                    .background(BrandColors.tealLight)
                                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                                            .stroke(BrandColors.tealDark.opacity(0.4), lineWidth: 1)
                                    )
                                }
                            }

                            StyledTextField(
                                label: "Refill Amount (USD)",
                                placeholder: "0.00",
                                text: $amountText,
                                keyboardType: .decimalPad,
                                prefix: "$"
                            )

                            StyledTextField(
                                label: "Source / Note (Optional)",
                                placeholder: "e.g., ATM Withdrawal, Cash from Bank",
                                text: $noteText
                            )

                            Text("Logs a Cash Income entry from \"ATM / Cash Wallet Refill\" under \(store.defaultPayerName).")
                                .font(.caption)
                                .foregroundStyle(BrandColors.textSecondary)
                        } else {
                            StyledTextField(
                                label: "Actual Cash Counted On Hand (USD)",
                                placeholder: "0.00",
                                text: $amountText,
                                keyboardType: .decimalPad,
                                prefix: "$"
                            )

                            Text("Adjusts your starting cash pool baseline so Cash in Hand matches this exact amount without creating a new transaction.")
                                .font(.caption)
                                .foregroundStyle(BrandColors.textSecondary)
                        }
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

                        Button(mode == .refill ? "Add Cash Refill" : "Update Baseline") {
                            submit()
                        }
                        .buttonStyle(PrimaryButtonStyle(backgroundColor: mode == .refill ? BrandColors.tealDark : BrandColors.navy900))
                    }
                    .padding(.bottom, 16)
                }
                .padding(16)
            }
            .navigationTitle("Cash in Hand Pool")
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

    private func submit() {
        guard let amount = FormatUtils.parseDecimalAmount(amountText) else {
            validationError = "Please enter a valid dollar amount."
            return
        }

        if mode == .refill {
            guard amount > 0 else {
                validationError = "Refill amount must be greater than $0.00."
                return
            }
            store.refillCashPool(amount: amount, note: noteText)
            store.showToast("Refilled \(FormatUtils.currency(amount)) cash")
        } else {
            guard amount >= 0 else {
                validationError = "Cash balance cannot be negative."
                return
            }
            store.setCashPoolBaseline(newTotal: amount)
            store.showToast("Cash balance set to \(FormatUtils.currency(amount))")
        }
        dismiss()
    }
}
