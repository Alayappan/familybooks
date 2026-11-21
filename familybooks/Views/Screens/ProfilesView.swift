import SwiftUI

struct ProfilesView: View {
    let store: LedgerStore
    let onOpenAddProfile: () -> Void
    let onEditProfile: (FamilyMember) -> Void
    let onAnalyzeProfile: (String) -> Void
    let onViewEntriesForProfile: (String) -> Void
    let onOpenExportImport: () -> Void

    @State private var memberToDelete: FamilyMember?

    private var summaries: [ProfileFinancialSummary] {
        store.profileFinancialSummaries
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    heroProfilesCard

                    if summaries.isEmpty {
                        EmptyStateCard(
                            icon: "person.badge.plus",
                            title: "No Family Profiles Yet",
                            message: "Create profiles for each family member to track individual spending, income, bill splits, and liabilities.",
                            actionTitle: "Create First Profile",
                            action: onOpenAddProfile
                        )
                    } else {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Tap a profile card to set it as your default active profile")
                                .font(.caption.weight(.medium))
                                .foregroundStyle(BrandColors.textSecondary)

                            LazyVStack(spacing: 12) {
                                ForEach(summaries) { summary in
                                    profileBreakdownCard(summary)
                                }
                            }
                        }
                    }

                    offlineBackupCard
                }
                .padding(16)
                .padding(.bottom, 84)
            }
            .background(BrandColors.backgroundLight)

            // Floating "Add Profile" Button
            Button(action: onOpenAddProfile) {
                HStack(spacing: 8) {
                    Image(systemName: "person.badge.plus")
                        .font(.subheadline.weight(.bold))
                    Text("Add Profile")
                        .font(.subheadline.weight(.bold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 18)
                .padding(.vertical, 14)
                .background(BrandColors.navy900)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(BrandColors.tealAccent, lineWidth: 1.8)
                )
                .shadow(color: BrandColors.navy900.opacity(0.28), radius: 8, x: 0, y: 4)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 18)
            .padding(.bottom, 18)
        }
        .alert(
            "Delete Family Profile?",
            isPresented: Binding(
                get: { memberToDelete != nil },
                set: { if !$0 { memberToDelete = nil } }
            ),
            presenting: memberToDelete
        ) { member in
            Button("Delete", role: .destructive) {
                store.deleteProfile(member)
                store.showToast("Profile removed")
                memberToDelete = nil
            }
            Button("Cancel", role: .cancel) {
                memberToDelete = nil
            }
        } message: { member in
            Text("Remove \"\(member.name)\" from saved profiles? Existing ledger entries will remain intact.")
        }
    }

    // MARK: - Hero Navy900 Card

    private var heroProfilesCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("FAMILY PROFILES")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.tealAccent)
                        .tracking(0.8)

                    Text("\(store.profiles.count) Saved \(store.profiles.count == 1 ? "Profile" : "Profiles")")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white)
                }

                Spacer()

                Button(action: onOpenAddProfile) {
                    Label("Add Profile", systemImage: "plus")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.navy900)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(BrandColors.tealAccent)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("3rd-Party Debt Owed")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.white.opacity(0.78))
                    Text(FormatUtils.currency(store.totalPendingThirdPartyPayable))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: "#FF8A80"))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(BrandColors.navy800)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 3) {
                    Text("3rd-Party Receivables")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.white.opacity(0.78))
                    Text(FormatUtils.currency(store.totalPendingThirdPartyReceivable))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(BrandColors.tealAccent)
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
    }

    // MARK: - Profile Breakdown Card

    private func profileBreakdownCard(_ summary: ProfileFinancialSummary) -> some View {
        let isActive = store.activeProfile.map { AnalyticsCalculator.matchesName($0.name, summary.name) }
            ?? AnalyticsCalculator.matchesName(store.activeProfileName, summary.name)
        let savedMember = summary.memberID.flatMap { id in store.profiles.first(where: { $0.id == id }) }

        return VStack(alignment: .leading, spacing: 12) {
            // Header Row
            HStack(alignment: .center, spacing: 12) {
                ProfileAvatarCircle(name: summary.name, colorHex: summary.avatarColorHex, size: 44)

                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(summary.name)
                            .font(.headline)
                            .foregroundStyle(BrandColors.textPrimary)

                        if isActive {
                            StatusBadgePill(
                                text: "Active",
                                foreground: BrandColors.tealDark,
                                background: BrandColors.tealLight
                            )
                        }
                    }

                    Text(summary.role)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BrandColors.textSecondary)
                }

                Spacer()

                if let member = savedMember {
                    HStack(spacing: 6) {
                        Button {
                            onEditProfile(member)
                        } label: {
                            Image(systemName: "pencil")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(BrandColors.navy900)
                                .padding(8)
                                .background(BrandColors.inputContainerLight)
                                .clipShape(Circle())
                                .overlay(Circle().stroke(BrandColors.cardBorderLight, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Edit \(member.name)")

                        Button {
                            memberToDelete = member
                        } label: {
                            Image(systemName: "trash")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(BrandColors.expenseRed)
                                .padding(8)
                                .background(BrandColors.expenseRedBg)
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Delete \(member.name)")
                    }
                } else {
                    Button("Save Profile") {
                        store.addProfile(
                            name: summary.name,
                            role: "Other",
                            avatarColorHex: summary.avatarColorHex,
                            notes: "",
                            setAsActive: store.profiles.isEmpty
                        )
                        store.showToast("Saved \(summary.name) as profile")
                    }
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(BrandColors.tealDark)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(BrandColors.tealLight)
                    .clipShape(Capsule())
                }
            }

            if !summary.notes.isEmpty {
                Text(summary.notes)
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
            }

            // 6-Metric Grid
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                metricCell(title: "Income", value: FormatUtils.currency(summary.totalIncome), color: BrandColors.incomeGreen)
                metricCell(title: "Paid Out", value: FormatUtils.currency(summary.totalPaidOut), color: BrandColors.expenseRed)
                metricCell(title: "Spent For", value: FormatUtils.currency(summary.totalSpentFor), color: BrandColors.blueBadge)
                metricCell(title: "Shares Given", value: FormatUtils.currency(summary.totalSharesContributed), color: BrandColors.purpleBadge)
                metricCell(title: "Family Debt", value: FormatUtils.currency(summary.familyTheyOwe), color: BrandColors.pendingOrange)
                metricCell(title: "3rd-Party Debt", value: FormatUtils.currency(summary.thirdPartyTheyOwe), color: BrandColors.expenseRed)
            }

            // Net Liability Standing Banner
            let standingPositive = summary.netLiabilityStanding >= 0
            HStack {
                Label(
                    "Net Liability Standing",
                    systemImage: standingPositive ? "arrow.up.right.circle.fill" : "arrow.down.right.circle.fill"
                )
                .font(.caption.weight(.bold))
                .foregroundStyle(standingPositive ? BrandColors.incomeGreen : BrandColors.expenseRed)

                Spacer()

                Text(FormatUtils.signedNetCurrency(summary.netLiabilityStanding))
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(standingPositive ? BrandColors.incomeGreen : BrandColors.expenseRed)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(standingPositive ? BrandColors.incomeGreenBg : BrandColors.expenseRedBg)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            // Analyze & View Entries Buttons
            HStack(spacing: 10) {
                Button {
                    onAnalyzeProfile(summary.name)
                } label: {
                    Label("Analyze", systemImage: "chart.bar.xaxis")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(BrandColors.navy900)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)

                Button {
                    onViewEntriesForProfile(summary.name)
                } label: {
                    Label("View Entries", systemImage: "list.bullet.rectangle")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(BrandColors.navy900)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(BrandColors.inputContainerLight)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(BrandColors.inputBorderUnfocused, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(BrandColors.surfaceLight)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(isActive ? BrandColors.tealDark : BrandColors.cardBorderLight, lineWidth: isActive ? 2 : 1)
        )
        .shadow(color: BrandColors.navy900.opacity(0.05), radius: 6, x: 0, y: 2)
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .onTapGesture {
            store.setActiveProfile(summary.name)
            store.showToast("Active profile set to \(summary.name)")
        }
    }

    private func metricCell(title: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(BrandColors.textSecondary)
                .lineLimit(1)
            Text(value)
                .font(.caption.weight(.bold))
                .foregroundStyle(color)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(9)
        .background(BrandColors.inputContainerLight)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    // MARK: - Offline Backup Card

    private var offlineBackupCard: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(BrandColors.tealLight)
                    .frame(width: 44, height: 44)
                Image(systemName: "externaldrive.fill")
                    .font(.title3)
                    .foregroundStyle(BrandColors.tealDark)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text("Offline Backup & Restore")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(BrandColors.textPrimary)
                Text("Export or import cross-platform JSON v2.0 & CSV backups locally.")
                    .font(.caption)
                    .foregroundStyle(BrandColors.textSecondary)
            }

            Spacer()

            Button("Manage", action: onOpenExportImport)
                .font(.caption.weight(.bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 9)
                .background(BrandColors.navy900)
                .clipShape(Capsule())
        }
        .cardSurface(cornerRadius: 16, padding: 14)
    }
}
