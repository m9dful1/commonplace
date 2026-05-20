import Foundation
import Observation

/// Lightweight observable store for cross-view reactivity. Each list view
/// re-fetches on `.onAppear` and on `bumpRefreshToken()` calls; that's the
/// iOS equivalent of the web app's `revalidatePath`.
@Observable
final class AppStore {
    var refreshToken: Int = 0
    var hasApiKey: Bool = SettingsStore.getAnthropicKey() != nil
    var hasSeenWelcome: Bool = UserDefaults.standard.hasSeenWelcome

    /// Mutable tab selection so the welcome screen can route the user
    /// straight to Settings when they tap "add an API key".
    var selectedTab: RootView.Tab = .fragments

    func bumpRefreshToken() {
        refreshToken &+= 1
    }

    func reloadHasApiKey() {
        hasApiKey = SettingsStore.getAnthropicKey() != nil
    }

    func markWelcomeSeen() {
        UserDefaults.standard.hasSeenWelcome = true
        hasSeenWelcome = true
    }

    /// Background-fired Ledger trigger after the Nth fragment. Mirrors the
    /// web app's fire-and-forget pattern in `addFragmentAction`.
    static let ledgerTriggerThreshold = 5

    func maybeFireLedgerTrigger() {
        guard SettingsStore.getAnthropicKey() != nil else { return }
        let count: Int
        do {
            count = try LedgerRepository.countFragmentsSinceLast()
        } catch {
            return
        }
        guard count >= Self.ledgerTriggerThreshold else { return }
        Task.detached(priority: .background) {
            do {
                let result = try await LedgerService.generate()
                if case .created = result {
                    await MainActor.run {
                        self.bumpRefreshToken()
                    }
                }
            } catch {
                // fire-and-forget; the recovery path is the manual "update
                // the ledger" button on the Ledger tab
            }
        }
    }
}
