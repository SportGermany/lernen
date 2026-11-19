package de.lernen.feature.katalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Fach
import de.lernen.core.data.model.KatalogEintrag
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.design.komponenten.Abzeichen
import de.lernen.core.design.komponenten.FachPille
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.theme.Lernen
import de.lernen.core.sync.KatalogRepository
import de.lernen.core.sync.KatalogZeile
import de.lernen.core.sync.PaketStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Serializable
data object KatalogRoute

data class KatalogUi(
    val zeilen: List<KatalogZeile> = emptyList(),
    val faecher: List<Fach> = emptyList(),
    val letzterSync: Long = 0,
    val quelle: String = "",
    val laeuft: Boolean = false,
    val fehler: String? = null,
    val arbeitetAn: Set<String> = emptySet(),
)

@HiltViewModel
class KatalogViewModel @Inject constructor(
    private val repo: KatalogRepository,
    faecher: FachRepository,
    einstellungen: EinstellungenRepository,
) : ViewModel() {
    private val arbeit = kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
    private val meldungen = Channel<String>(Channel.BUFFERED)
    val meldung = meldungen.receiveAsFlow()

    val ui = combine(repo.zeilen, faecher.faecherAlle, einstellungen.einstellungen, repo.zustand, arbeit) { z, f, e, s, a ->
        KatalogUi(z, f, e.letzterSync, e.quelle.anzeige, s.laeuft, s.fehler, a)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KatalogUi())

    fun synchronisieren() = viewModelScope.launch { repo.synchronisiere() }

    fun hinzufuegen(e: KatalogEintrag, update: Boolean) = viewModelScope.launch {
        arbeit.value += e.id
        repo.hinzufuegen(e)
            .onSuccess { meldungen.send(if (update) "„${e.titel}“ aktualisiert. Dein Fortschritt bleibt." else "„${e.titel}“ hinzugefügt.") }
            .onFailure { meldungen.send(it.message ?: "Fehler") }
        arbeit.value -= e.id
    }

    fun entfernen(e: KatalogEintrag, loeschen: Boolean) = viewModelScope.launch {
        repo.entfernen(e.id, loeschen)
        meldungen.send(if (loeschen) "Entfernt, Fortschritt gelöscht." else "Entfernt, Fortschritt bleibt gespeichert.")
    }
}

