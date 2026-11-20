package de.lernen.core.sync

import de.lernen.core.data.fragen.FrageFormat
import de.lernen.core.data.model.PaketQuelle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Antwort von GitHub, aufbereitet für Fehlermeldungen und Diagnose. */
data class GitHubAntwort(
    val code: Int,
    val body: String,
    /** "message" aus der JSON-Antwort */
    val meldung: String?,
    /** Header X-Accepted-GitHub-Permissions: welche Token-Berechtigung GitHub erwartet */
    val benoetigt: String?,
    /** Header X-OAuth-Scopes (nur klassische Tokens) */
    val scopes: String?,
    val sso: String?,
) {
    val ok get() = code in 200..299
    fun details(): String? = listOfNotNull(meldung, benoetigt?.let { "benötigt: $it" }).joinToString(" · ").ifBlank { null }
}

/**
 * GitHub REST (Contents-API) – nur für Fragen an Claude.
 * Jeder Schreib- und Löschaufruf prüft vorher den Pfad: erlaubt ist ausschließlich fragen/offen/.
 */
@Singleton
class GitHubApi @Inject constructor(private val netz: Netz) {
    var basisUrl = "https://api.github.com"
    private val json = Json { ignoreUnknownKeys = true }
    private val JSON_TYP = "application/json; charset=utf-8".toMediaType()

    private fun anfrage(url: String, token: String) = Request.Builder().url(url)
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Authorization", "Bearer ${token.trim()}")

