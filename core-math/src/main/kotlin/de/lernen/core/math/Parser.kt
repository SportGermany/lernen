package de.lernen.core.math

/**
 * Liest Schulschreibweise:
 * Zahlen mit Komma oder Punkt, Variablen (einzelne Buchstaben), + − · × * : ÷ /, ^, ², ³, √, π/pi,
 * Klammern ( ) [ ], Funktionen (sqrt, sin, cos, tan, ln, log, abs …) und weggelassene Malpunkte (2x, 3(x+1), (a+b)(a−b)).
 */
object Parser {

    private val FUNKTIONEN = setOf("sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "abs", "exp")

    fun eingabe(text: String): Eingabe {
        val t = normalisiere(text)
        val teile = t.split('=')
        return when (teile.size) {
            1 -> Eingabe.Term(term(teile[0]))
            2 -> {
                if (teile[0].isBlank() || teile[1].isBlank()) throw RechenFehler("Links und rechts vom = muss etwas stehen.")
                Eingabe.Gl(Gleichung(term(teile[0]), term(teile[1])))
            }
            else -> throw RechenFehler("Bitte nur ein = pro Schritt.")
        }
    }

    fun gleichung(text: String): Gleichung = (eingabe(text) as? Eingabe.Gl)?.g ?: throw RechenFehler("Hier fehlt das =.")

    fun term(text: String): Ausdruck {
        val p = Leser(tokens(normalisiere(text)))
        val a = p.summe()
        if (!p.ende()) throw RechenFehler("Unerwartetes Zeichen: ${p.aktuell()}")
        return a
    }

    internal fun normalisiere(s: String): String = s
        .replace('−', '-').replace('–', '-').replace('·', '*').replace('×', '*').replace('⋅', '*')
        .replace('÷', '/').replace(':', '/').replace('[', '(').replace(']', ')')
        .replace("²", "^2").replace("³", "^3")
        .replace(Regex("(\\d),(\\d)"), "$1.$2")

    private sealed interface Tok {
        data class Zahl(val z: Double) : Tok
        data class Name(val n: String) : Tok
        data class Op(val c: Char) : Tok
    }

    private fun tokens(s: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < s.length && s[i + 1].isDigit()) -> {
                    val st = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    out += Tok.Zahl(s.substring(st, i).toDoubleOrNull() ?: throw RechenFehler("Zahl unklar: ${s.substring(st, i)}"))
                }
                c == 'π' -> { out += Tok.Name("pi"); i++ }
                c == '√' -> { out += Tok.Name("sqrt"); i++ }
                c.isLetter() -> {
                    val st = i
                    while (i < s.length && s[i].isLetter()) i++
                    out += namen(s.substring(st, i))
                }
                c in "+-*/^()" -> { out += Tok.Op(c); i++ }
                else -> throw RechenFehler("Zeichen nicht erlaubt: $c")
            }
        }
        return out
    }

    /** "sinx" → sin x; "pi" → π; "xy" → x·y (einzelne Buchstaben sind Variablen). */
    private fun namen(w: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var rest = w
        while (rest.isNotEmpty()) {
            val f = (FUNKTIONEN + "pi" + "wurzel").filter { rest.lowercase().startsWith(it) }.maxByOrNull { it.length }
            if (f != null) {
                out += Tok.Name(if (f == "wurzel") "sqrt" else f)
                rest = rest.substring(f.length)
            } else {
                out += Tok.Name(rest.substring(0, 1))
                rest = rest.substring(1)
            }
        }
        return out
    }

    private class Leser(val t: List<Tok>) {
        var i = 0
        fun ende() = i >= t.size
        fun aktuell(): String = when (val x = t.getOrNull(i)) { is Tok.Op -> x.c.toString(); is Tok.Name -> x.n; is Tok.Zahl -> x.z.toString(); null -> "Ende" }
        fun istOp(c: Char) = (t.getOrNull(i) as? Tok.Op)?.c == c

        fun summe(): Ausdruck {
            var a = produkt()
            while (istOp('+') || istOp('-')) {
                val op = (t[i++] as Tok.Op).c
                a = Ausdruck.Rechnung(op, a, produkt())
            }
            return a
        }

        fun produkt(): Ausdruck {
            var a = unaer()
            while (true) {
                a = when {
                    istOp('*') || istOp('/') -> { val op = (t[i++] as Tok.Op).c; Ausdruck.Rechnung(op, a, unaer()) }
                    // weggelassener Malpunkt: Zahl, Name oder "(" folgt direkt
                    beginntFaktor() -> Ausdruck.Rechnung('*', a, potenz())
                    else -> return a
                }
            }
        }

        private fun beginntFaktor(): Boolean = when (val x = t.getOrNull(i)) {
            is Tok.Zahl, is Tok.Name -> true
            is Tok.Op -> x.c == '('
            null -> false
        }

        fun unaer(): Ausdruck = when {
            istOp('-') -> { i++; Ausdruck.Minus(unaer()) }
            istOp('+') -> { i++; unaer() }
            else -> potenz()
        }

        fun potenz(): Ausdruck {
            val basis = atom()
            if (istOp('^')) {
                i++
                return Ausdruck.Rechnung('^', basis, unaer2())
            }
            return basis
        }

        /** Exponent: rechtsassoziativ, darf Vorzeichen haben. */
        private fun unaer2(): Ausdruck = when {
            istOp('-') -> { i++; Ausdruck.Minus(unaer2()) }
            else -> potenz()
        }

        fun atom(): Ausdruck {
            val x = t.getOrNull(i) ?: throw RechenFehler("Da fehlt noch etwas.")
            i++
            return when (x) {
                is Tok.Zahl -> Ausdruck.Zahl(x.z)
                is Tok.Name -> when {
                    x.n == "pi" -> Ausdruck.Zahl(Math.PI)
                    x.n in FUNKTIONEN -> Ausdruck.Funktion(x.n, if (istOp('(')) atom() else potenz())
                    else -> Ausdruck.Variable(x.n)
                }
                is Tok.Op -> if (x.c == '(') {
                    val a = summe()
                    if (!istOp(')')) throw RechenFehler("Klammer nicht geschlossen.")
                    i++
                    a
                } else throw RechenFehler("Unerwartetes Zeichen: ${x.c}")
            }
        }
    }
}
