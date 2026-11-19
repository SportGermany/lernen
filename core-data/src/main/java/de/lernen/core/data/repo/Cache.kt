package de.lernen.core.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Kleine Textdateien im App-Speicher (z. B. zuletzt geladener Katalog und faecher.json). */
@Singleton
class Cache @Inject constructor(@ApplicationContext context: Context) {
    private val ordner = File(context.filesDir, "cache-sync").apply { mkdirs() }

    fun lies(name: String): String? = File(ordner, name).takeIf { it.exists() }?.readText()

    fun schreib(name: String, text: String) {
        val tmp = File(ordner, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(ordner, name))
    }
}
