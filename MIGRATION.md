# From Unity to Kotlin Multiplatform

This document explains why the Unity front-end was replaced, what replaced it and where every piece
of the old project went.

## The problem

The keyboard UI was a Unity project exported as a library (UaaL) and embedded in the Android input
method. On iOS the same approach failed: keyboard extensions have a very small memory budget (a few
tens of MB, enforced by the system) and Unity's runtime, renderer and scene could not fit in it.

## Options considered

| Option | One codebase for UI | Fits an iOS keyboard extension | Notes |
| --- | --- | --- | --- |
| Unity (current) | yes | **no** | Known UaaL memory issue, heavy runtime. |
| Flutter | yes | risky | A whole engine plus its renderer per extension; app extensions are not a supported Flutter target. |
| React Native | yes | risky | JS engine + bridge; RN documents a ~48 MB ceiling for keyboard extensions. |
| Compose Multiplatform (UI too) | yes | risky | Skia-based renderer in the extension; no published data for keyboard extensions. |
| Two native UIs (Kotlin + Swift) | no | yes | Duplicates all the logic and design, the two keyboards would drift apart. |
| **Kotlin Multiplatform core + shared "scene" + tiny native painters** | **yes** | **yes** | Chosen. |

## The chosen architecture

```
            tondo/core (Kotlin Multiplatform, Apache 2.0, open source)
  ┌──────────────────────────────────────────────────────────────────────┐
  │ KeyboardEngine: tondi gestures, shift/symbols, diacriticizers,       │
  │ swipes, emoji, notes, suggestions, settings, persistence             │
  │ SceneBuilder: the whole layout/design → Scene (circles, sectors,     │
  │ text, icons, images…) in design units, from Keyboard.prefab values   │
  └───────────────▲───────────────────────────────▲──────────────────────┘
        KeyboardHost│ Scene                 KeyboardHost│ Scene
  ┌────────────────┴──────────────┐   ┌───────────────┴───────────────────┐
  │ Android (tondo/android + app) │   │ iOS (Xcode/)                      │
  │ CanvasPainter  (~200 lines)   │   │ ScenePainter, CoreGraphics (~200) │
  │ TondoKeyboardView (touches)   │   │ TondoKeyboardView (touches)       │
  │ TondoInterface → InputMethod  │   │ KeyboardViewController → TextProxy│
  └───────────────────────────────┘   └───────────────────────────────────┘
```

* **Same structure as before**: a shared library (`tondo/core`, like the Unity export) imported by the
  Android Studio project and by the Xcode project. `TondoInterface.java` replaces `UnityInterface.java`
  and implements the `KeyboardHost` interface instead of exchanging `UnitySendMessage` strings.
* **One implementation of the design**: every position, size, color and animation lives in the shared
  `SceneBuilder`; platforms only know how to paint ~13 primitives. The keyboard is pixel-consistent on
  Android and iOS by construction.
* **Small footprint**: no game engine, no GPU surface, no JS VM. The Unity library (43 MB of native
  libraries for two ABIs plus 18 MB of data) is gone: the debug APK is now 17 MB, mostly the Noto emoji
  artwork. On iOS the extension only links Kotlin/Native code and CoreGraphics.
* **Testable**: the whole keyboard runs on the JVM. `./gradlew :core:jvmTest` types text through
  simulated touches and renders previews (`tondo/core/build/previews/*.png`) with a Java2D painter.

## Where things went

| Unity | New location |
| --- | --- |
| `NativeInterface.cs` / `UnityInterface.java` | `KeyboardHost.kt` (interface) / `TondoInterface.java` (Android), `KeyboardViewController.swift` (iOS) |
| `KeyboardController.cs` | `KeyboardEngine.showView / toggleView / controlMode / suggestions` |
| `TondoAnalogStick.cs`, `AnalogStick.cs`, `TondoGlyph.cs` | `KeyboardEngine` (region *tondo*) + `SceneBuilder.drawTondo` |
| `DirectionalAnalogStick.cs` | `KeyboardEngine.updateDirectional` |
| `ShiftButton.cs`, `SymbolsButton.cs`, `SymbolsTondosController.cs` | `KeyboardEngine` (region *shift & symbols*) |
| `BackspaceButton.cs`, `DoneButton.cs`, `HideButton.cs`, CTRL/Copy/Paste/... buttons | `SceneBuilder` buttons + `KeyboardEngine` actions |
| `SwipeAreaController.cs` | `KeyboardEngine.SwipeArea` / `checkSwipe` (same distances, times, priorities) |
| `SettingsManager.cs` | `TondoSettings` (same preference keys) |
| `CharacterCustomization.cs`, `DiacriticsCustomization.cs` | `Customization.kt` |
| `EmojiPersister.cs`, `MacrosPersister.cs` | `Stores.kt` (reads the JSON files written by Unity) |
| `EmojiSprite.cs`, `EmojisController.cs`, `SkinToneButton.cs`, emoji prefab | `Emoji.kt`, `EmojiData.kt` (extracted from the prefab), `SceneBuilder.drawEmoji` |
| `MacrosController.cs`, `ClipboardCommitButton.cs`, `PinButtonsContainer.cs` | `KeyboardEngine` (region *notes*) + `SceneBuilder.drawMacros` |
| `MicRings.cs`, `SinusoidalIcon.cs`, `SpeechInterface.cs` | `SceneBuilder.drawMic` + speech callbacks |
| `ProLock.cs`, `ToastAndRedirectLockOverride.cs`, `Toast.cs` | `KeyboardEngine.proLockTap / showToast` |
| `Suggester.cs` (WeCantSpell.Hunspell) | `Suggester.kt` (same bundled `.dic` files) |
| Themes `*.asset`, `PALETTE_ELEMENT.cs` | `Theme.kt` (exact colors) |
| `Keyboard.prefab` geometry | `Layout.kt` / `SceneBuilder.kt` |
| Sprites, Noto emoji | `tondo/assets` (shared by Android and iOS) |

## Behaviour differences worth knowing

* **Suggestions** (experimental in both versions): Hunspell affix rules are not expanded; suggestions
  come from dictionary stems (typos at edit distance 1, case fixes and completions). The goal was to
  keep the dictionaries' memory use compatible with the iOS extension.
* **Diacriticizers**: the last selected variant per letter is now restored after a restart (Unity
  saved it but never read it back). The "top bar hidden" state moves from Unity PlayerPrefs to the app
  preferences, so it resets once after the update.
* **Fonts**: the design uses Arial (TextMeshPro asset). iOS ships Arial; Android uses the system
  sans-serif unless a font is placed in `tondo/assets/fonts/tondo.ttf`.
* **iOS restrictions** (no microphone in keyboard extensions, no undo/redo/select-all API): see
  [Xcode/README.md](Xcode/README.md).

## Status

* `tondo/core`: builds; unit tests and preview rendering pass on the JVM.
* Android: `app` builds (`./gradlew :app:assembleDebug`) without Unity. It still has to be tried on a
  real device (touch feel, haptics, speech, IME lifecycle).
* iOS: sources and XcodeGen project are ready but were written without a Mac; they need a first build
  in Xcode and a memory measurement on device.
* `Unity/` is no longer used by any build and can be deleted once the new keyboard is validated.
