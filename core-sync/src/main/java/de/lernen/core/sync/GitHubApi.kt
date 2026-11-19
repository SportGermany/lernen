package de.lernen.core.sync


import de.lernen.core.data.fragen.FrageFormat
import de.lernen.core.data.model.PaketQuelle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

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
        .header("Authorization", "Bearer $token")

    private fun contentsUrl(q: PaketQuelle, pfad: String) =
        "$basisUrl/repos/${q.owner}/${q.repo}/contents/" + pfad.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    /** Legt eine NEUE Datei an (nie überschreiben: ohne sha schlägt PUT bei vorhandener Datei fehl). */
    suspend fun anlegen(q: PaketQuelle, token: String, pfad: String, inhalt: String, nachricht: String) = withContext(Dispatchers.IO) {
        FrageFormat.pruefePfad(pfad)
        val body = buildJsonObject {
            put("message", nachricht)
            put("content", java.util.Base64.getEncoder().encodeToString(inhalt.toByteArray(Charsets.UTF_8)))
            put("branch", q.branch)
        }.toString().toRequestBody(JSON_TYP)
        netz.client.newCall(anfrage(contentsUrl(q, pfad), token).put(body).build()).execute().use { r ->
            if (!r.isSuccessful) throw NetzFehler(r.code, "Hochladen fehlgeschlagen (HTTP ${r.code})")
        }
    }

    /** sha einer Datei oder null, wenn es sie nicht (mehr) gibt. */
    suspend fun sha(q: PaketQuelle, token: String, pfad: String): String? = withContext(Dispatchers.IO) {
        FrageFormat.pruefePfad(pfad)
        netz.client.newCall(anfrage(contentsUrl(q, pfad) + "?ref=" + q.branch, token).get().build()).execute().use { r ->
            if (r.code == 404) return@withContext null
            if (!r.isSuccessful) throw NetzFehler(r.code, "Abfrage fehlgeschlagen (HTTP ${r.code})")
            json.parseToJsonElement(r.body!!.string()).jsonObject["sha"]?.jsonPrimitive?.content
        }
    }

    suspend fun loeschen(q: PaketQuelle, token: String, pfad: String, sha: String, nachricht: String) = withContext(Dispatchers.IO) {
        FrageFormat.pruefePfad(pfad)
        val body = buildJsonObject { put("message", nachricht); put("sha", sha); put("branch", q.branch) }.toString().toRequestBody(JSON_TYP)
        netz.client.newCall(anfrage(contentsUrl(q, pfad), token).delete(body).build()).execute().use { r ->
            if (!r.isSuccessful && r.code != 404) throw NetzFehler(r.code, "Löschen fehlgeschlagen (HTTP ${r.code})")
        }
    }

    /** „Verbindung testen“: Repo erreichbar und Token darf schreiben? Gibt eine Meldung zurück. */
    suspend fun testen(q: PaketQuelle, token: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        runCatching {
            netz.client.newCall(anfrage("$basisUrl/repos/${q.owner}/${q.repo}", token).get().build()).execute().use { r ->
                when (r.code) {
                    401 -> false to "Token ungültig oder abgelaufen."
                    403, 404 -> false to "Kein Zugriff auf ${q.owner}/${q.repo}. Ist das Repo im Token freigegeben?"
                    in 200..299 -> {
                        val o = json.parseToJsonElement(r.body!!.string()).jsonObject
                        val push = (o["permissions"] as? JsonObject)?.get("push")?.jsonPrimitive?.boolean ?: false
                        if (push) true to "Verbindung klappt. Der Token darf Fragen hochladen."
                        else false to "Verbunden, aber der Token darf nicht schreiben. Bitte „Contents: Read and write“ erlauben."
                    }
                    else -> false to "GitHub antwortet mit HTTP ${r.code}."
                }
            }
        }.getOrElse { false to it.alsMeldung() }
    }
}
