package com.foschia.tondo.core

/**
 * Rules applied by the settings screens when the user edits the keyboard customization code,
 * shared by the Android settings and the iOS container app
 * (port of CharacterCustomizationEditTextPreference.onSharedPreferenceChanged).
 */
object CustomizationRules {
    /** Free version: maximum number of changed characters (R.integer.max_character_customization_value). */
    const val MAX_FREE_CHANGES = 6

    /** The main block always follows the shift state, so it is stored upper-case. */
    fun normalize(code: String): String {
        val blocks = code.split("\n\n").toMutableList()
        if (blocks.isNotEmpty()) blocks[0] = blocks[0].uppercase()
        return blocks.joinToString("\n\n")
    }

    /**
     * Number of characters of the default code that were replaced or removed, computed with a
     * longest common subsequence (equivalent to counting the INSERT hunks of diff-match-patch
     * on the space-separated strings, as the Android settings do).
     */
    fun changesFromDefault(code: String, default: String = DEFAULT_CHARACTER_CUSTOMIZATION): Int {
        val a = code
        val b = default
        val prev = IntArray(b.length + 1)
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                cur[j] = if (a[i - 1] == b[j - 1]) prev[j - 1] + 1 else maxOf(prev[j], cur[j - 1])
            }
            cur.copyInto(prev)
        }
        return b.length - prev[b.length]
    }

    /** Returns the value to store: [newCode] if allowed, otherwise [previous]. */
    fun accept(newCode: String, previous: String, isPro: Boolean): String {
        val normalized = normalize(newCode)
        if (isPro || normalized == DEFAULT_CHARACTER_CUSTOMIZATION) return normalized
        return if (changesFromDefault(normalized) > MAX_FREE_CHANGES) previous else normalized
    }
}
