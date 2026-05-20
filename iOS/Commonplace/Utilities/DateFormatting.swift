import Foundation

enum DateFormatting {
    private static let parser: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static let parserNoFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    private static let monthNames = [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ]

    /// Mirrors `src/lib/format.ts` — "Jan 5 · 14:32" within the same year,
    /// "Jan 5, 2024 · 14:32" otherwise.
    static func short(_ iso: String) -> String {
        guard let date = parse(iso) else { return iso }
        let cal = Calendar(identifier: .gregorian)
        let comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let now = cal.dateComponents([.year], from: Date())
        let month = monthNames[(comps.month ?? 1) - 1]
        let day = comps.day ?? 1
        let hh = String(format: "%02d", comps.hour ?? 0)
        let mm = String(format: "%02d", comps.minute ?? 0)
        let datePart: String
        if comps.year == now.year {
            datePart = "\(month) \(day)"
        } else {
            datePart = "\(month) \(day), \(comps.year ?? 0)"
        }
        return "\(datePart) · \(hh):\(mm)"
    }

    static func dateOnly(_ iso: String) -> String {
        return String(iso.prefix(10))
    }

    private static func parse(_ iso: String) -> Date? {
        parser.date(from: iso) ?? parserNoFractional.date(from: iso)
    }
}
