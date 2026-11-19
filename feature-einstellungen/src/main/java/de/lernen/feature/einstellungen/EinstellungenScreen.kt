package de.lernen.feature.einstellungen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.model.Einstellungen
import de.lernen.core.data.model.PaketQuelle
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.ZeileZwischen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object EinstellungenRoute

@HiltViewModel
class EinstellungenViewModel @Inject constructor(
    private val repo: EinstellungenRepository,
    faecherRepo: FachRepository,
) : ViewModel() {
    val einstellungen = repo.einstellungen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Einstellungen())
    val faecher = faecherRepo.faecherAlle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun darstellung(d: Darstellung) = viewModelScope.launch { repo.setzeDarstellung(d) }
    fun sichtbar(id: String, an: Boolean) = viewModelScope.launch { repo.setzeFachSichtbar(id, an) }
    fun reihenfolge(ids: List<String>) = viewModelScope.launch { repo.setzeReihenfolge(ids) }
    fun farbe(id: String, hex: String?) = viewModelScope.launch { repo.setzeFachFarbe(id, hex) }
    fun quelle(q: PaketQuelle) = viewModelScope.launch { repo.setzeQuelle(q) }
}

fun NavGraphBuilder.einstellungenBildschirm(version: String, onZurueck: () -> Unit) {
    composable<EinstellungenRoute> { EinstellungenScreen(version, onZurueck) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinstellungenScreen(version: String, onZurueck: () -> Unit, vm: EinstellungenViewModel = hiltViewModel()) {
    val e by vm.einstellungen.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Einstellungen") },
            navigationIcon = { IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Abschnitt("Darstellung")
                val optionen = listOf(Darstellung.SYSTEM to "System", Darstellung.HELL to "Hell", Darstellung.DUNKEL to "Dunkel")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    optionen.forEachIndexed { i, (d, t) ->
                        SegmentedButton(
                            selected = e.darstellung == d, onClick = { vm.darstellung(d) },
                            shape = SegmentedButtonDefaults.itemShape(i, optionen.size), icon = {},
                        ) { Text(t) }
                    }
                }
                Abschnitt("Fächer")
                val faecher by vm.faecher.collectAsStateWithLifecycle()
                FaecherAbschnitt(faecher, e.ausgeblendeteFaecher, e.fachFarben, vm::sichtbar, vm::reihenfolge, vm::farbe)
                Abschnitt("Erinnerung")
                Zeile("Täglich erinnern", "Meilenstein 4")
                Abschnitt("GitHub")
                QuelleZeile(e.quelle, vm::quelle)
                Zeile("Token für Fragen", "Meilenstein 4")
                Abschnitt("Daten")
                Zeile("Sicherung exportieren / importieren", "Meilenstein 4")
                Abschnitt("App")
                Zeile("Version", version)
            }
        }
    }
}

@Composable
private fun Abschnitt(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 2.dp))
}

@Composable
private fun Zeile(links: String, rechts: String) {
    ZeileZwischen(Modifier.padding(vertical = 13.dp)) {
        Text(links, modifier = Modifier.weight(1f))
        Grau(rechts)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun QuelleZeile(q: PaketQuelle, onSpeichern: (PaketQuelle) -> Unit) {
    var offen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    ZeileZwischen(Modifier.clickable { offen = true }.padding(vertical = 13.dp)) {
        Text("Paket-Quelle", modifier = Modifier.weight(1f))
        Grau(q.anzeige)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    if (offen) {
        var owner by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(q.owner) }
        var repo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(q.repo) }
        var branch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(q.branch) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { offen = false },
            title = { Text("Paket-Quelle (GitHub)") },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(owner, { owner = it }, label = { Text("Owner") }, singleLine = true)
                    androidx.compose.material3.OutlinedTextField(repo, { repo = it }, label = { Text("Repository") }, singleLine = true)
                    androidx.compose.material3.OutlinedTextField(branch, { branch = it }, label = { Text("Branch") }, singleLine = true)
                    Grau("Nach dem Ändern im Katalog auf ↻ tippen.", Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onSpeichern(PaketQuelle(owner, repo, branch)); offen = false }, enabled = owner.isNotBlank() && repo.isNotBlank()) { Text("Speichern") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { onSpeichern(PaketQuelle()); offen = false }) { Text("Zurücksetzen") }
            },
        )
    }
}
