package de.lernen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.lernen.core.data.model.Einstellungen
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.design.theme.LernenTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var einstellungen: EinstellungenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val e by einstellungen.einstellungen.collectAsStateWithLifecycle(initialValue = Einstellungen())
            LernenTheme(e.darstellung) { LernenApp() }
        }
    }
}
