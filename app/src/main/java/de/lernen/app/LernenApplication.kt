package de.lernen.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.lernen.core.data.repo.PaketRepository
import de.lernen.core.sync.KatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LernenApplication : Application() {
    @Inject lateinit var pakete: PaketRepository
    @Inject lateinit var katalog: KatalogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Meilenstein 1: mitgelieferte Deutsch-Pakete beim ersten Start übernehmen.
        scope.launch {
            // Beim ersten Start: mitgelieferte Deutsch-Pakete übernehmen (funktioniert offline).
            pakete.startpaketeEinrichten()
            // Beim App-Start mit dem Paket-Repo abgleichen (ohne Netz: still weiter mit dem Cache).
            if (katalog.synchronisiere()) runCatching { katalog.werkzeugPaketeNachladen() }
        }
    }
}
