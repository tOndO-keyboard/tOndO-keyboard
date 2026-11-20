package com.foschia.tondo.core

/** Port of DoneButton.DONE_ACTION_FLAG (same integer values used by the Android IME). */
enum class DoneAction(val value: Int) {
    DONE(0), NEWLINE(1), SEARCH(2), SEND(3);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: NEWLINE
    }
}

enum class CursorDirection { LEFT, RIGHT, UP, DOWN }

/** Entry names avoid clashing with NSObject selectors (copy/...) in the iOS export. */
enum class CtrlAction { COPY_TEXT, PASTE_TEXT, CUT_TEXT, SELECT_ALL, UNDO, REDO }

/** Port of EditorInputType.cs: bit flags. */
object EditorInputType {
    const val TEXT = 1
    const val NUMERICAL = 2
    const val DATE = 4
    const val PASSWORD = 8
}

/** Localization keys (the strings themselves live in the platform resources). */
object L10n {
    const val MACRO_TUTORIAL_LEFT = "macros_tutorial_left"
    const val MACRO_TUTORIAL_RIGHT = "macros_tutorial_right"
    const val MACRO_TUTORIAL_PRO = "macros_tutorial_pro"
    const val TOO_MANY_MACRO_TOAST = "too_many_macros_toast"
    const val PRO_REQUIRED_MESSAGE = "pro_required_message"
}

/**
 * Everything the shared keyboard needs from the platform. This is the replacement of
 * NativeInterface.cs (Unity side) + UnityInterface.java (Android side): instead of
 * reflection-based JNI calls through UnitySendMessage, the platform implements a plain interface.
 *
 * Android: implemented by the InputMethodService (KeyboardActionListener).
 * iOS: implemented by the UIInputViewController of the keyboard extension.
 */
interface KeyboardHost {
    // Text editing
    fun commitText(text: String)
    fun commitEmoji(text: String)
    fun replaceLastCharacters(text: String)
    fun commitBackspace()
    fun commitDone()
    fun commitCorrection(word: String)
    /** Up to [n] characters preceding the cursor (empty string when none). */
    fun textBeforeCursor(n: Int): String
    fun moveCursor(direction: CursorDirection, count: Int)
    fun performCtrl(action: CtrlAction)

    // System
    fun openKeyboardChooser()
    fun openSettings()
    fun hideKeyboard()
    fun vibrate(milliseconds: Long)
    fun clipboardText(): String
    fun isPro(): Boolean
    fun localize(key: String): String
    fun showToast(message: String)

    /** The keyboard wants a different height/width ratio (top bar hidden/shown). */
    fun setKeyboardProportion(proportion: Float)

    /** The engine has a new frame to draw (state changed or an animation is running). */
    fun requestRender()

    // Speech recognition (results are committed by the platform, the engine only drives the UI)
    /** False where the platform forbids it (iOS keyboard extensions have no microphone access): the mic button is hidden. */
    fun isSpeechRecognitionAvailable(): Boolean = true
    fun hasRecordAudioPermission(): Boolean
    fun requestRecordAudioPermission()
    fun startSpeechRecognition()
    fun cancelSpeechRecognition()

    /** Raw content of a bundled dictionary file (e.g. "en_US.dic"), or null. Used by the suggester. */
    fun readDictionary(fileName: String): ByteArray?
}

/** Info about the text field the keyboard is attached to. */
data class EditorInfo(
    val textBeforeCursor: String,
    val inputTypeFlags: Int = EditorInputType.TEXT,
    val doneAction: DoneAction = DoneAction.NEWLINE,
    val nightMode: Boolean = false,
)

/** Platform independent touch event. Coordinates are in view pixels. */
data class TouchEvent(val pointerId: Int, val action: TouchAction, val x: Float, val y: Float, val timeMillis: Long)

enum class TouchAction { DOWN, MOVE, UP, CANCEL }
