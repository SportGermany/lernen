package de.lernen.core.data.fragen

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** Antwortart einer Frage an Claude, mit festem Textbaustein (Abschnitt 9.3). */
enum class Antwortart(val schluessel: String, val titel: String, val baustein: String) {
    NUR_TIPP(
        "nur-tipp", "Nur Tipp",
        "Verrate keine Lösung. Gib einen Denkanstoß oder eine Frage, die zum nächsten Schritt führt. " +
            "Wenn der Schüler danach noch hängt, darf er nachfragen, dann gibst du einen konkreteren Tipp.",
    ),
    ERKLAEREN(
        "erklaeren", "Erklären",
        "Erkläre die Regel oder das Vorgehen einfach und mit einem eigenen kurzen Beispiel. Löse nicht die Aufgabe des Schülers.",
    ),
    LOESUNG_PRUEFEN(
        "loesung-pruefen", "Lösung prüfen",
        "Der Schüler schickt dir gleich seine Lösung (Text oder Foto). Prüfe sie, lobe Gelungenes konkret und zeige höchstens " +
            "die drei wichtigsten Verbesserungen mit Begründung. Schreibe seine Lösung nicht komplett neu.",
    ),
    BEISPIEL(
        "beispiel-zeigen", "Beispiel zeigen",
        "Zeige ein eigenes, ähnliches Beispiel, nicht die Aufgabe des Schülers. Markiere, worauf es ankommt.",
    );

    companion object {
        fun von(s: String) = entries.firstOrNull { it.schluessel == s } ?: NUR_TIPP
    }
}

/** Automatischer Kontext aus der App (nicht bearbeitbar). */
@kotlinx.serialization.Serializable
data class FrageKontext(
    val fachId: String? = null,
    val fachName: String? = null,
    val paketId: String? = null,
    val paketTitel: String? = null,
    /** Pfad im Paket-Repo, z. B. pakete/deutsch/….json */
    val paketPfad: String? = null,
    val schrittId: String? = null,
    val schrittTitel: String? = null,
    val schrittZiel: String? = null,
    /** Zeilen aus den Tipps (nur Typ arbeitsheft), z. B. "Z. 4–5, Z. 9–10" */
    val zeilen: List<String> = emptyList(),
    val angezeigteTipps: Int = 0,
)

data class Frage(
    val id: String,
    val erstellt: ZonedDateTime,
    val text: String,
    val antwortart: Antwortart,
    val kontext: FrageKontext,
) {
    val loeschenAb: ZonedDateTime get() = erstellt.plusHours(24)
    val pfad: String get() = FrageFormat.pfad(id)
}

object FrageFormat {
    const val ORDNER = "fragen/offen/"
    private val ID_MUSTER = Regex("^\\d{4}-\\d{2}-\\d{2}-\\d{4}-[a-z0-9]{4}$")
    private val PFAD_MUSTER = Regex("^fragen/offen/\\d{4}-\\d{2}-\\d{2}-\\d{4}-[a-z0-9]{4}\\.md$")
    private val ZEILEN = Regex("Z\\.\\s?\\d+(?:\\s?[–-]\\s?\\d+)?")
    private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /** `JJJJ-MM-TT-HHMM-xxxx` mit 4 zufälligen Zeichen a–z0–9. */
    fun neueId(zeit: ZonedDateTime, zufall: Random = Random.Default): String {
        val zeichen = "abcdefghijklmnopqrstuvwxyz0123456789"
        val x = (1..4).map { zeichen[zufall.nextInt(zeichen.length)] }.joinToString("")
        return zeit.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")) + "-" + x
    }

    fun pfad(id: String): String {
        require(ID_MUSTER.matches(id)) { "Ungültige Frage-id: $id" }
        return "$ORDNER$id.md"
    }

    /**
     * Schutz: Die App darf ausschließlich unter fragen/offen/ schreiben und löschen.
     * Wird vor JEDEM Schreib- oder Löschaufruf geprüft (der Token könnte technisch das ganze Repo beschreiben).
     */
    fun pruefePfad(pfad: String) {
        require(!pfad.contains("..") && !pfad.contains("//") && PFAD_MUSTER.matches(pfad)) {
            "Schreiben nur unter $ORDNER erlaubt (abgelehnt: $pfad)"
        }
    }

