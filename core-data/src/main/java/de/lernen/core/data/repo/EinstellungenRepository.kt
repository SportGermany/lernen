package de.lernen.core.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.model.Einstellungen
import kotlinx.coroutines.flow.Flow
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
    }

    val einstellungen: Flow<Einstellungen> = context.dataStore.data.map { p ->
        Einstellungen(
            darstellung = p[K.DARSTELLUNG]?.let { runCatching { Darstellung.valueOf(it) }.getOrNull() } ?: Darstellung.SYSTEM,
            ausgeblendeteFaecher = p[K.AUSGEBLENDET] ?: emptySet(),
            fachReihenfolge = p[K.REIHENFOLGE]?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
            fachFarben = p[K.FARBEN]?.split(',')?.mapNotNull {
                val t = it.split('=')
                if (t.size == 2) t[0] to t[1] else null
            }?.toMap() ?: emptyMap(),
        )
    }

    suspend fun setzeDarstellung(d: Darstellung) {
        context.dataStore.edit { it[K.DARSTELLUNG] = d.name }
    }
}
