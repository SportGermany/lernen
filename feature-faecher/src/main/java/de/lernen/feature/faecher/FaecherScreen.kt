package de.lernen.feature.faecher

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Paket
import de.lernen.core.data.repo.FachMitThemen
import de.lernen.core.data.repo.UebersichtRepository
import de.lernen.core.design.komponenten.Abzeichen
import de.lernen.core.design.komponenten.Balken
import de.lernen.core.design.komponenten.FachPunkt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.theme.fachFarbe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** [fachId]: optional, scrollt zu diesem Fach. */
@Serializable
data class FaecherRoute(val fachId: String? = null)

@HiltViewModel
class FaecherViewModel @Inject constructor(repo: UebersichtRepository) : ViewModel() {
    val uebersicht = repo.uebersicht.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

fun NavGraphBuilder.faecherBildschirm(onThema: (String) -> Unit, onKatalog: () -> Unit) {
    composable<FaecherRoute> { e -> FaecherScreen(e.toRoute<FaecherRoute>().fachId, onThema, onKatalog) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaecherScreen(fachId: String?, onThema: (String) -> Unit, onKatalog: () -> Unit, vm: FaecherViewModel = hiltViewModel()) {
    val u by vm.uebersicht.collectAsStateWithLifecycle()
    val liste = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Fächer") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        val faecher = u?.faecher ?: return@Column
        LaunchedEffect(fachId, faecher.size) {
            val i = faecher.indexOfFirst { it.fach.id == fachId }
            if (i > 0) liste.scrollToItem(i)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp),
                state = liste,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(faecher, key = { it.fach.id }) { FachAbschnitt(it, onThema, onKatalog) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FachAbschnitt(fm: FachMitThemen, onThema: (String) -> Unit, onKatalog: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FachPunkt(fm.fach)
            Spacer(Modifier.width(10.dp))
            Text(fm.fach.name, style = MaterialTheme.typography.titleMedium)
        }
        if (fm.themen.isEmpty()) {
            Karte {
                Grau("Noch keine Themen.")
                TextButton(onClick = onKatalog, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Im Katalog hinzufügen") }
            }
        }
        fm.themen.forEach { t ->
            val p = t.paket
            Karte(onClick = { onThema(p.id) }) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(p.titel, fontWeight = FontWeight.Medium)
                        Grau(listOf(if (p.typ == Paket.TYP_ARBEITSHEFT) "Arbeitsheft" else begleitWeg(fm.fach.begleiterTyp), "${p.begleiter.size} Schritte", p.stufe).filter { it.isNotBlank() }.joinToString(" · "))
                    }
                    Spacer(Modifier.width(8.dp))
                    Abzeichen("${t.fortschritt.erledigt}/${t.fortschritt.gesamt}")
                }
                Spacer(Modifier.height(10.dp))
                Balken(t.fortschritt.anteil, fachFarbe(fm.fach))
            }
        }
    }
}

private fun begleitWeg(typ: String) = when (typ) {
    "rechnen" -> "Rechnen"
    "physik" -> "Physik"
    else -> "Schreiben"
}
