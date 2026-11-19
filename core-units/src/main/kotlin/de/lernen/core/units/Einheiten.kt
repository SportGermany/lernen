package de.lernen.core.units

import kotlin.math.abs
import kotlin.math.max

/** Dimension als Exponenten der SI-Basisgrößen: m, kg, s, A, K, mol, cd. */
data class Dimension(val m: Int = 0, val kg: Int = 0, val s: Int = 0, val a: Int = 0, val k: Int = 0, val mol: Int = 0, val cd: Int = 0) {
    operator fun plus(o: Dimension) = Dimension(m + o.m, kg + o.kg, s + o.s, a + o.a, k + o.k, mol + o.mol, cd + o.cd)
    operator fun times(n: Int) = Dimension(m * n, kg * n, s * n, a * n, k * n, mol * n, cd * n)
    val los: Boolean get() = this == Dimension()
}

/** Eine Einheit: Faktor zur SI-Einheit und Dimension. */
data class Einheit(val zeichen: String, val faktor: Double, val dim: Dimension) {
    fun mal(o: Einheit) = Einheit("$zeichen·${o.zeichen}", faktor * o.faktor, dim + o.dim)
    fun durch(o: Einheit) = Einheit("$zeichen/${o.zeichen}", faktor / o.faktor, dim + o.dim * -1)
    fun hoch(n: Int) = Einheit("$zeichen^$n", Math.pow(faktor, n.toDouble()), dim * n)
}

/** Eine Größe mit Wert und Einheit, z. B. 8,7 A. */
data class Groesse(val wert: Double, val einheit: Einheit?) {
    val si: Double get() = wert * (einheit?.faktor ?: 1.0)
}

class EinheitFehler(m: String) : IllegalArgumentException(m)

/** Einheiten der Schulphysik (SI + übliche Schulgrößen). */
object Einheiten {
    private val L = Dimension(m = 1); private val M = Dimension(kg = 1); private val T = Dimension(s = 1); private val I = Dimension(a = 1)
    private val TEMP = Dimension(k = 1)
    private val KRAFT = L + M + T * -2
    private val ENERGIE = KRAFT + L
    private val LEISTUNG = ENERGIE + T * -1
    private val SPANNUNG = LEISTUNG + I * -1

    /** Grundeinheiten, die Vorsätze (k, M, m, µ …) bekommen dürfen. */
    private val MIT_VORSATZ: Map<String, Einheit> = listOf(
        Einheit("m", 1.0, L), Einheit("g", 1e-3, M), Einheit("s", 1.0, T), Einheit("A", 1.0, I),
        Einheit("N", 1.0, KRAFT), Einheit("J", 1.0, ENERGIE), Einheit("W", 1.0, LEISTUNG), Einheit("Wh", 3600.0, ENERGIE),
        Einheit("V", 1.0, SPANNUNG), Einheit("Ω", 1.0, SPANNUNG + I * -1), Einheit("Pa", 1.0, KRAFT + L * -2),
        Einheit("Hz", 1.0, T * -1), Einheit("l", 1e-3, L * 3), Einheit("L", 1e-3, L * 3), Einheit("C", 1.0, I + T),
        Einheit("Bq", 1.0, T * -1), Einheit("bar", 1e5, KRAFT + L * -2), Einheit("eV", 1.602176634e-19, ENERGIE),
    ).associateBy { it.zeichen }

    /** Einheiten ohne Vorsätze. */
    private val OHNE_VORSATZ: Map<String, Einheit> = listOf(
        Einheit("min", 60.0, T), Einheit("h", 3600.0, T), Einheit("d", 86400.0, T), Einheit("a", 365.25 * 86400, T),
        Einheit("t", 1000.0, M), Einheit("K", 1.0, TEMP), Einheit("°C", 1.0, TEMP),
        Einheit("km/h", 1 / 3.6, L + T * -1), Einheit("kmh", 1 / 3.6, L + T * -1),
        Einheit("cal", 4.1868, ENERGIE), Einheit("kcal", 4186.8, ENERGIE), Einheit("PS", 735.49875, LEISTUNG),
        Einheit("%", 0.01, Dimension()), Einheit("ohm", 1.0, SPANNUNG + I * -1), Einheit("Ohm", 1.0, SPANNUNG + I * -1),
    ).associateBy { it.zeichen }

    private val VORSATZ = mapOf("G" to 1e9, "M" to 1e6, "k" to 1e3, "h" to 1e2, "d" to 1e-1, "c" to 1e-2, "m" to 1e-3, "µ" to 1e-6, "u" to 1e-6, "n" to 1e-9)

    private fun einfach(z: String): Einheit {
        OHNE_VORSATZ[z]?.let { return it }
        MIT_VORSATZ[z]?.let { return it }
        if (z.length >= 2) {
            val v = VORSATZ[z.substring(0, 1)]
            val b = MIT_VORSATZ[z.substring(1)]
            if (v != null && b != null) return Einheit(z, v * b.faktor, b.dim)
        }
        throw EinheitFehler("Einheit unbekannt: $z")
    }

