package de.lernen.core.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.model.Einstellungen
import de.lernen.core.data.model.PaketQuelle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "einstellungen")

@Singleton
class EinstellungenRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object K {
        val DARSTELLUNG = stringPreferencesKey("darstellung")
        val AUSGEBLENDET = stringSetPreferencesKey("faecher_ausgeblendet")
        val REIHENFOLGE = stringPreferencesKey("faecher_reihenfolge")
        val FARBEN = stringPreferencesKey("faecher_farben")
        val OWNER = stringPreferencesKey("quelle_owner")
        val REPO = stringPreferencesKey("quelle_repo")
        val BRANCH = stringPreferencesKey("quelle_branch")
        val LETZTER_SYNC = longPreferencesKey("letzter_sync")
        val ERINNERUNG_AN = booleanPreferencesKey("erinnerung_an")
        val ERINNERUNG_MIN = intPreferencesKey("erinnerung_minuten")
        val UPDATE_PRUEFUNG = longPreferencesKey("update_pruefung")
    }

    val einstellungen: Flow<Einstellungen> = context.dataStore.data.map(::lies)

    suspend fun aktuell(): Einstellungen = einstellungen.first()

    private fun lies(p: Preferences): Einstellungen {
        val std = PaketQuelle()
        return Einstellungen(
            darstellung = p[K.DARSTELLUNG]?.let { runCatching { Darstellung.valueOf(it) }.getOrNull() } ?: Darstellung.SYSTEM,
            ausgeblendeteFaecher = p[K.AUSGEBLENDET] ?: emptySet(),
            fachReihenfolge = p[K.REIHENFOLGE]?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
            fachFarben = p[K.FARBEN]?.split(',')?.mapNotNull {
                val t = it.split('=')
                if (t.size == 2) t[0] to t[1] else null
            }?.toMap() ?: emptyMap(),
            quelle = PaketQuelle(p[K.OWNER] ?: std.owner, p[K.REPO] ?: std.repo, p[K.BRANCH] ?: std.branch),
            letzterSync = p[K.LETZTER_SYNC] ?: 0,
            erinnerungAn = p[K.ERINNERUNG_AN] ?: false,
            erinnerungMinuten = p[K.ERINNERUNG_MIN] ?: (19 * 60),
            letzteUpdatePruefung = p[K.UPDATE_PRUEFUNG] ?: 0,
        )
    }

    suspend fun setzeDarstellung(d: Darstellung) = context.dataStore.edit { it[K.DARSTELLUNG] = d.name }

    suspend fun setzeFachSichtbar(fachId: String, sichtbar: Boolean) = context.dataStore.edit {
        val alt = it[K.AUSGEBLENDET] ?: emptySet()
        it[K.AUSGEBLENDET] = if (sichtbar) alt - fachId else alt + fachId
    }

    suspend fun setzeReihenfolge(ids: List<String>) = context.dataStore.edit { it[K.REIHENFOLGE] = ids.joinToString(",") }

    /** [farbe] = null setzt auf die Farbe aus faecher.json zurück. */
    suspend fun setzeFachFarbe(fachId: String, farbe: String?) = context.dataStore.edit {
        val m = lies(it).fachFarben.toMutableMap()
        if (farbe == null) m.remove(fachId) else m[fachId] = farbe
        it[K.FARBEN] = m.entries.joinToString(",") { (k, v) -> "$k=$v" }
    }

    suspend fun setzeQuelle(q: PaketQuelle) = context.dataStore.edit {
        it[K.OWNER] = q.owner.trim(); it[K.REPO] = q.repo.trim(); it[K.BRANCH] = q.branch.trim().ifEmpty { "main" }
    }

    suspend fun setzeLetztenSync(zeit: Long) = context.dataStore.edit { it[K.LETZTER_SYNC] = zeit }

    suspend fun setzeErinnerung(an: Boolean, minuten: Int) = context.dataStore.edit {
        it[K.ERINNERUNG_AN] = an; it[K.ERINNERUNG_MIN] = minuten
    }

    suspend fun setzeUpdatePruefung(zeit: Long) = context.dataStore.edit { it[K.UPDATE_PRUEFUNG] = zeit }

    /** Für Datensicherung: alle Werte außer Token (der liegt gar nicht hier). */
    suspend fun alsMap(): Map<String, String> = context.dataStore.data.first().asMap().mapKeys { it.key.name }
        .mapValues { (_, v) -> if (v is Set<*>) v.joinToString(",") else v.toString() }

    suspend fun ausMap(m: Map<String, String>) = context.dataStore.edit { p ->
        p.clear()
        m[K.DARSTELLUNG.name]?.let { p[K.DARSTELLUNG] = it }
        m[K.AUSGEBLENDET.name]?.let { p[K.AUSGEBLENDET] = it.split(',').filter(String::isNotBlank).toSet() }
        m[K.REIHENFOLGE.name]?.let { p[K.REIHENFOLGE] = it }
        m[K.FARBEN.name]?.let { p[K.FARBEN] = it }
        m[K.OWNER.name]?.let { p[K.OWNER] = it }
        m[K.REPO.name]?.let { p[K.REPO] = it }
        m[K.BRANCH.name]?.let { p[K.BRANCH] = it }
        m[K.ERINNERUNG_AN.name]?.let { p[K.ERINNERUNG_AN] = it.toBoolean() }
        m[K.ERINNERUNG_MIN.name]?.toIntOrNull()?.let { p[K.ERINNERUNG_MIN] = it }
    }
}
