package com.foschia.tondo.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyboardEngineTest {
    private class Rig(pro: Boolean = false, configure: (MemoryPreferenceStore) -> Unit = {}) {
        val host = FakeHost().also { it.pro = pro }
        val prefs = MemoryPreferenceStore().also(configure)
        val engine = KeyboardEngine(host, prefs, MemoryFileStore())
        var t = 1000L

        init {
            engine.setViewSize(1080f, 720f)
            engine.onStartInput(EditorInfo(""))
            engine.render()
        }

        fun center(pos: StickPosition) = BottomLayout(1080f, Dimensions.TOP_BAR_HEIGHT, false).tondo(pos)

        fun ev(id: Int, a: TouchAction, p: Vec) {
            engine.onTouch(TouchEvent(id, a, p.x, p.y, t))
            engine.render()
        }

        fun advance(ms: Long) {
            val end = t + ms
            while (t < end) { t += 16; engine.tick(t) }
            engine.render()
        }

        /** Swipe on a tondo towards [dir] (or tap for CENTER), optionally holding before release. */
        fun type(pos: StickPosition, dir: CardinalDirection, holdMs: Long = 0) {
            val c = center(pos)
            ev(0, TouchAction.DOWN, c)
            val target = c + dir.glyphOffset(60f)
            if (dir != CardinalDirection.CENTER) {
                for (k in 1..5) { t += 10; ev(0, TouchAction.MOVE, c + (target - c) * (k / 5f)) }
            }
            if (holdMs > 0) advance(holdMs)
            t += 10
            ev(0, TouchAction.UP, target)
            t += 600
            engine.tick(t)
        }

        fun tapButton(id: String) {
            val b = engine.controls.filterIsInstance<Control.Button>().first { it.id == id }
            ev(0, TouchAction.DOWN, b.bounds.center)
            t += 30
            ev(0, TouchAction.UP, b.bounds.center)
            t += 600
        }
    }

    @Test
    fun typesLettersWithAutoCapitalization() {
        val r = Rig()
        r.type(StickPosition.TOP_LEFT, CardinalDirection.CENTER) // E (shift pressed at start)
        r.type(StickPosition.TOP_LEFT, CardinalDirection.EAST) // t
        r.type(StickPosition.TOP_LEFT, CardinalDirection.SOUTH_EAST) // r
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.SOUTH_WEST) // !
        r.type(StickPosition.BOTTOM_CENTER, CardinalDirection.CENTER) // space -> shift again
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.CENTER) // O
        assertEquals("Etr! O", r.host.text.toString())
    }

    @Test
    fun diacriticizerReplacesPrecedingCharacter() {
        val r = Rig()
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.EAST) // K (shift)
        r.type(StickPosition.TOP_LEFT, CardinalDirection.CENTER) // e
        // Left diacriticizer shows "è" (default index 1 of "éèēəęǝ").
        r.type(StickPosition.TOP_LEFT, CardinalDirection.NORTH_WEST)
        assertEquals("Kè", r.host.text.toString())
        r.type(StickPosition.TOP_LEFT, CardinalDirection.CENTER) // e
        // Right diacriticizer: default index 1 of "èéêëėɛ" -> "é" replaces "e".
        r.type(StickPosition.TOP_RIGHT, CardinalDirection.NORTH_EAST)
        assertEquals("Kèé", r.host.text.toString())
    }

    @Test
    fun longPressOnDiacriticizerShowsAlternatesAndRemembersChoice() {
        val r = Rig()
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.CENTER) // O
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.WEST) // b
        r.type(StickPosition.BOTTOM_RIGHT, CardinalDirection.NORTH_WEST) // n
        val c = r.center(StickPosition.TOP_RIGHT)
        r.ev(0, TouchAction.DOWN, c)
        val niche = c + CardinalDirection.NORTH_EAST.glyphOffset(60f)
        for (k in 1..5) { r.t += 10; r.ev(0, TouchAction.MOVE, c + (niche - c) * (k / 5f)) }
        r.advance(400) // long press -> alternates of "n" are spread on the tondo
        // Right alternates "ñńǹňᵰṅ" are placed starting from NW counter-clockwise: NW, W, SW, SE, E, center.
        val west = c + CardinalDirection.WEST.glyphOffset(60f)
        for (k in 1..5) { r.t += 10; r.ev(0, TouchAction.MOVE, niche + (west - niche) * (k / 5f)) }
        r.ev(0, TouchAction.UP, west)
        assertEquals("Obń", r.host.text.toString())
        // Next time the niche proposes the selected variant.
        assertEquals("ń", r.engine.diacritics.lastAlternate("n", false))
    }

    @Test
    fun symbolsAndSpaceReturnToLetters() {
        val r = Rig()
        r.tapButton("symbols")
        assertEquals(LayoutType.SYMBOLS_1, r.engine.layoutType)
        r.type(StickPosition.TOP_LEFT, CardinalDirection.CENTER) // $
        r.type(StickPosition.BOTTOM_CENTER, CardinalDirection.CENTER) // space
        assertEquals("$ ", r.host.text.toString())
        assertEquals(LayoutType.MAIN, r.engine.layoutType)
    }

    @Test
    fun shiftDoubleTapLocksCaps() {
        val r = Rig()
        r.type(StickPosition.TOP_LEFT, CardinalDirection.CENTER) // E -> shift released
        assertEquals(ShiftState.NOT_PRESSED, r.engine.shiftState)
        val b = r.engine.controls.filterIsInstance<Control.Button>().first { it.id == "shift" }
        r.ev(0, TouchAction.DOWN, b.bounds.center); r.t += 20; r.ev(0, TouchAction.UP, b.bounds.center)
        r.t += 150
        r.ev(0, TouchAction.DOWN, b.bounds.center); r.t += 20; r.ev(0, TouchAction.UP, b.bounds.center)
        assertEquals(ShiftState.DOUBLE_PRESSED, r.engine.shiftState)
        r.t += 600
        r.type(StickPosition.TOP_LEFT, CardinalDirection.EAST)
        r.type(StickPosition.TOP_LEFT, CardinalDirection.EAST)
        assertEquals("ETT", r.host.text.toString())
    }

    @Test
    fun backspaceDeletesAndSwipeLeftDeletesWord() {
        val r = Rig()
        r.host.text.append("hello world")
        r.tapButton("backspace")
        assertEquals("hello worl", r.host.text.toString())
        val b = r.engine.controls.filterIsInstance<Control.Button>().first { it.id == "backspace" }.bounds.center
        r.ev(0, TouchAction.DOWN, b)
        for (k in 1..6) { r.t += 15; r.ev(0, TouchAction.MOVE, b + Vec(-50f * k, 0f)) }
        r.ev(0, TouchAction.UP, b + Vec(-300f, 0f))
        assertEquals("hello ", r.host.text.toString())
    }

    @Test
    fun emojiCommitAndRecents() {
        val r = Rig()
        r.tapButton("emoji")
        assertEquals(KeyboardView.EMOJI, r.engine.view)
        val cell = r.engine.controls.filterIsInstance<Control.Button>().first { it.id.startsWith("emoji:1:") }
        r.ev(0, TouchAction.DOWN, cell.bounds.center); r.t += 20; r.ev(0, TouchAction.UP, cell.bounds.center)
        assertEquals("😀", r.host.text.toString())
        assertEquals(1, r.engine.emojis.count)
    }

    @Test
    fun pinClipboardNote() {
        val r = Rig()
        r.tapButton("macros")
        assertEquals(KeyboardView.MACROS, r.engine.view)
        assertEquals(3, r.engine.macros.count) // tutorial notes
        val clip = r.engine.controls.filterIsInstance<Control.Button>().first { it.id == "clipboard" }
        r.ev(0, TouchAction.DOWN, clip.bounds.center)
        r.advance(600) // long press -> pin icon
        r.ev(0, TouchAction.UP, clip.bounds.center)
        assertTrue(r.engine.clipboardFunctionActive)
        r.tapButton("clipboard")
        assertTrue(r.engine.macros.contains(r.host.clipboard))
    }

    @Test
    fun swipeDownHidesActionBar() {
        val r = Rig()
        val start = Vec(560f, 150f) // between tondi
        r.ev(0, TouchAction.DOWN, start)
        for (k in 1..5) { r.t += 20; r.ev(0, TouchAction.MOVE, start + Vec(0f, 50f * k)) }
        r.ev(0, TouchAction.UP, start + Vec(0f, 250f))
        assertTrue(r.engine.topBarHidden)
        assertEquals(Dimensions.HIDDEN_TOP_BAR_PROPORTION, r.engine.proportion())
    }

    @Test
    fun customizationCodeParsing() {
        val c = CharacterCustomization(DEFAULT_CHARACTER_CUSTOMIZATION)
        assertEquals("E", c.get(LayoutType.MAIN, StickPosition.TOP_LEFT, CardinalDirection.CENTER))
        assertEquals("*", c.get(LayoutType.MAIN, StickPosition.TOP_LEFT, CardinalDirection.NORTH_EAST))
        assertEquals("^", c.get(LayoutType.MAIN, StickPosition.TOP_RIGHT, CardinalDirection.NORTH_WEST))
        assertEquals("", c.get(LayoutType.MAIN, StickPosition.TOP_RIGHT, CardinalDirection.NORTH_EAST))
        assertEquals("#", c.get(LayoutType.MAIN, StickPosition.BOTTOM_LEFT, CardinalDirection.NORTH_WEST))
        assertEquals("≠", c.get(LayoutType.SYMBOLS_2, StickPosition.BOTTOM_RIGHT, CardinalDirection.NORTH_WEST))
    }

    @Test
    fun suggesterFindsTyposAndCompletions() {
        val s = Suggester { null }
        s.load("5\nhello/S\nhelp\nworld\nRome\nhelicopter/M\n".encodeToByteArray())
        assertTrue("hello" in s.suggest("helo"))
        assertEquals("Rome", s.suggest("rome").first())
        assertTrue("helicopter" in s.suggest("heli"))
    }

    @Test
    fun emojiStoreReadsUnityJson() {
        val files = MemoryFileStore()
        files.write(EmojiStore.FILE_NAME, """{"_usages":[3,7],"_emojis":["U+1F600","U+1F44B #skin_1"],"_skinTone1":127995,"_skinTone2":0,"_capacity":27}""")
        val store = EmojiStore(files)
        assertEquals(listOf("U+1F44B #skin_1", "U+1F600"), store.descendingKeys())
        assertEquals(SkinTone.LIGHT, store.skinTone1)
        assertEquals("👋🏻", Emoji.toText("U+1F44B #skin_1", store.skinTone1, store.skinTone2))
    }

    @Test
    fun customizationChangeLimit() {
        val d = DEFAULT_CHARACTER_CUSTOMIZATION
        assertEquals(0, CustomizationRules.changesFromDefault(d))
        assertEquals(1, CustomizationRules.changesFromDefault(d.replaceFirst('Q', 'K')))
        val many = d.replace('Q', '1').replace('W', '2').replace('R', '3').replace('T', '4').replace('Y', '5').replace('F', '6').replace('G', '7')
        assertEquals(d, CustomizationRules.accept(many, d, isPro = false))
        assertEquals(many, CustomizationRules.accept(many, d, isPro = true))
        assertEquals("EQWRT*\nabc".let { CustomizationRules.normalize(it) }, "EQWRT*\nABC")
    }
}
