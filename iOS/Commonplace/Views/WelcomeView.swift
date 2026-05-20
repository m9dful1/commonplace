import SwiftUI

/// First-launch welcome. Stays out of the way after dismissal — the
/// `commonplace.welcomeSeen` UserDefaults flag prevents it from appearing
/// again. The screen does three things and only three:
///   1. Says what the app is, in the project's own register.
///   2. Says it needs an Anthropic API key, and where to get one.
///   3. Hands off to Settings (or just dismisses, if the user already has a key).
struct WelcomeView: View {
    let hasApiKey: Bool
    var onContinue: () -> Void
    var onOpenSettings: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                Spacer().frame(height: 12)

                Text("Commonplace")
                    .font(Theme.Font.serif(size: 32))
                    .foregroundStyle(Theme.ink)

                VStack(alignment: .leading, spacing: 18) {
                    Text("A digital commonplace book for thinking with Claude.")
                        .font(Theme.Font.serif(size: 18))
                        .foregroundStyle(Theme.ink)
                        .fixedSize(horizontal: false, vertical: true)

                    Text("You capture fragments — quotes, observations, half-formed thoughts. Claude writes brief margin notes when you ask. Over time, a Ledger accumulates that describes the shape of what you’ve been gathering. Once in a while, you can request a Letter — a real letter, 250 to 400 words, from a thoughtful reader who has been keeping company with your fragments.")
                        .font(Theme.Font.serif(size: 16))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Divider().background(Theme.rule)

                VStack(alignment: .leading, spacing: 18) {
                    Text("What you’ll need")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)

                    Text("An Anthropic API key. The app uses it to ask Claude for marginalia, ledger entries, and letters — there’s no account on Commonplace itself. The key is stored in this device’s Keychain and is never sent anywhere except to Anthropic.")
                        .font(Theme.Font.serif(size: 16))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)

                    if let url = URL(string: "https://console.anthropic.com/settings/keys") {
                        Link(destination: url) {
                            Text("get an API key at console.anthropic.com →")
                                .font(Theme.Font.mono(size: 11))
                                .tracking(1.6)
                                .textCase(.lowercase)
                                .foregroundStyle(Theme.accent)
                        }
                    }

                    Text("Pricing is pay-as-you-go through Anthropic. A typical marginal note costs about $0.006 (Sonnet); a Letter costs about $0.03 (Opus). A few dollars of credit lasts most users a long time.")
                        .font(Theme.Font.serif(size: 14))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Divider().background(Theme.rule)

                VStack(alignment: .leading, spacing: 18) {
                    Text("On what this isn’t")
                        .font(Theme.Font.mono(size: 11))
                        .tracking(1.6)
                        .textCase(.uppercase)
                        .foregroundStyle(Theme.muted)

                    Text("Commonplace isn’t a chatbot. The app doesn’t respond unless you ask. There are no streaks, no notifications, no engagement loops. It opens fast, stays out of the way, and rewards slow attention.")
                        .font(Theme.Font.serif(size: 16))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer().frame(height: 8)

                HStack {
                    Spacer()
                    if hasApiKey {
                        Button(action: onContinue) {
                            Text("begin")
                                .font(Theme.Font.mono(size: 12))
                                .tracking(1.8)
                                .textCase(.uppercase)
                                .foregroundStyle(Theme.accent)
                        }
                    } else {
                        Button(action: onOpenSettings) {
                            Text("add an API key →")
                                .font(Theme.Font.mono(size: 12))
                                .tracking(1.8)
                                .textCase(.lowercase)
                                .foregroundStyle(Theme.accent)
                        }
                        Spacer().frame(width: 24)
                        Button(action: onContinue) {
                            Text("later")
                                .font(Theme.Font.mono(size: 11))
                                .tracking(1.6)
                                .textCase(.uppercase)
                                .foregroundStyle(Theme.muted)
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
    }
}

extension UserDefaults {
    fileprivate static let welcomeSeenKey = "commonplace.welcomeSeen"

    var hasSeenWelcome: Bool {
        get { bool(forKey: Self.welcomeSeenKey) }
        set { set(newValue, forKey: Self.welcomeSeenKey) }
    }
}
