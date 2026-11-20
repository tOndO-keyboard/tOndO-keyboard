# tOndO keyboard for iOS

The iOS keyboard extension runs the same shared Kotlin code as Android (`../tondo/core`), compiled by
Kotlin/Native into a small static framework (`TondoCore.xcframework`). The Swift side is limited to:

| File | Role |
| --- | --- |
| `TondoKeyboard/KeyboardViewController.swift` | Implements `KeyboardHost` on top of `UITextDocumentProxy` (the iOS counterpart of `TondoInterface.java`). |
| `TondoKeyboard/TondoKeyboardView.swift` | Forwards multi-touch to the engine, drives the frame loop with `CADisplayLink` only while something animates. |
| `TondoKeyboard/ScenePainter.swift` | Paints the shared `Scene` with CoreGraphics (mirror of `CanvasPainter.kt`). |
| `Shared/SharedStorage.swift` | App Group `UserDefaults` and files, with the same keys as Android. |
| `TondoApp/*` | Container app: settings (SwiftUI) and Pro purchase (StoreKit 2). |

Assets (icons, Noto emoji, dictionaries) come from `../tondo/assets`, shared with Android.

## Build

Requirements: macOS with Xcode 16+, a JDK 17+ (for Gradle) and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
cd Xcode
xcodegen generate
open tOndO.xcodeproj
```

The `TondoCoreFramework` target runs `./gradlew :core:assembleTondoCoreReleaseXCFramework` in `../tondo`
before compiling Swift. Set your team in the signing settings of both targets and create the App Group
`group.com.foschia.tondokeyboard` in the developer portal.

## Memory budget

Keyboard extensions are killed when they exceed a few tens of MB. Unity could not stay below it; this
implementation loads no game engine and no GPU surface: Kotlin/Native runtime + keyboard logic +
CoreGraphics drawing. Images are decoded lazily (only visible emojis) into an 8 MB bounded cache that is
emptied on memory warnings. Measure on device with Xcode → Debug Navigator → Memory while typing and
scrolling the emoji list.

## Platform differences (iOS restrictions, not choices)

* **Voice input**: keyboard extensions have no microphone access, so the mic button is hidden; users
  can use the system dictation key.
* **CTRL bar**: copy, cut and paste work (Full Access required for the clipboard); select all, undo and
  redo are not exposed to keyboard extensions by `UITextDocumentProxy`.
* **Cursor joystick**: left/right are exact; up/down jump to the previous/next line (no vertical
  movement API).
* **Keyboard chooser**: switches to the next keyboard (`advanceToNextInputMode`).
* **Clipboard notes and haptics** need "Allow Full Access".
