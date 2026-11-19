package de.lernen.feature.werkzeuge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.repo.WerkzeugDaten
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.komponenten.filtereWortkiste
import de.lernen.core.design.theme.Lernen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DeutschWerkzeugViewModel @Inject constructor(daten: WerkzeugDaten) : ViewModel() {
    val wortkiste = daten.wortkiste.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val korrektur = daten.korrekturzeichen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WortkisteWerkzeug(vm: DeutschWerkzeugViewModel = hiltViewModel()) {
    val gruppen by vm.wortkiste.collectAsStateWithLifecycle()
    var suche by rememberSaveable { mutableStateOf("") }
    val gefiltert = remember(gruppen, suche) { filtereWortkiste(gruppen, suche) }
    val f = Lernen.farben
    Column {
        OutlinedTextField(
            value = suche, onValueChange = { suche = it }, singleLine = true, shape = CircleShape,
            placeholder = { Text("Suchen, z. B. Angst oder sagen") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        if (gefiltert.isEmpty()) Grau("Nichts gefunden.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(gefiltert, key = { it.gruppe }) { g ->
                Column {
                    KartenTitel(g.gruppe)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        g.woerter.forEach { w ->
                            Text(w, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(f.ton).padding(horizontal = 9.dp, vertical = 5.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun KorrekturWerkzeug(vm: DeutschWerkzeugViewModel = hiltViewModel()) {
    val zeichen by vm.korrektur.collectAsStateWithLifecycle()
    val f = Lernen.farben
    LazyColumn {
        item {
            Karte {
                zeichen.forEachIndexed { i, k ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.width(52.dp)) {
                            Text(
                                k.zeichen, color = if (f.dunkel) Color(0xFF3A1F1D) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.widthIn(min = 34.dp).clip(RoundedCornerShape(6.dp)).background(f.schlecht).padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(k.bedeutung, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            if (k.beispiel.isNotBlank()) Grau(k.beispiel)
                        }
                    }
                    if (i < zeichen.lastIndex) HorizontalDivider(color = f.ton)
                }
                Spacer(Modifier.height(6.dp))
                Grau("Die Zeichen können bei deiner Lehrkraft abweichen.")
            }
        }
    }
}
