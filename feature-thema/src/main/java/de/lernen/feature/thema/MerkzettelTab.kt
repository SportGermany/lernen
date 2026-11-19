package de.lernen.feature.thema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KommtSpaeter
import de.lernen.core.design.komponenten.Ueberschrift
import de.lernen.core.design.theme.Lernen

@Composable
internal fun MerkzettelTab(z: ThemaZustand, breit: Boolean) {
    val f = Lernen.farben
    val eintraege = z.paket.merkEintraege
    if (eintraege.isEmpty()) { Grau("Dieses Thema hat keinen Merkzettel."); return }
    Zweispaltig(
        breit,
        links = {
            Karte {
                eintraege.forEachIndexed { i, m ->
                    Row(Modifier.padding(vertical = 10.dp)) {
                        Text(m.stichwort, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(118.dp))
                        Text(m.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    if (i < eintraege.lastIndex) HorizontalDivider(color = f.ton)
                }
            }
        },
        rechts = {
            val kz = z.paket.korrekturZeichen
            if (kz.isNotEmpty()) {
                if (!breit) Ueberschrift("Korrekturzeichen")
                Karte {
                    if (breit) Text("Korrekturzeichen", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                    kz.forEachIndexed { i, k ->
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
                        if (i < kz.lastIndex) HorizontalDivider(color = f.ton)
                    }
                    Spacer(Modifier.height(6.dp))
                    Grau("Die Zeichen können bei deiner Lehrkraft abweichen.")
                }
            }
        },
    )
}

@Composable
internal fun UebenPlatzhalter(z: ThemaZustand) {
    val u = z.paket.uebungen
    KommtSpaeter(
        "Üben kommt in Meilenstein 3",
        "Dieses Thema hat schon ${u.anzahl} Übungen: ${u.einstieg.size} Einstieg, ${u.sicher.size} Sicher, ${u.schulaufgabe.size} Schulaufgabe. " +
            "Sie werden mit dem nächsten Update freigeschaltet.",
    )
}
