package de.lernen.core.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Prüft Terme und Gleichungen auf Gleichwertigkeit.
 * Terme: Auswertung an vielen zufälligen Stellen.
 * Gleichungen: (1) f₁ = c·f₂ (Umformung durch Addieren/Multiplizieren) oder
 * (2) gleiche Lösungsmenge (eine Variable: Nullstellen numerisch, lineare Systeme: exakt gelöst).
 */
object Aequivalenz {
    private const val PUNKTE = 24

    private fun belegungen(vars: Set<String>, seed: Int = 7): List<Map<String, Double>> {
        val r = Random(seed)
        return (0 until PUNKTE).map { vars.associateWith { (r.nextDouble() * 2 - 1) * (if (r.nextBoolean()) 3.0 else 17.0) + 0.37 } }
    }

    private fun nahe(a: Double, b: Double, tol: Double = 1e-7) = abs(a - b) <= tol * max(1.0, max(abs(a), abs(b)))

    private fun sicher(f: () -> Double): Double? = runCatching(f).getOrNull()?.takeIf { it.isFinite() }

    fun termeGleich(a: Ausdruck, b: Ausdruck): Boolean {
        var gueltig = 0
        for (bel in belegungen(a.variablen() + b.variablen())) {
            val x = sicher { a.wert(bel, Winkel.BOGEN) } ?: continue
            val y = sicher { b.wert(bel, Winkel.BOGEN) } ?: continue
            if (!nahe(x, y)) return false
            gueltig++
        }
        return gueltig >= 6
    }

    /** f₁ = c·f₂ mit c ≠ 0 an allen Stellen? */
    fun proportional(g1: Gleichung, g2: Gleichung): Boolean {
        var c: Double? = null
        var gueltig = 0
        for (bel in belegungen(g1.variablen() + g2.variablen(), 11)) {
            val f1 = sicher { g1.differenz(bel) } ?: continue
            val f2 = sicher { g2.differenz(bel) } ?: continue
            if (abs(f2) < 1e-12) { if (abs(f1) > 1e-9) return false else continue }
            val k = f1 / f2
            if (abs(k) < 1e-12) return false
            if (c == null) c = k else if (!nahe(c, k, 1e-6)) return false
            gueltig++
        }
        return gueltig >= 6
    }

    enum class Ergebnis { GLEICHWERTIG, NICHT_GLEICHWERTIG, LOESUNG_VERLOREN, LOESUNG_DAZU, UNKLAR }

    fun gleichungenGleich(g1: Gleichung, g2: Gleichung): Ergebnis {
        if (proportional(g1, g2)) return Ergebnis.GLEICHWERTIG
        val vars = g1.variablen() + g2.variablen()
        if (vars.size == 1) {
            val v = vars.first()
            val l1 = Loeser.nullstellen(g1, v) ?: return Ergebnis.UNKLAR
            val l2 = Loeser.nullstellen(g2, v) ?: return Ergebnis.UNKLAR
            val fehlt = l1.any { a -> l2.none { nahe(a, it, 1e-5) } }
            val dazu = l2.any { b -> l1.none { nahe(b, it, 1e-5) } }
            return when {
                !fehlt && !dazu -> Ergebnis.GLEICHWERTIG
                fehlt && !dazu -> Ergebnis.LOESUNG_VERLOREN
                dazu && !fehlt -> Ergebnis.LOESUNG_DAZU
                else -> Ergebnis.NICHT_GLEICHWERTIG
            }
        }
        // mehrere Variablen, lineare Gleichungen: gleiche Lösungsmenge ⇔ proportional (schon geprüft)
        return Ergebnis.NICHT_GLEICHWERTIG
    }

    /** Gilt die Gleichung, wenn man die Lösung einsetzt? */
    fun erfuellt(g: Gleichung, loesung: Map<String, Double>): Boolean {
        if (!loesung.keys.containsAll(g.variablen())) return false
        val l = sicher { g.links.wert(loesung, Winkel.BOGEN) } ?: return false
        val r = sicher { g.rechts.wert(loesung, Winkel.BOGEN) } ?: return false
        return nahe(l, r, 1e-6)
    }
}

/** Löser für Schulaufgaben. */
object Loeser {

