import SwiftUI

struct FragmentForm: View {
    @Environment(AppStore.self) private var store

    @State private var fragmentBody: String = ""
    @State private var source: String = ""
    @State private var tags: String = ""
    @State private var showMeta: Bool = false
    @State private var saving: Bool = false
    @FocusState private var bodyFocused: Bool
    @FocusState private var sourceFocused: Bool
    @FocusState private var tagsFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            TextField("A fragment…", text: $fragmentBody, axis: .vertical)
                .font(Theme.Font.serif(size: 18))
                .foregroundStyle(Theme.ink)
                .lineLimit(3...12)
                .focused($bodyFocused)
                .frame(maxWidth: .infinity, alignment: .topLeading)
                .padding(.bottom, 12)
                .overlay(alignment: .bottom) {
                    Rectangle()
                        .fill(bodyFocused ? Theme.accent.opacity(0.55) : Theme.rule)
                        .frame(height: 1)
                }
                .contentShape(Rectangle())
                .onTapGesture { bodyFocused = true }

            if showMeta {
                VStack(alignment: .leading, spacing: 12) {
                    TextField("source (optional)", text: $source)
                        .font(Theme.Font.mono(size: 12))
                        .foregroundStyle(Theme.ink)
                        .focused($sourceFocused)
                        .padding(.bottom, 6)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(sourceFocused ? Theme.accent.opacity(0.55) : Theme.rule)
                                .frame(height: 1)
                        }
                    TextField("tags, comma separated", text: $tags)
                        .font(Theme.Font.mono(size: 12))
                        .foregroundStyle(Theme.ink)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .focused($tagsFocused)
                        .padding(.bottom, 6)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(tagsFocused ? Theme.accent.opacity(0.55) : Theme.rule)
                                .frame(height: 1)
                        }
                }
                .padding(.top, 4)
            }

            HStack(alignment: .firstTextBaseline) {
                Button(action: { showMeta.toggle() }) {
                    Text(showMeta ? "− metadata" : "+ source / tags")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)
                }
                Spacer()
                Button(action: save) {
                    Text(saving ? "saving…" : "keep")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(canSave ? Theme.accent : Theme.muted)
                }
                .keyboardShortcut(.return, modifiers: .command)
                .disabled(!canSave)
            }
        }
    }

    private var canSave: Bool {
        !saving && !fragmentBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func save() {
        let trimmed = fragmentBody.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let snapshot = (body: trimmed, source: source, tagsRaw: tags)
        saving = true
        Task {
            do {
                _ = try FragmentRepository.create(
                    body: snapshot.body,
                    source: snapshot.source.isEmpty ? nil : snapshot.source,
                    tags: TagsParser.parse(snapshot.tagsRaw)
                )
                await MainActor.run {
                    fragmentBody = ""
                    source = ""
                    tags = ""
                    showMeta = false
                    bodyFocused = false
                    sourceFocused = false
                    tagsFocused = false
                    saving = false
                    store.bumpRefreshToken()
                }
                store.maybeFireLedgerTrigger()
            } catch {
                await MainActor.run { saving = false }
            }
        }
    }
}
