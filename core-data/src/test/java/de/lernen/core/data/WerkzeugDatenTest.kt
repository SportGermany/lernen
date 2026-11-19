package de.lernen.core.data

import de.lernen.core.data.model.WortGruppe
import de.lernen.core.data.repo.WerkzeugDaten
import org.junit.Assert.assertEquals
import org.junit.Test

class WerkzeugDatenTest {
    @Test fun `gleiche Gruppen werden vereint, Wörter nicht doppelt`() {
        val r = WerkzeugDaten.zusammenfuehren(listOf(
            WortGruppe("Statt „gehen“", listOf("schleichen", "stapfen")),
            WortGruppe("Angst", listOf("Herz hämmert")),
            WortGruppe("Statt „gehen“ ", listOf("stapfen", "huschen")),
        ))
        assertEquals(2, r.size)
        assertEquals(listOf("schleichen", "stapfen", "huschen"), r[0].woerter)
    }
}
