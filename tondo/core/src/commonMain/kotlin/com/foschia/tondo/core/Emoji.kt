package com.foschia.tondo.core

/** Port of the static helpers of EmojiSprite.cs. */
object Emoji {
    private const val SKIN_1 = "#skin_1"
    private const val SKIN_2 = "#skin_2"

    /** Image names (primary, fallback) of the Noto artwork for an emoji code like "U+1F44B #skin_1". */
    fun imageNames(code: String): Pair<String, String> {
        val c = "emoji_u" + code.replace("U+", "").replace(" ", "_").replace("_FE0F", "").lowercase()
        val noSkin = c.replace("_$SKIN_1", "").replace("_$SKIN_2", "")
        val withSkin = c.replace(SKIN_1, "1f3fb").replace(SKIN_2, "1f3fb")
        return "emoji/$noSkin" to "emoji/$withSkin"
    }

    /** The text to commit for an emoji code, applying the selected skin tones. */
    fun toText(code: String, tone1: SkinTone, tone2: SkinTone): String {
        val sb = StringBuilder()
        for (part in code.split(' ')) {
            val cp = when (part) {
                SKIN_1 -> tone1.codePoint
                SKIN_2 -> tone2.codePoint
                else -> part.removePrefix("U+").toInt(16)
            }
            if (cp != 0) appendCodePoint(sb, cp)
        }
        return sb.toString()
    }

    fun appendCodePoint(sb: StringBuilder, cp: Int) {
        if (cp < 0x10000) {
            sb.append(cp.toChar())
        } else {
            val v = cp - 0x10000
            sb.append((0xD800 + (v shr 10)).toChar())
            sb.append((0xDC00 + (v and 0x3FF)).toChar())
        }
    }
}
