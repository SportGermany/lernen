package de.lernen.core.data

import de.lernen.core.data.model.Paket
import de.lernen.core.data.model.Schritt
import de.lernen.core.data.model.Uebung
import de.lernen.core.data.paket.PaketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PaketParserTest {

    private fun ok(text: String): PaketParser.Ergebnis.Ok =
        when (val e = PaketParser.lies(text)) {
            is PaketParser.Ergebnis.Ok -> e
            is PaketParser.Ergebnis.Fehler -> { fail("Fehler: ${e.meldungen}"); error("") }
        }

    @Test
    fun `alle drei Startpakete lassen sich fehlerfrei lesen`() {
        assertEquals(4, TestDaten.startpakete.size)
        TestDaten.startpakete.forEach { (name, text) ->
            val e = ok(text)
            assertEquals("$name: Warnungen ${e.warnungen}", emptyList<Any>(), e.warnungen)
        }
    }

    @Test
    fun `Arbeitsheft wird vollständig gelesen`() {
        val p = ok(TestDaten.paket(TestDaten.ARBEITSHEFT)).paket
        assertEquals("deutsch.ah-s31-klassensprecherfahrt", p.id)
        assertTrue(p.istArbeitsheft)
        assertEquals(3, p.ueberblick.karte.size)
        assertEquals("Z. 1–6", p.ueberblick.karte[0][0])
        assertEquals(8, p.begleiter.size)
        val a1 = p.begleiter.first()
        assertEquals("Erledigt?", a1.listenTitel)
        assertEquals(3, a1.hinweise.size)
        assertEquals(1, a1.frage!!.richtig)
        // Schritt ohne label → "Muss rein"
        assertEquals(Schritt.STANDARD_LABEL, p.begleiter.first { it.id == "einl" }.listenTitel)
        assertEquals("Eigenes Beispiel · zu lang", p.begleiter.first { it.id == "einl" }.vorherLabel)
        assertEquals(10, p.merkEintraege.size)
        assertEquals(8, p.korrekturZeichen.size)
        assertEquals(8, p.wortkiste.size)
    }

    @Test
    fun `Übungstypen werden erkannt, Tokens als Wörter und als Sätze`() {
        val p = ok(TestDaten.paket(TestDaten.ARBEITSHEFT)).paket
        assertEquals(6, p.uebungen.einstieg.size)
        assertTrue(p.uebungen.einstieg.all { it is Uebung.Auswahl })
        val woerter = p.uebungen.sicher[0] as Uebung.Tippen
        assertEquals(18, woerter.tokens.size)
        assertEquals(listOf("echt", "chillen", "futtern", "mega."), woerter.tokens.filter { it.richtig }.map { it.text })
        val saetze = p.uebungen.sicher[1] as Uebung.Tippen
        assertEquals("Ich sehe den See.", saetze.tokens[0].text)
        assertTrue(p.uebungen.sicher[2] is Uebung.Umschreiben)
        val text = p.uebungen.schulaufgabe[0] as Uebung.Text
        assertEquals("VORSICHT FEHLER · eigener Übungstext", text.vlabel)
    }

    @Test
    fun `Schreiben-Paket hat Muster, Aufbau und Hilfen`() {
        val p = ok(TestDaten.paket(TestDaten.SCHREIBEN)).paket
        assertFalse(p.istArbeitsheft)
        assertEquals("Drei Sekunden Stille", p.ueberblick.ueberschrift)
        assertEquals(3, p.ueberblick.aufbau.size)
        assertTrue(p.ueberblick.muster!!.contains("class=\"ms\""))
        assertEquals(2, p.begleiter.first { it.id == "planen" }.hilfen.size)
        assertEquals(2, p.uebungen.schulaufgabe.size)
        assertNull(p.ueberblick.quelle)
    }

    @Test
    fun `unbekannte Felder und unbekannte Übungstypen brechen nichts`() {
        val text = TestDaten.MINI.replace(
            "\"fach\": \"deutsch\",",
            "\"fach\": \"deutsch\", \"neuesFeld\": {\"x\": 1},"
        ).replace(
            "\"einstieg\": [",
            "\"einstieg\": [ {\"typ\": \"zuordnen\", \"paare\": []},"
        )
        val e = ok(text)
        assertTrue(e.paket.uebungen.einstieg[0] is Uebung.Unbekannt)
        assertTrue(e.warnungen.any { it.text.contains("zuordnen") })
    }

    @Test
    fun `kaputtes JSON gibt Fehler statt Absturz`() {
        assertTrue(PaketParser.lies("{ nicht json") is PaketParser.Ergebnis.Fehler)
        assertTrue(PaketParser.lies("""{"format":"lernen-paket/2"}""") is PaketParser.Ergebnis.Fehler)
        val ohneRichtig = TestDaten.MINI.replace("\"richtig\": 0,", "")
        assertTrue(PaketParser.lies(ohneRichtig) is PaketParser.Ergebnis.Fehler)
    }

    @Test
    fun `Mathe-, Physik-Paket und Formelsammlung werden gelesen`() {
        val m = ok(TestDaten.ressource("mathe-lgs-einsetzungsverfahren.json")).paket
        val mr = m.begleiter.first { it.id == "einsetzen" }.mitrechnen!!
        assertEquals(listOf("y = 2x − 1", "3x + 2y = 12"), mr.aufgabe)
        assertTrue(m.uebungen.sicher.all { it is Uebung.Rechnen })
        val ph = ok(TestDaten.ressource("physik-elektrische-leistung.json")).paket
        val pr = ph.begleiter.first { it.id == "rechnen" }.mitrechnen!!
        assertEquals("physik", pr.art)
        assertEquals("A", pr.ergebnis!!.einheit)
        val fs = ok(TestDaten.paket("werkzeuge-formelsammlung.json")).paket
        val f = fs.gebiete.flatMap { it.formeln }.first { it.id == pr.formel }
        assertEquals("I = P : U", f.umgestelltNach("I"))
        assertEquals("P = U·I", f.umgestelltNach("P"))
        assertTrue(fs.gebiete.flatMap { it.formeln }.size > 50)
    }

    @Test
    fun `Englisch-Pakete mit Useful phrases`() {
        val e = ok(TestDaten.ressource("englisch-formal-email.json"))
        assertEquals(emptyList<Any>(), e.warnungen)
        assertEquals(4, e.paket.begleiter.first { it.id == "opening" }.phrases.size)
        val g = ok(TestDaten.ressource("englisch-if-clauses.json"))
        assertEquals(emptyList<Any>(), g.warnungen)
        assertEquals(3, g.paket.ueberblick.aufbau.size)
    }

    @Test
    fun `Fortschritt zählt alle Abhakpunkte`() {
        val p: Paket = PaketParser.liesOderWirf(TestDaten.MINI)
        assertEquals(3, p.punkteGesamt)
    }

    @Test
    fun `faecher_json wird gelesen, auch unbekannte Fächer`() {
        val f = PaketParser.liesFaecher(
            """[{"id":"deutsch","name":"Deutsch","farbe":"#C0532F","farbeDunkel":"#F0937A","begleiterTyp":"schreiben"},
               {"id":"chemie","name":"Chemie","farbe":"#6A4C93","begleiterTyp":"physik","extra":true}]"""
        )
        assertEquals(2, f.size)
        assertEquals("#6A4C93", f[1].farbeDunkel) // fällt auf farbe zurück
        assertEquals("Ch", f[1].kuerzel)
        assertEquals("D", f[0].kuerzel)
    }
}
