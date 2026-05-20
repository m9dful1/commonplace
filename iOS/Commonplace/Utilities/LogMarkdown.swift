import SwiftUI

/// Renders the bundled CLAUDE_LOG.md to a SwiftUI view tree. Walks the file
/// once into a sequence of typed blocks, then renders each block with the
/// app's typography. Mirrors the intent of `src/lib/markdownToReact.tsx`
/// in the web app — H3 date+role headings get split typography (mono date,
/// indigo serif role), bold is reset to regular weight in body color, hr
/// renders as breath rather than a hairline, blockquotes use a thin indigo
/// rule. Inline parsing handles bold, italic, and inline code only.
enum LogMarkdown {
    enum Block {
        case datedHeading(date: String, role: String)
        case heading(level: Int, text: AttributedString)
        case paragraph(AttributedString)
        case blockquote([Block])
        case codeBlock(String)
        case bulletList([AttributedString])
        case orderedList([AttributedString])
        case rule
    }

    /// Skip the file's preamble — everything up to and including the first
    /// `---` line — to mirror the web's behavior where the page provides
    /// its own one-line tagline as design chrome.
    static func parse(_ source: String) -> [Block] {
        let body = stripPreamble(source)
        var blocks: [Block] = []
        let lines = body.components(separatedBy: "\n")
        var i = 0
        while i < lines.count {
            let line = lines[i]
            if line.trimmingCharacters(in: .whitespaces).isEmpty {
                i += 1
                continue
            }
            if line.hasPrefix("# ") {
                // skip H1 (the file title)
                i += 1
                continue
            }
            if line.hasPrefix("### ") {
                let text = String(line.dropFirst(4))
                if let dated = parseDatedHeading(text) {
                    blocks.append(.datedHeading(date: dated.date, role: dated.role))
                } else {
                    blocks.append(.heading(level: 3, text: parseInline(text)))
                }
                i += 1
                continue
            }
            if line.hasPrefix("## ") {
                let text = String(line.dropFirst(3))
                blocks.append(.heading(level: 2, text: parseInline(text)))
                i += 1
                continue
            }
            if line.trimmingCharacters(in: .whitespaces) == "---" {
                blocks.append(.rule)
                i += 1
                continue
            }
            if line.hasPrefix("> ") {
                var quoted: [String] = []
                while i < lines.count, lines[i].hasPrefix("> ") || lines[i].hasPrefix(">") {
                    var l = lines[i]
                    if l.hasPrefix("> ") { l.removeFirst(2) }
                    else if l.hasPrefix(">") { l.removeFirst() }
                    quoted.append(l)
                    i += 1
                }
                let inner = parse(quoted.joined(separator: "\n"))
                blocks.append(.blockquote(inner))
                continue
            }
            if line.hasPrefix("```") {
                var code = ""
                i += 1
                while i < lines.count, !lines[i].hasPrefix("```") {
                    code += lines[i] + "\n"
                    i += 1
                }
                i += 1 // consume closing ```
                blocks.append(.codeBlock(code))
                continue
            }
            // Lists
            if matchesUnorderedItem(line) {
                var items: [AttributedString] = []
                while i < lines.count, matchesUnorderedItem(lines[i]) {
                    let stripped = stripUnorderedMarker(lines[i])
                    items.append(parseInline(stripped))
                    i += 1
                }
                blocks.append(.bulletList(items))
                continue
            }
            if matchesOrderedItem(line) {
                var items: [AttributedString] = []
                while i < lines.count, matchesOrderedItem(lines[i]) {
                    let stripped = stripOrderedMarker(lines[i])
                    items.append(parseInline(stripped))
                    i += 1
                }
                blocks.append(.orderedList(items))
                continue
            }

            // Paragraph: collect until blank line or block-introducing line
            var paragraphLines: [String] = [line]
            i += 1
            while i < lines.count {
                let l = lines[i]
                let trimmed = l.trimmingCharacters(in: .whitespaces)
                if trimmed.isEmpty { break }
                if trimmed == "---" { break }
                if l.hasPrefix("# ") || l.hasPrefix("## ") || l.hasPrefix("### ") { break }
                if l.hasPrefix("> ") || l.hasPrefix(">") { break }
                if l.hasPrefix("```") { break }
                if matchesUnorderedItem(l) || matchesOrderedItem(l) { break }
                paragraphLines.append(l)
                i += 1
            }
            blocks.append(.paragraph(parseInline(paragraphLines.joined(separator: " "))))
        }
        return blocks
    }

    private static func stripPreamble(_ source: String) -> String {
        // Find first `---` on its own line.
        let lines = source.components(separatedBy: "\n")
        for (i, l) in lines.enumerated() {
            if l.trimmingCharacters(in: .whitespaces) == "---" {
                let rest = lines[(i + 1)...]
                return rest.joined(separator: "\n")
            }
        }
        return source
    }

