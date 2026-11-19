package de.lernen.core.sync

import de.lernen.core.data.model.PaketQuelle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GitHubApiTest {
    private val server = MockWebServer()
    private val api = GitHubApi(Netz())
    private val q = PaketQuelle("SportGermany", "lernen-pakete", "main")
    private val pfad = "fragen/offen/2026-11-19-1805-a7k2.md"

    @Before fun start() { server.start(); api.basisUrl = server.url("").toString().trimEnd('/') }
    @After fun stop() = server.shutdown()

    @Test fun `Anlegen schickt PUT mit Base64 Commit-Nachricht und Branch`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        api.anlegen(q, "tok", pfad, "# Frage\nÄpfel", "Frage 2026-11-19-1805-a7k2")
        val r = server.takeRequest()
        assertEquals("PUT", r.method)
        assertEquals("/repos/SportGermany/lernen-pakete/contents/$pfad", r.path)
        assertEquals("Bearer tok", r.getHeader("Authorization"))
        val body = r.body.readUtf8()
        assertTrue(body.contains("\"message\":\"Frage 2026-11-19-1805-a7k2\""))
        assertTrue(body.contains("\"branch\":\"main\""))
        assertTrue(body.contains(java.util.Base64.getEncoder().encodeToString("# Frage\nÄpfel".toByteArray())))
    }

    @Test fun `Schreiben ausserhalb von fragen offen wird vor dem Netz abgelehnt`() = runTest {
        for (p in listOf("katalog.json", "pakete/deutsch/a.json", "fragen/offen/../../katalog.json")) {
            try { api.anlegen(q, "tok", p, "x", "x"); fail("nicht abgelehnt: $p") } catch (_: IllegalArgumentException) {}
            try { api.loeschen(q, "tok", p, "sha", "x"); fail("nicht abgelehnt: $p") } catch (_: IllegalArgumentException) {}
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun `sha holen und loeschen`() = runTest {
        server.enqueue(MockResponse().setBody("""{"sha":"abc123","name":"x"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals("abc123", api.sha(q, "tok", pfad))
        api.loeschen(q, "tok", pfad, "abc123", "Frage x gelöscht")
        assertNull(api.sha(q, "tok", pfad))
        server.takeRequest()
        val del = server.takeRequest()
        assertEquals("DELETE", del.method)
        assertTrue(del.body.readUtf8().contains("\"sha\":\"abc123\""))
    }

    @Test fun `Verbindung testen erkennt fehlende Schreibrechte`() = runTest {
        server.enqueue(MockResponse().setBody("""{"permissions":{"push":false}}"""))
        server.enqueue(MockResponse().setBody("""{"permissions":{"push":true}}"""))
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(false, api.testen(q, "t").first)
        assertEquals(true, api.testen(q, "t").first)
        assertTrue(api.testen(q, "t").second.contains("ungültig"))
    }
}
