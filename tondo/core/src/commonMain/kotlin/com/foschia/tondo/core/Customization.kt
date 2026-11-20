package com.foschia.tondo.core

/**
 * Port of CharacterCustomization.cs: parses the keyboard customization code
 * (3 blocks separated by an empty line, 6 lines each, one char per glyph).
 */
class CharacterCustomization(code: String) {
    private val map: Map<LayoutType, Map<StickPosition, Map<CardinalDirection, String>>>

    init {
        val result = mutableMapOf<LayoutType, MutableMap<StickPosition, MutableMap<CardinalDirection, String>>>()
        val layoutStrings = code.split("\n\n")
        for (i in LAYOUT_ORDER.indices) {
            if (i >= layoutStrings.size) break
            // NB: in the Unity code the upper-casing of the main block happened *after* the split,
            // so it had no effect: case is handled by the shift state at render time.
            val stickStrings = layoutStrings[i].split('\n')
            val sticks = mutableMapOf<StickPosition, MutableMap<CardinalDirection, String>>()
            for (j in STICK_ORDER.indices) {
                if (j >= stickStrings.size) break
                val chars = stickStrings[j]
                val dirs = mutableMapOf<CardinalDirection, String>()
                for (k in DIRECTION_ORDER.indices) {
                    if (k >= chars.length) break
                    var idx = k
                    // Top right tondo: NE hosts the diacriticizer, so the 6th char goes to NW.
                    if (STICK_ORDER[j] == StickPosition.TOP_RIGHT && k == 5) idx++
                    if (idx >= DIRECTION_ORDER.size) break
                    val dir = DIRECTION_ORDER[idx]
                    if (dir !in dirs) dirs[dir] = chars[k].toString()
                }
                sticks[STICK_ORDER[j]] = dirs
            }
            result[LAYOUT_ORDER[i]] = sticks
        }
        map = result
    }

    fun get(layout: LayoutType, position: StickPosition, direction: CardinalDirection): String =
        map[layout]?.get(position)?.get(direction) ?: ""

    companion object {
        private val LAYOUT_ORDER = listOf(LayoutType.MAIN, LayoutType.SYMBOLS_1, LayoutType.SYMBOLS_2)
        private val STICK_ORDER = listOf(
            StickPosition.TOP_LEFT, StickPosition.TOP_CENTER, StickPosition.TOP_RIGHT,
            StickPosition.BOTTOM_LEFT, StickPosition.BOTTOM_CENTER, StickPosition.BOTTOM_RIGHT,
        )
        private val DIRECTION_ORDER = listOf(
            CardinalDirection.CENTER, CardinalDirection.WEST, CardinalDirection.SOUTH_WEST, CardinalDirection.SOUTH_EAST,
            CardinalDirection.EAST, CardinalDirection.NORTH_EAST, CardinalDirection.NORTH_WEST,
        )
    }
}

/** Port of DiacriticsDictionaryEntry. */
class DiacriticsEntry(
    val leftGlyphs: List<String>,
    val rightGlyphs: List<String>,
    var lastLeftIndex: Int,
    var lastRightIndex: Int,
)

/**
 * Port of DiacriticsCustomization.cs. The "preceding character" is passed in explicitly instead of
 * being fetched from the native interface, which keeps this class pure and testable.
 */
class DiacriticsCustomization(code: String, private val store: PreferenceStore? = null) {
    val entries: Map<String, DiacriticsEntry>

    init {
        val result = linkedMapOf<String, DiacriticsEntry>()
        for (line in code.split('\n')) {
            val parts = line.split(' ')
            val key = parts[0].lowercase()
            fun glyphs(s: String) = s.map { it.toString().lowercase() }
            val entry = DiacriticsEntry(
                leftGlyphs = if (parts.size > 1) glyphs(parts[1]) else emptyList(),
                rightGlyphs = if (parts.size > 2) glyphs(parts[2]) else emptyList(),
                lastLeftIndex = if (parts.size > 3) parts[3].trim().toIntOrNull() ?: 0 else 0,
                lastRightIndex = if (parts.size > 4) parts[4].trim().toIntOrNull() ?: 0 else 0,
            )
            if (key !in result) result[key] = entry
        }
        entries = result
        // Restore last selections persisted by previous sessions (Unity used PlayerPrefs with these keys).
        store?.let { s ->
            for ((k, e) in entries) {
                val lk = prefKey(k, true)
                val rk = prefKey(k, false)
                if (s.contains(lk)) e.lastLeftIndex = s.getInt(lk, e.lastLeftIndex)
                if (s.contains(rk)) e.lastRightIndex = s.getInt(rk, e.lastRightIndex)
            }
        }
    }

    private fun prefKey(s: String, isLeft: Boolean) = PREF_PREFIX + s + if (isLeft) "_l" else "_r"

    private fun applyCase(glyph: String, preceding: String) =
        if (preceding.first().isUpperCase()) glyph.uppercase() else glyph.lowercase()

    fun setLastSelectedIndex(preceding: String, index: Int, isLeft: Boolean) {
        if (preceding.isEmpty()) return
        val key = preceding.lowercase()
        val e = entries[key] ?: return
        if (isLeft) e.lastLeftIndex = index else e.lastRightIndex = index
        store?.putInt(prefKey(key, isLeft), index)
    }

    fun lastAlternate(preceding: String, isLeft: Boolean): String? {
        if (preceding.isEmpty()) return null
        val e = entries[preceding.lowercase()] ?: return null
        val glyphs = if (isLeft) e.leftGlyphs else e.rightGlyphs
        if (glyphs.isEmpty()) return null
        var idx = if (isLeft) e.lastLeftIndex else e.lastRightIndex
        if (idx >= glyphs.size || idx < 0) {
            idx = 0
            if (isLeft) e.lastLeftIndex = 0 else e.lastRightIndex = 0
        }
        val g = glyphs[idx]
        if (g.isEmpty()) return null
        return applyCase(g, preceding)
    }

    fun alternates(preceding: String, isLeft: Boolean): List<String>? {
        if (preceding.isEmpty()) return null
        val e = entries[preceding.lowercase()] ?: return null
        return (if (isLeft) e.leftGlyphs else e.rightGlyphs).map { applyCase(it, preceding) }
    }

    companion object {
        const val PREF_PREFIX = "DiacriticsDictionaryReferencePlayerPrefKey_"
    }
}
