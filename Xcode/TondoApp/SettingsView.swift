import SwiftUI
import TondoCore

/// Settings shared with the keyboard through the App Group, with the same keys and values
/// used by the Android preferences (root_preferences.xml), so the shared engine reads them unchanged.
struct SettingsView: View {
    @EnvironmentObject private var store: ProStore

    @AppStorage("InputType", store: AppGroup.defaults) private var inputType = "2"
    @AppStorage("ThemeType", store: AppGroup.defaults) private var themeType = "0"
    @AppStorage("ShowPopupPreview", store: AppGroup.defaults) private var showPopupPreview = true
    @AppStorage("DisableAutomaticCapitalization", store: AppGroup.defaults) private var disableAutoCaps = false
    @AppStorage("TopBarWentBottom", store: AppGroup.defaults) private var barOnBottom = false
    @AppStorage("VibrationMilliseconds", store: AppGroup.defaults) private var vibration = 10
    @AppStorage("ExternalGlyphSelectionDistanceFactor", store: AppGroup.defaults) private var distanceFactor = 7
    @AppStorage("EnableSuggestions", store: AppGroup.defaults) private var suggestions = false
    @AppStorage("SuggestionsLanguage", store: AppGroup.defaults) private var language = "en_US"
    @AppStorage("CharacterCustomizationString", store: AppGroup.defaults) private var characters = TondoCore.SettingsKt.DEFAULT_CHARACTER_CUSTOMIZATION
    @AppStorage("DiacriticsCustomizationString", store: AppGroup.defaults) private var diacritics = TondoCore.SettingsKt.DEFAULT_DIACRITICS_CUSTOMIZATION

    @State private var charactersDraft = ""
    @State private var limitAlert = false

    var body: some View {
        Form {
            Section {
                Text("Settings → General → Keyboard → Keyboards → Add New Keyboard… → tOndO keyboard. Enable “Allow Full Access” for notes (clipboard) and haptic feedback.")
                    .font(.footnote)
            } header: { Text("Enable tOndO") }

            if !store.isPro {
                Section {
                    Text("Unlimited changes in the keyboard customization code, CTRL+A, more recent emojis and up to 20 pinned notes.")
                        .font(.footnote)
                    Button("Unlock Pro\(store.product.map { " – \($0.displayPrice)" } ?? "")") { Task { await store.purchase() } }
                    Button("Restore purchase") { Task { await store.restore() } }
                } header: { Text("Pro version") }
            }

            Section {
                Picker("Input type", selection: $inputType) {
                    Text("Joystick").tag("2")
                    Text("Needle").tag("0")
                }
                Picker("Theme", selection: $themeType) {
                    Text("Follow system").tag("0")
                    Text("Light").tag("1")
                    Text("Dark").tag("2")
                }
                Toggle("Show popup preview", isOn: $showPopupPreview)
                Toggle("Disable automatic capitalization", isOn: $disableAutoCaps)
                Toggle("Action bar on bottom", isOn: $barOnBottom)
                Stepper("Haptic feedback: \(vibration) ms", value: $vibration, in: 0...50)
                Stepper("External glyph selection distance: \(distanceFactor)", value: $distanceFactor, in: 1...50)
            } header: { Text("Base settings") }

            Section {
                TextEditor(text: $charactersDraft)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 260)
                    .onAppear { charactersDraft = characters }
                Button("Save keyboard customization code") {
                    let accepted = CustomizationRules.shared.accept(newCode: charactersDraft, previous: characters, isPro: store.isPro)
                    if accepted == characters && charactersDraft != characters { limitAlert = true }
                    characters = accepted
                    charactersDraft = accepted
                }
                Button("Reset keyboard customization code", role: .destructive) {
                    characters = TondoCore.SettingsKt.DEFAULT_CHARACTER_CUSTOMIZATION
                    charactersDraft = characters
                }
                TextEditor(text: $diacritics)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 200)
                Button("Reset diacriticizers customization code", role: .destructive) {
                    diacritics = TondoCore.SettingsKt.DEFAULT_DIACRITICS_CUSTOMIZATION
                }
            } header: { Text("Character customization") } footer: {
                Text(store.isPro ? "" : "Max \(CustomizationRules.shared.MAX_FREE_CHANGES) changes. Unlock the Pro version for unlimited editing.")
            }

            Section {
                Toggle("Enable suggestions", isOn: $suggestions)
                Picker("Suggestions language", selection: $language) {
                    Text("Deutsch").tag("de_DE")
                    Text("English").tag("en_US")
                    Text("Español").tag("es")
                    Text("Français").tag("fr")
                    Text("Italiano").tag("it_IT")
                }
                .disabled(!suggestions)
            } header: { Text("Experimental") }

            Section {
                Text("Emoji artwork: Noto Emoji by Google, Apache License 2.0.").font(.footnote)
                Text("Dictionaries licensed under GNU GPL.").font(.footnote)
            } header: { Text("Licenses") }
        }
        .navigationTitle("tOndO keyboard")
        .alert("You can edit a maximum of \(CustomizationRules.shared.MAX_FREE_CHANGES) characters. Unlock the pro version for unlimited editing.", isPresented: $limitAlert) {
            Button("OK", role: .cancel) {}
        }
    }
}
