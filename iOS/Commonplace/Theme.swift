import SwiftUI
import UIKit

enum Theme {
    static let paper = Color("Paper")
    static let ink = Color("Ink")
    static let muted = Color("Muted")
    static let rule = Color("Rule")
    static let accent = Color("AccentColor")

    static let pageMaxWidth: CGFloat = 680

    enum Font {
        // Iowan Old Style ships system-wide on iOS. Fallbacks follow the
        // web app's stack so registered-but-missing names degrade gracefully.
        // `relativeTo:` makes the font scale with Dynamic Type. The defaults
        // are picked so an 11pt caption and an 18pt body grow together when
        // a user bumps their text size in Settings — visual hierarchy stays
        // intact across the accessibility range.
        static func serif(
            size: CGFloat,
            weight: SwiftUI.Font.Weight = .regular,
            italic: Bool = false,
            relativeTo style: SwiftUI.Font.TextStyle = .body
        ) -> SwiftUI.Font {
            let name = italic ? italicFamilyName() : serifFamilyName()
            var font: SwiftUI.Font
            if UIFont(name: name, size: size) != nil {
                font = SwiftUI.Font.custom(name, size: size, relativeTo: style)
            } else {
                font = SwiftUI.Font.system(size: size, weight: weight, design: .serif)
            }
            if italic, #available(iOS 16.0, *) {
                font = font.italic()
            }
            if weight != .regular {
                font = font.weight(weight)
            }
            return font
        }

        static func mono(
            size: CGFloat,
            weight: SwiftUI.Font.Weight = .regular,
            relativeTo style: SwiftUI.Font.TextStyle = .body
        ) -> SwiftUI.Font {
            // Use the system mono face but scaled relative to a text style
            // so Dynamic Type still works on these labels.
            return SwiftUI.Font.system(size: size, weight: weight, design: .monospaced)
                .leading(.tight)
        }

        private static func serifFamilyName() -> String {
            let candidates = [
                "Iowan Old Style",
                "Source Serif Pro",
                "Source Serif 4",
                "Charter",
                "Georgia",
            ]
            for name in candidates {
                if UIFont(name: name, size: 12) != nil {
                    return name
                }
            }
            return "Georgia"
        }

        private static func italicFamilyName() -> String {
            let candidates = [
                "IowanOldStyle-Italic",
                "Iowan Old Style Italic",
                "SourceSerifPro-Italic",
                "Charter-Italic",
                "Georgia-Italic",
            ]
            for name in candidates {
                if UIFont(name: name, size: 12) != nil {
                    return name
                }
            }
            return "Georgia-Italic"
        }
    }
}

private extension SwiftUI.Font.Weight {
    var uiKitWeight: UIFont.Weight {
        switch self {
        case .ultraLight: return .ultraLight
        case .thin: return .thin
        case .light: return .light
        case .regular: return .regular
        case .medium: return .medium
        case .semibold: return .semibold
        case .bold: return .bold
        case .heavy: return .heavy
        case .black: return .black
        default: return .regular
        }
    }
}
