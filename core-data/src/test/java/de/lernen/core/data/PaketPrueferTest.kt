package de.lernen.core.data

import de.lernen.core.data.paket.PaketParser
import de.lernen.core.data.paket.PaketPruefer
import org.junit.Assert.assertTrue
import org.junit.Test

class PaketPrueferTest {

    private fun probleme(text: String) = PaketPruefer.pruefe(PaketParser.json.decodeFromString(de.lernen.core.data.model.Paket.serializer(), text))
    private fun hatFehler(text: String, teil: String) {
        val p = probleme(text).filter { it.schwer }
        assertTrue("Erwartet Fehler mit '$teil', gefunden: $p", p.any { it.toString().contains(teil) })
    }

    @Test fun `gültiges Mini-Paket hat keine Fehler`() {
        assertTrue(probleme(TestDaten.MINI).none { it.schwer })
    }

    @Test fun `falsches Format`() = hatFehler(TestDaten.MINI.replace("lernen-paket/2", "lernen-paket/1"), "format")

    @Test fun `falscher Typ`() = hatFehler(TestDaten.MINI.replace("\"typ\": \"thema\"", "\"typ\": \"quiz\""), "typ")

    @Test fun `id mit Großbuchstaben`() = hatFehler(TestDaten.MINI.replace("deutsch.mini-test", "Deutsch.Mini"), "id")

    @Test fun `Version ungültig`() = hatFehler(TestDaten.MINI.replace("\"1.0\"", "\"v1\""), "version")

    @Test fun `doppelte Schritt-id`() = hatFehler(TestDaten.MINI.replace("\"id\": \"b\"", "\"id\": \"a\""), "doppelt")

    @Test fun `Prüffrage richtig außerhalb`() = hatFehler(TestDaten.MINI.replace("\"richtig\": 1", "\"richtig\": 5"), "außerhalb")

    @Test fun `Auswahl richtig außerhalb`() = hatFehler(TestDaten.MINI.replace("\"richtig\": 0", "\"richtig\": 2"), "außerhalb")

    @Test fun `Arbeitsheft ohne Quelle und Karte`() {
        val t = TestDaten.MINI.replace("\"typ\": \"thema\"", "\"typ\": \"arbeitsheft\"")
        hatFehler(t, "quelle")
        hatFehler(t, "karte")
    }

    @Test fun `unerlaubtes HTML wird gemeldet`() {
        hatFehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<script>x</script>\""), "<script>")
        hatFehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<span class=\\\"rot\\\">x</span>\""), "rot")
        hatFehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<p style=\\\"color:red\\\">x</p>\""), "style")
    }

    @Test fun `vorher ohne nachher`() =
        hatFehler(TestDaten.MINI.replace("\"muss\": [\"drei\"]", "\"muss\": [\"drei\"], \"vorher\": \"x\""), "vorher und nachher")

    @Test fun `Tippen ohne richtigen Token`() = hatFehler(
        TestDaten.MINI.replace(
            "\"einstieg\": [",
            "\"einstieg\": [ {\"typ\":\"tippen\",\"frage\":\"?\",\"tokens\":[[\"a\",0]],\"warum\":\"\"},"
        ), "kein Token"
    )
}
