package de.lernen.core.data.paket

/**
 * Winziger HTML-Leser für Pakettexte. Erlaubt sind nur:
 * `<p>`, `<b>`, `<br>` und `<span class="ms|mg|mw|mz|tag">`.
 * Ergebnis ist ein einfaches Modell, das core-design in einen AnnotatedString umwandelt (kein WebView).
 */
object HtmlMini {

    /** Markierungen im Text. */
    enum class Markierung(val klasse: String, val bedeutung: String) {
        SINNE("ms", "Sinne"),
        GEFUEHL("mg", "Gefühl/Gedanken"),
        WORTWAHL("mw", "Wortwahl/Bilder"),
        SATZBAU("mz", "Satzbau/Zeitlupe"),
        KORREKTURZEICHEN("tag", "Korrekturzeichen");

        companion object {
            fun von(klasse: String?) = entries.firstOrNull { it.klasse == klasse }
            val TEXTMARKEN = listOf(SINNE, GEFUEHL, WORTWAHL, SATZBAU)
        }
    }

    data class Stueck(val text: String, val fett: Boolean = false, val markierung: Markierung? = null)

    /** Ein Absatz besteht aus Stücken; Zeilenumbrüche (`<br>`) stehen als "\n" im Text. */
    data class Absatz(val stuecke: List<Stueck>) {
        val klartext: String get() = stuecke.joinToString("") { it.text }
    }

    private val TAG = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
    private val KLASSE = Regex("class\\s*=\\s*\"([^\"]*)\"")
    private val ERLAUBT = setOf("p", "b", "br", "span")

    /** Liefert unerlaubte Tags/Klassen/Attribute (für die Paketprüfung). */
    fun unerlaubteTags(html: String): List<String> {
        val out = mutableListOf<String>()
        for (m in TAG.findAll(html)) {
            val name = m.groupValues[2].lowercase()
            val attr = m.groupValues[3].trim().removeSuffix("/").trim()
            if (name !in ERLAUBT) { out += "<$name>"; continue }
            if (name == "span" && m.groupValues[1].isEmpty()) {
                val k = KLASSE.find(attr)?.groupValues?.get(1)
                if (Markierung.von(k) == null) out += "<span class=\"$k\">"
                if (attr.replace(KLASSE, "").isNotBlank()) out += "<span $attr>"
            } else if (attr.isNotEmpty()) out += "<$name $attr>"
        }
        return out.distinct()
    }

    fun lies(html: String): List<Absatz> {
        val absaetze = mutableListOf<Absatz>()
        var aktuell = mutableListOf<Stueck>()
        var fett = 0
        val spans = ArrayDeque<Markierung?>()

        fun absatzEnde() {
            val bereinigt = trimme(aktuell)
            if (bereinigt.isNotEmpty()) absaetze += Absatz(bereinigt)
            aktuell = mutableListOf()
        }
        fun text(t: String) {
            if (t.isEmpty()) return
            val sauber = entities(t.replace(Regex("[\\r\\n\\t ]+"), " "))
            if (sauber.isEmpty()) return
            val mark = spans.lastOrNull { it != null }
            aktuell += Stueck(sauber, fett > 0, mark)
        }

        var pos = 0
        for (m in TAG.findAll(html)) {
            text(html.substring(pos, m.range.first))
            pos = m.range.last + 1
            val zu = m.groupValues[1] == "/"
            when (m.groupValues[2].lowercase()) {
                "p" -> absatzEnde()
                "br" -> aktuell += Stueck("\n", fett > 0, null)
                "b", "strong" -> if (zu) fett = (fett - 1).coerceAtLeast(0) else fett++
                "span" -> if (zu) spans.removeLastOrNull() else spans.addLast(Markierung.von(KLASSE.find(m.groupValues[3])?.groupValues?.get(1)))
                else -> Unit // unbekannte Tags werden ignoriert, Text bleibt
            }
        }
        text(html.substring(pos))
        absatzEnde()
        return absaetze
    }

    /** Text ohne Tags (z. B. für Suche oder Teilen). */
    fun klartext(html: String): String = lies(html).joinToString("\n\n") { it.klartext }

    private fun trimme(st: List<Stueck>): List<Stueck> {
        if (st.isEmpty()) return st
        val l = st.toMutableList()
        while (l.isNotEmpty() && l.first().text.isBlank() && l.first().text != "\n") l.removeAt(0)
        while (l.isNotEmpty() && l.last().text.isBlank()) l.removeAt(l.size - 1)
        if (l.isEmpty()) return l
        l[0] = l[0].copy(text = l[0].text.trimStart(' '))
        l[l.size - 1] = l.last().copy(text = l.last().text.trimEnd(' '))
        // nach Zeilenumbruch kein führendes Leerzeichen
        for (i in 1 until l.size) if (l[i - 1].text.endsWith("\n")) l[i] = l[i].copy(text = l[i].text.trimStart(' '))
        return l.filter { it.text.isNotEmpty() }
    }

    private fun entities(s: String) = s
        .replace("&nbsp;", "\u00A0").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
}
