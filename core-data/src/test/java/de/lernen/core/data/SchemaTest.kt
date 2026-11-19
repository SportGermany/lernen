package de.lernen.core.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prüft, dass schema/paket.schema.json (wird auch von der GitHub Action benutzt) zu den Paketen passt. */
class SchemaTest {
    private val mapper = ObjectMapper()
    private val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        .getSchema(File("../schema/paket.schema.json").readText())

    private fun fehler(text: String) = schema.validate(mapper.readTree(text))

    @Test fun `Startpakete erfüllen das Schema`() {
        TestDaten.startpakete.forEach { (name, text) ->
            assertEquals("$name: ${fehler(text)}", 0, fehler(text).size)
        }
    }

    @Test fun `Mathe- und Physik-Paket erfüllen das Schema`() {
        for (n in listOf("mathe-lgs-einsetzungsverfahren.json", "physik-elektrische-leistung.json")) {
            assertEquals(n, emptySet<Any>(), fehler(TestDaten.ressource(n)))
        }
    }

    @Test fun `Mini-Paket erfüllt das Schema`() = assertEquals(emptySet<Any>(), fehler(TestDaten.MINI))

    @Test fun `Schema erkennt Fehler`() {
        assertTrue(fehler(TestDaten.MINI.replace("lernen-paket/2", "x")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"typ\": \"thema\"", "\"typ\": \"arbeitsheft\"")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<i>x</i>\"")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<span class=\\\"rot\\\">x</span>\"")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"<p style=\\\"a\\\">x</p>\"")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"muss\": [\"drei\"]", "\"muss\": [\"drei\"], \"tippfehler\": 1")).isNotEmpty())
        assertTrue(fehler(TestDaten.MINI.replace("\"richtig\": 0,", "")).isNotEmpty())
    }

    @Test fun `erlaubtes HTML besteht`() {
        val ok = "<p><b>T</b></p><p>a <span class=\\\"ms\\\">b</span><br>c <span class=\\\"tag\\\">T</span></p>"
        assertEquals(emptySet<Any>(), fehler(TestDaten.MINI.replace("\"aufgabe\": \"Text\"", "\"aufgabe\": \"$ok\"")))
    }
}
