package de.lernen.core.design.komponenten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.paket.HtmlMini
import de.lernen.core.data.paket.HtmlMini.Markierung
import de.lernen.core.design.theme.LernenFarben
import de.lernen.core.design.theme.LocalLernenFarben

/** Hintergrundfarbe einer Textmarkierung. */
fun LernenFarben.marke(m: Markierung): Color = when (m) {
    Markierung.SINNE -> markeSinne
    Markierung.GEFUEHL -> markeGefuehl
    Markierung.WORTWAHL -> markeWortwahl
    Markierung.SATZBAU -> markeSatzbau
    Markierung.KORREKTURZEICHEN -> schlecht
}

/** Wandelt einen Absatz aus [HtmlMini] in einen AnnotatedString (kein WebView). */
fun HtmlMini.Absatz.zuAnnotated(farben: LernenFarben, markierungen: Boolean): AnnotatedString = buildAnnotatedString {
    for (s in stuecke) {
        var stil = SpanStyle(fontWeight = if (s.fett) FontWeight.SemiBold else null)
        when (val m = s.markierung) {
            null -> Unit
            Markierung.KORREKTURZEICHEN -> stil = stil.merge(
                SpanStyle(
                    background = farben.schlecht, color = if (farben.dunkel) Color(0xFF3A1F1D) else Color.White,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 12.sp,
                )
            )
            else -> if (markierungen) stil = stil.merge(SpanStyle(background = farben.marke(m)))
        }
        val text = if (s.markierung == Markierung.KORREKTURZEICHEN) " ${s.text} " else s.text
        withStyle(stil) { append(text) }
    }
}

/**
 * Zeigt Paket-HTML (`<p>`, `<b>`, `<br>`, Markierungen) als Compose-Text an.
 * Jeder `<p>` wird ein eigener Absatz.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    stil: TextStyle = TextStyle.Default,
    markierungen: Boolean = true,
    farbe: Color = Color.Unspecified,
) {
    val farben = LocalLernenFarben.current
    val absaetze = remember(html) { HtmlMini.lies(html) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        absaetze.forEach { a ->
            Text(a.zuAnnotated(farben, markierungen), style = stil, color = farbe)
        }
    }
}
