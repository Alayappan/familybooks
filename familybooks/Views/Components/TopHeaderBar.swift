import SwiftUI

struct TopHeaderBar: View {
    let store: LedgerStore
    let onAvatarTap: () -> Void

    private var subtitleText: String {
        if let active = store.activeProfile {
            return "Offline Family Ledger • \(active.name)"
        }
        if !store.activeProfileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Offline Family Ledger • \(store.activeProfileName)"
        }
        let count = store.profiles.count
        if count > 0 {
            return "Offline Family Ledger • \(count) \(count == 1 ? "Profile" : "Profiles")"
        }
        return "Offline Individual Family Ledger"
    }

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 0) {
                    Text("Family")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white)
                    Text("Books")
                        .font(.title2.weight(.light))
                        .foregroundStyle(BrandColors.tealAccent)
                }

                Text(subtitleText)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(1)
                    .truncationMode(.tail)
            }

            Spacer(minLength: 8)

            Button(action: onAvatarTap) {
                ZStack {
                    if let active = store.activeProfile {
                        Circle()
                            .fill(Color(hex: active.avatarColorHex))
                            .frame(width: 36, height: 36)
                            .overlay(
                                Circle()
                                    .stroke(BrandColors.tealAccent, lineWidth: 1.8)
                            )
                            .overlay(
                                Text(FormatUtils.profileInitial(for: active.name))
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(.white)
                            )
                    } else {
                        Circle()
                            .fill(BrandColors.navy800)
                            .frame(width: 36, height: 36)
                            .overlay(
                                Circle()
                                    .stroke(BrandColors.tealAccent.opacity(0.85), lineWidth: 1.5)
                            )
                            .overlay(
                                Image(systemName: "person.badge.plus")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(BrandColors.tealAccent)
                            )
                    }
                }
                .frame(width: 44, height: 44)
                .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(store.profiles.isEmpty ? "Add Family Profile" : "Open Profiles")
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 12)
        .background(BrandColors.navy900.ignoresSafeArea(edges: .top))
    }
}
