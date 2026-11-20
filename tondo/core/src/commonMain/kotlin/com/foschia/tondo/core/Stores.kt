package com.foschia.tondo.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Text files persisted by the platform (Unity's Application.persistentDataPath). */
interface FileStore {
    fun read(name: String): String?
    fun write(name: String, content: String)
}

class MemoryFileStore : FileStore {
    val files = mutableMapOf<String, String>()
    override fun read(name: String) = files[name]
    override fun write(name: String, content: String) { files[name] = content }
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

enum class SkinTone(val codePoint: Int) {
    NONE(0), LIGHT(0x1f3fb), MEDIUM_LIGHT(0x1f3fc), MEDIUM(0x1f3fd), MEDIUM_DARK(0x1f3fe), DARK(0x1f3ff);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.codePoint == v } ?: NONE
    }
}

/** On-disk format identical to Unity's JsonUtility output, so recently used emojis survive the migration. */
@Serializable
private class EmojiFile(
    @SerialName("_usages") val usages: MutableList<Int> = mutableListOf(),
    @SerialName("_emojis") val emojis: MutableList<String> = mutableListOf(),
    @SerialName("_skinTone1") var skinTone1: Int = 0,
    @SerialName("_skinTone2") var skinTone2: Int = 0,
    @SerialName("_capacity") var capacity: Int = 10,
)

/** Port of EmojiPersister.cs (most used emojis + selected skin tones). */
class EmojiStore(private val files: FileStore) {
    private val data: EmojiFile = files.read(FILE_NAME)?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.decodeFromString(EmojiFile.serializer(), it) }.getOrNull() } ?: EmojiFile()

    var onChanged: (() -> Unit)? = null

    val count get() = data.emojis.size
    var skinTone1: SkinTone
        get() = SkinTone.from(data.skinTone1)
        set(v) { data.skinTone1 = v.codePoint; onChanged?.invoke() }
    var skinTone2: SkinTone
        get() = SkinTone.from(data.skinTone2)
        set(v) { data.skinTone2 = v.codePoint; onChanged?.invoke() }

    var capacity: Int
        get() = data.capacity
        set(value) {
            if (value < count) descendingKeys().drop(value).forEach { remove(it) }
            data.capacity = value
        }

    /** Stable sort, like LINQ OrderByDescending. */
    fun descendingKeys(): List<String> = data.emojis.indices.sortedByDescending { data.usages[it] }.map { data.emojis[it] }

    fun use(code: String) {
        val i = data.emojis.indexOf(code)
        if (i >= 0) {
            data.usages[i]++
        } else {
            if (count + 1 > data.capacity) descendingKeys().lastOrNull()?.let { remove(it) }
            data.emojis.add(code)
            data.usages.add(1)
        }
        onChanged?.invoke()
    }

    private fun remove(code: String) {
        val i = data.emojis.indexOf(code)
        if (i < 0) return
        data.emojis.removeAt(i)
        data.usages.removeAt(i)
    }

    fun save() = files.write(FILE_NAME, json.encodeToString(EmojiFile.serializer(), data))

    companion object {
        const val FILE_NAME = "EmojiPersister"
    }
}

@Serializable
private class MacrosFile(
    @SerialName("_usages") val usages: MutableList<Int> = mutableListOf(),
    @SerialName("_macros") val macros: MutableList<String> = mutableListOf(),
    @SerialName("_areTutorial") val areTutorial: MutableList<Boolean> = mutableListOf(),
    @SerialName("_capacity") var capacity: Int = 20,
)

/** Port of MacrosPersister.cs (pinned clipboard notes). */
class MacrosStore(private val files: FileStore, tutorialMacros: () -> List<String>) {
    private val data: MacrosFile

    var onChanged: (() -> Unit)? = null

    init {
        val raw = files.read(FILE_NAME)
        val parsed = raw?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(MacrosFile.serializer(), it) }.getOrNull() }
        data = parsed ?: MacrosFile().also { d ->
            // Inserted at index 0 one after the other, like AddTutorialMacro in Unity.
            for (t in tutorialMacros()) {
                d.macros.add(0, t); d.usages.add(0, 1); d.areTutorial.add(0, true)
            }
        }
        // Sanitize, like MacrosPersister.Sanitize().
        while (data.usages.size < data.macros.size) data.usages.add(1)
        while (data.usages.size > data.macros.size) data.usages.removeAt(data.usages.lastIndex)
        while (data.areTutorial.size < data.macros.size) data.areTutorial.add(false)
        while (data.areTutorial.size > data.macros.size) data.areTutorial.removeAt(data.areTutorial.lastIndex)
    }

    val keys: List<String> get() = data.macros.toList()
    val count get() = data.macros.size
    var capacity: Int
        get() = data.capacity
        set(value) {
            if (value < count) data.macros.indices.sortedByDescending { data.usages[it] }.drop(value)
                .map { data.macros[it] }.forEach { removeInternal(it) }
            data.capacity = value
        }

    fun contains(macro: String) = macro in data.macros
    fun isTutorial(macro: String): Boolean = data.macros.indexOf(macro).let { it >= 0 && data.areTutorial[it] }

    fun add(macro: String) {
        check(count + 1 <= data.capacity) { "Reached capacity" }
        data.macros.add(0, macro); data.usages.add(0, 1); data.areTutorial.add(0, false)
        onChanged?.invoke()
    }

    fun remove(macro: String): Boolean = removeInternal(macro).also { if (it) onChanged?.invoke() }

    private fun removeInternal(macro: String): Boolean {
        val i = data.macros.indexOf(macro)
        if (i < 0) return false
        data.macros.removeAt(i); data.usages.removeAt(i); data.areTutorial.removeAt(i)
        return true
    }

    fun incrementUsage(macro: String) {
        val i = data.macros.indexOf(macro)
        if (i >= 0) data.usages[i]++
    }

    fun save() = files.write(FILE_NAME, json.encodeToString(MacrosFile.serializer(), data))

    companion object {
        const val FILE_NAME = "MacrosPersister"
    }
}
