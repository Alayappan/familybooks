import Foundation

nonisolated enum FormatUtils {
    static let monthNames: [String] = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ]

    static let shortMonthNames: [String] = [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ]

    static var nowEpochMs: Int64 {
        Int64((Date().timeIntervalSince1970 * 1000.0).rounded())
    }

    static func date(fromEpochMs ms: Int64) -> Date {
        Date(timeIntervalSince1970: Double(ms) / 1000.0)
    }

    static func epochMs(from date: Date) -> Int64 {
        Int64((date.timeIntervalSince1970 * 1000.0).rounded())
    }

    static func roundToTwoDecimals(_ value: Double) -> Double {
        (value * 100.0).rounded() / 100.0
    }

    static func currency(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        formatter.currencySymbol = "$"
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.locale = Locale(identifier: "en_US")
        let normalized = abs(amount) < 0.005 ? 0.0 : amount
        return formatter.string(from: NSNumber(value: normalized)) ?? String(format: "$%.2f", normalized)
    }

    static func signedCurrency(_ amount: Double, isExpense: Bool) -> String {
        let absFormatted = currency(abs(amount))
        return isExpense ? "-\(absFormatted)" : "+\(absFormatted)"
    }

    static func signedNetCurrency(_ amount: Double) -> String {
        if abs(amount) < 0.005 {
            return "$0.00"
        }
        let absFormatted = currency(abs(amount))
        return amount >= 0 ? "+\(absFormatted)" : "-\(absFormatted)"
    }

    static func percentageChange(current: Double, previous: Double) -> Double? {
        if previous <= 0.0001 {
            return current > 0.0001 ? 100.0 : nil
        }
        return ((current - previous) / previous) * 100.0
    }

    static func shortDate(_ epochMs: Int64) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        formatter.locale = Locale(identifier: "en_US")
        return formatter.string(from: date(fromEpochMs: epochMs))
    }

    static func monthYearLabel(year: Int, oneBasedMonth: Int) -> String {
        let idx = max(0, min(11, oneBasedMonth - 1))
        return "\(monthNames[idx]) \(year)"
    }

    static func isoTimestamp(_ epochMs: Int64) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone.current
        return formatter.string(from: date(fromEpochMs: epochMs))
    }

    static func parseIsoTimestamp(_ string: String) -> Int64? {
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        let formats = [
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        ]
        for format in formats {
            let formatter = DateFormatter()
            formatter.dateFormat = format
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = TimeZone.current
            if let parsed = formatter.date(from: trimmed) {
                return epochMs(from: parsed)
            }
        }
        if let rawMs = Int64(trimmed) {
            return rawMs
        }
        return nil
    }

    static func yearAndZeroBasedMonth(fromEpochMs ms: Int64, calendar: Calendar = .current) -> (year: Int, zeroBasedMonth: Int) {
        let comps = calendar.dateComponents([.year, .month], from: date(fromEpochMs: ms))
        let year = comps.year ?? 2026
        let month = max(0, min(11, (comps.month ?? 1) - 1))
        return (year, month)
    }

    static func previousYearAndOneBasedMonth(year: Int, oneBasedMonth: Int) -> (year: Int, month: Int) {
        if oneBasedMonth == 1 {
            return (year - 1, 12)
        }
        return (year, oneBasedMonth - 1)
    }

    static func isEpochMs(_ ms: Int64, inYear year: Int, oneBasedMonth: Int, calendar: Calendar = .current) -> Bool {
        let comps = calendar.dateComponents([.year, .month], from: date(fromEpochMs: ms))
        return comps.year == year && comps.month == oneBasedMonth
    }

    static func parseDecimalAmount(_ text: String) -> Double? {
        let cleaned = text
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "$", with: "")
            .replacingOccurrences(of: ",", with: "")
        guard !cleaned.isEmpty, let value = Double(cleaned), value.isFinite else {
            return nil
        }
        return value
    }

    static func profileInitial(for name: String) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let first = trimmed.first else { return "?" }
        return String(first).uppercased()
    }
}
