package de.lernen.core.sync

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.paket.Versionen
import de.lernen.core.data.repo.EinstellungenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class NeueVersion(val version: String, val apkUrl: String, val notizen: String)

sealed interface UpdateZustand {
    data object Nichts : UpdateZustand
    data class Verfuegbar(val neu: NeueVersion) : UpdateZustand
    data class Laedt(val neu: NeueVersion) : UpdateZustand
    data class Fehler(val meldung: String) : UpdateZustand
    data object Aktuell : UpdateZustand
}

/** App-Update über GitHub Releases des öffentlichen App-Repos (ohne Token). */
@Singleton
class AppUpdate @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val netz: Netz,
    private val einstellungen: EinstellungenRepository,
) {
    private val _zustand = MutableStateFlow<UpdateZustand>(UpdateZustand.Nichts)
    val zustand: StateFlow<UpdateZustand> = _zustand.asStateFlow()

    /** Beim Start: höchstens einmal am Tag prüfen. */
    suspend fun taeglichPruefen(aktuelleVersion: String) {
        val letzte = einstellungen.aktuell().letzteUpdatePruefung
        if (System.currentTimeMillis() - letzte < 24L * 60 * 60 * 1000) return
        pruefen(aktuelleVersion, still = true)
    }

    suspend fun pruefen(aktuelleVersion: String, still: Boolean = false) {
        runCatching {
            val text = netz.text("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            einstellungen.setzeUpdatePruefung(System.currentTimeMillis())
            liesRelease(text)
        }.onSuccess { neu ->
            _zustand.value = if (neu != null && Versionen.istNeuer(neu.version, aktuelleVersion)) UpdateZustand.Verfuegbar(neu)
            else if (still) UpdateZustand.Nichts else UpdateZustand.Aktuell
        }.onFailure {
            if (!still) _zustand.value = UpdateZustand.Fehler(
                if (it is NetzFehler && it.code == 404) "Noch keine Release veröffentlicht." else it.alsMeldung()
            )
        }
    }

    /** Lädt die APK herunter und öffnet den Installer. */
    suspend fun installieren(neu: NeueVersion) {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _zustand.value = UpdateZustand.Fehler("Bitte erlaube „Apps aus dieser Quelle installieren“ und tippe dann nochmal auf „Installieren“.")
            return
        }
        _zustand.value = UpdateZustand.Laedt(neu)
        val datei = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "lernen-${neu.version}.apk")
        datei.delete()
        val dm = ctx.getSystemService(DownloadManager::class.java)
        val id = dm.enqueue(
            DownloadManager.Request(Uri.parse(neu.apkUrl))
                .setTitle("Lernen ${neu.version}")
                .setDestinationUri(Uri.fromFile(datei))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        )
        val ok = withContext(Dispatchers.IO) {
            while (true) {
                dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                    if (c.moveToFirst()) when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> return@withContext true
                        DownloadManager.STATUS_FAILED -> return@withContext false
                    } else return@withContext false
                }
                delay(500)
            }
            @Suppress("UNREACHABLE_CODE") false
        }
        if (!ok) { _zustand.value = UpdateZustand.Fehler("Download fehlgeschlagen."); return }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", datei)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        _zustand.value = UpdateZustand.Verfuegbar(neu)
    }

    fun ausblenden() { _zustand.value = UpdateZustand.Nichts }

    companion object {
        const val OWNER = "SportGermany"
        const val REPO = "lernen"
        private val json = Json { ignoreUnknownKeys = true }

        /** Liest die Antwort von /releases/latest: Tag vX.Y.Z und erste .apk-Datei. */
        fun liesRelease(text: String): NeueVersion? {
            val o = json.parseToJsonElement(text).jsonObject
            val tag = o["tag_name"]?.jsonPrimitive?.content ?: return null
            val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?.get("browser_download_url")?.jsonPrimitive?.content ?: return null
            return NeueVersion(tag.removePrefix("v"), apk, o["body"]?.jsonPrimitive?.content ?: "")
        }
    }
}
