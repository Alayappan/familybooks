import SwiftUI

struct TransactionRowCard: View {
    let transaction: LedgerTransaction
    let avatarColorHex: String
    var showActionButtons: Bool = false
    var onEdit: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil

    private var categoryColor: Color {
        BrandColors.categoryColor(for: transaction.category, isIncome: transaction.isIncome)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(categoryColor.opacity(0.12))
                        .frame(width: 42, height: 42)
                    Image(systemName: BrandColors.categoryIcon(for: transaction.category, isIncome: transaction.isIncome))
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(categoryColor)
                }

                VStack(alignment: .leading, spacing: 5) {
                    HStack(alignment: .center, spacing: 6) {
                        Text(transaction.category.isEmpty ? "Other" : transaction.category)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)
                            .lineLimit(1)

                        StatusBadgePill(
                            text: transaction.isExpense ? "By \(transaction.paidBy)" : "To \(transaction.paidBy)",
                            foreground: .white,
                            background: Color(hex: avatarColorHex)
                        )
                    }

                    // Attribution badges
                    FlowLayout(spacing: 6) {
                        if transaction.isExpense && !transaction.forWhom.isEmpty {
                            StatusBadgePill(
                                text: "For: \(transaction.forWhom)",
                                foreground: BrandColors.blueBadge,
                                background: BrandColors.blueBadgeBg
                            )
                        }
                        if transaction.isIncome && !transaction.fromWhom.isEmpty {
                            StatusBadgePill(
                                text: "From: \(transaction.fromWhom)",
                                foreground: BrandColors.incomeGreen,
                                background: BrandColors.incomeGreenBg
                            )
                        }
                        if transaction.contributedAmount > 0 && !transaction.contributedBy.isEmpty {
                            StatusBadgePill(
                                text: "Share: \(transaction.contributedBy) (\(FormatUtils.currency(transaction.contributedAmount)))",
                                foreground: BrandColors.purpleBadge,
                                background: BrandColors.purpleBadgeBg
                            )
                        }
                    }

                    if !transaction.notes.isEmpty {
                        Text(transaction.notes)
                            .font(.caption)
                            .foregroundStyle(BrandColors.textSecondary)
                            .lineLimit(2)
                    }

                    HStack(spacing: 6) {
                        Text(FormatUtils.shortDate(transaction.date))
                        Text("•")
                        Text(transaction.paymentMethod)
                    }
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(BrandColors.textTertiary)
                }

                Spacer(minLength: 4)

                VStack(alignment: .trailing, spacing: 4) {
                    Text(FormatUtils.signedCurrency(transaction.amount, isExpense: transaction.isExpense))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(transaction.isExpense ? BrandColors.expenseRed : BrandColors.incomeGreen)
                        .fixedSize(horizontal: true, vertical: false)

                    if transaction.contributedAmount > 0 {
                        Text("Net \(FormatUtils.currency(transaction.netOutOfPocketAmount))")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(BrandColors.textTertiary)
                            .fixedSize(horizontal: true, vertical: false)
                    }
                }
            }

            if showActionButtons {
                Divider()
                    .overlay(BrandColors.cardBorderLight)

                HStack(spacing: 12) {
                    Spacer()

                    if let onEdit {
                        Button(action: onEdit) {
                            Label("Edit", systemImage: "pencil")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.navy900)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(BrandColors.inputContainerLight)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(BrandColors.cardBorderLight, lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }

                    if let onDelete {
                        Button(action: onDelete) {
                            Label("Delete", systemImage: "trash")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.expenseRed)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(BrandColors.expenseRedBg)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .cardSurface(cornerRadius: 14, padding: 14)
    }
}
