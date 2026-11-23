import UIKit
import TondoCore

/// iOS keyboard extension entry point. It hosts the shared Kotlin `KeyboardEngine` (the same code
/// running on Android) and implements `KeyboardHost` on top of `UITextDocumentProxy`.
///
/// Memory: only the Kotlin/Native runtime, the keyboard logic and CoreGraphics drawing are loaded
/// (no game engine, no GPU surface), which keeps the extension far below the system limit.
final class KeyboardViewController: UIInputViewController {
    private var engine: KeyboardEngine!
    private var keyboardView: TondoKeyboardView!
    private var heightConstraint: NSLayoutConstraint?
    private var proportion: CGFloat = 1.5
    private lazy var haptics = UIImpactFeedbackGenerator(style: .light)

    override func viewDidLoad() {
        super.viewDidLoad()
        engine = KeyboardEngine(host: self, prefs: UserDefaultsPreferenceStore(), files: AppGroupFileStore())
        keyboardView = TondoKeyboardView(engine: engine)
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(keyboardView)
        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        startInput()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        engine.onHidden()
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        updateHeight()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        keyboardView.trimMemory()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        startInput()
    }

    override func selectionDidChange(_ textInput: UITextInput?) {
        super.selectionDidChange(textInput)
        engine.onCursorPositionChanged(textBeforeCursor: textDocumentProxy.documentContextBeforeInput ?? "")
    }

    private func startInput() {
        let proxy = textDocumentProxy
        var flags = EditorInputType.shared.TEXT
        switch proxy.keyboardType ?? .default {
        case .numberPad, .decimalPad, .phonePad, .numbersAndPunctuation, .asciiCapableNumberPad:
            flags = EditorInputType.shared.NUMERICAL
        default:
            break
        }
        if proxy.isSecureTextEntry == true { flags |= EditorInputType.shared.PASSWORD }
        let done: DoneAction
        switch proxy.returnKeyType ?? .default {
        case .done, .next, .continue, .join, .route: done = .done
        case .search, .google, .yahoo: done = .search
        case .send, .go: done = .send
        default: done = .newline
        }
        let dark = proxy.keyboardAppearance == .dark || traitCollection.userInterfaceStyle == .dark
        engine.onStartInput(info: EditorInfo(textBeforeCursor: proxy.documentContextBeforeInput ?? "",
                                             inputTypeFlags: flags, doneAction: done, nightMode: dark))
        keyboardView.requestRender()
    }

    private func updateHeight() {
        let width = view.bounds.width > 0 ? view.bounds.width : UIScreen.main.bounds.width
        let screenHeight = UIScreen.main.bounds.height
        // Same rule as KeyboardActionListener.setFrameLayoutParams on Android.
        let height = min(width / proportion, screenHeight / 1.82)
        if let c = heightConstraint {
            c.constant = height
        } else {
            let c = view.heightAnchor.constraint(equalToConstant: height)
            c.priority = UILayoutPriority(999)
            c.isActive = true
            heightConstraint = c
        }
    }

    /// Characters around the caret belonging to the word being edited.
    private func currentWordBeforeCursor() -> String {
        let before = textDocumentProxy.documentContextBeforeInput ?? ""
        return String(before.reversed().prefix { !$0.isWhitespace && !$0.isPunctuation }.reversed())
    }

    private func notifyEditingWord() {
        engine.onEditingWordChanged(word: currentWordBeforeCursor())
    }
}

// MARK: - KeyboardHost

extension KeyboardViewController: KeyboardHost {
    func commitText(text: String) {
        textDocumentProxy.insertText(text)
        notifyEditingWord()
    }

    func commitEmoji(text: String) {
        textDocumentProxy.insertText(text)
    }

    func replaceLastCharacters(text: String) {
        for _ in 0..<text.count { textDocumentProxy.deleteBackward() }
        textDocumentProxy.insertText(text)
    }

    func commitBackspace() {
        textDocumentProxy.deleteBackward()
        notifyEditingWord()
    }

    func commitDone() {
        textDocumentProxy.insertText("\n")
        notifyEditingWord()
    }

