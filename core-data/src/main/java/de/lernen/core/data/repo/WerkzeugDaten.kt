package de.lernen.core.data.repo

import de.lernen.core.data.model.Korrekturzeichen
import de.lernen.core.data.model.WortGruppe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wortkiste und Korrekturzeichen für die Werkzeuge: aus allen hinzugefügten Paketen zusammengeführt.
 * Ohne passende Pakete gibt es eine Standard-Wortkiste und Standard-Korrekturzeichen.
 */
@Singleton
class WerkzeugDaten @Inject constructor(pakete: PaketRepository) {

    val wortkiste: Flow<List<WortGruppe>> = pakete.pakete.map { l ->
        zusammenfuehren(l.flatMap { it.wortkiste }).ifEmpty { STANDARD_WORTKISTE }
    }

    val korrekturzeichen: Flow<List<Korrekturzeichen>> = pakete.pakete.map { l ->
        l.flatMap { it.korrekturZeichen }.distinctBy { it.zeichen }.ifEmpty { STANDARD_KORREKTUR }
    }

    companion object {
        /** Gruppen gleichen Namens werden vereint, doppelte Wörter entfernt. */
        fun zusammenfuehren(gruppen: List<WortGruppe>): List<WortGruppe> =
            gruppen.groupBy { it.gruppe.trim() }.map { (name, g) -> WortGruppe(name, g.flatMap { it.woerter }.distinct()) }

        val STANDARD_WORTKISTE = listOf(
            WortGruppe("Statt „gehen“", listOf("schleichen", "schlendern", "stapfen", "hasten", "huschen", "stolpern", "eilen")),
            WortGruppe("Statt „sagen“", listOf("flüstern", "murmeln", "rufen", "erklären", "antworten", "erwidern", "betonen")),
            WortGruppe("Statt „sehen“", listOf("betrachten", "beobachten", "erblicken", "mustern", "starren", "erkennen")),
            WortGruppe("Satzanfänge", listOf("Plötzlich", "Im selben Moment", "Inzwischen", "Allerdings", "Außerdem", "Schließlich")),
            WortGruppe("Gefühle zeigen", listOf("Das Herz hämmert", "Die Knie werden weich", "Ich atme tief durch", "Ein Grinsen breitet sich aus")),
            WortGruppe("Begründen", listOf("weil", "da", "denn", "deshalb", "aus diesem Grund", "das zeigt sich daran, dass")),
        )

        val STANDARD_KORREKTUR = listOf(
            Korrekturzeichen("T", "Tempus", "falsche Zeitform"),
            Korrekturzeichen("A", "Ausdruck", "unpassendes Wort, Umgangssprache"),
            Korrekturzeichen("W", "Wiederholung (auch Wdh)", "gleiches Wort kurz hintereinander"),
            Korrekturzeichen("Sb", "Satzbau", "Satz unvollständig oder verdreht"),
            Korrekturzeichen("Gr", "Grammatik", "falscher Fall, falsche Endung"),
            Korrekturzeichen("Bz", "Bezug", "unklar, worauf sich „er“ oder „das“ bezieht"),
            Korrekturzeichen("R", "Rechtschreibung", "„das“ statt „dass“"),
            Korrekturzeichen("Z", "Zeichensetzung", "Komma, Redezeichen"),
        )
    }
}
