package de.lernen.core.units

import de.lernen.core.units.Einheiten.Bewertung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EinheitenTest {
    private fun um(w: Double, a: String, b: String) = Einheiten.umrechnen(w, a, b)

    @Test fun umrechnen() {
        assertEquals(3.6, um(1.0, "kWh", "MJ"), 1e-9)
        assertEquals(36.0, um(10.0, "m/s", "km/h"), 1e-9)
        assertEquals(1000.0, um(1.0, "m³", "l"), 1e-9)
        assertEquals(10000.0, um(1.0, "m²", "cm²"), 1e-9)
        assertEquals(0.25, um(250.0, "mA", "A"), 1e-12)
        assertEquals(1013.0, um(1.013, "bar", "hPa"), 1e-9)
        assertEquals(293.15, um(20.0, "°C", "K"), 1e-9)
        assertEquals(68.0, um(20.0, "°C", "°F"), 1e-9)
        assertEquals(2.0, um(2000.0, "W", "kW"), 1e-12)
        assertEquals(1.0, um(1.0, "N·m", "J"), 1e-12)
        assertEquals(1000.0, um(1.0, "g/cm³", "kg/m³"), 1e-9)
    }

    @Test fun grosseLesen() {
        val g = Einheiten.groesse("8,7 A")
        assertEquals(8.7, g.wert, 1e-12); assertEquals("A", g.einheit!!.zeichen)
        assertEquals(3e5, Einheiten.groesse("3·10^5 Pa").wert, 1e-6)
        assertEquals(null, Einheiten.groesse("12").einheit)
        assertTrue(runCatching { Einheiten.groesse("8 Äpfel") }.isFailure)
    }

    @Test fun ergebnisPruefen() {
        // I = P : U = 2000 W : 230 V ≈ 8,7 A
        assertEquals(Bewertung.RICHTIG, Einheiten.pruefe("8,7 A", 8.7, "A").bewertung)
        assertEquals(Bewertung.RICHTIG, Einheiten.pruefe("8696 mA", 8.7, "A").bewertung)
        assertEquals(Bewertung.OHNE_EINHEIT, Einheiten.pruefe("8,7", 8.7, "A").bewertung)
        assertEquals(Bewertung.FALSCHE_EINHEIT, Einheiten.pruefe("8,7 V", 8.7, "A").bewertung)
        assertEquals(Bewertung.FALSCHE_ZAHL, Einheiten.pruefe("0,115 A", 8.7, "A").bewertung)
        assertEquals(Bewertung.UNLESBAR, Einheiten.pruefe("acht", 8.7, "A").bewertung)
        assertTrue(Einheiten.pruefe("8,7", 8.7, "A").meldung.startsWith("Halb richtig"))
    }

    @Test fun alleKategorienLesbar() {
        Einheiten.KATEGORIEN.forEach { (_, l) -> l.zipWithNext().forEach { (a, b) -> um(1.0, a, b) } }
    }
}
