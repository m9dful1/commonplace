import Foundation

enum AppPaths {
    /// `<appContainer>/Library/Application Support/Commonplace/db.sqlite`
    /// — the iOS-correct location for opaque private app data.
    static func databaseURL() -> URL {
        let supportDir = applicationSupportDirectory()
        return supportDir.appendingPathComponent("db.sqlite")
    }

    /// User-visible Documents directory. Mirror files (`LEDGER.md`,
    /// `LETTERS.md`) live here so the user can grab them via the Files app.
    static func documentsDirectory() -> URL {
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        ensureDirectoryExists(url)
        return url
    }

    static func ledgerMirrorURL() -> URL {
        return documentsDirectory().appendingPathComponent("LEDGER.md")
    }

    static func lettersMirrorURL() -> URL {
        return documentsDirectory().appendingPathComponent("LETTERS.md")
    }

    private static func applicationSupportDirectory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let dir = base.appendingPathComponent("Commonplace", isDirectory: true)
        ensureDirectoryExists(dir)
        return dir
    }

    private static func ensureDirectoryExists(_ url: URL) {
        let fm = FileManager.default
        if !fm.fileExists(atPath: url.path) {
            try? fm.createDirectory(at: url, withIntermediateDirectories: true)
        }
    }
}
