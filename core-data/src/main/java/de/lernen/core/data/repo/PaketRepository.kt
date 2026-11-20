package de.lernen.core.data.repo

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.db.FortschrittDao
import de.lernen.core.data.db.PaketDao
import de.lernen.core.data.db.PaketEntity
import de.lernen.core.data.model.Paket
import de.lernen.core.data.paket.PaketParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaketRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paketDao: PaketDao,
    private val fortschrittDao: FortschrittDao,
) {
    /** Zwischenspeicher für eingelesene Pakete (Schlüssel: id@version). */
    private val cache = ConcurrentHashMap<String, Paket>()

    private fun lies(e: PaketEntity): Paket? = cache.getOrPut("${e.id}@${e.version}@${e.json.hashCode()}") {
        runCatching { PaketParser.liesOderWirf(e.json) }
            .onFailure { Log.w("Lernen", "Paket ${e.id} defekt: ${it.message}") }
            .getOrNull() ?: return null
    }

    val pakete: Flow<List<Paket>> = paketDao.alle().map { l -> l.mapNotNull(::lies) }

    fun paket(id: String): Flow<Paket?> = paketDao.eines(id).map { it?.let(::lies) }

    /** Speichert ein Paket (neu oder Update). Der Fortschritt bleibt über die Schritt-ids erhalten. */
    suspend fun speichere(json: String, herkunft: String): Paket {
        val p = PaketParser.liesOderWirf(json)
        paketDao.speichere(
            PaketEntity(p.id, p.typ, p.fach, p.titel, p.version, json, System.currentTimeMillis(), herkunft)
        )
        // Haken zu Schritten/Punkten, die es nicht mehr gibt, verfallen still.
        val gueltig = p.begleiter.associate { it.id to it.muss.size }
        fortschrittDao.hakenListe(p.id)
            .filter { h -> (gueltig[h.schrittId] ?: 0) <= h.index }
            .forEach { fortschrittDao.entferneHaken(it.paketId, it.schrittId, it.index) }
        return p
    }

    suspend fun idsJetzt(): List<String> = pakete.first().map { it.id }

    /** Die installierte Formelsammlung (Werkzeug), falls vorhanden. */
    suspend fun formelsammlung(): Paket? = pakete.first().firstOrNull { it.typ == Paket.TYP_FORMELSAMMLUNG }

    /** Entfernt ein Paket. Mit [fortschrittLoeschen] = false bleibt der Fortschritt für später erhalten. */
    suspend fun entferne(id: String, fortschrittLoeschen: Boolean) {
        paketDao.loesche(id)
        if (fortschrittLoeschen) {
            fortschrittDao.loescheHaken(id)
            fortschrittDao.loescheTipps(id)
            fortschrittDao.loeschePruefAntworten(id)
            fortschrittDao.loeschePosition(id)
            fortschrittDao.loescheErgebnisse(id)
        }
    }

    /**
     * Beim ersten Start alle mitgelieferten Pakete übernehmen (alle Fächer + Formelsammlung, offline).
     * Alles Weitere kommt aus dem Katalog.
     */
    suspend fun startpaketeEinrichten() {
        val ersterStart = paketDao.anzahl() == 0
        val dateien = context.assets.list(STARTORDNER).orEmpty().filter { it.endsWith(".json") }
            // Nach dem ersten Start nur noch die Formelsammlung nachrüsten, falls sie fehlt (Werkzeug, immer offline).
            .filter { ersterStart || (it.startsWith("werkzeuge-") && formelsammlung() == null) }
        for (d in dateien) {
            val text = context.assets.open("$STARTORDNER/$d").bufferedReader().use { it.readText() }
            runCatching { speichere(text, HERKUNFT_START) }
                .onFailure { Log.e("Lernen", "Startpaket $d ungültig", it) }
        }
    }

    companion object {
        const val STARTORDNER = "startpakete"
        const val HERKUNFT_START = "start"
        const val HERKUNFT_KATALOG = "katalog"
    }
}
