import SwiftUI

/// One marginal note: indented, indigo, italic body serif, with a thin
/// indigo rule on the leading edge — the same vocabulary the web app's
/// `<MarginaliaBlock>` uses.
struct MarginaliaBlock: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            Rectangle()
                .fill(Theme.accent.opacity(0.3))
                .frame(width: 2)
            Text(LightMarkdown.render(text))
                .font(Theme.Font.serif(size: 16, italic: true))
                .foregroundStyle(Theme.accent)
                .padding(.leading, 14)
                .padding(.vertical, 2)
        }
        .padding(.leading, 18)
    }
}

struct MarginaliaList: View {
    let items: [Marginalia]

    var body: some View {
        if items.isEmpty {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(items) { item in
                    MarginaliaBlock(text: item.body)
                }
            }
        }
    }
}
