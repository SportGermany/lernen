package de.lernen.core.design.komponenten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.design.theme.Lernen

/** Segment-Umschalter (Pillenform), z. B. Überblick · Begleiter · Üben · Merkzettel. */
@Composable
fun Segmente(optionen: List<String>, gewaehlt: Int, onWahl: (Int) -> Unit, modifier: Modifier = Modifier) {
    val f = Lernen.farben
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(CircleShape).background(f.karte).border(1.dp, f.rand, CircleShape).selectableGroup()
    ) {
        optionen.forEachIndexed { i, t ->
            val an = i == gewaehlt
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(f.rand))
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (an) f.ton else Color.Transparent)
                    .selectable(selected = an, role = Role.Tab, onClick = { onWahl(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (an) MaterialTheme.colorScheme.primary else f.text2,
                    fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