    private fun contentsUrl(q: PaketQuelle, pfad: String) =
        "$basisUrl/repos/${q.owner}/${q.repo}/contents/" + pfad.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun lies(r: Response): GitHubAntwort {
        val body = r.body?.string().orEmpty()
        val meldung = runCatching { (json.parseToJsonElement(body) as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull }.getOrNull()
        return GitHubAntwort(r.code, body, meldung, r.header("X-Accepted-GitHub-Permissions"), r.header("X-OAuth-Scopes"), r.header("X-GitHub-SSO"))
    }

    private suspend fun ausfuehren(req: Request): GitHubAntwort = withContext(Dispatchers.IO) {
        netz.client.newCall(req).execute().use(::lies)
    }

    private fun fehler(a: GitHubAntwort, was: String): Nothing = throw NetzFehler(a.code, "$was fehlgeschlagen (HTTP ${a.code})", a.details())

    private fun putBody(q: PaketQuelle, inhalt: String, nachricht: String) = buildJsonObject {
        put("message", nachricht)
        put("content", java.util.Base64.getEncoder().encodeToString(inhalt.toByteArray(Charsets.UTF_8)))
        put("branch", q.branch)
    }.toString().toRequestBody(JSON_TYP)

    /** Legt eine NEUE Datei an (nie überschreiben: ohne sha schlägt PUT bei vorhandener Datei fehl). */
    suspend fun anlegen(q: PaketQuelle, token: String, pfad: String, inhalt: String, nachricht: String) {
        FrageFormat.pruefePfad(pfad)
        val a = ausfuehren(anfrage(contentsUrl(q, pfad), token).put(putBody(q, inhalt, nachricht)).build())
        if (!a.ok) fehler(a, "Hochladen")
    }

    /** sha einer Datei oder null, wenn es sie nicht (mehr) gibt. */
    suspend fun sha(q: PaketQuelle, token: String, pfad: String): String? {
        FrageFormat.pruefePfad(pfad)
        val a = ausfuehren(anfrage(contentsUrl(q, pfad) + "?ref=" + q.branch, token).get().build())
        if (a.code == 404) return null
        if (!a.ok) fehler(a, "Abfrage")
        return runCatching { (json.parseToJsonElement(a.body) as JsonObject)["sha"]?.jsonPrimitive?.content }.getOrNull()
    }

    suspend fun loeschen(q: PaketQuelle, token: String, pfad: String, sha: String, nachricht: String) {
        FrageFormat.pruefePfad(pfad)
        val body = buildJsonObject { put("message", nachricht); put("sha", sha); put("branch", q.branch) }.toString().toRequestBody(JSON_TYP)
        val a = ausfuehren(anfrage(contentsUrl(q, pfad), token).delete(body).build())
        if (!a.ok && a.code != 404) fehler(a, "Löschen")
    }

    /** „Verbindung testen“ = kurze Diagnose (schreibt und löscht wirklich eine Testdatei). */
    suspend fun testen(q: PaketQuelle, token: String): Pair<Boolean, String> {
        val d = diagnose(q, token)
        return d.ok to (if (d.ok) "Verbindung klappt. Der Token darf Fragen hochladen und löschen." else d.loesung)
    }

    // ---------------- Fehlerdiagnose ----------------

    data class Schritt(val titel: String, val ok: Boolean, val text: String)
    data class Diagnose(val schritte: List<Schritt>, val ok: Boolean, val loesung: String, val bericht: String)

    /**
     * Prüft Schritt für Schritt: Internet, Token-Format, Token gültig, Repo, Branch, echter Schreib- und Löschtest.
     * Der Token selbst erscheint nie im Bericht.
     */
    suspend fun diagnose(q: PaketQuelle, tokenRoh: String?, appVersion: String = ""): Diagnose {
        val schritte = mutableListOf<Schritt>()
        var loesung = ""
        fun s(titel: String, ok: Boolean, text: String) = schritte.add(Schritt(titel, ok, text))
        fun ende(): Diagnose {
            val ok = schritte.all { it.ok } && loesung.isEmpty()
            val bericht = buildString {
                appendLine("Lernen – Fehlerdiagnose Fragen an Claude")
                if (appVersion.isNotBlank()) appendLine("App-Version: $appVersion")
                appendLine("Zeit: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                appendLine("Paket-Quelle: ${q.owner}/${q.repo} (Branch ${q.branch})")
                schritte.forEach { appendLine("${if (it.ok) "OK  " else "FEHLER"} ${it.titel}: ${it.text}") }
                if (loesung.isNotBlank()) appendLine("Lösung: $loesung")
            }
            return Diagnose(schritte, ok, if (ok) "Alles in Ordnung. Fragen können hochgeladen werden." else loesung, bericht)
        }

        // 1. Token vorhanden und Format
        val token = tokenRoh?.trim()
        if (token.isNullOrEmpty()) {
            s("Token gespeichert", false, "Es ist kein Token gespeichert.")
            loesung = "Einstellungen → GitHub → Token einfügen und speichern."
            return ende()
        }
        val art = when {
            token.startsWith("github_pat_") -> "Fine-grained Token"
            token.startsWith("ghp_") -> "Klassischer Token"
            else -> "unbekanntes Format"
        }
        val formatOk = art != "unbekanntes Format" && !token.any { it.isWhitespace() }
        s("Token-Format", formatOk, "$art, ${token.length} Zeichen" + if (!formatOk) ". Ein Token beginnt mit „github_pat_“ oder „ghp_“. Wurde er vollständig kopiert?" else "")
        if (!formatOk) { loesung = "Token auf GitHub neu kopieren (vollständig, ohne Leerzeichen) und in den Einstellungen neu einfügen."; return ende() }

        // 2. Internet + Token gültig
        val user = runCatching { ausfuehren(anfrage("$basisUrl/user", token).get().build()) }.getOrElse {
            s("Verbindung zu GitHub", false, it.alsMeldung())
            loesung = "Internetverbindung prüfen und nochmal versuchen."
            return ende()
        }
        s("Verbindung zu GitHub", true, "erreichbar")
        val login = runCatching { (json.parseToJsonElement(user.body) as JsonObject)["login"]?.jsonPrimitive?.content }.getOrNull()
        when {
            user.code == 401 -> {
                s("Token gültig", false, "GitHub kennt den Token nicht (abgelaufen oder gelöscht).")
                loesung = "Neuen Token erstellen und in den Einstellungen einfügen."
                return ende()
            }
            user.ok -> s("Token gültig", true, "gehört zum Konto „$login“" + (user.scopes?.let { ", Scopes: ${it.ifBlank { "keine" }}" } ?: ""))
            else -> s("Token gültig", true, "Konto nicht lesbar (HTTP ${user.code}), wird weiter geprüft")
        }
        if (login != null && art == "Fine-grained Token" && !login.equals(q.owner, ignoreCase = true)) {
            s("Konto passt zum Repo", false, "Der Token gehört „$login“, das Repo aber „${q.owner}“.")
            loesung = "Den Token mit dem GitHub-Konto „${q.owner}“ erstellen (angemeldet als ${q.owner}). Ein Fine-grained Token wirkt nur für Repos des eigenen Kontos."
            return ende()
        }

        // 3. Repo und Branch
        val repo = ausfuehren(anfrage("$basisUrl/repos/${q.owner}/${q.repo}", token).get().build())
        if (!repo.ok) {
            s("Repo ${q.owner}/${q.repo}", false, "HTTP ${repo.code} ${repo.meldung.orEmpty()}")
            loesung = "Stimmt die Paket-Quelle in den Einstellungen? Erwartet: SportGermany/lernen-pakete."
            return ende()
        }
        s("Repo ${q.owner}/${q.repo}", true, "gefunden")
        val branch = ausfuehren(anfrage("$basisUrl/repos/${q.owner}/${q.repo}/branches/${q.branch}", token).get().build())
        s("Branch ${q.branch}", branch.ok, if (branch.ok) "vorhanden" else "nicht gefunden (HTTP ${branch.code})")
        if (!branch.ok) { loesung = "In den Einstellungen bei Paket-Quelle den Branch auf „main“ setzen."; return ende() }

        // 4. Echter Schreibtest + Löschen
        val pfad = "${FrageFormat.ORDNER}${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}-test.md"
        FrageFormat.pruefePfad(pfad)
        val put = ausfuehren(anfrage(contentsUrl(q, pfad), token).put(putBody(q, "Verbindungstest der Lern-App. Wird sofort wieder gelöscht.\n", "Verbindungstest")).build())
        if (!put.ok && put.code != 422) {
            s("Schreiben in fragen/offen/", false, "HTTP ${put.code}: ${put.details() ?: "ohne Meldung"}")
            loesung = erklaereSchreibfehler(put, art, q)
            return ende()
        }
        s("Schreiben in fragen/offen/", true, "Testdatei angelegt")
        val sha = runCatching { sha(q, token, pfad) }.getOrNull()
        val geloescht = sha == null || runCatching { loeschen(q, token, pfad, sha, "Verbindungstest gelöscht") }.isSuccess
        s("Löschen in fragen/offen/", geloescht, if (geloescht) "Testdatei wieder gelöscht" else "Löschen hat nicht geklappt")
        if (!geloescht) loesung = "Der Token darf schreiben, aber nicht löschen. Bitte „Contents: Read and write“ prüfen."
        return ende()
    }

    private fun erklaereSchreibfehler(a: GitHubAntwort, art: String, q: PaketQuelle): String {
        val m = a.meldung.orEmpty()
        return when {
            a.sso != null -> "Die Organisation verlangt Single Sign-on für diesen Token. Auf GitHub beim Token „Configure SSO“ → Autorisieren."
            m.contains("forbids access via a fine-grained", true) -> "Die Organisation erlaubt keine Fine-grained Tokens. Einen klassischen Token mit Scope „public_repo“ verwenden."
            m.contains("Resource not accessible by personal access token", true) && art == "Fine-grained Token" ->
                "Dem Token fehlt das Schreibrecht für ${q.repo}. Auf GitHub: Settings → Developer settings → Fine-grained tokens → Token öffnen → " +
                    "„Repository access“: Only select repositories → ${q.repo} auswählen; „Permissions → Repository permissions → Contents“: Read and write. " +
                    "Speichern (der Token bleibt gleich, nichts neu einfügen) und die Diagnose nochmal starten."
            art == "Klassischer Token" && a.code in listOf(403, 404) ->
                "Dem klassischen Token fehlt der Scope „public_repo“ (oder „repo“). Auf GitHub beim Token den Haken bei „public_repo“ setzen."
            a.code == 404 -> "GitHub verweigert den Zugriff auf ${q.owner}/${q.repo} (HTTP 404). Beim Token unter „Repository access“ das Repo ${q.repo} auswählen und „Contents: Read and write“ erlauben."
            a.code == 409 -> "Konflikt auf GitHub (HTTP 409): Ist der Branch „${q.branch}“ geschützt? Branch-Schutz für fragen/ aufheben."
            else -> "GitHub lehnt das Schreiben ab (HTTP ${a.code}): ${a.details() ?: "keine Meldung"}."
        }
    }

    companion object {
        /** Token-Seite mit vorausgefüllten Feldern (Name, Ablauf, Contents: write). */
        fun tokenSeite(q: PaketQuelle) =
            "https://github.com/settings/personal-access-tokens/new?name=Lernen-App-Fragen&description=Fragen%20an%20Claude%20aus%20der%20Lern-App" +
                "&target_name=${q.owner}&expires_in=366&contents=write"
    }
}
