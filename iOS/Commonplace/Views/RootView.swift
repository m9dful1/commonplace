import SwiftUI
import UIKit

struct RootView: View {
    @Environment(AppStore.self) private var store

    enum Tab: Hashable {
        case fragments
        case log
        case ledger
        case letters
        case settings
    }

    var body: some View {
        @Bindable var bindable = store
        TabView(selection: $bindable.selectedTab) {
            NavigationStack { FragmentsView() }
                .tabItem {
                    Label("fragments", systemImage: "doc.plaintext")
                }
                .tag(Tab.fragments)

            NavigationStack { LogView() }
                .tabItem {
                    Label("log", systemImage: "book.closed")
                }
                .tag(Tab.log)

            NavigationStack { LedgerView() }
                .tabItem {
                    Label("ledger", systemImage: "list.bullet")
                }
                .tag(Tab.ledger)

            NavigationStack { LettersView() }
                .tabItem {
                    Label("letters", systemImage: "envelope")
                }
                .tag(Tab.letters)

            NavigationStack { SettingsView() }
                .tabItem {
                    Label("settings", systemImage: "gearshape")
                }
                .tag(Tab.settings)
        }
        .tint(Theme.accent)
        .background(Theme.paper)
        .sheet(isPresented: welcomeBinding) {
            WelcomeView(
                hasApiKey: store.hasApiKey,
                onContinue: { store.markWelcomeSeen() },
                onOpenSettings: {
                    store.markWelcomeSeen()
                    store.selectedTab = .settings
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.hidden)
            .interactiveDismissDisabled(true)
        }
        .onAppear {
            // System UI nudges: paper-tinted tab bar, ink-toned text, no
            // shadow line at the top — the web app has no chrome and the iOS
            // equivalent is the un-emphasized tab bar.
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor(Theme.paper)
            appearance.shadowColor = UIColor(Theme.rule)

            // Use a smaller font for the tab labels so monospace lowercase
            // labels fit on iPhone alongside the SF Symbol icons.
            let labelFont = UIFont.monospacedSystemFont(ofSize: 10, weight: .regular)
            for state in [appearance.stackedLayoutAppearance,
                          appearance.inlineLayoutAppearance,
                          appearance.compactInlineLayoutAppearance] {
                state.normal.titleTextAttributes = [
                    .font: labelFont,
                    .foregroundColor: UIColor(Theme.muted)
                ]
                state.selected.titleTextAttributes = [
                    .font: labelFont,
                    .foregroundColor: UIColor(Theme.accent)
                ]
                state.normal.iconColor = UIColor(Theme.muted)
                state.selected.iconColor = UIColor(Theme.accent)
            }
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance

            let nav = UINavigationBarAppearance()
            nav.configureWithOpaqueBackground()
            nav.backgroundColor = UIColor(Theme.paper)
            nav.shadowColor = .clear
            nav.titleTextAttributes = [
                .foregroundColor: UIColor(Theme.ink),
                .font: UIFont(name: "Iowan Old Style", size: 17) ?? UIFont.systemFont(ofSize: 17)
            ]
            nav.largeTitleTextAttributes = [
                .foregroundColor: UIColor(Theme.ink),
                .font: UIFont(name: "Iowan Old Style", size: 28) ?? UIFont.systemFont(ofSize: 28)
            ]
            UINavigationBar.appearance().standardAppearance = nav
            UINavigationBar.appearance().compactAppearance = nav
            UINavigationBar.appearance().scrollEdgeAppearance = nav
        }
    }

    private var welcomeBinding: Binding<Bool> {
        Binding(
            get: { !store.hasSeenWelcome },
            set: { newValue in
                if !newValue { store.markWelcomeSeen() }
            }
        )
    }
}
