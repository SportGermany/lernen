package de.lernen.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {
    private fun w(t: String, vararg b: Pair<String, Double>) = Parser.term(t).wert(mapOf(*b))

    @Test fun grundrechnen() {
        assertEquals(7.0, w("1 + 2 · 3"), 1e-12)
        assertEquals(2.5, w("5 : 2"), 1e-12)
        assertEquals(2.5, w("2,5"), 1e-12)
        assertEquals(-1.0, w("−3 + 2"), 1e-12)
        assertEquals(81.0, w("3^4"), 1e-12)
        assertEquals(-9.0, w("-3²"), 1e-12) // wie in der Schule: −(3²)
        assertEquals(9.0, w("(-3)²"), 1e-12)
        assertEquals(2.0, w("2^3^0"), 1e-12) // rechtsassoziativ: 2^(3^0)
    }

    @Test fun weggelassenerMalpunkt() {
        assertEquals(10.0, w("2x", "x" to 5.0), 1e-12)
        assertEquals(18.0, w("3(x+1)", "x" to 5.0), 1e-12)
        assertEquals(21.0, w("(a+b)(a−b)", "a" to 5.0, "b" to 2.0), 1e-12)
        assertEquals(30.0, w("2xy", "x" to 3.0, "y" to 5.0), 1e-12)
        assertEquals(50.0, w("2x²", "x" to 5.0), 1e-12)
    }

    @Test fun funktionenUndKonstanten() {
        assertEquals(4.0, w("√16"), 1e-12)
        assertEquals(5.0, w("√(9+16)"), 1e-12)
        assertEquals(Math.PI * 4, w("π r²", "r" to 2.0), 1e-12)
        assertEquals(0.5, Parser.term("sin(30)").wert(emptyMap(), Winkel.GRAD), 1e-12)
        assertEquals(2.0, w("log(100)"), 1e-12)
        assertEquals(-2.0, w("(-8)^(1/3)"), 1e-12)
    }

    @Test fun fehler() {
        for (t in listOf("2+", "(1+2", "3 = = 4", "x # 2")) {
            assertTrue(t, runCatching { Parser.eingabe(t) }.isFailure)
        }
    }
}

class AequivalenzTest {
    private fun g(t: String) = Parser.gleichung(t)
    private fun t(t: String) = Parser.term(t)

    @Test fun terme() {
        assertTrue(Aequivalenz.termeGleich(t("(a+b)²"), t("a² + 2ab + b²")))
        assertFalse(Aequivalenz.termeGleich(t("(a+b)²"), t("a² + b²")))
        assertTrue(Aequivalenz.termeGleich(t("3(x−2) − (x+4)"), t("2x − 10")))
        assertFalse(Aequivalenz.termeGleich(t("3(x−2) − (x+4)"), t("2x − 2")))
    }

    @Test fun gleichungenUmformen() {
        val e = Aequivalenz.Ergebnis.GLEICHWERTIG
        assertEquals(e, Aequivalenz.gleichungenGleich(g("3x + 4x − 2 = 12"), g("7x − 2 = 12")))
        assertEquals(e, Aequivalenz.gleichungenGleich(g("7x − 2 = 12"), g("7x = 14")))
        assertEquals(e, Aequivalenz.gleichungenGleich(g("7x = 14"), g("x = 2")))
        assertEquals(e, Aequivalenz.gleichungenGleich(g("x/2 + 3 = 5"), g("x + 6 = 10")))
        assertEquals(Aequivalenz.Ergebnis.NICHT_GLEICHWERTIG, Aequivalenz.gleichungenGleich(g("7x − 2 = 12"), g("7x = 10")))
    }

    @Test fun quadratischeGleichungen() {
        assertEquals(Aequivalenz.Ergebnis.GLEICHWERTIG, Aequivalenz.gleichungenGleich(g("x² − 5x + 6 = 0"), g("(x−2)(x−3) = 0")))
        assertEquals(Aequivalenz.Ergebnis.LOESUNG_VERLOREN, Aequivalenz.gleichungenGleich(g("x² = 9"), g("x = 3")))
        assertEquals(Aequivalenz.Ergebnis.GLEICHWERTIG, Aequivalenz.gleichungenGleich(g("(x−2)² = 0"), g("x = 2")))
    }

    @Test fun nullstellen() {
        assertEquals(listOf(-3.0, 3.0), Loeser.nullstellen(g("x² = 9"), "x"))
        assertEquals(listOf(2.0), Loeser.nullstellen(g("(x−2)² = 0"), "x"))
        assertEquals(emptyList<Double>(), Loeser.nullstellen(g("x² = −1"), "x"))
        assertEquals(listOf(0.5), Loeser.nullstellen(g("4x − 2 = 0"), "x"))
        assertNull(Loeser.nullstellen(g("2(x+1) = 2x + 2"), "x"))
    }

    @Test fun lgs() {
        assertEquals(mapOf("x" to 2.0, "y" to 3.0), Loeser.lgs(listOf(g("y = 2x − 1"), g("3x + 2y = 12"))))
        assertEquals(mapOf("x" to 5.0, "y" to 5.0), Loeser.lgs(listOf(g("x + y = 10"), g("2x − y = 5"))))
        assertNull(Loeser.lgs(listOf(g("x + y = 1"), g("2x + 2y = 2"))))
        assertNull(Loeser.lgs(listOf(g("x² + y = 1"), g("x − y = 2"))))
    }
}

class MitrechnenTest {
    @Test fun einsetzungsverfahren() {
        val m = Mitrechnen(listOf("y = 2x − 1", "3x + 2y = 12"))
        assertTrue(m.pruefe("3x + 2(2x − 1) = 12").ok)
        assertTrue(m.pruefe("3x + 4x − 2 = 12").ok)
        assertFalse(m.pruefe("3x + 4x − 1 = 12").ok)
        assertTrue(m.pruefe("7x = 14").ok)
        val x = m.pruefe("x = 2")
        assertTrue(x.ok); assertTrue(x.meldung.contains("y"))
        val y = m.pruefe("y = 3")
        assertTrue(y.ok); assertTrue(y.meldung.contains("Alle Unbekannten"))
        assertFalse(m.pruefe("y = 4").ok)
    }

    @Test fun gleichungSchrittFuerSchritt() {
        val m = Mitrechnen(listOf("5(x − 1) = 2x + 7"))
        assertTrue(m.pruefe("5x − 5 = 2x + 7").ok)
        assertFalse(m.pruefe("3x = 2").ok)
        assertTrue(m.pruefe("3x = 12").ok)
        val f = m.pruefe("x = 4")
        assertTrue(f.ok); assertTrue(f.fertig)
    }

    @Test fun termUmformen() {
        val m = Mitrechnen(listOf("(x + 3)²"))
        assertTrue(m.pruefe("x² + 6x + 9").ok)
        assertFalse(m.pruefe("x² + 9").ok)
    }

    @Test fun gleichungsnummern() {
        val m = Mitrechnen(listOf("(I) y = 3x − 2", "(II) 2x + y = 8"))
        assertTrue(m.pruefe("2x + (3x − 2) = 8").ok)
        assertTrue(m.pruefe("(II) 5x = 10").ok)
    }

    @Test fun unleserlich() {
        val m = Mitrechnen(listOf("2x = 4"))
        assertFalse(m.pruefe("2x = ").ok)
    }

    @Test fun zahlen() {
        assertEquals("8,7", Zahlen.text(8.7))
        assertEquals("2", Zahlen.text(2.0000000000001))
        assertEquals("−3,5", Zahlen.text(-3.5))
        assertEquals("0,333333", Zahlen.text(1.0 / 3))
    }
}
