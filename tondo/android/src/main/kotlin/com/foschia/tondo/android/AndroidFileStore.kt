package com.foschia.tondo.android

import android.content.Context
import com.foschia.tondo.core.FileStore
import java.io.File

/**
 * Persists the shared stores (recent emojis, pinned notes) in the app files directory.
 * Files written by the Unity version (Application.persistentDataPath, i.e. the external or
 * internal files directory) are picked up transparently so users keep their data.
 */
class AndroidFileStore(context: Context) : FileStore {
    private val dir: File = context.filesDir
    private val legacyDirs: List<File> = listOfNotNull(context.getExternalFilesDir(null), context.filesDir)

    override fun read(name: String): String? {
        val f = File(dir, name)
        if (f.exists()) return runCatching { f.readText() }.getOrNull()
        return legacyDirs.map { File(it, name) }.firstOrNull { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }
    }

    override fun write(name: String, content: String) {
        runCatching { File(dir, name).writeText(content) }
    }
}
