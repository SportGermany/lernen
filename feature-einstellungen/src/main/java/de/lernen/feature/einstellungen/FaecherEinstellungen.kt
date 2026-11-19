package de.lernen.feature.einstellungen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.lernen.core.data.model.Fach
import de.lernen.core.design.komponenten.FachPunkt
import de.lernen.core.design.theme.farbeAusHex

/** Gedeckte Farben zur Auswahl (keine grellen Töne). */
private val FARBEN = listOf(
    "#C0532F", "#2F6CB3", "#2E8656", "#B07D0C", "#7A4E9C", "#1F7A8C", "#A23B5C", "#5B6B2E", "#6B5B4B", "#3F51B5",
)

@Composable
internal fun FaecherAbschnitt(
    faecher: List<Fach>,
    ausgeblendet: Set<String>,
    eigeneFarben: Map<String, String>,
    onSichtbar: (String, Boolean) -> Unit,
    onReihenfolge: (List<String>) -> Unit,
    onFarbe: (String, String?) -> Unit,
) {
    var farbeFuer by remember { mutableStateOf<Fach?>(null) }
    faecher.forEachIndexed { i, f ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { farbeFuer = f }) { FachPunkt(f, 34.dp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name)
                Text("Farbe ändern: Punkt antippen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onReihenfolge(faecher.map { it.id }.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, "${f.name} nach oben")
            }
            IconButton(onClick = { onReihenfolge(faecher.map { it.id }.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < faecher.lastIndex) {
                Icon(Icons.Filled.KeyboardArrowDown, "${f.name} nach unten")
            }
            Switch(checked = f.id !in ausgeblendet, onCheckedChange = { onSichtbar(f.id, it) })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
    farbeFuer?.let { f -> FarbDialog(f, f.id in eigeneFarben, { farbeFuer = null }) { onFarbe(f.id, it); farbeFuer = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FarbDialog(f: Fach, hatEigene: Boolean, onAbbrechen: () -> Unit, onWahl: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text("Farbe für ${f.name}") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FARBEN.forEach { hex ->
                    val an = hex.equals(f.farbe, ignoreCase = true)
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(farbeAusHex(hex))
                            .then(if (an) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { onWahl(hex) }
                    )
                }
            }
        },
        confirmButton = { if (hatEigene) TextButton(onClick = { onWahl(null) }) { Text("Standardfarbe") } },
        dismissButton = { TextButton(onClick = onAbbrechen) { Text("Abbrechen") } },
    )
}
