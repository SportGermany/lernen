package de.lernen.core.data

import de.lernen.core.data.paket.Fortschritt
import de.lernen.core.data.paket.HakenSchluessel
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.data.paket.Versionen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FortschrittTest {
    private val paket = PaketParser.liesOderWirf(TestDaten.MINI)

    @Test fun `leerer Fortschritt`() {
        val f = Fortschritt(paket, emptySet())
        assertEquals(0, f.erledigt)
        assertEquals(3, f.gesamt)
        assertEquals(0f, f.anteil)
    }

    @Test fun `Haken zählen und Schritt fertig`() {
        val f = Fortschritt(paket, setOf(HakenSchluessel("a", 0), HakenSchluessel("a", 1)))
        assertEquals(2, f.erledigt)
        assertTrue(f.schrittFertig(paket.begleiter[0]))
        assertFalse(f.schrittFertig(paket.begleiter[1]))
        assertEquals(2f / 3f, f.anteil, 0.0001f)
    }

    @Test fun `verwaiste Haken nach Update verfallen still`() {
        val alle = setOf(
            HakenSchluessel("a", 0),
            HakenSchluessel("weg", 0),   // Schritt gibt es nicht mehr
            HakenSchluessel("b", 5),     // Punkt gibt es nicht mehr
        )
        val f = Fortschritt(paket, alle)
        assertEquals(1, f.erledigt)
        assertEquals(setOf(HakenSchluessel("weg", 0), HakenSchluessel("b", 5)), f.verwaiste(alle))
    }

    @Test fun `Arbeitsheft hat 52 Abhakpunkte`() {
        val p = PaketParser.liesOderWirf(TestDaten.paket(TestDaten.ARBEITSHEFT))
        assertEquals(p.begleiter.sumOf { it.muss.size }, Fortschritt(p, emptySet()).gesamt)
    }

    @Test fun `Versionen werden numerisch verglichen`() {
        assertTrue(Versionen.istNeuer("1.10", "1.9"))
        assertTrue(Versionen.istNeuer("1.2", "1.1.9"))
        assertFalse(Versionen.istNeuer("1.0", "1"))
        assertFalse(Versionen.istNeuer("1.0", "1.0"))
        assertEquals(-1, Versionen.vergleiche("0.9", "1.0").coerceIn(-1, 1))
    }
}