    private static func parseDatedHeading(_ text: String) -> (date: String, role: String)? {
        // Match `YYYY-MM-DD — Role`
        let pattern = #/^(\d{4}-\d{2}-\d{2})\s+—\s+(.+)$/#
        if let m = try? pattern.wholeMatch(in: text) {
            return (String(m.output.1), String(m.output.2))
        }
        return nil
    }

    private static func matchesUnorderedItem(_ line: String) -> Bool {
        let trimmed = line.drop { $0 == " " || $0 == "\t" }
        guard let first = trimmed.first else { return false }
        if (first == "-" || first == "*" || first == "+"),
           trimmed.dropFirst().first == " " {
            return true
        }
        return false
    }

    private static func stripUnorderedMarker(_ line: String) -> String {
        var s = line
        while let first = s.first, first == " " || first == "\t" { s.removeFirst() }
        if let first = s.first, first == "-" || first == "*" || first == "+" {
            s.removeFirst()
        }
        if s.first == " " { s.removeFirst() }
        return s
    }

    private static func matchesOrderedItem(_ line: String) -> Bool {
        let trimmed = line.drop { $0 == " " || $0 == "\t" }
        var sawDigit = false
        var i = trimmed.startIndex
        while i < trimmed.endIndex, trimmed[i].isNumber {
            sawDigit = true
            i = trimmed.index(after: i)
        }
        guard sawDigit, i < trimmed.endIndex, trimmed[i] == ".",
              trimmed.index(after: i) < trimmed.endIndex,
              trimmed[trimmed.index(after: i)] == " "
        else { return false }
        return true
    }

    private static func stripOrderedMarker(_ line: String) -> String {
        var s = line
        while let first = s.first, first == " " || first == "\t" { s.removeFirst() }
        while let first = s.first, first.isNumber { s.removeFirst() }
        if s.first == "." { s.removeFirst() }
        if s.first == " " { s.removeFirst() }
        return s
    }

    /// Inline parser: bold (`**...**`), italic (`*...*` and `_..._`), and
    /// inline code (`` `...` ``). Builds an AttributedString with the right
    /// presentation intents and a monospaced run for code.
    static func parseInline(_ source: String) -> AttributedString {
        var result = AttributedString()
        var buffer = ""
        let chars = Array(source)
        var i = 0

        func flush() {
            if !buffer.isEmpty {
                result.append(AttributedString(buffer))
                buffer = ""
            }
        }

        while i < chars.count {
            let c = chars[i]
            // Inline code: `...`
            if c == "`" {
                if let close = findClosing(chars, after: i, marker: "`") {
                    flush()
                    let inner = String(chars[(i + 1)..<close])
                    var attr = AttributedString(inner)
                    attr.font = .system(size: 14, design: .monospaced)
                    result.append(attr)
                    i = close + 1
                    continue
                }
            }
            // Bold: ** or __
            if (c == "*" || c == "_") && i + 1 < chars.count && chars[i + 1] == c {
                let marker = String(c) + String(c)
                if let close = findClosingPair(chars, after: i, marker: marker) {
                    flush()
                    let inner = String(chars[(i + 2)..<close])
                    var attr = parseInline(inner)
                    attr.inlinePresentationIntent = .stronglyEmphasized
                    result.append(attr)
                    i = close + 2
                    continue
                }
            }
            // Italic: * or _ (single)
            if (c == "*" || c == "_") {
                if let close = findClosing(chars, after: i, marker: c) {
                    // Make sure this isn't actually a bold
                    if i + 1 < chars.count && chars[i + 1] == c { /* handled above */ }
                    else {
                        flush()
                        let inner = String(chars[(i + 1)..<close])
                        var attr = parseInline(inner)
                        attr.inlinePresentationIntent = .emphasized
                        result.append(attr)
                        i = close + 1
                        continue
                    }
                }
            }
            buffer.append(c)
            i += 1
        }
        flush()
        return result
    }

    private static func findClosing(_ chars: [Character], after start: Int, marker: Character) -> Int? {
        var i = start + 1
        while i < chars.count {
            if chars[i] == "\n" { return nil }
            if chars[i] == marker {
                if i + 1 < chars.count && chars[i + 1] == marker { i += 2; continue }
                if i == start + 1 { i += 1; continue }
                return i
            }
            i += 1
        }
        return nil
    }

    private static func findClosingPair(_ chars: [Character], after start: Int, marker: String) -> Int? {
        let m = Array(marker)
        var i = start + m.count
        while i + m.count <= chars.count {
            if chars[i] == "\n" { return nil }
            var match = true
            for k in 0..<m.count {
                if chars[i + k] != m[k] { match = false; break }
            }
            if match && i > start + m.count {
                return i
            }
            i += 1
        }
        return nil
    }
}