    /** Zeilenangaben wie „Z. 4–5“ aus Tipps herausziehen. */
    fun zeilenAus(tipps: List<String>): List<String> =
        tipps.flatMap { t -> ZEILEN.findAll(t).map { it.value.replace(" ", " ").replace(" ", " ") }.toList() }.distinct()

    fun rawUrl(owner: String, repo: String, branch: String, pfad: String) =
        "https://raw.githubusercontent.com/$owner/$repo/$branch/$pfad"

    /** Inhalt der Markdown-Datei (Abschnitt 9.2). */
    fun markdown(f: Frage, paketUrl: String?): String = buildString {
        val k = f.kontext
        appendLine("---")
        appendLine("id: ${f.id}")
        appendLine("erstellt: ${f.erstellt.truncatedTo(ChronoUnit.SECONDS).format(ISO)}")
        appendLine("loeschen_ab: ${f.loeschenAb.truncatedTo(ChronoUnit.SECONDS).format(ISO)}")
        k.fachId?.let { appendLine("fach: $it") }
        k.paketId?.let { appendLine("paket: $it") }
        paketUrl?.let { appendLine("paket_url: $it") }
        if (k.schrittId != null) appendLine("schritt: ${k.schrittId} – ${k.schrittTitel ?: ""}".trimEnd())
        appendLine("antwortart: ${f.antwortart.schluessel}")
        appendLine("---")
        appendLine()
        appendLine("# Frage")
        appendLine(f.text.trim())
        appendLine()
        appendLine("# Kontext aus der App")
        k.fachName?.let { appendLine("- Fach: $it") }
        k.paketTitel?.let { appendLine("- Thema: $it") }
        k.schrittTitel?.let { appendLine("- Schritt: $it") }
        k.schrittZiel?.takeIf { it.isNotBlank() }?.let { appendLine("- Ziel des Schritts: $it") }
        if (k.zeilen.isNotEmpty()) appendLine("- Zeilen im Heft: ${k.zeilen.joinToString(", ")}")
        if (k.schrittId != null) appendLine("- Schon angezeigte Tipps: " + if (k.angezeigteTipps == 0) "keine" else (1..k.angezeigteTipps).joinToString(", "))
        if (k.paketTitel == null) appendLine("- Allgemeine Frage, ohne Thema")
        appendLine()
        appendLine("# So antwortest du")
        appendLine("Du antwortest einem Schüler der 9. Klasse Realschule Bayern, der allein auf einem Sport-Lehrgang lernt.")
        appendLine("Antworte auf Deutsch, kurz und gut lesbar auf dem Handy (höchstens etwa 8 Sätze oder eine kurze Liste).")
        if (paketUrl != null) appendLine("Du kannst die Paketdatei unter `paket_url` lesen, um Schritt und Thema genau zu kennen.")
        appendLine("Schreib keine Texte aus Schulbüchern oder Lösungsheften ab.")
        appendLine()
        appendLine("**Antwortart: ${f.antwortart.titel}**")
        appendLine(f.antwortart.baustein)
        appendLine()
        appendLine("Wenn du die Aufgabe selbst sehen musst, bitte den Schüler, ein Foto in diesen Chat zu schicken.")
    }

    /** Befehl zum Kopieren in einen neuen Claude-Chat (Abschnitt 9.4), mit Ersatzteil falls der Link nicht geht. */
    fun befehl(f: Frage, dateiUrl: String): String = buildString {
        val k = f.kontext
        appendLine("Lies diese Frage aus meiner Lern-App und antworte genau nach den Regeln, die in der Datei stehen:")
        appendLine(dateiUrl)
        appendLine()
        appendLine("Falls du den Link nicht öffnen kannst, hier die Frage direkt:")
        val teile = listOfNotNull(
            k.fachName?.let { "Fach: $it" },
            k.paketTitel?.let { "Thema: $it" },
            k.schrittTitel?.let { "Schritt: ${kurzerSchritt(it)}" },
            "Antwortart: ${f.antwortart.titel}",
        )
        appendLine(teile.joinToString(" · "))
        appendLine("„${f.text.trim()}“")
        append("Regel: ${f.antwortart.baustein}")
    }

    /** "1c · Schwächen rot markieren" → "1c" (wie im Beispiel); sonst ganzer Titel. */
    private fun kurzerSchritt(titel: String) = titel.substringBefore(" · ").takeIf { titel.contains(" · ") } ?: titel

    fun commitNeu(id: String) = "Frage $id"
    fun commitGeloescht(id: String) = "Frage $id gelöscht"
}
