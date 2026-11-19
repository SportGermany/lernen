package de.lernen.core.data.repo

import androidx.room.withTransaction
import de.lernen.core.data.db.FrageEntity
import de.lernen.core.data.db.HakenEntity
import de.lernen.core.data.db.LernenDatenbank
import de.lernen.core.data.db.PositionEntity
import de.lernen.core.data.db.PruefAntwortEntity
import de.lernen.core.data.db.SicherungDao
import de.lernen.core.data.db.TippStandEntity
import de.lernen.core.data.db.UebungErgebnisEntity
import de.lernen.core.data.paket.PaketParser
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Inhalt einer Sicherungsdatei. Enthält KEINEN Token. Pakete selbst werden nicht gesichert (kommen aus dem Katalog). */
@Serializable
data class Sicherung(
    val format: String = FORMAT,
    val erstellt: Long,
    val pakete: List<String> = emptyList(),
    val haken: List<HakenEntity> = emptyList(),
    val tipps: List<TippStandEntity> = emptyList(),
    val pruefAntworten: List<PruefAntwortEntity> = emptyList(),
    val positionen: List<PositionEntity> = emptyList(),
    val ergebnisse: List<UebungErgebnisEntity> = emptyList(),
    val fragen: List<FrageEntity> = emptyList(),
    val einstellungen: Map<String, String> = emptyMap(),
) {
    companion object {
        const val FORMAT = "lernen-sicherung/1"
    }
}

data class SicherungInfo(val erstellt: Long, val haken: Int, val ergebnisse: Int, val pakete: List<String>)

@Singleton
class SicherungRepository @Inject constructor(
    private val db: LernenDatenbank,
    private val dao: SicherungDao,
    private val einstellungen: EinstellungenRepository,
    private val pakete: PaketRepository,
) {
    suspend fun exportieren(): String {
        val s = Sicherung(
            erstellt = System.currentTimeMillis(),
            pakete = pakete.idsJetzt(),
            haken = dao.haken(), tipps = dao.tipps(), pruefAntworten = dao.pruefAntworten(),
            positionen = dao.positionen(), ergebnisse = dao.ergebnisse(),
            // Fragen nur als Verlauf: ohne Kontext
            fragen = dao.fragen().map { it.copy(kontextJson = null, fehler = null) },
            einstellungen = einstellungen.alsMap().filterKeys { it != "update_pruefung" },
        )
        return PaketParser.json.encodeToString(Sicherung.serializer(), s)
    }

    /** Liest eine Datei und gibt eine Vorschau für die Bestätigung. Wirft bei ungültiger Datei. */
    fun lies(text: String): Pair<Sicherung, SicherungInfo> {
        val s = runCatching { PaketParser.json.decodeFromString(Sicherung.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("Das ist keine Sicherungsdatei von Lernen.") }
        require(s.format == Sicherung.FORMAT) { "Unbekanntes Sicherungsformat: ${s.format}" }
        return s to SicherungInfo(s.erstellt, s.haken.size, s.ergebnisse.size, s.pakete)
    }

    /** Ersetzt Fortschritt und Einstellungen durch die Sicherung. */
    suspend fun importieren(s: Sicherung) {
        db.withTransaction {
            dao.fortschrittLoeschen()
            dao.haken(s.haken); dao.tipps(s.tipps); dao.pruefAntworten(s.pruefAntworten)
            dao.positionen(s.positionen); dao.ergebnisse(s.ergebnisse); dao.fragen(s.fragen)
        }
        einstellungen.ausMap(s.einstellungen)
    }

    suspend fun fortschrittZuruecksetzen() = dao.fortschrittLoeschen()
}
