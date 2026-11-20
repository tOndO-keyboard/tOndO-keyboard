package com.foschia.tondo.core

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the shared scene with the Java2D painter into build/previews.
 * Run with: ./gradlew :core:jvmTest --tests '*PreviewTest*'
 */
class PreviewTest {
    private val assets = File("../assets").takeIf { it.exists() } ?: File("assets")
    private val out = File("build/previews").apply { mkdirs() }

    private fun shot(name: String, w: Int = 1080, h: Int = 720, pro: Boolean = false, prefs: (MemoryPreferenceStore) -> Unit = {}, night: Boolean = false, setup: (KeyboardEngine, FakeHost) -> Unit = { _, _ -> }) {
        val host = FakeHost().also { it.pro = pro }
        val store = MemoryPreferenceStore().also(prefs)
        val engine = KeyboardEngine(host, store, MemoryFileStore())
        engine.setViewSize(w.toFloat(), h.toFloat())
        engine.onStartInput(EditorInfo("", nightMode = night))
        engine.render()
        setup(engine, host)
        engine.setViewSize(w.toFloat(), h.toFloat())
        val scene = engine.render()
        val img = Java2DPainter(assets).paint(scene, w, h)
        val f = File(out, "$name.png")
        ImageIO.write(img, "png", f)
        assertTrue(f.length() > 0)
    }

    private fun press(e: KeyboardEngine, pos: StickPosition, dir: CardinalDirection, hold: Long = 0) {
        val c = BottomLayout(e.widthUnits, if (e.topBarHidden) 10f else Dimensions.TOP_BAR_HEIGHT, e.landscape).tondo(pos)
        var t = 5000L
        e.onTouch(TouchEvent(0, TouchAction.DOWN, c.x * e.scale, c.y * e.scale, t))
        val target = c + dir.glyphOffset(70f)
        for (k in 1..5) {
            t += 10
            val p = c + (target - c) * (k / 5f)
            e.onTouch(TouchEvent(0, TouchAction.MOVE, p.x * e.scale, p.y * e.scale, t))
        }
        var tt = t
        while (tt < t + hold) { tt += 16; e.tick(tt) }
    }

    @Test
    fun renderPreviews() {
        shot("01_light")
        shot("02_dark", night = true)
        shot("03_pressed_joystick") { e, _ -> press(e, StickPosition.TOP_CENTER, CardinalDirection.CENTER) ; e.onTouch(TouchEvent(0, TouchAction.MOVE, (616f + 50f), 137f + 161.5f - 5f, 5100)) }
        shot("04_needle", prefs = { it.putString("InputType", "0") }) { e, _ -> press(e, StickPosition.TOP_CENTER, CardinalDirection.EAST) }
        shot("05_after_e_niche") { e, h -> h.text.append("He"); e.onCursorPositionChanged("He") }
        shot("06_long_press_alternates") { e, h -> h.text.append("n"); e.onCursorPositionChanged("n"); press(e, StickPosition.TOP_RIGHT, CardinalDirection.NORTH_EAST, hold = 400) }
        shot("07_symbols") { e, _ -> e.onSymbolsTap() }
        shot("08_symbols2") { e, _ -> e.onSymbolsTap(); e.toggleAlternateSymbols() }
        shot("09_hidden_bar", h = (1080 / 1.82f).toInt(), prefs = { it.putBoolean(TondoSettings.KEY_TOP_BAR_HIDDEN, true) })
        shot("10_bar_bottom_dark", night = true, prefs = { it.putBoolean("TopBarWentBottom", true) })
        shot("11_ctrl") { e, _ -> e.toggleControlMode() }
        shot("12_emoji", night = true) { e, _ -> e.commitEmoji("U+1F606"); e.commitEmoji("U+1F49A"); e.showView(KeyboardView.EMOJI) }
        shot("13_macros", night = true) { e, _ -> e.showView(KeyboardView.MACROS) }
        shot("14_mic") { e, _ -> e.showView(KeyboardView.MIC); e.tick(10_000); e.tick(10_500); e.onSpeechReady(); e.tick(11_000) }
        shot("15_landscape", w = 2400, h = 720)
        shot("16_suggestions") { e, _ -> e.suggestions = listOf("hello", "help", "helicopter"); e.showSuggestions(true) }
        shot("17_shift_locked") { e, _ -> e.setShift(ShiftState.DOUBLE_PRESSED) }
    }
}