    func commitCorrection(word: String) {
        for _ in 0..<currentWordBeforeCursor().count { textDocumentProxy.deleteBackward() }
        textDocumentProxy.insertText(word + " ")
    }

    func textBeforeCursor(n: Int32) -> String {
        String((textDocumentProxy.documentContextBeforeInput ?? "").suffix(Int(n)))
    }

    func moveCursor(direction: CursorDirection, count: Int32) {
        let proxy = textDocumentProxy
        switch direction {
        case .left:
            if !(proxy.documentContextBeforeInput ?? "").isEmpty { proxy.adjustTextPosition(byCharacterOffset: -Int(count)) }
        case .right:
            if !(proxy.documentContextAfterInput ?? "").isEmpty { proxy.adjustTextPosition(byCharacterOffset: Int(count)) }
        case .up:
            // UITextDocumentProxy has no vertical movement: jump to the start of the previous line.
            let before = proxy.documentContextBeforeInput ?? ""
            let column = before.reversed().firstIndex(of: "\n").map { before.distance(from: $0.base, to: before.endIndex) } ?? before.count
            proxy.adjustTextPosition(byCharacterOffset: -(column + 1))
        case .down:
            let after = proxy.documentContextAfterInput ?? ""
            let toEnd = after.firstIndex(of: "\n").map { after.distance(from: after.startIndex, to: $0) } ?? after.count
            proxy.adjustTextPosition(byCharacterOffset: toEnd + 1)
        default:
            break
        }
    }

    func performCtrl(action: CtrlAction) {
        let proxy = textDocumentProxy
        switch action {
        case .theCopyText:
            if let s = proxy.selectedText { UIPasteboard.general.string = s }
        case .cutText:
            if let s = proxy.selectedText { UIPasteboard.general.string = s; proxy.deleteBackward() }
        case .pasteText:
            if let s = UIPasteboard.general.string { proxy.insertText(s) }
        default:
            // Select all / undo / redo are not exposed to keyboard extensions by iOS.
            break
        }
    }

    func openKeyboardChooser() {
        advanceToNextInputMode()
    }

    func openSettings() {
        // Keyboard extensions cannot call UIApplication.open: walk the responder chain to the host app.
        guard let url = URL(string: "tondokeyboard://settings") else { return }
        var responder: UIResponder? = self
        while let r = responder {
            if let app = r as? UIApplication {
                app.open(url, options: [:], completionHandler: nil)
                return
            }
            responder = r.next
        }
    }

    func hideKeyboard() {
        dismissKeyboard()
    }

    func vibrate(milliseconds: Int64) {
        guard milliseconds > 0 else { return }
        haptics.impactOccurred(intensity: min(1, CGFloat(milliseconds) / 20))
    }

    func clipboardText() -> String {
        guard hasFullAccess, UIPasteboard.general.hasStrings else { return "" }
        return UIPasteboard.general.string ?? ""
    }

    func isPro() -> Bool {
        AppGroup.defaults.bool(forKey: AppGroup.proKey)
    }

    func localize(key: String) -> String {
        NSLocalizedString(key, comment: "")
    }

    func showToast(message: String) {
        NSLog("[tOndO] %@", message)
    }

    func setKeyboardProportion(proportion: Float) {
        self.proportion = CGFloat(proportion)
        updateHeight()
    }

    func requestRender() {
        keyboardView?.requestRender()
    }

    func isSpeechRecognitionAvailable() -> Bool {
        // iOS keyboard extensions cannot access the microphone: users use the system dictation key.
        false
    }

    func hasRecordAudioPermission() -> Bool { false }
    func requestRecordAudioPermission() {}
    func startSpeechRecognition() {}
    func cancelSpeechRecognition() {}

    func readDictionary(fileName: String) -> KotlinByteArray? {
        let url = Bundle.main.bundleURL.appendingPathComponent("assets/dictionaries/\(fileName)")
        guard let data = try? Data(contentsOf: url, options: .mappedIfSafe) else { return nil }
        let bytes = KotlinByteArray(size: Int32(data.count))
        data.withUnsafeBytes { (raw: UnsafeRawBufferPointer) in
            for i in 0..<raw.count { bytes.set(index: Int32(i), value: Int8(bitPattern: raw[i])) }
        }
        return bytes
    }
}