    /** Liest Einheiten wie "m/s", "km/h", "kWh", "N·m", "m²", "kg/m^3", "1/s". */
    fun lies(text: String): Einheit {
        val t = text.trim().replace("²", "^2").replace("³", "^3").replace('*', '·').replace('⋅', '·').replace(" ", "")
        if (t.isEmpty()) throw EinheitFehler("Einheit fehlt")
        OHNE_VORSATZ[t]?.let { return it }
        val teile = t.split('/')
        var e = Einheit(t, 1.0, Dimension())
        teile.forEachIndexed { i, teil ->
            val faktoren = teil.split('·').filter { it.isNotEmpty() }
            for (f in faktoren) {
                if (f == "1") continue
                val (name, exp) = f.split('^').let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 1) }
                val x = einfach(name).hoch(exp)
                e = if (i == 0) Einheit(t, e.faktor * x.faktor, e.dim + x.dim) else Einheit(t, e.faktor / x.faktor, e.dim + x.dim * -1)
            }
        }
        return e.copy(zeichen = text.trim())
    }

    private val ZAHL = Regex("^\\s*([+\\-−]?\\d+(?:[.,]\\d+)?(?:[eE][+\\-]?\\d+)?)\\s*(?:·\\s*10\\^?([+\\-−]?\\d+))?\\s*(.*)$")

    /** Liest "8,7 A", "8.7A", "2 kW", "3·10^5 Pa", "12". */
    fun groesse(text: String): Groesse {
        val m = ZAHL.find(text.trim()) ?: throw EinheitFehler("Keine Zahl gefunden")
        var w = m.groupValues[1].replace(',', '.').replace('−', '-').toDouble()
        m.groupValues[2].takeIf { it.isNotEmpty() }?.let { w *= Math.pow(10.0, it.replace('−', '-').toDouble()) }
        val rest = m.groupValues[3].trim()
        return Groesse(w, if (rest.isEmpty()) null else lies(rest))
    }

    /** Umrechnen in eine Zieleinheit (gleiche Dimension). Temperatur mit Verschiebung. */
    fun umrechnen(wert: Double, von: String, nach: String): Double {
        if (von in TEMPERATUR || nach in TEMPERATUR) {
            val k = when (von) { "°C" -> wert + 273.15; "°F" -> (wert - 32) * 5 / 9 + 273.15; "K" -> wert; else -> throw EinheitFehler("Keine Temperatur: $von") }
            return when (nach) { "°C" -> k - 273.15; "°F" -> (k - 273.15) * 9 / 5 + 32; "K" -> k; else -> throw EinheitFehler("Keine Temperatur: $nach") }
        }
        val a = lies(von); val b = lies(nach)
        if (a.dim != b.dim) throw EinheitFehler("$von und $nach passen nicht zusammen")
        return wert * a.faktor / b.faktor
    }

    private val TEMPERATUR = setOf("°C", "°F", "K")

    enum class Bewertung { RICHTIG, OHNE_EINHEIT, FALSCHE_EINHEIT, FALSCHE_ZAHL, UNLESBAR }

    data class Pruefung(val bewertung: Bewertung, val meldung: String)

    /**
     * Prüft ein Physik-Ergebnis: Zahl und Einheit.
     * Ohne Einheit: „halb richtig“ mit Hinweis. Andere, aber passende Einheit (z. B. mA statt A) zählt, wenn umgerechnet stimmt.
     */
    fun pruefe(eingabe: String, sollWert: Double, sollEinheit: String, toleranz: Double = 0.02): Pruefung {
        val g = runCatching { groesse(eingabe) }.getOrElse {
            return Pruefung(Bewertung.UNLESBAR, "Das kann ich nicht lesen. Schreib z. B. „8,7 A“.")
        }
        val soll = lies(sollEinheit)
        fun nahe(a: Double, b: Double) = abs(a - b) <= toleranz * max(abs(b), 1e-12)
        val e = g.einheit ?: return if (nahe(g.wert, sollWert)) Pruefung(Bewertung.OHNE_EINHEIT, "Halb richtig: Die Zahl stimmt, aber die Einheit fehlt. Ohne Einheit gibt es in der Schulaufgabe Abzug.")
        else Pruefung(Bewertung.FALSCHE_ZAHL, "Die Zahl stimmt noch nicht, und die Einheit fehlt.")
        if (e.dim != soll.dim) return Pruefung(Bewertung.FALSCHE_EINHEIT, "Die Einheit passt nicht zur gesuchten Größe. Gesucht ist etwas in $sollEinheit.")
        return if (nahe(g.si, sollWert * soll.faktor)) Pruefung(Bewertung.RICHTIG, "Richtig, mit Einheit. Jetzt noch der Antwortsatz.")
        else Pruefung(Bewertung.FALSCHE_ZAHL, "Die Einheit passt, aber die Zahl stimmt noch nicht. Prüfe Umstellen und Einsetzen (Einheiten umrechnen?).")
    }

    /** Für den Umrechner: Größenarten mit typischen Einheiten. */
    val KATEGORIEN: List<Pair<String, List<String>>> = listOf(
        "Länge" to listOf("mm", "cm", "dm", "m", "km"),
        "Fläche" to listOf("mm²", "cm²", "dm²", "m²", "km²"),
        "Volumen" to listOf("ml", "cl", "l", "cm³", "dm³", "m³"),
        "Masse" to listOf("mg", "g", "kg", "t"),
        "Zeit" to listOf("ms", "s", "min", "h", "d"),
        "Geschwindigkeit" to listOf("m/s", "km/h"),
        "Kraft" to listOf("mN", "N", "kN"),
        "Energie" to listOf("J", "kJ", "MJ", "Wh", "kWh", "kcal"),
        "Leistung" to listOf("mW", "W", "kW", "MW", "PS"),
        "Spannung" to listOf("mV", "V", "kV"),
        "Stromstärke" to listOf("mA", "A"),
        "Widerstand" to listOf("Ω", "kΩ", "MΩ"),
        "Druck" to listOf("Pa", "hPa", "kPa", "bar"),
        "Temperatur" to listOf("°C", "K", "°F"),
    )
}
