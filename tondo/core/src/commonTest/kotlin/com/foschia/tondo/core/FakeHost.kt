package com.foschia.tondo.core

/** A text field simulator implementing [KeyboardHost], used by tests and previews. */
class FakeHost : KeyboardHost {
    val text = StringBuilder()
    val vibrations = mutableListOf<Long>()
    val events = mutableListOf<String>()
    var pro = false
    var clipboard = "lorem ipsum, quia dolor sit, amet, consectetur"

    override fun commitText(text: String) { this.text.append(text) }
    override fun commitEmoji(text: String) { this.text.append(text) }
    override fun replaceLastCharacters(text: String) {
        if (this.text.length >= text.length) this.text.setLength(this.text.length - text.length)
        this.text.append(text)
    }
    override fun commitBackspace() { if (text.isNotEmpty()) text.setLength(text.length - 1) }
    override fun commitDone() { text.append('\n') }
    override fun commitCorrection(word: String) { events += "correction:$word" }
    override fun textBeforeCursor(n: Int) = text.takeLast(n).toString()
    override fun moveCursor(direction: CursorDirection, count: Int) { events += "cursor:$direction" }
    override fun performCtrl(action: CtrlAction) { events += "ctrl:$action" }
    override fun openKeyboardChooser() { events += "chooser" }
    override fun openSettings() { events += "settings" }
    override fun hideKeyboard() { events += "hide" }
    override fun vibrate(milliseconds: Long) { vibrations += milliseconds }
    override fun clipboardText() = clipboard
    override fun isPro() = pro
    override fun localize(key: String) = when (key) {
        L10n.MACRO_TUTORIAL_LEFT -> "Use the button above to paste the most recent text you copied! Or use a long press to pin it here!"
        L10n.MACRO_TUTORIAL_RIGHT -> "You can paste pinned text wherever you want, or long press it to remove it from your collection."
        L10n.MACRO_TUTORIAL_PRO -> "You have 6 notes at your disposal. Unlock the Pro version to increase the space to 20."
        else -> key
    }
    override fun showToast(message: String) { events += "toast:$message" }
    override fun setKeyboardProportion(proportion: Float) { events += "proportion:$proportion" }
    override fun requestRender() {}
    override fun hasRecordAudioPermission() = true
    override fun requestRecordAudioPermission() {}
    override fun startSpeechRecognition() { events += "speech:start" }
    override fun cancelSpeechRecognition() { events += "speech:cancel" }
    override fun readDictionary(fileName: String): ByteArray? = null
}
