package de.lernen.core.math

/** Ergebnis der Prüfung eines eigenen Rechenschritts. */
data class SchrittPruefung(val ok: Boolean, val fertig: Boolean, val meldung: String)

/**
 * Mitrechnen: Die Aufgabe (ein Term, eine Gleichung oder ein Gleichungssystem) und jeder eigene Schritt
 * werden geprüft. Bei Fehlern gibt es einen Hinweis, aber nie die Lösung.
 */
class Mitrechnen(aufgabeZeilen: List<String>) {

    val aufgabe: List<Eingabe> = aufgabeZeilen.map(::ohneNummer).filter { it.isNotBlank() }.map { Parser.eingabe(it) }
    private val gleichungen = aufgabe.filterIsInstance<Eingabe.Gl>().map { it.g }
    private val istSystem = gleichungen.size >= 2
    private val systemLoesung: Map<String, Double>? = if (istSystem) Loeser.lgs(gleichungen) else null
    private val einVar: String? = gleichungen.singleOrNull()?.variablen()?.singleOrNull()
    private val einLoesung: List<Double>? = einVar?.let { Loeser.nullstellen(gleichungen.single(), it) }

    /** Letzter richtiger Schritt (Startwert: die Aufgabe). */
    private var vorher: Eingabe? = aufgabe.singleOrNull()

    init {
        require(aufgabe.isNotEmpty()) { "Bitte zuerst die Aufgabe eingeben." }
        require(aufgabe.all { it is Eingabe.Gl } || aufgabe.size == 1) { "Ein Gleichungssystem besteht nur aus Gleichungen." }
    }

    val beschreibung: String = when {
        istSystem && systemLoesung != null -> "Lineares Gleichungssystem mit ${systemLoesung.size} Unbekannten"
        istSystem -> "Gleichungssystem (nicht eindeutig lösbar oder nicht linear)"
        gleichungen.size == 1 -> "Gleichung" + (einVar?.let { " mit der Unbekannten $it" } ?: "")
        else -> "Term umformen"
    }

    fun pruefe(text: String): SchrittPruefung {
        val e = try { Parser.eingabe(ohneNummer(text)) } catch (ex: RechenFehler) {
            return SchrittPruefung(false, false, "Das kann ich nicht lesen: ${ex.message}")
        }
        val r = when (e) {
            is Eingabe.Term -> pruefeTerm(e)
            is Eingabe.Gl -> pruefeGleichung(e.g)
        }
        if (r.ok) vorher = e
        return r
    }

    private fun pruefeTerm(e: Eingabe.Term): SchrittPruefung {
        val v = vorher as? Eingabe.Term ?: return SchrittPruefung(false, false, "Hier erwarte ich eine Gleichung mit =.")
        return if (Aequivalenz.termeGleich(v.a, e.a)) {
            SchrittPruefung(true, false, "Stimmt. Der Term ist gleichwertig.")
        } else SchrittPruefung(false, false, "Nicht gleichwertig. Prüfe Vorzeichen und Klammern. Setz zur Probe eine Zahl ein.")
    }

