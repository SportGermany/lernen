package de.lernen.core.data

import de.lernen.core.data.fragen.Antwortart
import de.lernen.core.data.fragen.Frage
import de.lernen.core.data.fragen.FrageFormat
import de.lernen.core.data.fragen.FrageKontext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class FrageFormatTest {
    private val zeit = ZonedDateTime.of(2026, 11, 19, 18, 5, 0, 0, ZoneId.of("Europe/Berlin"))
    private val frage = Frage(
        "2026-11-19-1805-a7k2", zeit,
        "Ich weiß nicht, wie ich die zwei „Ich höre“-Sätze umbauen soll, ohne dass es komisch klingt.",
        Antwortart.NUR_TIPP,
        FrageKontext(
            fachId = "deutsch", fachName = "Deutsch", paketId = "deutsch.ah-s31-klassensprecherfahrt",
            paketTitel = "Schilderung: Abend am See (AH S. 31)", paketPfad = "pakete/deutsch/deutsch-arbeitsheft-s31-klassensprecherfahrt.json",
            schrittId = "c1", schrittTitel = "1c · Schwächen rot markieren", schrittZiel = "Du findest die drei Fehlerarten aus der Aufgabe …",
            zeilen = listOf("Z. 4–5", "Z. 9–10", "Z. 11"), angezeigteTipps = 2,
        ),
    )

    @Test fun `id hat das Format JJJJ-MM-TT-HHMM-xxxx`() {
        val id = FrageFormat.neueId(zeit, Random(1))
        assertTrue(id, Regex("^2026-11-19-1805-[a-z0-9]{4}$").matches(id))
        assertEquals("fragen/offen/$id.md", FrageFormat.pfad(id))
    }

    @Test fun `Pfadschutz lässt nur fragen offen zu`() {
        FrageFormat.pruefePfad("fragen/offen/2026-11-19-1805-a7k2.md")
        listOf(
            "pakete/deutsch/x.json", "katalog.json", "fragen/offen/../../katalog.json", "fragen/offen/x.md",
            "fragen/offen/2026-11-19-1805-a7k2.md/../x", "/fragen/offen/2026-11-19-1805-a7k2.md", "fragen/offen/2026-11-19-1805-A7K2.md",
        ).forEach { p -> assertThrows(p, IllegalArgumentException::class.java) { FrageFormat.pruefePfad(p) } }
    }

    @Test fun `Markdown enthält Kopf, Frage, Kontext und Regeln`() {
        val md = FrageFormat.markdown(frage, "https://raw.githubusercontent.com/SportGermany/lernen-pakete/main/pakete/deutsch/x.json")
        assertTrue(md.startsWith("---\nid: 2026-11-19-1805-a7k2\nerstellt: 2026-11-19T18:05:00+01:00\nloeschen_ab: 2026-11-20T18:05:00+01:00\n"))
        assertTrue(md.contains("schritt: c1 – 1c · Schwächen rot markieren"))
        assertTrue(md.contains("antwortart: nur-tipp"))
        assertTrue(md.contains("# Frage\nIch weiß nicht"))
        assertTrue(md.contains("- Schon angezeigte Tipps: 1, 2"))
        assertTrue(md.contains("- Zeilen im Heft: Z. 4–5, Z. 9–10, Z. 11"))
        assertTrue(md.contains("**Antwortart: Nur Tipp**\nVerrate keine Lösung."))
        assertTrue(md.contains("bitte den Schüler, ein Foto"))
    }

    @Test fun `Befehl mit Link und Ersatzteil`() {
        val b = FrageFormat.befehl(frage, "https://raw.githubusercontent.com/SportGermany/lernen-pakete/main/fragen/offen/2026-11-19-1805-a7k2.md")
        val z = b.lines()
        assertEquals("Lies diese Frage aus meiner Lern-App und antworte genau nach den Regeln, die in der Datei stehen:", z[0])
        assertTrue(z[1].endsWith("/fragen/offen/2026-11-19-1805-a7k2.md"))
        assertEquals("Fach: Deutsch · Thema: Schilderung: Abend am See (AH S. 31) · Schritt: 1c · Antwortart: Nur Tipp", z[4])
        assertTrue(z[5].startsWith("„Ich weiß nicht"))
        assertTrue(z[6].startsWith("Regel: Verrate keine Lösung."))
    }

    @Test fun `Frage ohne Thema`() {
        val f = frage.copy(kontext = FrageKontext())
        val md = FrageFormat.markdown(f, null)
        assertFalse(md.contains("paket_url"))
        assertTrue(md.contains("Allgemeine Frage"))
        assertTrue(FrageFormat.befehl(f, "u").contains("Antwortart: Nur Tipp\n"))
    }

    @Test fun `Zeilen aus Tipps`() {
        val z = FrageFormat.zeilenAus(listOf("Umgangssprache: In Z. 4–5 stecken zwei Stellen, in Z. 9–10 drei.", "Lies Z. 11 laut.", "nichts"))
        assertEquals(listOf("Z. 4–5", "Z. 9–10", "Z. 11"), z)
    }

    @Test fun `Antwortarten haben die Bausteine aus dem Auftrag`() {
        assertEquals(4, Antwortart.entries.size)
        assertTrue(Antwortart.ERKLAEREN.baustein.endsWith("Löse nicht die Aufgabe des Schülers."))
        assertEquals(Antwortart.BEISPIEL, Antwortart.von("beispiel-zeigen"))
    }
}
