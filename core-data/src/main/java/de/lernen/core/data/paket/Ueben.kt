package de.lernen.core.data.paket

import de.lernen.core.data.model.Paket
import de.lernen.core.data.model.Token
import de.lernen.core.data.model.Uebung

enum class ErgebnisStatus(val schluessel: String) {
    RICHTIG("richtig"), FALSCH("falsch"), UNSICHER("unsicher");

    val offen: Boolean get() = this != RICHTIG

    companion object {
        fun von(s: String) = entries.firstOrNull { it.schluessel == s }
    }
}

enum class Stufe(val schluessel: String, val titel: String) {
    EINSTIEG("einstieg", "Einstieg"), SICHER("sicher", "Sicher"), SCHULAUFGABE("schulaufgabe", "Schulaufgabe");

    companion object {
        fun von(s: String?) = entries.firstOrNull { it.schluessel == s }
    }
}

/** Eine Aufgabe mit stabiler id (fest aus dem Paket oder "{stufe}-{index}"). */
data class Aufgabe(val id: String, val stufe: Stufe, val uebung: Uebung)

object Ueben {
    /** Alle Aufgaben einer Stufe. Unbekannte Typen werden übersprungen. */
    fun aufgaben(p: Paket, stufe: Stufe): List<Aufgabe> {
        val l = when (stufe) {
            Stufe.EINSTIEG -> p.uebungen.einstieg
            Stufe.SICHER -> p.uebungen.sicher
            Stufe.SCHULAUFGABE -> p.uebungen.schulaufgabe
        }
        return l.mapIndexedNotNull { i, u -> if (u is Uebung.Unbekannt) null else Aufgabe(u.id ?: "${stufe.schluessel}-$i", stufe, u) }
    }

    fun alle(p: Paket): List<Aufgabe> = Stufe.entries.flatMap { aufgaben(p, it) }

    /** Wiederholen = alles mit „falsch“ oder „noch unsicher“. Richtig gelöste fallen heraus. */
    fun wiederholen(p: Paket, ergebnisse: Map<String, ErgebnisStatus>): List<Aufgabe> =
        alle(p).filter { ergebnisse[it.id]?.offen == true }

    /** Bewertung beim Antippen: grün = gefunden, gelb = übersehen, rot = falsch markiert. */
    enum class TokenBewertung { GEFUNDEN, UEBERSEHEN, FALSCH_MARKIERT, NEUTRAL }

    fun bewerte(tokens: List<Token>, gewaehlt: Set<Int>): List<TokenBewertung> = tokens.mapIndexed { i, t ->
        when {
            t.richtig && i in gewaehlt -> TokenBewertung.GEFUNDEN
            t.richtig -> TokenBewertung.UEBERSEHEN
            i in gewaehlt -> TokenBewertung.FALSCH_MARKIERT
            else -> TokenBewertung.NEUTRAL
        }
    }

    fun tippenRichtig(tokens: List<Token>, gewaehlt: Set<Int>) =
        bewerte(tokens, gewaehlt).none { it == TokenBewertung.UEBERSEHEN || it == TokenBewertung.FALSCH_MARKIERT }
}
