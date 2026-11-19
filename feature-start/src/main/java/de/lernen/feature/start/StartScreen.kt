package de.lernen.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.repo.Uebersicht
import de.lernen.core.data.repo.UebersichtRepository
import de.lernen.core.design.komponenten.Balken
import de.lernen.core.design.komponenten.FachPunkt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.Ueberschrift
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.fachFarbe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object StartRoute

@HiltViewModel
class StartViewModel @Inject constructor(repo: UebersichtRepository) : ViewModel() {
    val uebersicht = repo.uebersicht.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

fun NavGraphBuilder.startBildschirm(
    onThema: (paketId: String) -> Unit,
    onWiederholen: (paketId: String) -> Unit,
    onFach: (fachId: String) -> Unit,
    onEinstellungen: () -> Unit,
) {
    composable<StartRoute> { StartScreen(onThema, onWiederholen, onFach, onEinstellungen) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onThema: (String) -> Unit,
    onWiederholen: (String) -> Unit,
    onFach: (String) -> Unit,
    onEinstellungen: () -> Unit,
    vm: StartViewModel = hiltViewModel(),
) {
    val u by vm.uebersicht.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Lernen") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            actions = { IconButton(onClick = onEinstellungen) { Icon(Icons.Outlined.Settings, "Einstellungen", tint = Lernen.farben.text2) } },
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            u?.let { Inhalt(it, onThema, onWiederholen, onFach) }
        }
    }
}

@Composable
private fun Inhalt(u: Uebersicht, onThema: (String) -> Unit, onWiederholen: (String) -> Unit, onFach: (String) -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
        val w = u.weitermachen
        if (w != null) {
            Karte(onClick = { onThema(w.thema.paket.id) }, linksStreifen = fachFarbe(w.fach)) {
                Grau((if (w.begonnen) "Weitermachen" else "Neu hinzugefügt") + " · " + (w.fach?.name ?: ""))
                Text(w.thema.paket.titel, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 3.dp))
                val f = w.thema.fortschritt
                Grau(listOfNotNull("Begleiter", w.schrittTitel, "${f.erledigt} von ${f.gesamt} abgehakt").joinToString(" · "))
                Spacer(Modifier.height(9.dp))
                Balken(f.anteil, fachFarbe(w.fach))
            }
        } else {
            Karte {
                Text("Noch kein Thema", fontWeight = FontWeight.Medium)
                Grau("Füge im Katalog ein Thema hinzu.")
            }
        }

        Ueberschrift("Deine Fächer")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            u.faecher.forEach { fm ->
                Karte(onClick = { onFach(fm.fach.id) }, innen = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FachPunkt(fm.fach)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(fm.fach.name, fontWeight = FontWeight.Medium)
                            Grau(
                                when (fm.themen.size) {
                                    0 -> "noch keine Themen"
                                    1 -> "1 Thema"
                                    else -> "${fm.themen.size} Themen"
                                }
                            )
                        }
                        Text("›", color = Lernen.farben.text2)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(
            onClick = { u.wiederholenPaketId?.let(onWiederholen) },
            enabled = u.wiederholenPaketId != null,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Wiederholen" + if (u.offeneWiederholungen > 0) " · ${u.offeneWiederholungen} offen" else "") }
    }
}
