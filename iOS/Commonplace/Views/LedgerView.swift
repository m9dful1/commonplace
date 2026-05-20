import SwiftUI

struct LedgerView: View {
    @Environment(AppStore.self) private var store
    @State private var entries: [LedgerEntry] = []
    @State private var genState: GenState = .idle

    enum GenState {
        case idle
        case generating
        case passed
        case error(String)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                Text("what you’ve been gathering — the collection’s own journal")
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.lowercase)
                    .foregroundStyle(Theme.muted)

                if entries.isEmpty {
                    Text("The Ledger is empty. After five fragments, Claude may begin a first entry — or you can ask for one now.")
                        .font(Theme.Font.serif(size: 16, italic: true))
                        .foregroundStyle(Theme.muted)
                } else {
                    VStack(alignment: .leading, spacing: 36) {
                        ForEach(entries) { entry in
                            LedgerEntryRow(entry: entry, onChange: reload)
                        }
                    }
                }

                Divider()
                    .background(Theme.rule)

                generateAffordance
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Ledger")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { reload() }
        .onChange(of: store.refreshToken) { _, _ in reload() }
    }

    @ViewBuilder
    private var generateAffordance: some View {
        switch genState {
        case .generating:
            PulsingDots()
        case .passed:
            VStack(alignment: .leading, spacing: 6) {
                Text("pass — nothing has changed enough to warrant a new entry.")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
                Button(action: { genState = .idle }) {
                    Text("dismiss")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)
                }
            }
        case .error(let message):
            VStack(alignment: .leading, spacing: 6) {
                Text("couldn’t reach Claude — \(message)")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
                Button(action: generate) {
                    Text("try again")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.accent)
                }
            }
        case .idle:
            if !store.hasApiKey {
                Text("Add an Anthropic API key in Settings to update the Ledger.")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
            } else {
                Button(action: generate) {
                    Text("update the ledger")
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
            entries = try LedgerRepository.list()
        } catch {
            entries = []
        }
    }

    private func generate() {
        genState = .generating
        Task {
            do {
                let result = try await LedgerService.generate()
                await MainActor.run {
                    switch result {
                    case .pass:
                        genState = .passed
                    case .created:
                        genState = .idle
                        store.bumpRefreshToken()
                    }
                }
            } catch {
                let message: String
                if case AnthropicError.missingApiKey = error {
                    message = "no API key set — visit Settings to add one."
                } else {
                    message = error.localizedDescription
                }
                await MainActor.run { genState = .error(message) }
            }
        }
    }
}

private struct LedgerEntryRow: View {
    let entry: LedgerEntry
    let onChange: () -> Void

    @State private var editing: Bool = false
    @State private var draft: String = ""
    @State private var saving: Bool = false
    @State private var showingDeleteAlert: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(DateFormatting.dateOnly(entry.createdAt))
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Theme.muted)
                Text(entry.author.rawValue)
                    .font(Theme.Font.mono(size: 11))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Theme.accent)
                Spacer()
                if !editing {
                    Button(action: { startEditing() }) {
                        Text("edit")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.muted)
                    }
                    Button(action: { showingDeleteAlert = true }) {
                        Text("delete")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.muted)
                    }
                }
            }
            if editing {
                TextEditor(text: $draft)
                    .font(Theme.Font.serif(size: 16))
                    .foregroundStyle(Theme.ink)
                    .frame(minHeight: 120)
                    .scrollContentBackground(.hidden)
                    .background(Theme.paper)
                    .overlay(alignment: .bottom) {
                        Rectangle().fill(Theme.rule).frame(height: 1)
                    }
                HStack {
                    Button(action: cancel) {
                        Text("cancel")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Button(action: save) {
                        Text(saving ? "saving…" : "save")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(saving ? Theme.muted : Theme.accent)
                    }
                    .keyboardShortcut(.return, modifiers: .command)
                    .disabled(saving)
                }
            } else {
                Text(LightMarkdown.render(entry.body))
                    .font(Theme.Font.serif(size: 16))
                    .foregroundStyle(Theme.ink)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .alert("Delete this Ledger entry?", isPresented: $showingDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive, action: remove)
        }
    }

    private func startEditing() {
        draft = entry.body
        editing = true
    }

    private func cancel() {
        draft = entry.body
        editing = false
    }

    private func save() {
        let next = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        if next.isEmpty || next == entry.body.trimmingCharacters(in: .whitespacesAndNewlines) {
            editing = false
            return
        }
        saving = true
        Task {
            defer { saving = false }
            do {
                try LedgerRepository.update(id: entry.id, body: next)
                Mirrors.writeLedgerMirror()
                await MainActor.run {
                    editing = false
                    onChange()
                }
            } catch {
                await MainActor.run { editing = false }
            }
        }
    }

    private func remove() {
        Task {
            do {
                try LedgerRepository.delete(id: entry.id)
                Mirrors.writeLedgerMirror()
                await MainActor.run { onChange() }
            } catch {
                // best-effort
            }
        }
    }
}
