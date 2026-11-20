package de.lernen.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** HTTP-Fehler. [details]: Meldung von GitHub und benötigte Berechtigung, falls vorhanden. */
class NetzFehler(val code: Int, meldung: String, val details: String? = null) : IOException(meldung)

/** Gemeinsamer HTTP-Client. Lesen ohne Token. */
@Singleton
class Netz @Inject constructor() {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Lädt Text. raw.githubusercontent.com cached einige Minuten; no-cache bittet um frische Daten. */
    suspend fun text(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK).header("Cache-Control", "no-cache").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw NetzFehler(r.code, "HTTP ${r.code} für $url")
            r.body?.string() ?: throw NetzFehler(r.code, "Leere Antwort")
        }
    }
}

/** Verständliche Fehlermeldung für die Oberfläche. */
fun Throwable.alsMeldung(): String = when (this) {
    is NetzFehler -> when (code) {
        404 -> "Nicht gefunden (HTTP 404)."
        401 -> "Token ungültig oder abgelaufen (HTTP 401)."
        403 -> "Kein Schreibzugriff (HTTP 403). Einstellungen → GitHub → Fehlerdiagnose zeigt die Ursache."
        else -> "Server-Fehler (HTTP $code)."
    } + (details?.let { " – $it" } ?: "")
    is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException -> "Keine Verbindung zum Internet."
    is IllegalArgumentException -> message ?: "Ungültige Daten."
    else -> message ?: "Unbekannter Fehler."
}
