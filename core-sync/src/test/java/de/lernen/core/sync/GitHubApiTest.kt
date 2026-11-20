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

    private val tok = "github_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz"

    @Test fun `Diagnose erkennt fehlendes Schreibrecht beim Fine-grained Token`() = runTest {
        server.enqueue(MockResponse().setBody("""{"login":"SportGermany"}"""))
        server.enqueue(MockResponse().setBody("""{"name":"lernen-pakete"}"""))
        server.enqueue(MockResponse().setBody("""{"name":"main"}"""))
        server.enqueue(MockResponse().setResponseCode(403).setHeader("X-Accepted-GitHub-Permissions", "contents=write")
            .setBody("""{"message":"Resource not accessible by personal access token"}"""))
        val d = api.diagnose(q, tok, "1.0.1")
        assertEquals(false, d.ok)
        assertTrue(d.loesung, d.loesung.contains("Contents") && d.loesung.contains("Read and write"))
        assertTrue(d.bericht.contains("HTTP 403"))
        assertTrue(d.bericht.contains("contents=write"))
        assertTrue("Token darf nie im Bericht stehen", !d.bericht.contains(tok))
    }

    @Test fun `Diagnose erkennt Token eines anderen Kontos`() = runTest {
        server.enqueue(MockResponse().setBody("""{"login":"jemand-anderes"}"""))
        val d = api.diagnose(q, tok)
        assertEquals(false, d.ok)
        assertTrue(d.loesung.contains("SportGermany"))
    }

    @Test fun `Diagnose erkennt abgelaufenen Token und falsches Format`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Bad credentials"}"""))
        assertTrue(api.diagnose(q, tok).loesung.contains("Neuen Token"))
        assertTrue(api.diagnose(q, "abc").loesung.contains("neu kopieren"))
        assertEquals(1, server.requestCount)
    }

    @Test fun `Diagnose erfolgreich schreibt und loescht eine Testdatei`() = runTest {
        server.enqueue(MockResponse().setBody("""{"login":"SportGermany"}"""))
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        server.enqueue(MockResponse().setBody("""{"sha":"s1"}"""))
        server.enqueue(MockResponse().setBody("{}"))
        val d = api.diagnose(q, tok)
        assertTrue(d.bericht, d.ok)
        repeat(3) { server.takeRequest() }
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertTrue(put.path!!.matches(Regex("/repos/SportGermany/lernen-pakete/contents/fragen/offen/\\d{4}-\\d{2}-\\d{2}-\\d{4}-test\\.md")))
        server.takeRequest()
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test fun `Fehlermeldung beim Hochladen enthaelt GitHub-Meldung`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Resource not accessible by personal access token"}"""))
        val e = runCatching { api.anlegen(q, tok, pfad, "x", "x") }.exceptionOrNull() as NetzFehler
        assertTrue(e.alsMeldung().contains("Resource not accessible"))
    }
}
