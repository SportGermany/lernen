package de.lernen.core.data.paket

import de.lernen.core.data.model.Paket
import de.lernen.core.data.model.Uebung

/**
 * Prüft ein eingelesenes Paket auf Regeln, die über die reine JSON-Struktur hinausgehen.
 * Spiegelt die Regeln aus schema/paket.schema.json und ergänzt Querbezüge (z. B. "richtig" im Bereich).
 */
object PaketPruefer {

    data class Problem(val ort: String, val text: String, val schwer: Boolean = true) {
        override fun toString() = "$ort: $text"
    }

    private val ID_MUSTER = Regex("^[a-z]+\\.[a-z0-9]+(-[a-z0-9]+)*$")
    private val VERSION_MUSTER = Regex("^\\d+(\\.\\d+){0,2}$")
    private val SCHRITT_ID_MUSTER = Regex("^[a-z0-9][a-z0-9-]*$")

    fun pruefe(p: Paket): List<Problem> {
        val out = mutableListOf<Problem>()
        fun fehler(ort: String, text: String) = out.add(Problem(ort, text))
        fun warnung(ort: String, text: String) = out.add(Problem(ort, text, schwer = false))

        if (p.format != Paket.FORMAT) fehler("format", "erwartet \"${Paket.FORMAT}\", gefunden \"${p.format}\"")
        if (p.typ !in Paket.TYPEN) fehler("typ", "unbekannt: \"${p.typ}\"")
        if (!ID_MUSTER.matches(p.id)) fehler("id", "muss wie \"fach.thema-name\" aussehen: \"${p.id}\"")
        if (!p.id.startsWith(p.fach + ".") && p.typ != Paket.TYP_FORMELSAMMLUNG) warnung("id", "beginnt nicht mit dem Fach \"${p.fach}\"")
        if (!VERSION_MUSTER.matches(p.version)) fehler("version", "muss wie 1.0 oder 1.2.3 aussehen: \"${p.version}\"")
        if (p.fach.isBlank()) fehler("fach", "fehlt")
        if (p.titel.isBlank()) fehler("titel", "fehlt")

        if (p.typ == Paket.TYP_FORMELSAMMLUNG) {
            if (p.gebiete == null) fehler("gebiete", "fehlt bei Typ formelsammlung")
            return out
        }

        val u = p.ueberblick
        u.aufgabe?.let { html("ueberblick.aufgabe", it, out) }
        u.muster?.let { html("ueberblick.muster", it, out) }
        u.nachher?.let { html("ueberblick.nachher", it, out) }
        u.aufbau.forEachIndexed { i, t -> if (t.size != 2) fehler("ueberblick.aufbau[$i]", "braucht genau 2 Einträge [Teil, Beschreibung]") }
        if (p.typ == Paket.TYP_ARBEITSHEFT) {
            if (u.quelle.isNullOrBlank()) fehler("ueberblick.quelle", "fehlt bei Typ arbeitsheft")
            if (u.karte.isEmpty()) fehler("ueberblick.karte", "fehlt bei Typ arbeitsheft")
            u.karte.forEachIndexed { i, t -> if (t.size != 3) fehler("ueberblick.karte[$i]", "braucht genau 3 Einträge [Zeilen, Teil, Was tun]") }
        }

        if (p.begleiter.isEmpty()) fehler("begleiter", "mindestens ein Schritt nötig")
        val ids = mutableSetOf<String>()
        p.begleiter.forEachIndexed { i, s ->
            val ort = "begleiter[$i] (${s.id})"
            if (!SCHRITT_ID_MUSTER.matches(s.id)) fehler(ort, "id ungültig")
            if (!ids.add(s.id)) fehler(ort, "id doppelt")
            if (s.kurz.isBlank() || s.titel.isBlank()) fehler(ort, "kurz und titel nötig")
            if (s.muss.isEmpty()) warnung(ort, "keine Abhakpunkte")
            if (s.vnLabel.isNotEmpty() && s.vnLabel.size != 2) fehler(ort, "vnLabel braucht genau 2 Einträge")
            if ((s.vorher == null) != (s.nachher == null)) fehler(ort, "vorher und nachher nur gemeinsam")
            s.nachher?.let { html("$ort.nachher", it, out) }
            s.frage?.let { f ->
                if (f.optionen.size < 2) fehler("$ort.frage", "mindestens 2 Optionen")
                if (f.richtig !in f.optionen.indices) fehler("$ort.frage", "richtig=${f.richtig} liegt außerhalb der Optionen")
            }
            if (s.wortkiste && p.wortkiste.isEmpty()) warnung(ort, "wortkiste=true, aber das Paket hat keine Wortkiste")
            s.pruefung?.let { if (it !in setOf("aequivalenz", "loesung", "einheit")) fehler(ort, "pruefung unbekannt: $it") }
            if (s.hinweise.size > 3) warnung(ort, "mehr als 3 Tipps")
        }

        val stufen = listOf("einstieg" to p.uebungen.einstieg, "sicher" to p.uebungen.sicher, "schulaufgabe" to p.uebungen.schulaufgabe)
        val uebIds = mutableSetOf<String>()
        for ((name, liste) in stufen) liste.forEachIndexed { i, ue ->
            val ort = "uebungen.$name[$i]"
            ue.id?.let { if (!uebIds.add(it)) fehler(ort, "id doppelt") }
            when (ue) {
                is Uebung.Auswahl -> {
                    if (ue.optionen.size < 2) fehler(ort, "mindestens 2 Optionen")
                    if (ue.richtig !in ue.optionen.indices) fehler(ort, "richtig=${ue.richtig} liegt außerhalb der Optionen")
                }
                is Uebung.Tippen -> {
                    if (ue.tokens.isEmpty()) fehler(ort, "keine Tokens")
                    if (ue.tokens.none { it.richtig }) fehler(ort, "kein Token ist als richtig markiert")
                }
                is Uebung.Text -> html("$ort.loesung", ue.loesung, out)
                is Uebung.Umschreiben -> Unit
                is Uebung.Unbekannt -> warnung(ort, "Übungstyp \"${ue.typ}\" kennt diese App-Version nicht, wird übersprungen")
            }
        }

        p.merkzettel.forEachIndexed { i, t -> if (t.size != 2) fehler("merkzettel[$i]", "braucht genau 2 Einträge [Stichwort, Text]") }
        p.korrektur.forEachIndexed { i, t -> if (t.size != 3) fehler("korrektur[$i]", "braucht genau 3 Einträge [Zeichen, Bedeutung, Beispiel]") }
        return out
    }

    private fun html(ort: String, text: String, out: MutableList<Problem>) {
        HtmlMini.unerlaubteTags(text).forEach { out.add(Problem(ort, "HTML nicht erlaubt: $it")) }
    }
}
