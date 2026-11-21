import SwiftUI

struct CardSurfaceModifier: ViewModifier {
    var cornerRadius: CGFloat = 16
    var padding: CGFloat = 16

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(BrandColors.surfaceLight)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(BrandColors.cardBorderLight, lineWidth: 1)
            )
            .shadow(color: BrandColors.navy900.opacity(0.04), radius: 6, x: 0, y: 2)
    }
}

extension View {
    func cardSurface(cornerRadius: CGFloat = 16, padding: CGFloat = 16) -> some View {
        modifier(CardSurfaceModifier(cornerRadius: cornerRadius, padding: padding))
    }

    func familyBooksSheetStyle() -> some View {
        self
            .preferredColorScheme(.light)
            .background(BrandColors.backgroundLight.ignoresSafeArea())
    }
}

struct StyledTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var prefix: String? = nil
    var icon: String? = nil
    var errorMessage: String? = nil

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !label.isEmpty {
                Text(label)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(BrandColors.textSecondary)
            }

            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon)
                        .font(.subheadline)
                        .foregroundStyle(isFocused ? BrandColors.navy900 : BrandColors.textTertiary)
                }

                if let prefix {
                    Text(prefix)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(BrandColors.textSecondary)
                }

                TextField(
                    "",
                    text: $text,
                    prompt: Text(placeholder).foregroundStyle(BrandColors.textTertiary)
                )
                .font(.body)
                .foregroundStyle(BrandColors.textPrimary)
                .tint(BrandColors.navy900)
                .keyboardType(keyboardType)
                .textInputAutocapitalization(keyboardType == .decimalPad ? .never : .words)
                .autocorrectionDisabled(keyboardType == .decimalPad)
                .focused($isFocused)

                if !text.isEmpty {
                    Button {
                        text = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.subheadline)
                            .foregroundStyle(BrandColors.textTertiary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Clear \(label)")
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(isFocused ? BrandColors.surfaceLight : BrandColors.inputContainerLight)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(
                        errorMessage != nil
                            ? BrandColors.expenseRed
                            : (isFocused ? BrandColors.navy900 : BrandColors.inputBorderUnfocused),
                        lineWidth: isFocused ? 1.8 : 1.1
                    )
            )

            if let errorMessage, !errorMessage.isEmpty {
                Text(errorMessage)
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(BrandColors.expenseRed)
            }
        }
    }
}

struct StyledTextEditor: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var minHeight: CGFloat = 100
    var monospaced: Bool = false

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !label.isEmpty {
                Text(label)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(BrandColors.textSecondary)
            }

            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text(placeholder)
                        .font(monospaced ? .caption.monospaced() : .body)
                        .foregroundStyle(BrandColors.textTertiary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .allowsHitTesting(false)
                }

                TextEditor(text: $text)
                    .font(monospaced ? .caption.monospaced() : .body)
                    .foregroundStyle(BrandColors.textPrimary)
                    .tint(BrandColors.navy900)
                    .scrollContentBackground(.hidden)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .frame(minHeight: minHeight)
                    .focused($isFocused)
            }
            .background(isFocused ? BrandColors.surfaceLight : BrandColors.inputContainerLight)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(
                        isFocused ? BrandColors.navy900 : BrandColors.inputBorderUnfocused,
                        lineWidth: isFocused ? 1.8 : 1.1
                    )
            )
        }
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var backgroundColor: Color = BrandColors.navy900
    var foregroundColor: Color = .white
    var isDisabled: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(foregroundColor)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(isDisabled ? BrandColors.inputBorderUnfocused : backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .opacity(configuration.isPressed ? 0.85 : 1.0)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(BrandColors.navy900)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(BrandColors.surfaceLight)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(BrandColors.inputBorderUnfocused, lineWidth: 1.2)
            )
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}
