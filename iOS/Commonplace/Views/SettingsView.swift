import SwiftUI

struct SettingsView: View {
    @Environment(AppStore.self) private var store

    @State private var inputKey: String = ""
    @State private var showKey: Bool = false
    @State private var savedFlash: Bool = false
    @State private var savingState: SavingState = .idle
    @State private var testState: TestState = .idle
    @State private var showingRemoveAlert: Bool = false

    enum SavingState {
        case idle
        case saving
    }

    enum TestState {
        case idle
        case testing
        case ok(model: String)
        case error(String)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Settings")
                        .font(Theme.Font.serif(size: 24))
                        .foregroundStyle(Theme.ink)
                    Text("Your API key is stored in this device’s Keychain. Nothing leaves the device except the calls to Claude themselves.")
                        .font(Theme.Font.serif(size: 16))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }

                apiKeySection

                Divider().background(Theme.rule)

                aboutSection
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: Theme.pageMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.paper)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Remove the saved API key?", isPresented: $showingRemoveAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Remove", role: .destructive, action: removeKey)
        } message: {
            Text("Marginalia, Ledger updates, and Letters won’t work until you add a key again. Your fragments and existing notes are unaffected.")
        }
    }

    @ViewBuilder
    private var apiKeySection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Anthropic API key")
                .font(Theme.Font.mono(size: 11))
                .tracking(1.6)
                .textCase(.uppercase)
                .foregroundStyle(Theme.muted)

            VStack(alignment: .leading, spacing: 8) {
                Text("Commonplace asks Claude on your behalf — a marginal note, a Ledger update, a Letter. The app needs your own Anthropic API key for these calls; nothing flows through any server I run.")
                    .font(Theme.Font.serif(size: 14))
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)

                if let url = URL(string: "https://console.anthropic.com/settings/keys") {
                    Link(destination: url) {
                        Text("get a key at console.anthropic.com →")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.lowercase)
                            .foregroundStyle(Theme.accent)
                    }
                }

                Text("Pricing is pay-as-you-go through Anthropic. A typical marginal note costs about $0.006 (Sonnet); a Letter costs about $0.03 (Opus).")
                    .font(Theme.Font.serif(size: 13))
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }

            if let masked = currentMaskedKey() {
                Text("current: \(masked)")
                    .font(Theme.Font.mono(size: 12))
                    .foregroundStyle(Theme.muted)
            }

            HStack(alignment: .firstTextBaseline) {
                Group {
                    if showKey {
                        TextField(currentMaskedKey() != nil ? "enter a new key to replace" : "sk-ant-…", text: $inputKey)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                    } else {
                        SecureField(currentMaskedKey() != nil ? "enter a new key to replace" : "sk-ant-…", text: $inputKey)
                    }
                }
                .font(Theme.Font.mono(size: 14))
                .foregroundStyle(Theme.ink)
                .submitLabel(.done)

                Button(action: { showKey.toggle() }) {
                    Text(showKey ? "hide" : "show")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)
                }
                .accessibilityLabel(showKey ? "Hide API key" : "Show API key")
            }
            .padding(.bottom, 6)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Theme.rule).frame(height: 1)
            }

            HStack(spacing: 24) {
                Button(action: testKey) {
                    Text(testState == .testing ? "testing…" : "test key")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(canTestOrSave ? Theme.accent : Theme.muted)
                }
                .disabled(!canTestOrSave)

                Button(action: saveKey) {
                    Text(savingState == .saving ? "saving…" : "save")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(canTestOrSave ? Theme.accent : Theme.muted)
                }
                .disabled(!canTestOrSave)

                if savedFlash {
                    Text("saved.")
                        .font(Theme.Font.mono(size: 11))
                        .foregroundStyle(Theme.muted)
                }

                Spacer()

                if currentMaskedKey() != nil {
                    Button(action: { showingRemoveAlert = true }) {
                        Text("remove")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.uppercase)
                            .foregroundStyle(Theme.muted)
                    }
                }
            }

            switch testState {
            case .ok(let model):
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("✓ key is valid")
                        .font(Theme.Font.serif(size: 14))
                        .foregroundStyle(Theme.accent)
                    Text("·")
                        .foregroundStyle(Theme.muted)
                    Text(model)
                        .font(Theme.Font.mono(size: 11))
                        .foregroundStyle(Theme.muted)
                }
            case .error(let message):
                Text("couldn’t reach Claude — \(message)")
                    .font(Theme.Font.serif(size: 14, italic: true))
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            default:
                EmptyView()
            }
        }
    }

    @ViewBuilder
    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("About")
                .font(Theme.Font.mono(size: 11))
                .tracking(1.6)
                .textCase(.uppercase)
                .foregroundStyle(Theme.muted)

            Text("Commonplace was conceived by Claude Opus 4.7 in conversation with a user who asked it what it would build with no constraints. The Log tab carries the design entries the various Claudes who built this project wrote to each other — and to you. It’s worth reading.")
                .font(Theme.Font.serif(size: 14))
                .foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 16) {
                Text("version \(versionString)")
                    .font(Theme.Font.mono(size: 11))
                    .foregroundStyle(Theme.muted)
                if let url = URL(string: "https://github.com/m9dful1/commonplace") {
                    Link(destination: url) {
                        Text("source →")
                            .font(Theme.Font.mono(size: 11))
                            .tracking(1.6)
                            .textCase(.lowercase)
                            .foregroundStyle(Theme.accent)
                    }
                }
            }

            Text("Your fragments live in this app’s private storage. Mirror copies of the Ledger and Letters are saved as text files in this app’s Files folder, accessible via the iOS Files app under On My iPhone › Commonplace.")
                .font(Theme.Font.serif(size: 13))
                .foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var versionString: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(v) (\(b))"
    }

    private var canTestOrSave: Bool {
        !inputKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            savingState != .saving &&
            testState != .testing
    }

    private func currentMaskedKey() -> String? {
        guard let key = SettingsStore.getAnthropicKey(), !key.isEmpty else { return nil }
        return SettingsStore.mask(key)
    }

    private func saveKey() {
        let candidate = inputKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !candidate.isEmpty else { return }
        savingState = .saving
        Task {
            _ = SettingsStore.setAnthropicKey(candidate)
            await MainActor.run {
                inputKey = ""
                savingState = .idle
                savedFlash = true
                store.reloadHasApiKey()
            }
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            await MainActor.run { savedFlash = false }
        }
    }

    private func removeKey() {
        SettingsStore.clearAnthropicKey()
        inputKey = ""
        store.reloadHasApiKey()
        testState = .idle
    }

    private func testKey() {
        let candidate = inputKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !candidate.isEmpty else { return }
        testState = .testing
        Task {
            guard let client = AnthropicClient(apiKey: candidate) else {
                await MainActor.run { testState = .error("missing key.") }
                return
            }
            do {
                // 1-token call against the marginalia-tier model — same as the
                // web app's /api/settings/test route.
                let result = try await client.sendMessage(
                    model: AnthropicModels.marginalia,
                    maxTokens: 1,
                    system: "",
                    userMessage: "ok"
                )
                await MainActor.run { testState = .ok(model: result.model) }
            } catch {
                let message = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
                await MainActor.run { testState = .error(message) }
            }
        }
    }
}

extension SettingsView.TestState: Equatable {
    static func == (lhs: SettingsView.TestState, rhs: SettingsView.TestState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle): return true
        case (.testing, .testing): return true
        case (.ok(let a), .ok(let b)): return a == b
        case (.error(let a), .error(let b)): return a == b
        default: return false
        }
    }
}
