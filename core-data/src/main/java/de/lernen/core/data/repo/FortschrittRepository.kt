package de.lernen.core.data.repo

import de.lernen.core.data.db.FortschrittDao
import de.lernen.core.data.db.HakenEntity
import de.lernen.core.data.db.PositionEntity
import de.lernen.core.data.db.PruefAntwortEntity
import de.lernen.core.data.db.TippStandEntity
import de.lernen.core.data.db.UebungErgebnisEntity
import de.lernen.core.data.paket.ErgebnisStatus
import de.lernen.core.data.paket.HakenSchluessel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Alles, was der Schüler im Thema macht. Jede Änderung wird sofort gespeichert. */
@Singleton
class FortschrittRepository @Inject constructor(
    private val dao: FortschrittDao,
) {
    fun haken(paketId: String): Flow<Set<HakenSchluessel>> =
        dao.haken(paketId).map { l -> l.mapTo(mutableSetOf()) { HakenSchluessel(it.schrittId, it.index) } }

    /** Haken aller Pakete, gruppiert nach Paket-id. */
    val alleHaken: Flow<Map<String, Set<HakenSchluessel>>> = dao.alleHaken().map { l ->
        l.groupBy { it.paketId }.mapValues { (_, v) -> v.mapTo(mutableSetOf()) { HakenSchluessel(it.schrittId, it.index) } }
    }

    suspend fun setzeHaken(paketId: String, schrittId: String, index: Int, an: Boolean) {
        if (an) dao.setzeHaken(HakenEntity(paketId, schrittId, index, System.currentTimeMillis()))
        else dao.entferneHaken(paketId, schrittId, index)
    }

    /** Aufgedeckte Tipps je Schritt-id. */
    fun tipps(paketId: String): Flow<Map<String, Int>> =
        dao.tipps(paketId).map { l -> l.associate { it.schrittId to it.aufgedeckt } }

    suspend fun setzeTipps(paketId: String, schrittId: String, anzahl: Int) =
        dao.setzeTipps(TippStandEntity(paketId, schrittId, anzahl))

    /** Gewählte Antwort der Prüffrage je Schritt-id. */
    fun pruefAntworten(paketId: String): Flow<Map<String, Int>> =
        dao.pruefAntworten(paketId).map { l -> l.associate { it.schrittId to it.gewaehlt } }

    suspend fun beantworte(paketId: String, schrittId: String, gewaehlt: Int, richtig: Boolean) =
        dao.setzePruefAntwort(PruefAntwortEntity(paketId, schrittId, gewaehlt, richtig))

    suspend fun nochmalVersuchen(paketId: String, schrittId: String) = dao.entfernePruefAntwort(paketId, schrittId)

    val positionen: Flow<List<PositionEntity>> = dao.positionen()

    suspend fun position(paketId: String): PositionEntity? = dao.position(paketId)

    suspend fun merkePosition(paketId: String, schrittId: String?, tab: String) =
        dao.setzePosition(PositionEntity(paketId, schrittId, tab, System.currentTimeMillis()))

    /** Ergebnis je Aufgaben-id eines Pakets. */
    fun ergebnisse(paketId: String): Flow<Map<String, ErgebnisStatus>> = dao.ergebnisse(paketId).map { l ->
        l.mapNotNull { e -> ErgebnisStatus.von(e.status)?.let { e.aufgabeId to it } }.toMap()
    }

    suspend fun setzeErgebnis(paketId: String, aufgabeId: String, stufe: String, status: ErgebnisStatus) =
        dao.setzeErgebnis(UebungErgebnisEntity(paketId, aufgabeId, stufe, status.schluessel, System.currentTimeMillis()))

    /** Anzahl Übungen mit "falsch" oder "unsicher" (für "Wiederholen"). */
    val offeneWiederholungen: Flow<Map<String, Int>> = dao.ergebnisse().map { l ->
        l.filter { it.status == "falsch" || it.status == "unsicher" }.groupingBy { it.paketId }.eachCount()
    }
}
