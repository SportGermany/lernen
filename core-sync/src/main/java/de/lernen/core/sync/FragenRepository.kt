package de.lernen.core.sync

import de.lernen.core.data.db.FrageDao
import de.lernen.core.data.db.FrageEntity
import de.lernen.core.data.fragen.Antwortart
import de.lernen.core.data.fragen.Frage
import de.lernen.core.data.fragen.FrageFormat
import de.lernen.core.data.fragen.FrageKontext
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.data.repo.EinstellungenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

enum class FrageStatus(val schluessel: String, val anzeige: String) {
    WARTET("wartet", "wartet auf Netz"), HOCHGELADEN("hochgeladen", "hochgeladen"), GELOESCHT("geloescht", "gelöscht");

    companion object {
        fun von(s: String) = entries.firstOrNull { it.schluessel == s } ?: WARTET
    }
}

data class FrageEintrag(val frage: Frage, val status: FrageStatus, val fehler: String?, val befehl: String?)

/**
 * Fragen an Claude: lokal in Room, Hochladen per WorkManager sobald Netz da ist,
 * automatisches Löschen 24 h nach dem Erstellen.
 */
@Singleton
class FragenRepository @Inject constructor(
    private val dao: FrageDao,
    private val api: GitHubApi,
    private val token: TokenSpeicher,
    private val einstellungen: EinstellungenRepository,
) {
    private val mutex = Mutex()

    val fragen: Flow<List<FrageEintrag>> = dao.alle().map { l -> l.map { eintrag(it) } }

    fun frage(id: String): Flow<FrageEintrag?> = dao.beobachte(id).map { it?.let { e -> eintrag(e) } }

    private suspend fun eintrag(e: FrageEntity): FrageEintrag {
        val f = zuFrage(e)
        val st = FrageStatus.von(e.status)
        val q = einstellungen.aktuell().quelle
        val befehl = if (st == FrageStatus.HOCHGELADEN) FrageFormat.befehl(f, FrageFormat.rawUrl(q.owner, q.repo, q.branch, f.pfad)) else null
        return FrageEintrag(f, st, e.fehler, befehl)
    }

    /** Speichert die Frage lokal (Status „wartet auf Netz“). Das Hochladen übernimmt der Worker. */
    suspend fun erstelle(text: String, art: Antwortart, kontext: FrageKontext): String {
        val jetzt = ZonedDateTime.now()
        val id = FrageFormat.neueId(jetzt)
        dao.speichere(
            FrageEntity(
                id, jetzt.toInstant().toEpochMilli(), kontext.fachId, kontext.paketId, kontext.paketTitel,
                kontext.schrittId, kontext.schrittTitel, text.trim(), art.schluessel, FrageStatus.WARTET.schluessel,
                FrageFormat.pfad(id), PaketParser.json.encodeToString(FrageKontext.serializer(), kontext), null,
            )
        )
        return id
    }

    /** Lädt alle wartenden Fragen hoch. Rückgabe false = später nochmal versuchen. */
    suspend fun hochladen(): Boolean = mutex.withLock {
        val t = token.lies() ?: return true // ohne Token: nichts zu tun, Hinweis steht in der App
        val q = einstellungen.aktuell().quelle
        var alleOk = true
        for (e in dao.mitStatus(FrageStatus.WARTET.schluessel)) {
            val f = zuFrage(e)
            try {
                val paketUrl = f.kontext.paketPfad?.let { q.rawUrl(it) }
                api.anlegen(q, t, f.pfad, FrageFormat.markdown(f, paketUrl), FrageFormat.commitNeu(f.id))
                dao.setzeStatus(f.id, FrageStatus.HOCHGELADEN.schluessel, null)
            } catch (ex: NetzFehler) {
                // 422 = Datei existiert schon (z. B. nach Abbruch) → gilt als hochgeladen
                if (ex.code == 422 && api.sha(q, t, f.pfad) != null) dao.setzeStatus(f.id, FrageStatus.HOCHGELADEN.schluessel, null)
                else { dao.setzeFehler(f.id, ex.alsMeldung()); alleOk = alleOk && ex.code !in 400..499 }
            } catch (ex: Exception) {
                dao.setzeFehler(f.id, ex.alsMeldung()); alleOk = false
            }
        }
        alleOk
    }

    /** Löscht eigene Dateien, deren loeschen_ab erreicht ist (24 h nach dem Erstellen). */
    suspend fun aufraeumen(jetzt: Long = System.currentTimeMillis()): Boolean = mutex.withLock {
        val t = token.lies() ?: return true
        val q = einstellungen.aktuell().quelle
        var alleOk = true
        for (e in dao.mitStatus(FrageStatus.HOCHGELADEN.schluessel)) {
            if (e.erstellt + LEBENSDAUER_MS > jetzt) continue
            val pfad = FrageFormat.pfad(e.id)
            try {
                val sha = api.sha(q, t, pfad)
                if (sha != null) api.loeschen(q, t, pfad, sha, FrageFormat.commitGeloescht(e.id))
                dao.setzeStatus(e.id, FrageStatus.GELOESCHT.schluessel, null)
            } catch (ex: Exception) {
                dao.setzeFehler(e.id, ex.alsMeldung()); alleOk = false
            }
        }
        alleOk
    }

    private fun zuFrage(e: FrageEntity): Frage {
        val k = e.kontextJson?.let { runCatching { PaketParser.json.decodeFromString(FrageKontext.serializer(), it) }.getOrNull() }
            ?: FrageKontext(fachId = e.fach, paketId = e.paketId, paketTitel = e.paketTitel, schrittId = e.schrittId, schrittTitel = e.schrittTitel)
        return Frage(e.id, ZonedDateTime.ofInstant(Instant.ofEpochMilli(e.erstellt), ZoneId.systemDefault()), e.text, Antwortart.von(e.antwortart), k)
    }

    companion object {
        const val LEBENSDAUER_MS = 24L * 60 * 60 * 1000
    }
}
