import SwiftUI

struct LettersView: View {
    @Environment(AppStore.self) private var store
    @State private var letters: [Letter] = []
    @State private var compose: ComposeState = .idle

    enum ComposeState {
        case idle
        case writing
        case error(String)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 36) {
                Text("letters from Claude — written on request, kept whole")
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.lowercase)
                    .foregroundStyle(Theme.muted)

                composeAffordance

                if letters.isEmpty {
                    if case .idle = compose {
                        Text("No Letters yet. The first one is a click away.")
                            .font(Theme.Font.serif(size: 16, italic: true))
                            .foregroundStyle(Theme.muted)
                    }
                    if case .error = compose {
                        Text("No Letters yet. The first one is a click away.")
                            .font(Theme.Font.serif(size: 16, italic: true))
                            .foregroundStyle(Theme.muted)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 56) {
                        ForEach(letters) { letter in
                            LetterRow(letter: letter, onChange: reload)
                        }
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Letters")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { reload() }
        .onChange(of: store.refreshToken) { _, _ in reload() }
    }

    @ViewBuilder
    private var composeAffordance: some View {
        switch compose {
        case .writing:
            Text("Claude is writing…")
                .font(Theme.Font.serif(size: 16, italic: true))
                .foregroundStyle(Theme.muted)
        case .error(let message):
            VStack(alignment: .leading, spacing: 6) {
                Text("couldn’t reach Claude — \(message)")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
                Button(action: composeLetter) {
                    Text("try again")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.accent)
                }
            }
        case .idle:
            if !store.hasApiKey {
                Text("Add an Anthropic API key in Settings to compose a Letter.")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
            } else {
                Button(action: composeLetter) {
                    Text("compose a letter")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.accent)
                }
            }
        }
    }

    private func reload() {
        do {
            letters = try LetterRepository.list()
        } catch {
            letters = []
        }
    }

    private func composeLetter() {
        compose = .writing
        Task {
            do {
                _ = try await LetterService.generate()
                await MainActor.run {
                    compose = .idle
                    store.bumpRefreshToken()
                }
            } catch {
                let message: String
                if case AnthropicError.missingApiKey = error {
                    message = "no API key set — visit Settings to add one."
                } else {
                    message = error.localizedDescription
                }
                await MainActor.run { compose = .error(message) }
            }
        }
    }
}

private struct LetterRow: View {
    let letter: Letter
    let onChange: () -> Void

    @State private var showingDeleteAlert: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                Text(DateFormatting.dateOnly(letter.createdAt))
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Theme.muted)
                Spacer()
                Button(action: { showingDeleteAlert = true }) {
                    Text("delete")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)
                }
            }

            VStack(alignment: .leading, spacing: 16) {
                ForEach(paragraphs(of: letter.body), id: \.self) { para in
                    Text(para)
                        .font(Theme.Font.serif(size: 16))
                        .foregroundStyle(Theme.ink)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .alert("Delete this Letter? This can’t be undone.", isPresented: $showingDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive, action: remove)
        }
    }

    private func paragraphs(of source: String) -> [String] {
        let trimmed = source.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed
            .components(separatedBy: "\n\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    private func remove() {
        Task {
            do {
                try LetterRepository.delete(id: letter.id)
                Mirrors.writeLettersMirror()
                await MainActor.run { onChange() }
            } catch {
                // best-effort
            }
        }
    }
}
