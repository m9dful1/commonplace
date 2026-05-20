import SwiftUI

/// Minimal markdown for marginalia bodies. Mirrors `src/lib/lightMarkdown.ts`:
/// `**bold**` / `__bold__` → bold, `*italic*` / `_italic_` → italic,
/// `--` → em-dash. Nothing else. The marginalia prompt forbids structure;
/// if it produces some, fix the prompt, not the renderer.
///
/// Returns an `AttributedString` which SwiftUI's `Text(_:)` renders with
/// inline runs respected.
enum LightMarkdown {
    static func render(_ source: String) -> AttributedString {
        let withDashes = source.replacingOccurrences(of: "--", with: "—")
        let runs = parse(withDashes, depth: 0)
        return runs.reduce(into: AttributedString()) { acc, run in
            acc.append(run.attributed())
        }
    }

    fileprivate enum Run {
        case text(String)
        case bold([Run])
        case italic([Run])

        func attributed() -> AttributedString {
            switch self {
            case .text(let s):
                return AttributedString(s)
            case .bold(let inner):
                var combined = inner.reduce(into: AttributedString()) { acc, r in
                    acc.append(r.attributed())
                }
                combined.inlinePresentationIntent = .stronglyEmphasized
                return combined
            case .italic(let inner):
                var combined = inner.reduce(into: AttributedString()) { acc, r in
                    acc.append(r.attributed())
                }
                combined.inlinePresentationIntent = .emphasized
                return combined
            }
        }
    }

    /// Two-pass parser: bold first, then italic, mirroring the JS version's
    /// pattern ordering. Each pattern has two alternates (`**` / `__` and
    /// `*` / `_`) and the inner content is re-parsed for the next level.
    fileprivate static func parse(_ text: String, depth: Int) -> [Run] {
        if text.isEmpty { return [] }
        if depth > 1 { return [.text(text)] }

        let isBold = (depth == 0)
        return scan(text: text, isBold: isBold, depth: depth)
    }

    private static func scan(text: String, isBold: Bool, depth: Int) -> [Run] {
        var out: [Run] = []
        var index = text.startIndex
        let chars = Array(text)
        var i = 0

        while i < chars.count {
            if isBold {
                if let m = matchPair(chars, start: i, marker: "**") ?? matchPair(chars, start: i, marker: "__") {
                    if i > 0 {
                        let leading = String(chars[0..<i])
                        out.append(contentsOf: parse(leading, depth: depth + 1))
                    }
                    let inner = String(chars[m.innerStart..<m.innerEnd])
                    out.append(.bold(parse(inner, depth: depth + 1)))
                    let trailing = String(chars[m.end..<chars.count])
                    out.append(contentsOf: scan(text: trailing, isBold: isBold, depth: depth))
                    return out
                }
            } else {
                // Italic: single * or _, but must not be part of ** or __
                if let m = matchSingle(chars, start: i, marker: "*") ?? matchSingle(chars, start: i, marker: "_") {
                    if i > 0 {
                        let leading = String(chars[0..<i])
                        out.append(.text(leading))
                    }
                    let inner = String(chars[m.innerStart..<m.innerEnd])
                    out.append(.italic([.text(inner)]))
                    let trailing = String(chars[m.end..<chars.count])
                    out.append(contentsOf: scan(text: trailing, isBold: isBold, depth: depth))
                    return out
                }
            }
            i += 1
            index = text.index(after: index)
            _ = index
        }
        // No match found at this level — pass through to next level / text
        if isBold {
            return parse(text, depth: depth + 1)
        }
        return [.text(text)]
    }

    private struct Match { let innerStart: Int; let innerEnd: Int; let end: Int }

    private static func matchPair(_ chars: [Character], start: Int, marker: String) -> Match? {
        let m = Array(marker)
        guard start + m.count <= chars.count else { return nil }
        for k in 0..<m.count {
            if chars[start + k] != m[k] { return nil }
        }
        // Find closing marker
        var j = start + m.count
        while j + m.count <= chars.count {
            if chars[j] == "\n" { return nil }
            var matchClose = true
            for k in 0..<m.count {
                if chars[j + k] != m[k] { matchClose = false; break }
            }
            if matchClose && j > start + m.count {
                return Match(innerStart: start + m.count, innerEnd: j, end: j + m.count)
            }
            j += 1
        }
        return nil
    }

    private static func matchSingle(_ chars: [Character], start: Int, marker: Character) -> Match? {
        guard chars[start] == marker else { return nil }
        // Don't treat ** or __ as italic boundaries
        if start + 1 < chars.count && chars[start + 1] == marker { return nil }
        if start > 0 && chars[start - 1] == marker { return nil }
        var j = start + 1
        while j < chars.count {
            if chars[j] == "\n" { return nil }
            if chars[j] == marker {
                // Don't close if followed by another marker (would be **)
                if j + 1 < chars.count && chars[j + 1] == marker { j += 1; continue }
                if j == start + 1 { j += 1; continue }
                return Match(innerStart: start + 1, innerEnd: j, end: j + 1)
            }
            j += 1
        }
        return nil
    }
}
