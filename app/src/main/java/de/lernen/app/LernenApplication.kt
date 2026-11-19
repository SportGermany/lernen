package de.lernen.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.data.repo.PaketRepository
import de.lernen.core.sync.AppUpdate
import de.lernen.core.sync.Benachrichtigung
import de.lernen.core.sync.Hintergrund
import de.lernen.core.sync.SyncKoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LernenApplication : Application(), Configuration.Provider {
    @Inject lateinit var pakete: PaketRepository
    @Inject lateinit var sync: SyncKoordinator
    @Inject lateinit var hintergrund: Hintergrund
    @Inject lateinit var einstellungen: EinstellungenRepository
    @Inject lateinit var update: AppUpdate
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Benachrichtigung.kanalAnlegen(this)
        scope.launch {
            // Beim ersten Start: mitgelieferte Deutsch-Pakete übernehmen (funktioniert offline).
            pakete.startpaketeEinrichten()
            val e = einstellungen.aktuell()
            hintergrund.erinnerungPlanen(e.erinnerungAn, e.erinnerungMinuten)
            hintergrund.aufraeumenPlanen()
            // Beim App-Start abgleichen (ohne Netz: still weiter mit dem Cache).
            sync.abgleichen()
            update.taeglichPruefen(BuildConfig.VERSION_NAME)
        }
    }
}
