package de.lernen.core.sync

import javax.inject.Inject
import javax.inject.Singleton

/** Ein kompletter Abgleich: Katalog + Fächer, Werkzeug-Pakete, wartende Fragen hochladen, alte Fragen löschen. */
@Singleton
class SyncKoordinator @Inject constructor(
    private val katalog: KatalogRepository,
    private val fragen: FragenRepository,
    private val hintergrund: Hintergrund,
) {
    suspend fun abgleichen(): Boolean {
        val ok = katalog.synchronisiere()
        if (ok) {
            runCatching { katalog.werkzeugPaketeNachladen() }
            runCatching { fragen.hochladen() }
            runCatching { fragen.aufraeumen() }
        } else {
            hintergrund.fragenHochladen()
        }
        return ok
    }
}
