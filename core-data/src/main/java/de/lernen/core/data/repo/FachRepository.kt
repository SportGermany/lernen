package de.lernen.core.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.model.Fach
import de.lernen.core.data.paket.PaketParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fächer aus faecher.json. In Meilenstein 1 aus den App-Assets,
 * ab Meilenstein 2 aus dem Paket-Repo (mit Assets als Rückfall).
 */
@Singleton
class FachRepository @Inject constructor(
    @ApplicationContext context: Context,
    einstellungen: EinstellungenRepository,
) {
    private val alleFaecher = MutableStateFlow(
        PaketParser.liesFaecher(context.assets.open("faecher.json").bufferedReader().use { it.readText() })
    )

    /** Alle Fächer inkl. ausgeblendeter, in eingestellter Reihenfolge und mit eigenen Farben. */
    val faecherAlle: Flow<List<Fach>> = combine(alleFaecher, einstellungen.einstellungen) { f, e ->
        val reihen = e.fachReihenfolge
        f.map { fach -> e.fachFarben[fach.id]?.let { fach.copy(farbe = it, farbeDunkel = it) } ?: fach }
            .sortedBy { reihen.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** Sichtbare Fächer. */
    val faecher: Flow<List<Fach>> = combine(faecherAlle, einstellungen.einstellungen) { f, e ->
        f.filter { it.id !in e.ausgeblendeteFaecher }
    }

    /** Ab Meilenstein 2 vom Sync aufgerufen. */
    fun aktualisiere(neu: List<Fach>) {
        if (neu.isNotEmpty()) alleFaecher.value = neu
    }
}
