import SwiftUI

struct AddEditProfileSheet: View {
    let store: LedgerStore
    let editingMember: FamilyMember?

    @Environment(\.dismiss) private var dismiss

    @State private var name: String
    @State private var role: String
    @State private var avatarColorHex: String
    @State private var notes: String
    @State private var setAsActive: Bool
    @State private var validationError: String?

    init(store: LedgerStore, editingMember: FamilyMember? = nil) {
        self.store = store
        self.editingMember = editingMember

        if let member = editingMember {
            _name = State(initialValue: member.name)
            _role = State(initialValue: member.role)
            _avatarColorHex = State(initialValue: member.avatarColorHex)
            _notes = State(initialValue: member.notes)
            _setAsActive = State(initialValue: member.isPrimary || AnalyticsCalculator.matchesName(store.activeProfileName, member.name))
        } else {
            let nextColor = FamilyMember.avatarColorPresets[store.profiles.count % FamilyMember.avatarColorPresets.count]
            _name = State(initialValue: "")
            _role = State(initialValue: store.profiles.isEmpty ? "Self" : "Spouse")
            _avatarColorHex = State(initialValue: nextColor)
            _notes = State(initialValue: "")
            _setAsActive = State(initialValue: store.profiles.isEmpty)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Live Avatar Preview
                    HStack(spacing: 14) {
                        ProfileAvatarCircle(
                            name: name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "?" : name,
                            colorHex: avatarColorHex,
                            size: 56
                        )

                        VStack(alignment: .leading, spacing: 3) {
                            Text(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "New Family Member" : name)
                                .font(.headline)
                                .foregroundStyle(BrandColors.textPrimary)
                            Text(role)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(BrandColors.textSecondary)
                        }

                        Spacer()

                        if setAsActive || store.profiles.isEmpty {
                            StatusBadgePill(
                                text: "Default Active",
                                foreground: BrandColors.tealDark,
                                background: BrandColors.tealLight
                            )
                        }
                    }
                    .cardSurface()

                    // Name & Notes
                    VStack(alignment: .leading, spacing: 14) {
                        StyledTextField(
                            label: "Member Name",
                            placeholder: "e.g., Alex, Jordan, Mom",
                            text: $name
                        )

                        StyledTextField(
                            label: "Notes (Optional)",
                            placeholder: "e.g., Primary earner, student allowance",
                            text: $notes
                        )
                    }
                    .cardSurface()

                    // Role Chips
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Family Role")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        FlowLayout(spacing: 8) {
                            ForEach(FamilyMember.rolePresets, id: \.self) { preset in
                                SelectableChip(
                                    title: preset,
                                    isSelected: role == preset
                                ) {
                                    role = preset
                                }
                            }
                        }
                    }
                    .cardSurface()

                    // 8 Avatar Colors
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Avatar Color")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(BrandColors.textPrimary)

                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 4), spacing: 12) {
                            ForEach(FamilyMember.avatarColorPresets, id: \.self) { hex in
                                let selected = avatarColorHex.caseInsensitiveCompare(hex) == .orderedSame
                                Button {
                                    avatarColorHex = hex
                                } label: {
                                    ZStack {
                                        Circle()
                                            .fill(Color(hex: hex))
                                            .frame(width: 44, height: 44)
                                        if selected {
                                            Circle()
                                                .stroke(BrandColors.tealAccent, lineWidth: 3)
                                                .frame(width: 50, height: 50)
                                            Image(systemName: "checkmark")
                                                .font(.subheadline.weight(.bold))
                                                .foregroundStyle(.white)
                                        }
                                    }
                                    .frame(height: 52)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Color \(hex)")
                            }
                        }
                    }
                    .cardSurface()

                    // Default Active Profile Toggle
                    VStack(alignment: .leading, spacing: 6) {
                        Toggle(isOn: $setAsActive) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Set as Default Active Profile")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(BrandColors.textPrimary)
                                Text(store.profiles.isEmpty
                                     ? "First profile created automatically becomes your default active profile."
                                     : "Pre-fills Paid By / Received By and shows in the top header.")
                                    .font(.caption)
                                    .foregroundStyle(BrandColors.textSecondary)
                            }
                        }
                        .tint(BrandColors.tealDark)
                        .disabled(store.profiles.isEmpty && editingMember == nil)
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

                        Button(editingMember == nil ? "Save Profile" : "Update Profile") {
                            saveProfile()
                        }
                        .buttonStyle(PrimaryButtonStyle())
                    }
                    .padding(.bottom, 16)
                }
                .padding(16)
            }
            .navigationTitle(editingMember == nil ? "Add Family Profile" : "Edit Family Profile")
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

    private func saveProfile() {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            validationError = "Member Name is required."
            return
        }

        let duplicateExists = store.profiles.contains { existing in
            existing.id != editingMember?.id && AnalyticsCalculator.matchesName(existing.name, trimmed)
        }
        if duplicateExists {
            validationError = "A family profile named \"\(trimmed)\" already exists."
            return
        }

        if let existing = editingMember {
            store.updateProfile(
                existing,
                name: trimmed,
                role: role,
                avatarColorHex: avatarColorHex,
                notes: notes,
                setAsActive: setAsActive
            )
            store.showToast("Profile updated")
        } else {
            store.addProfile(
                name: trimmed,
                role: role,
                avatarColorHex: avatarColorHex,
                notes: notes,
                setAsActive: setAsActive || store.profiles.isEmpty
            )
            store.showToast("Profile added")
        }
        dismiss()
    }
}
