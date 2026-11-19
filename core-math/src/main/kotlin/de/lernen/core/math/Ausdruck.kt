package de.lernen.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Winkelmaß für sin/cos/tan (Taschenrechner: Grad wie in der Schule). */
enum class Winkel { GRAD, BOGEN }

/** Ausdrucksbaum eines Terms. */
sealed interface Ausdruck {
    fun wert(belegung: Map<String, Double>, winkel: Winkel = Winkel.GRAD): Double
    fun variablen(): Set<String>

    data class Zahl(val z: Double) : Ausdruck {
        override fun wert(belegung: Map<String, Double>, winkel: Winkel) = z
        override fun variablen() = emptySet<String>()
    }

    data class Variable(val name: String) : Ausdruck {
        override fun wert(belegung: Map<String, Double>, winkel: Winkel) =
            belegung[name] ?: throw RechenFehler("Unbekannte Größe: $name")
        override fun variablen() = setOf(name)
    }

    data class Minus(val a: Ausdruck) : Ausdruck {
        override fun wert(belegung: Map<String, Double>, winkel: Winkel) = -a.wert(belegung, winkel)
        override fun variablen() = a.variablen()
    }

    data class Rechnung(val op: Char, val a: Ausdruck, val b: Ausdruck) : Ausdruck {
        override fun wert(belegung: Map<String, Double>, winkel: Winkel): Double {
            val x = a.wert(belegung, winkel); val y = b.wert(belegung, winkel)
            return when (op) {
                '+' -> x + y
                '-' -> x - y
                '*' -> x * y
                '/' -> x / y
                '^' -> potenz(x, y)
                else -> throw RechenFehler("Operator $op")
            }
        }
        override fun variablen() = a.variablen() + b.variablen()
    }

    data class Funktion(val name: String, val arg: Ausdruck) : Ausdruck {
        override fun wert(belegung: Map<String, Double>, winkel: Winkel): Double {
            val x = arg.wert(belegung, winkel)
            val r = if (winkel == Winkel.GRAD) PI / 180 else 1.0
            return when (name) {
                "sqrt" -> sqrt(x)
                "sin" -> sin(x * r)
                "cos" -> cos(x * r)
                "tan" -> tan(x * r)
                "asin" -> asin(x) / r
                "acos" -> acos(x) / r
                "atan" -> atan(x) / r
                "ln" -> ln(x)
                "log" -> log10(x)
                "abs" -> abs(x)
                "exp" -> exp(x)
                else -> throw RechenFehler("Unbekannte Funktion $name")
            }
        }
        override fun variablen() = arg.variablen()
    }

    companion object {
        /** Potenz; negative Basis mit ungeradem Kehrwert-Exponenten (z. B. ∛) wie in der Schule. */
        fun potenz(x: Double, y: Double): Double {
            if (x < 0 && y != Math.rint(y)) {
                val k = 1 / y
                if (abs(k - Math.rint(k)) < 1e-12 && Math.rint(k).toLong() % 2 != 0L) return -(-x).pow(y)
            }
            return x.pow(y)
        }
    }
}

class RechenFehler(meldung: String) : IllegalArgumentException(meldung)

/** Eine Gleichung links = rechts. */
data class Gleichung(val links: Ausdruck, val rechts: Ausdruck) {
    fun variablen() = links.variablen() + rechts.variablen()
    /** f = links − rechts (Gleichung ⇔ f = 0) */
    fun differenz(b: Map<String, Double>) = links.wert(b) - rechts.wert(b)
}

/** Eingabe: entweder Term oder Gleichung. */
sealed interface Eingabe {
    data class Term(val a: Ausdruck) : Eingabe
    data class Gl(val g: Gleichung) : Eingabe
}
