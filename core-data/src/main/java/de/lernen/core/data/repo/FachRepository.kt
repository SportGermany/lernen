package de.lernen.core.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.model.Fach
import de.lernen.core.data.paket.PaketParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fächer aus faecher.json: zuletzt synchronisierte Fassung aus dem Paket-Repo,
 * beim allerersten Start die mitgelieferte Fassung aus den Assets.
 */
@Singleton
class FachRepository @Inject constructor(
    @ApplicationContext context: Context,
    einstellungen: EinstellungenRepository,
    private val cache: Cache,
) {
    private val alleFaecher = MutableStateFlow(
        cache.lies(CACHE_NAME)?.let { runCatching { PaketParser.liesFaecher(it) }.getOrNull() }?.takeIf { it.isNotEmpty() }
            ?: PaketParser.liesFaecher(context.assets.open("faecher.json").bufferedReader().use { it.readText() })
    )

    /** Alle Fächer inkl. ausgeblendeter, in eingestellter Reihenfolge und mit eigenen Farben. */
    val faecherAlle: Flow<List<Fach>> = combine(alleFaecher, einstellungen.einstellungen) { f, e ->
        val reihen = e.fachReihenfolge
        f.map { fach -> e.fachFarben[fach.id]?.let { fach.copy(farbe = it, farbeDunkel = hellerFuerDunkel(it)) } ?: fach }
            .sortedBy { reihen.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** Sichtbare Fächer. */
    val faecher: Flow<List<Fach>> = combine(faecherAlle, einstellungen.einstellungen) { f, e ->
        f.filter { it.id !in e.ausgeblendeteFaecher }
    }

    /** Vom Sync aufgerufen. Ungültige Datei → alte Liste bleibt. */
    fun aktualisiere(json: String) {
        val neu = runCatching { PaketParser.liesFaecher(json) }.getOrNull() ?: return
        if (neu.isEmpty()) return
        cache.schreib(CACHE_NAME, json)
        alleFaecher.value = neu
    }

    companion object {
        const val CACHE_NAME = "faecher.json"

        /** Eigene Farbe im dunklen Modus etwas aufhellen, damit sie lesbar bleibt. */
        fun hellerFuerDunkel(hex: String): String = runCatching {
            val v = hex.removePrefix("#").toInt(16)
            val r = (v shr 16) and 0xFF; val g = (v shr 8) and 0xFF; val b = v and 0xFF
            fun h(c: Int) = (c + (255 - c) * 0.45).toInt().coerceIn(0, 255)
            "#%02X%02X%02X".format(h(r), h(g), h(b))
        }.getOrDefault(hex)
    }
}