fun NavGraphBuilder.katalogBildschirm(onThema: (String) -> Unit) {
    composable<KatalogRoute> { KatalogScreen(onThema) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KatalogScreen(onThema: (String) -> Unit, vm: KatalogViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var entfernen by remember { mutableStateOf<KatalogEintrag?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.meldung.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Katalog") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (ui.laeuft) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    IconButton(onClick = vm::synchronisieren, enabled = !ui.laeuft) { Icon(Icons.Filled.Refresh, "Synchronisieren", tint = Lernen.farben.text2) }
                },
            )
        },
    ) { innen ->
        Box(Modifier.fillMaxSize().padding(innen), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp)) {
                Grau(syncText(ui))
                ui.fehler?.let { Text(it, color = Lernen.farben.schlecht, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp)) }
                val faecherImKatalog = ui.zeilen.map { it.eintrag.fach }.toSet()
                val chips = ui.faecher.filter { it.id in faecherImKatalog || ui.zeilen.isEmpty() }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Alle") })
                    chips.forEach { f -> FilterChip(selected = filter == f.id, onClick = { filter = f.id }, label = { Text(f.name) }) }
                }
                val liste = ui.zeilen.filter { filter == null || it.eintrag.fach == filter }
                if (liste.isEmpty()) {
                    Karte {
                        Text(if (ui.zeilen.isEmpty()) "Noch kein Katalog geladen" else "Keine Themen in diesem Fach", fontWeight = FontWeight.Medium)
                        Grau(if (ui.zeilen.isEmpty()) "Tippe oben auf ↻, um die Themen von GitHub zu laden (Internet nötig)." else "Neue Themen kommen über das Paket-Repo dazu.")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(liste, key = { it.eintrag.id }) { z ->
                        Zeile(z, ui.faecher.firstOrNull { it.id == z.eintrag.fach }, z.eintrag.id in ui.arbeitetAn,
                            onHinzufuegen = { vm.hinzufuegen(z.eintrag, z.status == PaketStatus.UPDATE) },
                            onEntfernen = { entfernen = z.eintrag },
                            onOeffnen = { onThema(z.eintrag.id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    entfernen?.let { e ->
        AlertDialog(
            onDismissRequest = { entfernen = null },
            title = { Text("„${e.titel}“ entfernen?") },
            text = { Text("Soll dein Fortschritt (Haken, Tipps, Übungsergebnisse) behalten oder gelöscht werden? Behaltener Fortschritt ist wieder da, wenn du das Thema später erneut hinzufügst.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { vm.entfernen(e, false); entfernen = null }) { Text("Fortschritt behalten") }
                    TextButton(onClick = { vm.entfernen(e, true); entfernen = null }) { Text("Fortschritt löschen", color = Lernen.farben.schlecht) }
                    TextButton(onClick = { entfernen = null }) { Text("Abbrechen") }
                }
            },
        )
    }
}

@Composable
private fun Zeile(z: KatalogZeile, fach: Fach?, arbeitet: Boolean, onHinzufuegen: () -> Unit, onEntfernen: () -> Unit, onOeffnen: () -> Unit) {
    val e = z.eintrag
    Karte(onClick = if (z.status != PaketStatus.NEU && e.typ != "formelsammlung") onOeffnen else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (e.typ == "formelsammlung") Abzeichen("Werkzeug") else FachPille(fach ?: Fach(e.fach, e.fach.replaceFirstChar { it.uppercase() }, "#888888"))
                Text(e.titel, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                Grau(
                    when (z.status) {
                        PaketStatus.NEU -> listOf(e.stufe.removePrefix("Realschule Bayern · "), "Version ${e.version}").filter { it.isNotBlank() }.joinToString(" · ")
                        PaketStatus.HINZUGEFUEGT -> "hinzugefügt · Version ${e.version}"
                        PaketStatus.UPDATE -> "Version ${e.version} verfügbar (du hast ${z.installierteVersion})"
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                arbeitet -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                z.status == PaketStatus.NEU -> Button(onClick = onHinzufuegen) { Text("Hinzufügen") }
                z.status == PaketStatus.UPDATE -> Button(onClick = onHinzufuegen) { Text("Update") }
                else -> Column(horizontalAlignment = Alignment.End) {
                    Abzeichen("✓ hinzugefügt", Lernen.farben.ok)
                    TextButton(onClick = onEntfernen) { Text("Entfernen", color = Lernen.farben.text2) }
                }
            }
        }
    }
}

private fun syncText(ui: KatalogUi): String {
    val n = ui.zeilen.count { it.eintrag.typ != "formelsammlung" }
    if (ui.letzterSync == 0L) return if (ui.laeuft) "Synchronisiere …" else "Noch nie synchronisiert · Quelle: ${ui.quelle}"
    return "Zuletzt synchronisiert ${zeitText(ui.letzterSync)} · $n Themen auf GitHub"
}

internal fun zeitText(ms: Long): String {
    val jetzt = Calendar.getInstance(); val c = Calendar.getInstance().apply { timeInMillis = ms }
    val uhr = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(ms))
    val tage = (jetzt.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR)) + 365 * (jetzt.get(Calendar.YEAR) - c.get(Calendar.YEAR))
    return when (tage) {
        0 -> "heute $uhr"
        1 -> "gestern $uhr"
        else -> SimpleDateFormat("d.M.yyyy HH:mm", Locale.GERMANY).format(Date(ms))
    }
}
