package de.lernen.core.sync

import de.lernen.core.data.model.Katalog
import de.lernen.core.data.model.KatalogEintrag
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.data.paket.Versionen
import de.lernen.core.data.repo.Cache
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.data.repo.PaketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

enum class PaketStatus { NEU, HINZUGEFUEGT, UPDATE }

data class KatalogZeile(val eintrag: KatalogEintrag, val status: PaketStatus, val installierteVersion: String?)

data class SyncZustand(val laeuft: Boolean = false, val fehler: String? = null)

/**
 * Liest katalog.json und faecher.json aus dem Paket-Repo, lädt Pakete herunter.
 * Hinzugefügte Pakete liegen komplett in Room und funktionieren offline.
 */
@Singleton
class KatalogRepository @Inject constructor(
    private val netz: Netz,
    private val cache: Cache,
    private val einstellungen: EinstellungenRepository,
    private val faecher: FachRepository,
    private val pakete: PaketRepository,
) {
    private val mutex = Mutex()
    private val katalog = MutableStateFlow(cache.lies(CACHE_NAME)?.let(::liesKatalog) ?: Katalog())
    private val _zustand = MutableStateFlow(SyncZustand())
    val zustand: StateFlow<SyncZustand> = _zustand.asStateFlow()

    /** Katalog mit Status je Paket (Hinzufügen / ✓ hinzugefügt / Update). */
    val zeilen: Flow<List<KatalogZeile>> = combine(katalog, pakete.pakete) { k, installiert ->
        zeilenAus(k, installiert.associate { it.id to it.version })
    }

    /** Anzahl verfügbarer Updates (für einen Hinweis). */
    val anzahlUpdates: Flow<Int> = combine(zeilen, katalog) { z, _ -> z.count { it.status == PaketStatus.UPDATE } }

    /** Gleicht mit dem Repo ab. Gibt false bei Fehler zurück (Fehlertext in [zustand]). */
    suspend fun synchronisiere(): Boolean = mutex.withLock {
        _zustand.value = SyncZustand(laeuft = true)
        try {
            val q = einstellungen.aktuell().quelle
            val kText = netz.text(q.rawUrl("katalog.json"))
            val k = liesKatalog(kText) ?: throw IllegalArgumentException("katalog.json ist ungültig.")
            runCatching { faecher.aktualisiere(netz.text(q.rawUrl("faecher.json"))) }
            cache.schreib(CACHE_NAME, kText)
            katalog.value = k
            einstellungen.setzeLetztenSync(System.currentTimeMillis())
            _zustand.value = SyncZustand()
            true
        } catch (e: Exception) {
            _zustand.value = SyncZustand(fehler = e.alsMeldung())
            false
        }
    }

    /** Lädt ein Paket (neu oder Update). Der Fortschritt bleibt über die Schritt-ids erhalten. */
    suspend fun hinzufuegen(e: KatalogEintrag): Result<Unit> = runCatching {
        val q = einstellungen.aktuell().quelle
        val text = netz.text(q.rawUrl(e.pfad))
        val p = PaketParser.liesOderWirf(text)
        require(p.id == e.id) { "Paket-id passt nicht zum Katalog (${p.id} ≠ ${e.id})." }
        pakete.speichere(text, PaketRepository.HERKUNFT_KATALOG)
        Unit
    }.recoverCatching { throw IllegalStateException(it.alsMeldung()) }

    suspend fun entfernen(id: String, fortschrittLoeschen: Boolean) = pakete.entferne(id, fortschrittLoeschen)

    /** Formelsammlung: falls im Katalog vorhanden und noch nicht da, still nachladen. */
    suspend fun werkzeugPaketeNachladen() {
        val installiert = pakete.pakete.first().associate { it.id to it.version }
        katalog.value.pakete.filter { it.typ == "formelsammlung" }.forEach { e ->
            val iv = installiert[e.id]
            if (iv == null || Versionen.istNeuer(e.version, iv)) hinzufuegen(e)
        }
    }

    companion object {
        const val CACHE_NAME = "katalog.json"

        /** Status je Katalog-Eintrag aus installierten Versionen (id → version). */
        fun zeilenAus(k: Katalog, installiert: Map<String, String>): List<KatalogZeile> = k.pakete.map { e ->
            val iv = installiert[e.id]
            val st = when {
                iv == null -> PaketStatus.NEU
                Versionen.istNeuer(e.version, iv) -> PaketStatus.UPDATE
                else -> PaketStatus.HINZUGEFUEGT
            }
            KatalogZeile(e, st, iv)
        }

        fun liesKatalog(text: String): Katalog? =
            runCatching { PaketParser.json.decodeFromString(Katalog.serializer(), text) }.getOrNull()
    }
}
