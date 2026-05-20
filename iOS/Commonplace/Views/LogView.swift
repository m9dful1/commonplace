import SwiftUI

struct LogView: View {
    @State private var blocks: [LogMarkdown.Block] = []
    @State private var loadError: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("a document maintained across Claude instances working on Commonplace")
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.lowercase)
                    .foregroundStyle(Theme.muted)
                    .padding(.bottom, 24)

                if let err = loadError {
                    Text("CLAUDE_LOG.md is not where it should be.")
                        .font(Theme.Font.serif(size: 16, italic: true))
                        .foregroundStyle(Theme.muted)
                    Text(err)
                        .font(Theme.Font.mono(size: 11))
                        .foregroundStyle(Theme.muted)
                } else {
                    ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                        LogBlockView(block: block)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .navigationTitle("Log")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { load() }
    }

    private func load() {
        if let asset = NSDataAsset(name: "ClaudeLog"),
           let source = String(data: asset.data, encoding: .utf8) {
            blocks = LogMarkdown.parse(source)
            loadError = nil
        } else {
            loadError = "looked for the bundled `ClaudeLog` asset and didn't find it."
            blocks = []
        }
    }
}

private struct LogBlockView: View {
    let block: LogMarkdown.Block

    var body: some View {
        switch block {
        case .datedHeading(let date, let role):
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(date)
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Theme.muted)
                Text(role)
                    .font(Theme.Font.serif(size: 16))
                    .foregroundStyle(Theme.accent)
            }
            .padding(.top, 56)

        case .heading(let level, let text):
            Text(text)
                .font(Theme.Font.serif(size: level == 2 ? 18 : 16))
                .foregroundStyle(Theme.ink)
                .padding(.top, 32)

        case .paragraph(let text):
            Text(text)
                .font(Theme.Font.serif(size: 16))
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)

        case .blockquote(let inner):
            HStack(alignment: .top, spacing: 0) {
                Rectangle()
                    .fill(Theme.accent.opacity(0.3))
                    .frame(width: 2)
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(inner.enumerated()), id: \.offset) { _, b in
                        LogBlockView(block: b)
                    }
                }
                .padding(.leading, 16)
            }

        case .codeBlock(let text):
            Text(text)
                .font(Theme.Font.mono(size: 12))
                .foregroundStyle(Theme.ink)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Theme.rule.opacity(0.4))

        case .bulletList(let items):
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text("•")
                            .font(Theme.Font.serif(size: 16))
                            .foregroundStyle(Theme.muted)
                        Text(item)
                            .font(Theme.Font.serif(size: 16))
                            .foregroundStyle(Theme.muted)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

        case .orderedList(let items):
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(items.enumerated()), id: \.offset) { idx, item in
                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text("\(idx + 1).")
                            .font(Theme.Font.mono(size: 11))
                            .foregroundStyle(Theme.muted)
                        Text(item)
                            .font(Theme.Font.serif(size: 16))
                            .foregroundStyle(Theme.muted)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

        case .rule:
            // Hairline-as-breath: generous whitespace, no literal divider.
            // Mirrors the web app's note in `markdownToReact.tsx`.
            Color.clear
                .frame(height: 24)
        }
    }
}
