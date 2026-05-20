import SwiftUI

@main
struct CommonplaceApp: App {
    @State private var store = AppStore()

    init() {
        try? Database.shared.openAndMigrate()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .preferredColorScheme(.light)
                .tint(Theme.accent)
        }
    }
}
