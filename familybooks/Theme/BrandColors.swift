import SwiftUI
import UIKit

enum BrandColors {
    // Primary Brand
    static let navy900 = Color(hex: "#0A2540")
    static let navy800 = Color(hex: "#133558")
    static let tealAccent = Color(hex: "#00D4B2")
    static let tealDark = Color(hex: "#00A88D")
    static let tealLight = Color(hex: "#E0FAF5")

    // Surfaces & Borders
    static let backgroundLight = Color(hex: "#F5F7FA")
    static let surfaceLight = Color(hex: "#FFFFFF")
    static let cardBorderLight = Color(hex: "#D5DEE8")
    static let inputBorderUnfocused = Color(hex: "#8C9BAE")
    static let inputContainerLight = Color(hex: "#F8FAFC")

    // Typography
    static let textPrimary = Color(hex: "#0A2540")
    static let textSecondary = Color(hex: "#475569")
    static let textTertiary = Color(hex: "#64748B")

    // Semantic Status
    static let expenseRed = Color(hex: "#D32F2F")
    static let expenseRedBg = Color(hex: "#FFEBEE")
    static let incomeGreen = Color(hex: "#2E7D32")
    static let incomeGreenBg = Color(hex: "#E8F5E9")
    static let pendingOrange = Color(hex: "#ED6C02")
    static let pendingOrangeBg = Color(hex: "#FFF3E0")

    // Soft Accent Badges
    static let blueBadge = Color(hex: "#1E5B94")
    static let blueBadgeBg = Color(hex: "#E8F1FA")
    static let purpleBadge = Color(hex: "#7B1FA2")
    static let purpleBadgeBg = Color(hex: "#F3E5F5")

    // UIColor equivalents for UIKit appearance APIs
    static let uiNavy900 = UIColor(red: 10/255, green: 37/255, blue: 64/255, alpha: 1)
    static let uiTealAccent = UIColor(red: 0/255, green: 212/255, blue: 178/255, alpha: 1)
    static let uiTabUnselected = UIColor(red: 176/255, green: 196/255, blue: 216/255, alpha: 1)

    static let avatarPresetHexes: [String] = [
        "#0A2540",
        "#00A88D",
        "#1E5B94",
        "#8E24AA",
        "#D81B60",
        "#FB8C00",
        "#00897B",
        "#5C6BC0"
    ]

    static func categoryIcon(for category: String, isIncome: Bool = false) -> String {
        switch category {
        case "Groceries": return "cart.fill"
        case "Utilities": return "bolt.fill"
        case "Rent": return "house.fill"
        case "Dining": return "fork.knife"
        case "Education": return "book.fill"
        case "Healthcare": return "cross.case.fill"
        case "Transport": return "car.fill"
        case "Entertainment": return "film.fill"
        case "Shopping": return "bag.fill"
        case "Salary": return "briefcase.fill"
        case "Bonus": return "star.fill"
        case "Investment": return "chart.line.uptrend.xyaxis"
        case "Freelance": return "laptopcomputer"
        case "Gift": return "gift.fill"
        case "Refund": return "arrow.uturn.backward.circle.fill"
        default: return isIncome ? "arrow.down.circle.fill" : "tag.fill"
        }
    }

    static func categoryColor(for category: String, isIncome: Bool = false) -> Color {
        if isIncome {
            return incomeGreen
        }
        switch category {
        case "Groceries": return Color(hex: "#00897B")
        case "Utilities": return Color(hex: "#FB8C00")
        case "Rent": return Color(hex: "#1E5B94")
        case "Dining": return Color(hex: "#D81B60")
        case "Education": return Color(hex: "#5C6BC0")
        case "Healthcare": return Color(hex: "#D32F2F")
        case "Transport": return Color(hex: "#00A88D")
        case "Entertainment": return Color(hex: "#8E24AA")
        case "Shopping": return Color(hex: "#E65100")
        default: return navy900
        }
    }
}

extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&int)
        let r, g, b, a: UInt64
        switch cleaned.count {
        case 6:
            (r, g, b, a) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF, 255)
        case 8:
            (r, g, b, a) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (r, g, b, a) = (10, 37, 64, 255)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255.0,
            green: Double(g) / 255.0,
            blue: Double(b) / 255.0,
            opacity: Double(a) / 255.0
        )
    }
}
