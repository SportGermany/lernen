package de.lernen.core.design.komponenten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.model.WortGruppe
import de.lernen.core.design.theme.Lernen

/** Ein Punkt der Abhakliste. Abgehakte Punkte werden grau und durchgestrichen. */
@Composable
fun AbhakZeile(text: String, abgehakt: Boolean, onUmschalten: () -> Unit, letzte: Boolean = false) {
    val f = Lernen.farben
    val primaer = MaterialTheme.colorScheme.primary
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox, onClick = onUmschalten)
                .semantics { stateDescription = if (abgehakt) "abgehakt" else "offen" }
                .padding(vertical = 11.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .padding(top = 1.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (abgehakt) primaer else Color.Transparent)
                    .border(2.dp, if (abgehakt) primaer else f.text2, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (abgehakt) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (abgehakt) f.text2 else f.text,
                textDecoration = if (abgehakt) TextDecoration.LineThrough else null,
            )
        }
        if (!letzte) HorizontalDivider(color = f.ton)
    }
}

data class SchrittChip(val kurz: String, val fertig: Boolean)

/** Horizontale Schritt-Leiste: aktiver Schritt hervorgehoben, fertige mit ✓. */
@Composable
fun SchrittLeiste(schritte: List<SchrittChip>, aktiv: Int, onWahl: (Int) -> Unit, modifier: Modifier = Modifier) {
    val f = Lernen.farben
    val primaer = MaterialTheme.colorScheme.primary
    val state = rememberLazyListState()
    LaunchedEffect(aktiv) { if (aktiv in schritte.indices) state.animateScrollToItem((aktiv - 1).coerceAtLeast(0)) }
    LazyRow(
        modifier.fillMaxWidth(), state = state,
        horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        itemsIndexed(schritte) { i, s ->
            val an = i == aktiv
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(f.karte)
                    .border(1.dp, if (an) primaer else f.rand, RoundedCornerShape(10.dp))
                    .clickable { onWahl(i) }
                    .semantics { contentDescription = "Schritt ${i + 1}: ${s.kurz}" + if (s.fertig) ", fertig" else "" }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (hg, vg) = when {
                    s.fertig -> f.ok to (if (f.dunkel) Color(0xFF0F1C2B) else Color.White)
                    an -> primaer to MaterialTheme.colorScheme.onPrimary
                    else -> f.ton to f.text2
                }
                Box(Modifier.size(20.dp).clip(CircleShape).background(hg), contentAlignment = Alignment.Center) {
                    if (s.fertig) Icon(Icons.Filled.Check, null, tint = vg, modifier = Modifier.size(13.dp))
                    else Text("${i + 1}", fontSize = 11.sp, color = vg, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    s.kurz, fontSize = 13.sp,
                    color = if (an) f.text else f.text2,
                    fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Wortkiste als Bottom-Sheet mit Suche. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WortkisteSheet(gruppen: List<WortGruppe>, onSchliessen: () -> Unit) {
    var suche by rememberSaveable { mutableStateOf("") }
    val f = Lernen.farben
    val gefiltert = remember(gruppen, suche) { filtereWortkiste(gruppen, suche) }
    ModalBottomSheet(onDismissRequest = onSchliessen, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Wortkiste", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
            OutlinedTextField(
                value = suche, onValueChange = { suche = it },
                placeholder = { Text("Suchen, z. B. Angst oder gehen") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true, shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            if (gefiltert.isEmpty()) Grau("Nichts gefunden.")
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth()) {
                items(gefiltert.size) { gi ->
                    val g = gefiltert[gi]
                    Column(Modifier.padding(bottom = 14.dp)) {
                        KartenTitel(g.gruppe)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            g.woerter.forEach { w ->
                                Text(
                                    w, fontSize = 14.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(f.ton).padding(horizontal = 9.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Filtert Gruppen: passt der Gruppenname, bleibt die ganze Gruppe; sonst nur passende Wörter. */
fun filtereWortkiste(gruppen: List<WortGruppe>, suche: String): List<WortGruppe> {
    val q = suche.trim().lowercase()
    if (q.isEmpty()) return gruppen
    return gruppen.mapNotNull { g ->
        if (g.gruppe.lowercase().contains(q)) g
        else g.woerter.filter { it.lowercase().contains(q) }.takeIf { it.isNotEmpty() }?.let { g.copy(woerter = it) }
    }
}