    private fun pruefeGleichung(g: Gleichung): SchrittPruefung {
        // Fertig? „x = Zahl“
        val ziel = (g.links as? Ausdruck.Variable)?.name
        val zahl = runCatching { g.rechts.wert(emptyMap()) }.getOrNull()
        val vorherG = (vorher as? Eingabe.Gl)?.g

        if (istSystem) {
            val l = systemLoesung ?: return SchrittPruefung(false, false, "Dieses System hat keine eindeutige Lösung. Prüfe die Aufgabe.")
            // gleichwertig zum vorherigen Schritt (z. B. Gleichung umgestellt)?
            if (vorherG != null && Aequivalenz.proportional(vorherG, g)) return ok(g, ziel, zahl, l, "Stimmt. Die Umformung ist richtig.")
            if (Aequivalenz.erfuellt(g, l.filterKeys { it in g.variablen() }.takeIf { it.size == g.variablen().size } ?: l)) {
                return ok(g, ziel, zahl, l, "Stimmt. Der Schritt passt zum Gleichungssystem.")
            }
            return SchrittPruefung(false, false, "Das passt nicht zum Gleichungssystem. Prüfe Einsetzen, Klammern und Vorzeichen.")
        }

        if (vorherG == null) return SchrittPruefung(false, false, "Hier erwarte ich einen Term ohne =.")
        return when (Aequivalenz.gleichungenGleich(vorherG, g)) {
            Aequivalenz.Ergebnis.GLEICHWERTIG -> {
                val fertig = ziel != null && zahl != null && g.variablen() == setOf(ziel)
                SchrittPruefung(true, fertig, if (fertig) "Stimmt. Lösung: $ziel = ${Zahlen.text(zahl!!)}. Mach noch die Probe." else "Stimmt. Die Umformung ist richtig.")
            }
            Aequivalenz.Ergebnis.LOESUNG_VERLOREN -> SchrittPruefung(false, false, "Hier geht eine Lösung verloren. Denk an ± beim Wurzelziehen oder ans Teilen durch einen Term mit x.")
            Aequivalenz.Ergebnis.LOESUNG_DAZU -> SchrittPruefung(false, false, "Hier kommt eine Lösung dazu, die vorher keine war. Prüfe Quadrieren oder Multiplizieren mit x.")
            Aequivalenz.Ergebnis.UNKLAR -> SchrittPruefung(false, false, "Das kann ich nicht sicher prüfen. Mach die Probe durch Einsetzen.")
            Aequivalenz.Ergebnis.NICHT_GLEICHWERTIG -> SchrittPruefung(false, false, "Nicht gleichwertig zum vorherigen Schritt. Hast du auf beiden Seiten dasselbe gemacht?")
        }
    }

    private fun ok(g: Gleichung, ziel: String?, zahl: Double?, l: Map<String, Double>, text: String): SchrittPruefung {
        val fertigVar = ziel != null && zahl != null && g.variablen() == setOf(ziel)
        return if (fertigVar) SchrittPruefung(true, false, "Stimmt: $ziel = ${Zahlen.text(zahl!!)}." + offen(l, ziel!!))
        else SchrittPruefung(true, false, text)
    }

    private val gefunden = mutableSetOf<String>()
    private fun offen(l: Map<String, Double>, v: String): String {
        gefunden += v
        val rest = l.keys - gefunden
        return if (rest.isEmpty()) " Alle Unbekannten gefunden. Mach die Probe und schreib die Lösungsmenge auf." else " Jetzt noch ${rest.joinToString(" und ")} berechnen."
    }

    /** Für Tipps: Lösung (nur intern, wird nie angezeigt). */
    internal fun loesungIntern(): Map<String, Double>? = systemLoesung ?: einVar?.let { v -> einLoesung?.singleOrNull()?.let { mapOf(v to it) } }
}

/** "(I) y = 2x − 1" → "y = 2x − 1" (Gleichungsnummern ignorieren). */
fun ohneNummer(zeile: String): String = zeile.replace(Regex("^\\s*\\((I{1,3}|IV|V|1|2|3)\\)\\s*"), "").trim()

/** Zahlen deutsch formatieren: Komma, höchstens 6 Nachkommastellen, ohne Rauschen. */
object Zahlen {
    fun text(x: Double, stellen: Int = 6): String {
        if (x.isNaN()) return "nicht definiert"
        if (x.isInfinite()) return if (x > 0) "∞" else "−∞"
        val r = Loeser.runde(x)
        if (r == Math.rint(r) && kotlin.math.abs(r) < 1e15) return r.toLong().toString().replace('-', '−')
        val s = "%.${stellen}f".format(java.util.Locale.ROOT, r).trimEnd('0').trimEnd('.')
        return s.replace('.', ',').replace('-', '−')
    }
}
