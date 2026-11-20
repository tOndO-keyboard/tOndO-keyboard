import Foundation
import TondoCore

/// App Group shared by the container app (settings, Pro purchase) and the keyboard extension.
enum AppGroup {
    static let identifier = "group.com.foschia.tondokeyboard"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: identifier) ?? .standard
    }

    static var containerURL: URL {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: identifier)
            ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    }

    /// Same key used by the Android billing code.
    static let proKey = "PURCHASED_PRO_V1"
}

/// The shared engine preferences, stored in the App Group UserDefaults with the same keys as Android.
final class UserDefaultsPreferenceStore: NSObject, PreferenceStore {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = AppGroup.defaults) {
        self.defaults = defaults
    }

    func getString(key: String, defaultValue: String) -> String {
        defaults.string(forKey: key) ?? defaultValue
    }

    func getInt(key: String, defaultValue: Int32) -> Int32 {
        defaults.object(forKey: key) == nil ? defaultValue : Int32(defaults.integer(forKey: key))
    }

    func getBoolean(key: String, defaultValue: Bool) -> Bool {
        defaults.object(forKey: key) == nil ? defaultValue : defaults.bool(forKey: key)
    }

    func putString(key: String, value: String) { defaults.set(value, forKey: key) }

    func putInt(key: String, value: Int32) { defaults.set(Int(value), forKey: key) }

    func putBoolean(key: String, value: Bool) { defaults.set(value, forKey: key) }

    func contains(key: String) -> Bool { defaults.object(forKey: key) != nil }
}

/// Recent emojis and pinned notes, stored as files in the App Group container.
final class AppGroupFileStore: NSObject, FileStore {
    private let directory: URL

    init(directory: URL = AppGroup.containerURL) {
        self.directory = directory
    }

    func read(name: String) -> String? {
        try? String(contentsOf: directory.appendingPathComponent(name), encoding: .utf8)
    }

    func write(name: String, content: String) {
        try? content.write(to: directory.appendingPathComponent(name), atomically: true, encoding: .utf8)
    }
}
