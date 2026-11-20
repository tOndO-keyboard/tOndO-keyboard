package com.foschia.tondo.core

/**
 * Lightweight, memory-frugal replacement of the WeCantSpell.Hunspell based Suggester.cs.
 *
 * It reads the same bundled Hunspell `.dic` files, keeps them as a single UTF-8 byte array plus a
 * sorted index of word offsets (a few MB even for German, compatible with the iOS keyboard extension
 * memory budget) and proposes:
 *  1. the word itself when it is a known word with different case (e.g. "rome" -> "Rome");
 *  2. known words at edit distance 1 (typos);
 *  3. completions sharing the typed prefix.
 *
 * Affix rules (.aff) are not expanded: suggestions are based on dictionary stems, which is the
 * main functional difference with the Unity version (an experimental feature there too).
 */
class Suggester(private val loadDictionary: (String) -> ByteArray?) {
    private var lang: String? = null
    private var bytes = ByteArray(0)
    private var starts = IntArray(0)
    private var ends = IntArray(0)

    val isLoaded get() = starts.isNotEmpty()

    fun ensureLanguage(language: String) {
        if (language == lang) return
        lang = language
        val data = loadDictionary("$language.dic") ?: run { bytes = ByteArray(0); starts = IntArray(0); ends = IntArray(0); return }
        load(data)
    }

    internal fun load(data: ByteArray) {
        bytes = data
        val s = ArrayList<Int>()
        val e = ArrayList<Int>()
        var i = 0
        var first = true
        while (i < data.size) {
            var j = i
            while (j < data.size && data[j] != '\n'.code.toByte()) j++
            if (first) {
                first = false // first line is the word count
            } else {
                var end = i
                while (end < j && data[end] != '/'.code.toByte() && data[end] != '\t'.code.toByte() && data[end] != '\r'.code.toByte() && data[end] != ' '.code.toByte()) end++
                if (end > i) { s.add(i); e.add(end) }
            }
            i = j + 1
        }
        val order = s.indices.sortedWith { a, b -> compareRange(s[a], e[a], s[b], e[b]) }
        starts = IntArray(order.size) { s[order[it]] }
        ends = IntArray(order.size) { e[order[it]] }
    }

    private fun compareRange(a0: Int, a1: Int, b0: Int, b1: Int): Int {
        val la = a1 - a0
        val lb = b1 - b0
        val n = minOf(la, lb)
        for (k in 0 until n) {
            val x = bytes[a0 + k].toInt() and 0xFF
            val y = bytes[b0 + k].toInt() and 0xFF
            if (x != y) return x - y
        }
        return la - lb
    }

    private fun compareTo(index: Int, key: ByteArray, prefixOnly: Boolean): Int {
        val a0 = starts[index]
        val la = ends[index] - a0
        val n = minOf(la, key.size)
        for (k in 0 until n) {
            val x = bytes[a0 + k].toInt() and 0xFF
            val y = key[k].toInt() and 0xFF
            if (x != y) return x - y
        }
        if (prefixOnly && la >= key.size) return 0
        return la - key.size
    }

    private fun lowerBound(key: ByteArray): Int {
        var lo = 0
        var hi = starts.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareTo(mid, key, false) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun word(i: Int) = bytes.decodeToString(starts[i], ends[i])

    fun contains(word: String): Boolean {
        if (!isLoaded || word.isEmpty()) return false
        val key = word.encodeToByteArray()
        val i = lowerBound(key)
        return i < starts.size && compareTo(i, key, false) == 0
    }

    private fun completions(prefix: String, limit: Int): List<String> {
        val key = prefix.encodeToByteArray()
        val out = ArrayList<String>()
        var i = lowerBound(key)
        while (i < starts.size && compareTo(i, key, true) == 0 && out.size < limit * 8) {
            val w = word(i)
            if (w.length > prefix.length) out.add(w)
            i++
        }
        return out.sortedBy { it.length }.take(limit)
    }

    private fun edits1(word: String): Sequence<String> = sequence {
        val alphabet = ALPHABET
        for (i in 0..word.length) {
            if (i < word.length) yield(word.removeRange(i, i + 1))
            if (i < word.length - 1) yield(word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2))
            for (c in alphabet) {
                if (i < word.length) yield(word.substring(0, i) + c + word.substring(i + 1))
                yield(word.substring(0, i) + c + word.substring(i))
            }
        }
    }

    private fun matchCase(candidate: String, typed: String): String = when {
        typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> candidate.uppercase()
        typed.first().isUpperCase() -> candidate.replaceFirstChar { it.uppercaseChar() }
        else -> candidate
    }

    fun suggest(typed: String, max: Int = 3): List<String> {
        if (!isLoaded || typed.isBlank()) return emptyList()
        val variants = linkedSetOf(typed, typed.lowercase(), typed.lowercase().replaceFirstChar { it.uppercaseChar() })
        val result = linkedSetOf<String>()
        if (!contains(typed)) variants.drop(1).firstOrNull { contains(it) }?.let { result.add(it) }
        val lower = typed.lowercase()
        for (cand in edits1(lower)) {
            if (result.size >= max) break
            if (cand == lower || cand.isEmpty()) continue
            when {
                contains(cand) -> result.add(matchCase(cand, typed))
                contains(cand.replaceFirstChar { it.uppercaseChar() }) -> result.add(cand.replaceFirstChar { it.uppercaseChar() })
            }
        }
        if (result.size < max) {
            for (prefix in variants) {
                for (c in completions(prefix, max)) {
                    if (result.size >= max) break
                    result.add(if (prefix == lower) matchCase(c, typed) else c)
                }
            }
        }
        return result.filter { it != typed }.take(max)
    }

    companion object {
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzàáâäãåæçèéêëìíîïñòóôöõøœßùúûü'"
    }
}
