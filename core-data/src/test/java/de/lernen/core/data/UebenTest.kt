package de.lernen.core.data

import de.lernen.core.data.model.Token
import de.lernen.core.data.paket.ErgebnisStatus
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.data.paket.Stufe
import de.lernen.core.data.paket.Ueben
import de.lernen.core.data.paket.Ueben.TokenBewertung.FALSCH_MARKIERT
import de.lernen.core.data.paket.Ueben.TokenBewertung.GEFUNDEN
import de.lernen.core.data.paket.Ueben.TokenBewertung.NEUTRAL
import de.lernen.core.data.paket.Ueben.TokenBewertung.UEBERSEHEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UebenTest {
    private val p = PaketParser.liesOderWirf(TestDaten.paket(TestDaten.ARBEITSHEFT))

    @Test fun `Aufgaben-ids aus Stufe und Index`() {
        val a = Ueben.aufgaben(p, Stufe.SICHER)
        assertEquals(7, a.size)
        assertEquals("sicher-0", a[0].id)
        assertEquals(14, Ueben.alle(p).size)
    }

    @Test fun `Wiederholen enthält nur falsch und unsicher`() {
        val e = mapOf("einstieg-0" to ErgebnisStatus.FALSCH, "einstieg-1" to ErgebnisStatus.RICHTIG, "sicher-2" to ErgebnisStatus.UNSICHER)
        assertEquals(listOf("einstieg-0", "sicher-2"), Ueben.wiederholen(p, e).map { it.id })
        // nach richtiger Lösung fällt die Aufgabe heraus
        assertEquals(listOf("sicher-2"), Ueben.wiederholen(p, e + ("einstieg-0" to ErgebnisStatus.RICHTIG)).map { it.id })
    }

    @Test fun `Tokens bewerten`() {
        val t = listOf(Token("a", true), Token("b", false), Token("c", true), Token("d", false))
        assertEquals(listOf(GEFUNDEN, FALSCH_MARKIERT, UEBERSEHEN, NEUTRAL), Ueben.bewerte(t, setOf(0, 1)))
        assertFalse(Ueben.tippenRichtig(t, setOf(0, 1)))
        assertTrue(Ueben.tippenRichtig(t, setOf(0, 2)))
    }
}
