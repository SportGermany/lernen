package de.lernen.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class HintergrundTest {
    @Test fun `Erinnerung heute oder morgen`() {
        val jetzt = LocalDateTime.of(2026, 11, 19, 18, 0)
        assertEquals(Duration.ofHours(1), Hintergrund.verzoegerungBis(19 * 60, jetzt))
        assertEquals(Duration.ofHours(23), Hintergrund.verzoegerungBis(17 * 60, jetzt))
        assertEquals(Duration.ofHours(24), Hintergrund.verzoegerungBis(18 * 60, jetzt))
    }

    @Test fun `Release lesen`() {
        val r = AppUpdate.liesRelease(
            """{"tag_name":"v0.4.0","body":"Neu","assets":[{"name":"notes.txt","browser_download_url":"x"},{"name":"lernen-0.4.0.apk","browser_download_url":"https://github.com/a.apk"}]}"""
        )!!
        assertEquals("0.4.0", r.version)
        assertEquals("https://github.com/a.apk", r.apkUrl)
        assertNull(AppUpdate.liesRelease("""{"tag_name":"v1.0.0","assets":[]}"""))
    }

    @Test fun `Versionsdatei im Repo lesen`() {
        val v = AppUpdate.liesVersionsdatei("""{"version":"1.0.0","apk":"update/lernen-1.0.0.apk","notizen":"Erste Version"}""")!!
        assertEquals("1.0.0", v.version)
        assertEquals("https://raw.githubusercontent.com/SportGermany/lernen/main/update/lernen-1.0.0.apk", v.apkUrl)
        assertNull(AppUpdate.liesVersionsdatei("""{"version":"1.0.0"}"""))
    }
}
