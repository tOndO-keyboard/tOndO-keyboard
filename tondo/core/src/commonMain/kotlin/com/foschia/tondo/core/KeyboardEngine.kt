package com.foschia.tondo.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The whole tOndO keyboard, platform independent.
 *
 * It replaces the Unity scene: KeyboardController, TondoAnalogStick, ShiftButton, SymbolsButton,
 * HideButton, BackspaceButton, SwipeAreaController, Emoji/Macros controllers... are all ported
 * here, while drawing is delegated to the platform through [render] (a [Scene] of primitives).
 *
 * Threading: every method must be called from the platform UI thread.
 */
class KeyboardEngine(
    internal val host: KeyboardHost,
    internal val prefs: PreferenceStore,
    files: FileStore,
) {
    // region state
    internal var settings = TondoSettings.load(prefs)
    internal var characters = CharacterCustomization(settings.characterCustomizationString)
    internal var diacritics = DiacriticsCustomization(settings.diacriticsCustomizationString, prefs)
    internal val emojis = EmojiStore(files)
    internal val macros = MacrosStore(files) {
        listOf(L10n.MACRO_TUTORIAL_PRO, L10n.MACRO_TUTORIAL_RIGHT, L10n.MACRO_TUTORIAL_LEFT).map { host.localize(it) }
    }
    internal val suggester = Suggester { host.readDictionary(it) }

    internal var theme = ColorTheme.LIGHT
    internal val palette get() = Palette.of(theme)

    internal var view = KeyboardView.MAIN
    internal var controlMode = false
    internal var suggestionsMode = false
    internal var suggestions: List<String> = emptyList()
    internal var layoutType = LayoutType.MAIN
    internal var shiftState = ShiftState.PRESSED
    private var setPressedAfterNextCommit = false
    private var lastShiftPressedTime = -10_000L
    internal var inputTypeFlags = EditorInputType.TEXT
    internal var doneAction = DoneAction.NEWLINE
    internal var precedingCharacter = ""
    internal var lastCommitWasEmoji = false
    internal var isPro = false
    internal var listeningToSpeech = false
    private var requestedDuringSpeech = KeyboardView.MAIN
    internal var toastText: String? = null
    internal var toastStart = 0L
    private var proToastFirstTapAt = -1L

    /** Clipboard text shown in the notes view. */
    internal var clipboard = ""
    /** Note currently showing its function icon (pin/delete) after a long press. */
    internal var functionActiveMacro: String? = null
    internal var clipboardFunctionActive = false

    internal val emojiScroller = Scroller()
    internal val macrosScroller = Scroller()
    internal var micRings = mutableListOf<MicRing>()
    private var nextRingAt = 0L

    internal var now = 0L
    private var lastTick = 0L
    // endregion

    // region geometry
    private var viewWidthPx = 1f
    private var viewHeightPx = 1f
    internal var scale = 1f
    internal var widthUnits = Dimensions.REFERENCE_WIDTH
    internal var heightUnits = 720f
    internal val landscape get() = widthUnits / heightUnits > Dimensions.LANDSCAPE_PROPORTION
    internal val topBarHidden get() = settings.topBarHidden
    internal val barOnBottom get() = settings.topBarWentBottom
    internal val contentHeight get() = Dimensions.BOTTOM_HEIGHT + if (topBarHidden) Dimensions.HIDDEN_TOP_HEIGHT else Dimensions.TOP_BAR_HEIGHT
    // endregion

    // region per-gesture state
    internal class TondoTouch(val pointerId: Int, val start: Vec) {
        var selected = CardinalDirection.CENTER
        var previousSelected = CardinalDirection.NULL
        var firstDirectionSelected = true
        var angle = 0f
        var distance = 0f
        var longPressPrevious = CardinalDirection.NULL
        var longPressTimer = 0L
        var isLongPressed = false
        var alternatesShown = false
        var alternates: List<String> = emptyList()
        var alternatesLeft = false
        /** Temporary labels shown on long press (direction -> glyph). */
        val temporaryLabels = mutableMapOf<CardinalDirection, String>()
    }

    internal val tondoTouches = mutableMapOf<StickPosition, TondoTouch>()
    /** Joystick visual offset per tondo, lerped back to zero on release. */
    internal val stickOffsets = mutableMapOf<StickPosition, Vec>()

    internal class DirectionalTouch(val pointerId: Int, val start: Vec) {
        var current = start
        var lastSubmit = -10_000L
        var previousSelected = CardinalDirection.NULL
        var firstDirectionSelected = true
    }

    internal var directionalTouch: DirectionalTouch? = null
    internal var directionalOffset = Vec.ZERO

    private class ButtonTouch(val pointerId: Int, val button: Control.Button, val start: Vec, val downTime: Long) {
        var last = start
        var cancelled = false
        var longPressFired = false
        var scrolling = false
        var lastRepeat = 0L
        var deletingWords = false
    }

    private val buttonTouches = mutableMapOf<Int, ButtonTouch>()
    internal val pressedButtons get() = buttonTouches.values.filter { !it.cancelled }.map { it.button.id }.toSet()

    private class SwipeTrack(val pointerId: Int, val start: Vec, val startTime: Long, val areas: List<SwipeArea>) {
        var fired = false
        var cancelled = false
    }

    internal class SwipeArea(
        val rect: Rect,
        val direction: SwipeDirection,
        val priority: Int,
        val minDistance: Float,
        val maxTimeMillis: Long,
        val checkOtherAreas: Boolean,
        val action: () -> Unit,
    )

    enum class SwipeDirection { UP, RIGHT, DOWN, LEFT }

    private var swipe: SwipeTrack? = null
    internal var controls: List<Control> = emptyList()
    internal var swipeAreas: List<SwipeArea> = emptyList()
    // endregion

    init {
        isPro = host.isPro()
        applySettings()
    }

    // region lifecycle (called by the platform)

    /** Port of NativeInterface.OnStartInputView + OnSetInputType. */
    fun onStartInput(info: EditorInfo) {
        settings = TondoSettings.load(prefs)
        applySettings()
        isPro = host.isPro()
        theme = when (settings.themeType) {
            ThemeType.FOLLOW_SYSTEM -> if (info.nightMode) ColorTheme.DARK else ColorTheme.LIGHT
            ThemeType.DARK -> ColorTheme.DARK
            ThemeType.LIGHT -> ColorTheme.LIGHT
        }
        doneAction = info.doneAction
        precedingCharacter = info.textBeforeCursor.takeLast(1)
        lastCommitWasEmoji = false
        controlMode = false
        suggestionsMode = false
        showView(KeyboardView.MAIN)
        // ShiftButton.OnInputViewStarted
        if (!settings.disableAutomaticCapitalization) setShift(if (precedingCharacter.isEmpty()) ShiftState.PRESSED else ShiftState.NOT_PRESSED)
        // SymbolsButton.OnInputViewStarted -> letters
        layoutType = LayoutType.MAIN
        // OnSetInputType
        inputTypeFlags = info.inputTypeFlags
        if ((inputTypeFlags and EditorInputType.NUMERICAL) != 0) layoutType = LayoutType.SYMBOLS_1
        if (!settings.disableAutomaticCapitalization && (inputTypeFlags and EditorInputType.PASSWORD) != 0) setShift(ShiftState.NOT_PRESSED)
        if (settings.enableSuggestions) suggester.ensureLanguage(settings.suggestionsLanguage)
        host.setKeyboardProportion(proportion())
        host.requestRender()
    }

    /** Port of NativeInterface.OnCursorPositionChanged. */
    fun onCursorPositionChanged(textBeforeCursor: String) {
        precedingCharacter = textBeforeCursor.takeLast(1)
        if (!settings.disableAutomaticCapitalization && shiftState == ShiftState.NOT_PRESSED &&
            precedingCharacter.isEmpty() && (inputTypeFlags and EditorInputType.PASSWORD) == 0
        ) setShift(ShiftState.PRESSED)
        host.requestRender()
    }

    /** Port of NativeInterface.OnEditingWordChange (the platform computes the word around the caret). */
    fun onEditingWordChanged(word: String) {
        if (!settings.enableSuggestions) return
        if (word.isEmpty()) {
            suggestions = emptyList()
            showSuggestions(false)
            return
        }
        suggester.ensureLanguage(settings.suggestionsLanguage)
        suggestions = if (suggester.isLoaded) suggester.suggest(word) else listOf(word)
        showSuggestions(suggestions.isNotEmpty())
    }

    fun onClipboardChanged(text: String) {
        clipboard = text
        host.requestRender()
    }

    fun onConfigurationChanged() {
        host.setKeyboardProportion(proportion())
        host.requestRender()
    }

    // Speech recognition callbacks (SpeechListener.java)
    fun onSpeechReady() { listeningToSpeech = true; host.requestRender() }
    fun onSpeechEnd() { listeningToSpeech = false; showView(KeyboardView.MAIN) }
    fun onSpeechError() {
        listeningToSpeech = false
        val v = requestedDuringSpeech
        requestedDuringSpeech = KeyboardView.MAIN
        showView(v)
    }
    fun onSpeechAudioLevel(@Suppress("UNUSED_PARAMETER") db: Float) {
        if (view == KeyboardView.MIC) emitRing(3f)
    }

    /** Keyboard hidden / app paused: persist and stop speech (KeyboardController.EnsureClosure). */
    fun onHidden() {
        if (listeningToSpeech) host.cancelSpeechRecognition()
        emojis.save()
        macros.save()
        tondoTouches.clear(); buttonTouches.clear(); directionalTouch = null; swipe = null
    }

    /** Width/height ratio the platform should give to the keyboard view. */
    fun proportion() = if (topBarHidden) Dimensions.HIDDEN_TOP_BAR_PROPORTION else Dimensions.DEFAULT_PROPORTION

    fun setViewSize(widthPx: Float, heightPx: Float) {
        viewWidthPx = widthPx.coerceAtLeast(1f)
        viewHeightPx = heightPx.coerceAtLeast(1f)
        scale = min(viewWidthPx / Dimensions.REFERENCE_WIDTH, viewHeightPx / contentHeight)
        widthUnits = viewWidthPx / scale
        heightUnits = viewHeightPx / scale
    }
    // endregion

    private fun applySettings() {
        characters = CharacterCustomization(settings.characterCustomizationString)
        diacritics = DiacriticsCustomization(settings.diacriticsCustomizationString, prefs)
        if (settings.disableAutomaticCapitalization && shiftState == ShiftState.PRESSED) shiftState = ShiftState.NOT_PRESSED
    }

    // region views (KeyboardController)
    internal fun showView(v: KeyboardView) {
        view = v
        if (v == KeyboardView.EMOJI) emojiScroller.offset = 0f
        if (v == KeyboardView.MACROS) {
            clipboard = host.clipboardText()
            macros.capacity = MACRO_SLOTS
            clipboardFunctionActive = false
            functionActiveMacro = null
        }
        if (v == KeyboardView.MIC && !listeningToSpeech) {
            micRings.clear()
            if (host.hasRecordAudioPermission()) host.startSpeechRecognition() else host.requestRecordAudioPermission()
        } else if (v != KeyboardView.MIC && listeningToSpeech) {
            requestedDuringSpeech = v
            host.cancelSpeechRecognition()
        }
        host.requestRender()
    }

    internal fun toggleView(v: KeyboardView) = showView(if (view != v) v else KeyboardView.MAIN)

    internal fun toggleControlMode() {
        controlMode = !controlMode
        host.requestRender()
    }

    internal fun showSuggestions(on: Boolean) {
        suggestionsMode = on
        if (controlMode) controlMode = false
        host.requestRender()
    }
    // endregion

    // region shift & symbols
    internal fun setShift(state: ShiftState) {
        if (shiftState == state) return
        shiftState = state
        host.requestRender()
    }

    /** ShiftButton.OnKeyTrigger */
    internal fun onShiftTap() {
        if (now - lastShiftPressedTime < 500) {
            setShift(ShiftState.DOUBLE_PRESSED)
            lastShiftPressedTime = -10_000L
            return
        }
        lastShiftPressedTime = now
        setShift(if (shiftState == ShiftState.NOT_PRESSED) ShiftState.PRESSED else ShiftState.NOT_PRESSED)
    }

    /** SymbolsButton.OnKeyTrigger */
    internal fun onSymbolsTap() {
        layoutType = if (layoutType == LayoutType.MAIN) LayoutType.SYMBOLS_1 else LayoutType.MAIN
        host.requestRender()
    }

    internal fun toggleAlternateSymbols() {
        layoutType = when (layoutType) {
            LayoutType.SYMBOLS_1 -> LayoutType.SYMBOLS_2
            LayoutType.SYMBOLS_2 -> LayoutType.SYMBOLS_1
            LayoutType.MAIN -> LayoutType.MAIN
        }
        host.requestRender()
    }

    /** TondoAnalogStick.GlyphCommitting listeners (auto capitalization after . ? ! etc). */
    private fun onGlyphCommitting(preceding: String, glyph: String) {
        if (preceding.isEmpty() || glyph.isEmpty() || settings.disableAutomaticCapitalization) return
        if (shiftState == ShiftState.NOT_PRESSED && preceding in SENTENCE_END && (glyph == " " || glyph == "\n")) {
            setPressedAfterNextCommit = true
        }
    }

    /** TondoAnalogStick.GlyphCommitted listeners (ShiftButton + SymbolsButton). */
    private fun onGlyphCommitted(glyph: String) {
        if (shiftState == ShiftState.PRESSED) setShift(ShiftState.NOT_PRESSED)
        if (!settings.disableAutomaticCapitalization && setPressedAfterNextCommit) {
            setPressedAfterNextCommit = false
            setShift(ShiftState.PRESSED)
        }
        if (glyph == " " && layoutType != LayoutType.MAIN) {
            layoutType = LayoutType.MAIN
            host.requestRender()
        }
    }

    /** Label of a glyph for the current layout and shift state (TondoGlyph.getCustomizedCharacter + SetCase). */
    internal fun glyphLabel(position: StickPosition, direction: CardinalDirection): String {
        val ch = characters.get(layoutType, position, direction)
        if (layoutType != LayoutType.MAIN || ch.length != 1 || !ch[0].isLetter()) return ch
        return if (shiftState == ShiftState.NOT_PRESSED) ch.lowercase() else ch.uppercase()
    }

    internal fun diacriticLabel(isLeft: Boolean): String =
        if (lastCommitWasEmoji) "" else diacritics.lastAlternate(precedingCharacter, isLeft) ?: ""
    // endregion

    // region text commits
    private fun refreshPreceding() {
        precedingCharacter = host.textBeforeCursor(1)
    }

    internal fun commitString(s: String) {
        lastCommitWasEmoji = false
        host.commitText(s)
        refreshPreceding()
    }

    internal fun commitEmoji(code: String) {
        emojis.use(code)
        host.commitEmoji(Emoji.toText(code, emojis.skinTone1, emojis.skinTone2))
        lastCommitWasEmoji = true
        refreshPreceding()
        host.requestRender()
    }

    internal fun commitBackspace() {
        lastCommitWasEmoji = false
        host.commitBackspace()
        refreshPreceding()
    }

    internal fun commitDone() {
        lastCommitWasEmoji = false
        host.commitDone()
        refreshPreceding()
        // ShiftButton.OnDoneCommitted
        if (!settings.disableAutomaticCapitalization && shiftState == ShiftState.NOT_PRESSED && doneAction == DoneAction.NEWLINE) {
            setShift(ShiftState.PRESSED)
        }
    }

    /** BackspaceButton.DeleteLastWord */
    internal fun deleteLastWord(): Boolean {
        var deleted = false
        var p = host.textBeforeCursor(1)
        fun isSep(s: String) = s.isNotEmpty() && (s == " " || s == "\n" || isPunctuation(s[0]))
        while (p.isNotEmpty() && isSep(p)) { host.commitBackspace(); p = host.textBeforeCursor(1); deleted = true }
        while (p.isNotEmpty() && !isSep(p)) { host.commitBackspace(); p = host.textBeforeCursor(1); deleted = true }
        refreshPreceding()
        host.requestRender()
        return deleted
    }

    internal fun vibrate() {
        val ms = settings.vibrationMillis
        if (ms > 0) host.vibrate(ms.toLong())
    }

    internal fun showToast(text: String) {
        toastText = text
        toastStart = now
        host.requestRender()
    }

    /** ToastAndRedirectLockOverride: first tap shows a toast, a second tap within 2s opens the settings. */
    internal fun proLockTap() {
        if (proToastFirstTapAt >= 0 && now - proToastFirstTapAt < 2000) {
            proToastFirstTapAt = -1
            host.openSettings()
        } else {
            proToastFirstTapAt = now
            showToast(host.localize(L10n.PRO_REQUIRED_MESSAGE))
        }
    }

    internal fun setTopBarHidden(hidden: Boolean) {
        if (settings.topBarHidden == hidden) return
        settings = settings.copy(topBarHidden = hidden)
        prefs.putBoolean(TondoSettings.KEY_TOP_BAR_HIDDEN, hidden)
        host.setKeyboardProportion(proportion())
        host.requestRender()
    }
    // endregion

    // region notes (MacrosController + ClipboardCommitButton)
    internal val macroLimit get() = if (isPro) MACRO_SLOTS else MACRO_SLOTS_FREE

    internal fun visibleMacros(): List<String> = macros.keys.take(if (isPro) macros.count else min(MACRO_SLOTS_FREE, macros.count))

    internal fun onClipboardTap() {
        if (clipboardFunctionActive) {
            clipboardFunctionActive = false
            if (!macros.contains(clipboard)) {
                if (macros.count < macroLimit) macros.add(clipboard) else showToast(host.localize(L10n.TOO_MANY_MACRO_TOAST))
            }
            host.requestRender()
            return
        }
        commitString(clipboard)
        if (macros.contains(clipboard)) macros.incrementUsage(clipboard)
    }

    internal fun onMacroTap(macro: String) {
        if (functionActiveMacro == macro) {
            functionActiveMacro = null
            macros.remove(macro)
            host.requestRender()
            return
        }
        functionActiveMacro = null
        commitString(macro)
        macros.incrementUsage(macro)
    }
    // endregion

    // region input
    private fun toUnits(x: Float, y: Float) = Vec(x / scale, y / scale)

    fun onTouch(e: TouchEvent) {
        now = e.timeMillis
        val p = toUnits(e.x, e.y)
        when (e.action) {
            TouchAction.DOWN -> onDown(e.pointerId, p)
            TouchAction.MOVE -> onMove(e.pointerId, p)
            TouchAction.UP -> onUp(e.pointerId, p, cancel = false)
            TouchAction.CANCEL -> onUp(e.pointerId, p, cancel = true)
        }
        host.requestRender()
    }

    private fun onDown(id: Int, p: Vec) {
        if (toastText != null && now - toastStart > 300) toastText = null
        val target = controls.lastOrNull { it.hit(p) }
        if (swipe == null) {
            swipe = SwipeTrack(id, p, now, swipeAreas.filter { p in it.rect })
        }
        when (target) {
            is Control.Tondo -> {
                if (tondoTouches.containsKey(target.position)) return
                vibrate()
                tondoTouches[target.position] = TondoTouch(id, p)
                swipe?.cancelled = true // checkAnalogSticksSelection
            }
            is Control.Directional -> {
                vibrate()
                directionalTouch = DirectionalTouch(id, p)
                swipe?.cancelled = true
            }
            is Control.Button -> {
                val t = ButtonTouch(id, target, p, now)
                buttonTouches[id] = t
                if (target.triggerOnDown) {
                    if (target.haptic) vibrate()
                    target.onTap()
                }
                if (target.repeating) t.lastRepeat = now
            }
            null -> Unit
        }
    }

    private fun onMove(id: Int, p: Vec) {
        tondoTouches.entries.firstOrNull { it.value.pointerId == id }?.let { (pos, t) -> moveTondo(pos, t, p) }
        directionalTouch?.takeIf { it.pointerId == id }?.let { it.current = p; updateDirectional(it) }
        buttonTouches[id]?.let { t ->
            val dy = p.y - t.last.y
            val sc = t.button.scroller
            if (sc != null && (t.scrolling || abs(p.y - t.start.y) > SCROLL_SLOP)) {
                t.scrolling = true
                t.cancelled = true
                sc.dragBy(dy)
                sc.velocity = if (now > 0) dy / 16f else 0f
            } else if (!t.cancelled && !t.button.bounds.inset(-CANCEL_SLOP).contains(p) && sc == null) {
                // KeyboardButton.OnPointerExit with deselectOnPointerExit
                t.cancelled = true
            }
            t.last = p
        }
        swipe?.takeIf { it.pointerId == id }?.let { checkSwipe(it, p) }
    }

    private fun onUp(id: Int, p: Vec, cancel: Boolean) {
        tondoTouches.entries.firstOrNull { it.value.pointerId == id }?.let { (pos, t) ->
            if (!cancel) submitTondo(pos, t)
            tondoTouches.remove(pos)
        }
        directionalTouch?.takeIf { it.pointerId == id }?.let { directionalTouch = null }
        buttonTouches.remove(id)?.let { t ->
            val b = t.button
            when {
                cancel || t.cancelled || b.triggerOnDown -> Unit
                b.onLongPress != null && t.longPressFired -> Unit
                b.repeating -> {
                    // BackspaceButton: a short tap deletes a single character.
                    if (now - t.downTime < BACKSPACE_INITIAL_DELAY && !t.deletingWords && swipe?.fired != true) {
                        if (b.haptic) vibrate()
                        b.onTap()
                    }
                }
                else -> {
                    if (b.haptic) vibrate()
                    b.onTap()
                }
            }
        }
        if (swipe?.pointerId == id) swipe = null
    }

    private fun checkSwipe(s: SwipeTrack, p: Vec) {
        if (s.fired || s.cancelled) return
        if (tondoTouches.isNotEmpty() || directionalTouch != null) { s.cancelled = true; return }
        val dt = now - s.startTime
        val dx = p.x - s.start.x
        val dy = p.y - s.start.y
        val dist = Vec(dx, dy).length()
        for (area in s.areas.sortedByDescending { it.priority }) {
            if (dt >= area.maxTimeMillis || dist <= area.minDistance) continue
            val ok = when (area.direction) {
                SwipeDirection.UP -> abs(dx) < MAX_SWIPE_DEVIATION && dy < 0
                SwipeDirection.DOWN -> abs(dx) < MAX_SWIPE_DEVIATION && dy > 0
                SwipeDirection.RIGHT -> abs(dy) < MAX_SWIPE_DEVIATION && dx > 0
                SwipeDirection.LEFT -> abs(dy) < MAX_SWIPE_DEVIATION && dx < 0
            }
            if (!ok) continue
            if (area.checkOtherAreas && s.areas.any { it !== area && it.direction == area.direction && it.priority > area.priority }) continue
            s.fired = true
            // A swipe started on a button replaces its tap (pointer left the button).
            buttonTouches[s.pointerId]?.let { it.cancelled = true; it.deletingWords = true }
            vibrate()
            area.action()
            return
        }
    }
    // endregion

    // region tondo (TondoAnalogStick)
    private fun moveTondo(pos: StickPosition, t: TondoTouch, p: Vec) {
        val size = Dimensions.TONDO_SIZE
        val clove = 360 / TONDO_VALID_DIRECTIONS.size
        var angle = if (settings.reverse) MathUtils.angleDegYUp(p, t.start) else MathUtils.angleDegYUp(t.start, p)
        val distance = p.distanceTo(t.start)
        if (settings.sticky) {
            val closest = MathUtils.closestMultiple(angle, clove)
            val threshold = clove * 0.5f
            if (abs(closest - angle) < threshold) angle = MathUtils.squircleAngle(angle, 3f, closest, threshold)
        } else if (settings.verySticky) {
            angle = MathUtils.closestMultiple(angle, clove)
        }
        if (angle < 0) angle += 360f
        if (angle >= 360f) angle -= 360f
        t.angle = angle
        t.distance = distance
        if (settings.inputMode == InputMode.JOYSTICK) {
            val d = min(distance * 0.5f, size * 0.06f)
            val rad = angle * DEG2RAD
            stickOffsets[pos] = Vec(d * cos(rad), -d * sin(rad))
        }
        var newSel = CardinalDirection.CENTER
        if (distance > size * (settings.externalGlyphSelectionDistanceFactor * 0.01f)) {
            newSel = TONDO_VALID_DIRECTIONS[0]
            val first = -(clove / 2f)
            for (i in TONDO_VALID_DIRECTIONS.indices) {
                if (angle >= first + clove * i && angle < first + clove * (i + 1)) newSel = TONDO_VALID_DIRECTIONS[i]
            }
        }
        if (!t.firstDirectionSelected && t.previousSelected != t.selected) vibrate()
        t.previousSelected = t.selected
        if (t.selected != CardinalDirection.CENTER && t.selected != CardinalDirection.NULL) t.firstDirectionSelected = false
        t.selected = newSel
    }

    private fun tickTondo(pos: StickPosition, t: TondoTouch, dt: Long) {
        if (t.longPressPrevious != CardinalDirection.NULL && t.longPressPrevious == t.selected) {
            t.longPressTimer += dt
            updateLongPress(pos, t, t.longPressTimer > LONG_PRESS_ALTERNATES)
        } else {
            t.longPressTimer = 0
            updateLongPress(pos, t, false)
        }
        t.longPressPrevious = t.selected
    }

    private fun updateLongPress(pos: StickPosition, t: TondoTouch, value: Boolean) {
        if (t.isLongPressed == value) return
        t.isLongPressed = value
        if (value) t.firstDirectionSelected = false
        if (!value || t.alternatesShown) return
        val diacritic = diacriticizerDirection(pos) ?: return
        if (t.selected != diacritic) return
        val isLeft = pos == StickPosition.TOP_LEFT
        if (lastCommitWasEmoji) return
        val alternates = diacritics.alternates(precedingCharacter, isLeft)
        if (alternates.isNullOrEmpty()) return
        val sorted = (TONDO_VALID_DIRECTIONS + CardinalDirection.CENTER).sortedWith(CardinalDirectionSorter(CardinalDirection.NORTH, isLeft))
        var index = 0
        for (d in sorted) {
            if (d == diacritic) continue
            t.temporaryLabels[d] = alternates.getOrElse(index) { "" }
            index++
        }
        t.alternates = alternates
        t.alternatesLeft = isLeft
        t.alternatesShown = true
        host.requestRender()
    }

    private fun submitTondo(pos: StickPosition, t: TondoTouch) {
        val dir = t.selected
        if (dir == CardinalDirection.NULL) return
        val diacritic = diacriticizerDirection(pos)
        val isDiacritic = dir == diacritic
        val selected: String = when {
            isDiacritic -> diacriticLabel(pos == StickPosition.TOP_LEFT)
            t.alternatesShown -> t.temporaryLabels[dir] ?: ""
            else -> glyphLabel(pos, dir).replace('␣', ' ')
        }
        if (t.alternatesShown) {
            val index = t.alternates.lastIndexOf(selected)
            if (index >= 0 && !lastCommitWasEmoji) diacritics.setLastSelectedIndex(precedingCharacter, index, t.alternatesLeft)
        }
        if (selected.isEmpty()) return
        onGlyphCommitting(precedingCharacter, selected)
        if (isDiacritic || t.alternatesShown) {
            lastCommitWasEmoji = false
            host.replaceLastCharacters(selected)
            refreshPreceding()
        } else {
            commitString(selected)
        }
        onGlyphCommitted(selected)
    }
    // endregion

    // region cursor joystick (DirectionalAnalogStick)
    private fun updateDirectional(t: DirectionalTouch) {
        val clove = 45
        val distance = t.current.distanceTo(t.start)
        var angle = MathUtils.closestMultiple(MathUtils.angleDegYUp(t.start, t.current), clove)
        if (angle == 360f) angle = 0f
        val d = min(distance * 0.5f, Dimensions.BUTTON_SIZE * 0.09f)
        val rad = angle * DEG2RAD
        directionalOffset = Vec(d * cos(rad), -d * sin(rad))
        var selected = CardinalDirection.NULL
        if (distance > Dimensions.BUTTON_SIZE * 0.025f) {
            val first = -(clove / 2f)
            for (i in DIRECTIONAL_DIRECTIONS.indices) {
                if (angle >= first + clove * i && angle < first + clove * (i + 1)) selected = DIRECTIONAL_DIRECTIONS[i]
            }
        }
        if (!t.firstDirectionSelected && t.previousSelected != selected) vibrate()
        t.previousSelected = selected
        if (selected != CardinalDirection.CENTER && selected != CardinalDirection.NULL) t.firstDirectionSelected = false
        if (selected == CardinalDirection.NULL) return
        val maxSpeedDistance = widthUnits * 0.5f
        val delay = MathUtils.lerp(250f, 10f, distance / maxSpeedDistance)
        if (now - t.lastSubmit > delay) {
            t.lastSubmit = now
            when (selected) {
                CardinalDirection.WEST, CardinalDirection.SOUTH_WEST, CardinalDirection.NORTH_WEST -> host.moveCursor(CursorDirection.LEFT, 1)
                CardinalDirection.EAST, CardinalDirection.SOUTH_EAST, CardinalDirection.NORTH_EAST -> host.moveCursor(CursorDirection.RIGHT, 1)
                CardinalDirection.NORTH -> host.moveCursor(CursorDirection.UP, 1)
                CardinalDirection.SOUTH -> host.moveCursor(CursorDirection.DOWN, 1)
                else -> Unit
            }
        }
    }
    // endregion

    // region animation
    internal class MicRing(val start: Long, val speed: Float, val fade: Float)

    private fun jitter(v: Float) = v + v * 0.05f * (((now * 7919L) % 2001L).toFloat() / 1000f - 1f)

    internal fun emitRing(factor: Float = 1f) {
        if (micRings.size >= 10) return
        micRings.add(MicRing(now, jitter(20f * factor), jitter(2000f)))
    }

    /**
     * Advances timers and animations. Call it on every frame while it returns true
     * (and after any touch event).
     * @return whether another frame is needed.
     */
    fun tick(timeMillis: Long): Boolean {
        val dt = if (lastTick == 0L) 16L else (timeMillis - lastTick).coerceIn(0L, 100L)
        lastTick = timeMillis
        now = timeMillis
        var animating = false

        for ((pos, t) in tondoTouches.toList()) tickTondo(pos, t, dt)
        directionalTouch?.let { updateDirectional(it); animating = true }

        // Long presses and backspace auto-repeat.
        for (t in buttonTouches.values) {
            val b = t.button
            if (t.cancelled) continue
            if (b.onLongPress != null && !t.longPressFired && now - t.downTime >= b.longPressMillis) {
                t.longPressFired = true
                vibrate()
                b.onLongPress.invoke()
            }
            if (b.repeating && now - t.downTime >= BACKSPACE_INITIAL_DELAY && now - t.lastRepeat > BACKSPACE_REPEAT_DELAY && swipe?.fired != true) {
                t.lastRepeat = now
                b.onTap()
            }
            animating = true
        }
        if (tondoTouches.isNotEmpty()) animating = true

        // Joystick spring back (AnalogStick.LerpAnalogStickToInitialLocation, 0.1s).
        for ((pos, off) in stickOffsets.toList()) {
            if (pos in tondoTouches) continue
            val k = (dt / 100f).coerceIn(0f, 1f)
            val n = off * (1f - k)
            if (n.length() < 0.01f) stickOffsets.remove(pos) else stickOffsets[pos] = n
            animating = true
        }
        if (directionalTouch == null && directionalOffset != Vec.ZERO) {
            val n = directionalOffset * (1f - (dt / 100f).coerceIn(0f, 1f))
            directionalOffset = if (n.length() < 0.01f) Vec.ZERO else n
            animating = true
        }

        if (emojiScroller.tick(now, dt)) animating = true
        if (macrosScroller.tick(now, dt)) animating = true

        if (view == KeyboardView.MIC) {
            if (now >= nextRingAt) {
                emitRing()
                nextRingAt = now + jitter(800f).toLong()
            }
            micRings.removeAll { now - it.start > it.fade }
            animating = true
        }
        toastText?.let {
            if (now - toastStart > TOAST_DURATION) toastText = null else animating = true
        }
        return animating
    }
    // endregion

    /** Builds the frame for the current state. */
    fun render(): Scene = SceneBuilder(this).build()

    internal fun registerFrame(controls: List<Control>, swipes: List<SwipeArea>) {
        this.controls = controls
        this.swipeAreas = swipes
    }

    companion object {
        internal const val DEG2RAD = (kotlin.math.PI / 180.0).toFloat()
        internal const val LONG_PRESS_ALTERNATES = 250L
        internal const val BACKSPACE_INITIAL_DELAY = 500L
        internal const val BACKSPACE_REPEAT_DELAY = 50L
        internal const val MAX_SWIPE_DEVIATION = 75f
        internal const val SCROLL_SLOP = 20f
        internal const val CANCEL_SLOP = 40f
        internal const val TOAST_DURATION = 4200L
        internal const val MACRO_SLOTS = 20
        internal const val MACRO_SLOTS_FREE = 6
        private val SENTENCE_END = setOf(".", "?", "!", "¿", "¡", "‽")
        private val DIRECTIONAL_DIRECTIONS = listOf(
            CardinalDirection.EAST, CardinalDirection.NORTH_EAST, CardinalDirection.NORTH, CardinalDirection.NORTH_WEST,
            CardinalDirection.WEST, CardinalDirection.SOUTH_WEST, CardinalDirection.SOUTH, CardinalDirection.SOUTH_EAST,
        )

        /** Equivalent of .NET char.IsPunctuation (Unicode categories Pc, Pd, Ps, Pe, Pi, Pf, Po). */
        internal fun isPunctuation(c: Char) = when (c.category) {
            CharCategory.CONNECTOR_PUNCTUATION, CharCategory.DASH_PUNCTUATION, CharCategory.START_PUNCTUATION,
            CharCategory.END_PUNCTUATION, CharCategory.INITIAL_QUOTE_PUNCTUATION, CharCategory.FINAL_QUOTE_PUNCTUATION,
            CharCategory.OTHER_PUNCTUATION,
            -> true
            else -> false
        }
    }
}
