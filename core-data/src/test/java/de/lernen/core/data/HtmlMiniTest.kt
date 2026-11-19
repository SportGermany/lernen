package de.lernen.core.data

import de.lernen.core.data.paket.HtmlMini
import de.lernen.core.data.paket.HtmlMini.Markierung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlMiniTest {

    @Test fun `einfacher Text ist ein Absatz`() {
        val a = HtmlMini.lies("Nur Text.")
        assertEquals(1, a.size)
        assertEquals("Nur Text.", a[0].klartext)
    }

    @Test fun `Absätze, fett und Markierungen`() {
        val a = HtmlMini.lies("<p><b>Titel</b></p>\n<p>Vor <span class=\"ms\">Licht</span> und <span class=\"mg\">Angst</span>.</p>")
        assertEquals(2, a.size)
        assertTrue(a[0].stuecke.single().fett)
        val st = a[1].stuecke
        assertEquals("Vor Licht und Angst.", a[1].klartext)
        assertEquals(Markierung.SINNE, st.first { it.text == "Licht" }.markierung)
        assertEquals(Markierung.GEFUEHL, st.first { it.text == "Angst" }.markierung)
        assertEquals(null, st.first().markierung)
    }

    @Test fun `Zeilenumbruch und Entities`() {
        val a = HtmlMini.lies("1 · Eins<br>2 · <b>Zwei</b> &amp; drei")
        assertEquals("1 · Eins\n2 · Zwei & drei", a[0].klartext)
        assertTrue(a[0].stuecke.first { it.text == "Zwei" }.fett)
    }

    @Test fun `Korrekturzeichen-Tag`() {
        val a = HtmlMini.lies("Dann <span class=\"tag\">W</span> bin")
        assertEquals(Markierung.KORREKTURZEICHEN, a[0].stuecke[1].markierung)
    }

    @Test fun `unerlaubte Tags werden erkannt`() {
        assertEquals(emptyList<String>(), HtmlMini.unerlaubteTags("<p><b>a</b><br><br/><span class=\"mz\">x</span></p>"))
        assertEquals(listOf("<script>"), HtmlMini.unerlaubteTags("<script>x</script>"))
        assertEquals(listOf("<p style=\"x\">"), HtmlMini.unerlaubteTags("<p style=\"x\">a</p>"))
        assertEquals(listOf("<span class=\"rot\">"), HtmlMini.unerlaubteTags("<span class=\"rot\">a</span>"))
    }

    @Test fun `alle Texte der Startpakete sind erlaubt`() {
        TestDaten.startpakete.values.forEach { t ->
            Regex("\"((?:[^\"\\\\]|\\\\.)*<(?:[^\"\\\\]|\\\\.)*)\"").findAll(t).forEach { m ->
                val html = m.groupValues[1].replace("\\\"", "\"")
                assertEquals(html, emptyList<String>(), HtmlMini.unerlaubteTags(html))
            }
        }
    }
}
