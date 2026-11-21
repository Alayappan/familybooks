import Foundation
import SwiftData

@Model
final class FamilyMember {
    @Attribute(.unique) var id: String
    var name: String
    var role: String
    var avatarColorHex: String
    var isPrimary: Bool
    var notes: String
    var createdAt: Int64
    var updatedAt: Int64
    var isDeleted: Bool

    init(
        id: String = UUID().uuidString,
        name: String,
        role: String = "Self",
        avatarColorHex: String = "#0A2540",
        isPrimary: Bool = false,
        notes: String = "",
        createdAt: Int64 = FormatUtils.nowEpochMs,
        updatedAt: Int64 = FormatUtils.nowEpochMs,
        isDeleted: Bool = false
    ) {
        self.id = id
        self.name = name
        self.role = role
        self.avatarColorHex = avatarColorHex
        self.isPrimary = isPrimary
        self.notes = notes
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.isDeleted = isDeleted
    }

    static let rolePresets: [String] = [
        "Self",
        "Spouse",
        "Parent",
        "Child",
        "Sibling",
        "Household",
        "Other"
    ]

    static let avatarColorPresets: [String] = [
        "#0A2540",
        "#00A88D",
        "#1E5B94",
        "#8E24AA",
        "#D81B60",
        "#FB8C00",
        "#00897B",
        "#5C6BC0"
    ]
}
