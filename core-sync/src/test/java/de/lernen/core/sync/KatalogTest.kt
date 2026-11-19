package de.lernen.core.sync

import de.lernen.core.data.model.PaketQuelle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class KatalogTest {
    private val text = """
        {"format":"lernen-katalog/1","pakete":[
          {"id":"deutsch.a","typ":"thema","fach":"deutsch","titel":"A","version":"1.0","pfad":"pakete/deutsch/deutsch-a.json","groesse":10,"geaendert":"2026-11-19T10:00:00Z","neu":1},
          {"id":"deutsch.b","typ":"thema","fach":"deutsch","titel":"B","version":"1.2","pfad":"pakete/deutsch/deutsch-b.json"},
          {"id":"mathe.c","typ":"thema","fach":"mathe","titel":"C","version":"2.0","pfad":"pakete/mathe/mathe-c.json"}
        ]}
    """.trimIndent()

    @Test fun `Katalog wird gelesen, unbekannte Felder stören nicht`() {
        val k = KatalogRepository.liesKatalog(text)!!
        assertEquals(3, k.pakete.size)
        assertEquals("pakete/deutsch/deutsch-a.json", k.pakete[0].pfad)
    }

    @Test fun `kaputter Katalog gibt null`() = assertNull(KatalogRepository.liesKatalog("{"))

    @Test fun `Status Hinzufügen, hinzugefügt und Update`() {
        val k = KatalogRepository.liesKatalog(text)!!
        val z = KatalogRepository.zeilenAus(k, mapOf("deutsch.a" to "1.0", "deutsch.b" to "1.1"))
        assertEquals(listOf(PaketStatus.HINZUGEFUEGT, PaketStatus.UPDATE, PaketStatus.NEU), z.map { it.status })
        assertEquals("1.1", z[1].installierteVersion)
    }

    @Test fun `neuere installierte Version ist kein Update`() {
        val k = KatalogRepository.liesKatalog(text)!!
        assertEquals(PaketStatus.HINZUGEFUEGT, KatalogRepository.zeilenAus(k, mapOf("mathe.c" to "2.1"))[2].status)
    }

    @Test fun `raw-URL der Paket-Quelle`() {
        assertEquals(
            "https://raw.githubusercontent.com/SportGermany/lernen-pakete/main/pakete/x.json",
            PaketQuelle().rawUrl("/pakete/x.json"),
        )
    }

    @Test fun `echter Katalog aus dem Paket-Repo ist lesbar`() {
        val f = File("../../lernen-pakete/katalog.json")
        if (!f.exists()) return
        val k = KatalogRepository.liesKatalog(f.readText())!!
        assert(k.pakete.isNotEmpty())
    }
}
