package de.lernen.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Ein Themenpaket im Format "lernen-paket/2".
 * Unbekannte Felder werden beim Einlesen ignoriert, damit ältere App-Versionen neuere Pakete lesen können.
 */
@Serializable
data class Paket(
    val format: String,
    val typ: String,
    val id: String,
    val version: String,
    val fach: String,
    val titel: String,
    val stufe: String = "",
    val ueberblick: Ueberblick = Ueberblick(),
    val begleiter: List<Schritt> = emptyList(),
    val uebungen: Uebungen = Uebungen(),
    val raster: List<String> = emptyList(),
    val merkzettel: List<List<String>> = emptyList(),
    val wortkiste: List<WortGruppe> = emptyList(),
    val korrektur: List<List<String>> = emptyList(),
    /** Nur Typ "formelsammlung" (Meilenstein 5). */
    val gebiete: JsonElement? = null,
) {
    val istArbeitsheft: Boolean get() = typ == TYP_ARBEITSHEFT

    /** Anzahl aller Abhak-Punkte im Begleiter. */
    val punkteGesamt: Int get() = begleiter.sumOf { it.muss.size }

    val merkEintraege: List<MerkEintrag>
        get() = merkzettel.mapNotNull { if (it.size >= 2) MerkEintrag(it[0], it[1]) else null }

    val korrekturZeichen: List<Korrekturzeichen>
        get() = korrektur.mapNotNull {
            if (it.size >= 2) Korrekturzeichen(it[0], it[1], it.getOrElse(2) { "" }) else null
        }

    companion object {
        const val FORMAT = "lernen-paket/2"
        const val TYP_THEMA = "thema"
        const val TYP_ARBEITSHEFT = "arbeitsheft"
        const val TYP_FORMELSAMMLUNG = "formelsammlung"
        val TYPEN = setOf(TYP_THEMA, TYP_ARBEITSHEFT, TYP_FORMELSAMMLUNG)
    }
}

@Serializable
data class Ueberblick(
    val aufgabe: String? = null,
    // Beispiel als Vorher/Nachher (Überarbeiten)
    val vorher: String? = null,
    val nachher: String? = null,
    // Beispiel als Ausgangslage/Auftrag/Muster (Schreiben)
    val ausgangslage: String? = null,
    val auftrag: String? = null,
    val ueberschrift: String? = null,
    val muster: String? = null,
    /** Paare [Teil, Beschreibung] */
    val aufbau: List<List<String>> = emptyList(),
    val kriterien: List<String> = emptyList(),
    val erlaubt: List<String> = emptyList(),
    val verboten: List<String> = emptyList(),
    // Nur Typ "arbeitsheft"
    val quelle: String? = null,
    /** Tripel [Zeilen, Teil, Was tun] */
    val karte: List<List<String>> = emptyList(),
    val ziel: List<String> = emptyList(),
    val regel: String? = null,
)

@Serializable
data class Schritt(
    val id: String,
    val kurz: String,
    val titel: String,
    val ziel: String = "",
    val label: String? = null,
    val muss: List<String> = emptyList(),
    val wissen: String? = null,
    val fehler: List<String> = emptyList(),
    /** Tipps in Stufen: allgemein → Ort/Zeilen → konkrete Wörter */
    val hinweise: List<String> = emptyList(),
    val frage: Pruefrage? = null,
    val vorher: String? = null,
    val nachher: String? = null,
    val vnLabel: List<String> = emptyList(),
    val hilfen: List<Hilfe> = emptyList(),
    val wortkiste: Boolean = false,
    /** Mathe/Physik (Meilenstein 5): "aequivalenz" | "loesung" | "einheit" */
    val pruefung: String? = null,
    val mitrechnen: JsonElement? = null,
) {
    /** Überschrift der Abhakliste. */
    val listenTitel: String get() = label ?: STANDARD_LABEL

    val vorherLabel: String get() = vnLabel.getOrNull(0) ?: "Vorher"
    val nachherLabel: String get() = vnLabel.getOrNull(1) ?: "Nachher"

    companion object {
        const val STANDARD_LABEL = "Muss rein"
    }
}

@Serializable
data class Pruefrage(
    val frage: String,
    val optionen: List<String>,
    val richtig: Int,
    val warum: String = "",
)

@Serializable
data class Hilfe(
    @SerialName("t") val titel: String,
    val items: List<String> = emptyList(),
)

@Serializable
data class WortGruppe(
    @SerialName("g") val gruppe: String,
    @SerialName("i") val woerter: List<String> = emptyList(),
)

@Serializable
data class Uebungen(
    val einstieg: List<Uebung> = emptyList(),
    val sicher: List<Uebung> = emptyList(),
    val schulaufgabe: List<Uebung> = emptyList(),
) {
    val anzahl: Int get() = einstieg.size + sicher.size + schulaufgabe.size
}

data class MerkEintrag(val stichwort: String, val text: String)

data class Korrekturzeichen(val zeichen: String, val bedeutung: String, val beispiel: String)
