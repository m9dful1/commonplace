import Foundation
import Security

/// Anthropic API key storage. iOS Keychain — the platform-correct local
/// store for secrets. The settings table from the web app's schema is
/// preserved (in `Migrations.swift`) but unused here; if a future config
/// needs to live in the database, that's where it would go.
enum SettingsStore {
    private static let service = "com.commonplace.app"
    private static let account = "anthropic_api_key"

    static func getAnthropicKey() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status != errSecSuccess {
            return nil
        }
        guard let data = result as? Data, let s = String(data: data, encoding: .utf8) else { return nil }
        let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    @discardableResult
    static func setAnthropicKey(_ key: String) -> Bool {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return clearAnthropicKey()
        }
        let data = Data(trimmed.utf8)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let attrs: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]

        let updateStatus = SecItemUpdate(query as CFDictionary, attrs as CFDictionary)
        if updateStatus == errSecSuccess { return true }
        if updateStatus == errSecItemNotFound {
            var addQuery = query
            addQuery[kSecValueData as String] = data
            addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
            return addStatus == errSecSuccess
        }
        return false
    }

    @discardableResult
    static func clearAnthropicKey() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    /// Mirrors the web app's `maskKey` — first four, last four, dots between.
    static func mask(_ key: String?) -> String {
        guard let key = key else { return "" }
        if key.count <= 8 { return String(repeating: "•", count: key.count) }
        let prefix = key.prefix(4)
        let suffix = key.suffix(4)
        let middle = String(repeating: "•", count: key.count - 8)
        return "\(prefix)\(middle)\(suffix)"
    }
}
