import Foundation
import SQLite3

// SQLite returns these as macros that aren't bridged to Swift. Pull them in
// the way the standard pattern does — use `unsafeBitCast` so the optimizer
// can collapse them. (See SQLITE_TRANSIENT in Apple's docs.)
internal let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

enum DatabaseError: Error {
    case openFailed(message: String)
    case prepareFailed(message: String)
    case stepFailed(message: String)
    case bindFailed(message: String)
}

final class Database: @unchecked Sendable {
    static let shared = Database()

    // Serial queue so all SQLite work happens off the main actor and
    // serialized against itself. WAL would let us do reader/writer concurrency
    // but a personal app's database is rarely contested; one queue is the
    // simplest correct thing.
    let queue = DispatchQueue(label: "com.commonplace.db", qos: .userInitiated)

    private var handle: OpaquePointer?

    private init() {}

    func openAndMigrate() throws {
        try queue.sync {
            try openLocked()
            try Migrations.run(on: handle!)
        }
    }

    private func openLocked() throws {
        if handle != nil { return }
        let path = AppPaths.databaseURL().path
        var db: OpaquePointer?
        let flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX
        let rc = sqlite3_open_v2(path, &db, flags, nil)
        if rc != SQLITE_OK {
            let msg = String(cString: sqlite3_errmsg(db))
            sqlite3_close(db)
            throw DatabaseError.openFailed(message: "could not open db at \(path): \(msg)")
        }
        // WAL mode + foreign keys, mirroring the web app's pragmas.
        sqlite3_exec(db, "PRAGMA journal_mode = WAL;", nil, nil, nil)
        sqlite3_exec(db, "PRAGMA foreign_keys = ON;", nil, nil, nil)
        handle = db
    }

    /// Run a closure with the raw connection on the database queue.
    /// Repositories use this; views never touch sqlite3 directly.
    func read<T>(_ block: (OpaquePointer) throws -> T) throws -> T {
        try queue.sync {
            guard let h = handle else {
                throw DatabaseError.openFailed(message: "database is not open")
            }
            return try block(h)
        }
    }

    func write<T>(_ block: (OpaquePointer) throws -> T) throws -> T {
        try queue.sync {
            guard let h = handle else {
                throw DatabaseError.openFailed(message: "database is not open")
            }
            return try block(h)
        }
    }

    func errorMessage() -> String {
        guard let h = handle else { return "" }
        return String(cString: sqlite3_errmsg(h))
    }
}

// MARK: - Statement helpers

struct Statement {
    let pointer: OpaquePointer

    init(_ db: OpaquePointer, _ sql: String) throws {
        var ptr: OpaquePointer?
        let rc = sqlite3_prepare_v2(db, sql, -1, &ptr, nil)
        if rc != SQLITE_OK || ptr == nil {
            let msg = String(cString: sqlite3_errmsg(db))
            throw DatabaseError.prepareFailed(message: "prepare failed: \(msg) — sql: \(sql)")
        }
        self.pointer = ptr!
    }

    func bindText(_ index: Int32, _ value: String?) throws {
        let rc: Int32
        if let value = value {
            rc = sqlite3_bind_text(pointer, index, value, -1, SQLITE_TRANSIENT)
        } else {
            rc = sqlite3_bind_null(pointer, index)
        }
        if rc != SQLITE_OK {
            throw DatabaseError.bindFailed(message: "bind text @\(index) failed (rc=\(rc))")
        }
    }

    func bindInt(_ index: Int32, _ value: Int64) throws {
        let rc = sqlite3_bind_int64(pointer, index, value)
        if rc != SQLITE_OK {
            throw DatabaseError.bindFailed(message: "bind int @\(index) failed (rc=\(rc))")
        }
    }

    func step() throws -> Bool {
        let rc = sqlite3_step(pointer)
        if rc == SQLITE_ROW { return true }
        if rc == SQLITE_DONE { return false }
        throw DatabaseError.stepFailed(message: "step failed (rc=\(rc))")
    }

    func text(_ column: Int32) -> String? {
        guard let cstr = sqlite3_column_text(pointer, column) else { return nil }
        return String(cString: cstr)
    }

    func int(_ column: Int32) -> Int64 {
        return sqlite3_column_int64(pointer, column)
    }

    func finalize() {
        sqlite3_finalize(pointer)
    }
}

extension Statement {
    /// Convenience: run() that finalizes for you and asserts step() returns done.
    static func exec(_ db: OpaquePointer, _ sql: String, bind: ((Statement) throws -> Void)? = nil) throws {
        let stmt = try Statement(db, sql)
        defer { stmt.finalize() }
        if let bind = bind { try bind(stmt) }
        _ = try stmt.step()
    }
}
