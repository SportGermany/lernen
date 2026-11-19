package de.lernen.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.lernen.core.data.repo.PaketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LernenApplication : Application() {
    @Inject lateinit var pakete: PaketRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Meilenstein 1: mitgelieferte Deutsch-Pakete beim ersten Start übernehmen.
        scope.launch { pakete.startpaketeEinrichten() }
    }
}