    /** Reelle Lösungen einer Gleichung in einer Variablen (im Bereich ±10 000), oder null wenn unklar. */
    fun nullstellen(g: Gleichung, v: String): List<Double>? {
        val f = { x: Double -> runCatching { g.differenz(mapOf(v to x)) }.getOrDefault(Double.NaN) }
        // Allgemeingültig oder unlösbar?
        val proben = listOf(-7.3, -1.1, 0.4, 2.9, 13.7).map(f)
        if (proben.all { it.isFinite() && abs(it) < 1e-10 }) return null // für alle x wahr → Sonderfall
        val out = mutableListOf<Double>()
        fun neu(x: Double) { if (out.none { abs(it - x) < 1e-6 * max(1.0, abs(x)) }) out += x }
        for ((von, bis, n) in listOf(Triple(-100.0, 100.0, 40000), Triple(-10000.0, 10000.0, 40000))) {
            val h = (bis - von) / n
            var xa = von; var ya = f(xa)
            for (k in 1..n) {
                val xb = von + k * h; val yb = f(xb)
                if (ya.isFinite() && yb.isFinite()) {
                    if (ya == 0.0) neu(xa)
                    else if (ya * yb < 0) bisektion(f, xa, xb)?.let(::neu)
                    else if (k >= 2) {
                        // Berührstelle (doppelte Nullstelle): lokales Minimum von |f| nahe 0
                        val xm = xa - h; val ym = f(xm)
                        if (ym.isFinite() && abs(ya) <= abs(ym) && abs(ya) <= abs(yb) && abs(ya) < 1e-3) {
                            minimum(f, xm, xb)?.let { if (abs(f(it)) < 1e-9) neu(it) }
                        }
                    }
                }
                xa = xb; ya = yb
            }
        }
        return out.map { runde(it) }.sorted()
    }

    private fun bisektion(f: (Double) -> Double, a0: Double, b0: Double): Double? {
        var a = a0; var b = b0; var fa = f(a)
        repeat(200) {
            val m = (a + b) / 2; val fm = f(m)
            if (!fm.isFinite()) return null
            if (fm == 0.0 || (b - a) < 1e-13) return m
            if (fa * fm < 0) b = m else { a = m; fa = fm }
        }
        val m = (a + b) / 2
        return if (abs(f(m)) < 1e-6) m else null // Polstelle statt Nullstelle
    }

    private fun minimum(f: (Double) -> Double, a0: Double, b0: Double): Double? {
        var a = a0; var b = b0
        val g = (Math.sqrt(5.0) - 1) / 2
        repeat(200) {
            val c = b - g * (b - a); val d = a + g * (b - a)
            if (abs(f(c)) < abs(f(d))) b = d else a = c
        }
        return (a + b) / 2
    }

    /** Rundet Rechenrauschen weg (2.0000000001 → 2). */
    fun runde(x: Double): Double {
        val r = Math.rint(x * 1e9) / 1e9
        return if (r == -0.0) 0.0 else r
    }

    /** Ist f linear in allen Variablen? Liefert dann Koeffizienten (a_i) und Konstante c: f = Σ a_i·v_i + c */
    fun linear(g: Gleichung, vars: List<String>): Pair<DoubleArray, Double>? {
        val null0 = vars.associateWith { 0.0 }
        val c = runCatching { g.differenz(null0) }.getOrNull()?.takeIf { it.isFinite() } ?: return null
        val a = DoubleArray(vars.size) { i -> runCatching { g.differenz(null0 + (vars[i] to 1.0)) - c }.getOrDefault(Double.NaN) }
        if (a.any { !it.isFinite() }) return null
        val r = Random(3)
        repeat(8) {
            val p = vars.associateWith { r.nextDouble() * 20 - 10 }
            val soll = vars.indices.sumOf { a[it] * p.getValue(vars[it]) } + c
            val ist = runCatching { g.differenz(p) }.getOrDefault(Double.NaN)
            if (!ist.isFinite() || abs(ist - soll) > 1e-6 * max(1.0, abs(soll))) return null
        }
        return a to c
    }

    /** Löst ein lineares Gleichungssystem. null = nicht linear oder keine eindeutige Lösung. */
    fun lgs(gleichungen: List<Gleichung>): Map<String, Double>? {
        val vars = gleichungen.flatMap { it.variablen() }.distinct().sorted()
        if (vars.isEmpty() || gleichungen.size < vars.size) return null
        val zeilen = gleichungen.map { linear(it, vars) ?: return null }
        val n = vars.size
        val m = Array(zeilen.size) { i -> DoubleArray(n + 1) { j -> if (j < n) zeilen[i].first[j] else -zeilen[i].second } }
        var zeile = 0
        val pivotSpalte = IntArray(n) { -1 }
        for (sp in 0 until n) {
            val p = (zeile until m.size).maxByOrNull { abs(m[it][sp]) } ?: return null
            if (abs(m[p][sp]) < 1e-12) return null
            val tmp = m[p]; m[p] = m[zeile]; m[zeile] = tmp
            for (r in m.indices) if (r != zeile) {
                val k = m[r][sp] / m[zeile][sp]
                for (j in sp..n) m[r][j] -= k * m[zeile][j]
            }
            pivotSpalte[sp] = zeile
            zeile++
        }
        // überzählige Gleichungen müssen 0 = 0 ergeben
        for (r in zeile until m.size) if (abs(m[r][n]) > 1e-9) return null
        return vars.indices.associate { vars[it] to runde(m[pivotSpalte[it]][n] / m[pivotSpalte[it]][it]) }
    }
}
