package de.lernen.core.data.paket

import de.lernen.core.data.model.Paket
import de.lernen.core.data.model.Schritt

/** Ein gesetzter Haken: Punkt [index] in Schritt [schrittId]. */
data class HakenSchluessel(val schrittId: String, val index: Int)

/**
 * Fortschrittsrechnung. Haken werden über die Schritt-id zugeordnet.
 * Haken zu Schritten oder Punkten, die es (nach einem Update) nicht mehr gibt, zählen nicht.
 */
class Fortschritt(private val paket: Paket, haken: Set<HakenSchluessel>) {

    private val gueltig: Set<HakenSchluessel> = haken.filterTo(mutableSetOf()) { h ->
        val s = paket.begleiter.firstOrNull { it.id == h.schrittId }
        s != null && h.index in s.muss.indices
    }

    val gesamt: Int = paket.punkteGesamt
    val erledigt: Int = gueltig.size
    val anteil: Float = if (gesamt == 0) 0f else erledigt.toFloat() / gesamt

    fun istAbgehakt(schrittId: String, index: Int) = HakenSchluessel(schrittId, index) in gueltig

    fun erledigtIn(s: Schritt): Int = s.muss.indices.count { istAbgehakt(s.id, it) }

    fun schrittFertig(s: Schritt): Boolean = s.muss.isNotEmpty() && erledigtIn(s) == s.muss.size

    /** Haken, die nicht mehr passen (werden beim Update still entfernt). */
    fun verwaiste(alle: Set<HakenSchluessel>): Set<HakenSchluessel> = alle - gueltig
}

/** Vergleicht Versionen wie "1.0", "1.2", "1.10.1" numerisch. */
object Versionen {
    fun vergleiche(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    fun istNeuer(kandidat: String, vorhanden: String) = vergleiche(kandidat, vorhanden) > 0
}
