package de.lernen.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.lernen.core.data.model.Darstellung

/** Schrift für Lesetexte (Muster, Beispiele) – wie "--read" in der Konzept-HTML. */
val LeseSchrift: FontFamily = FontFamily.Serif

private val Typo = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 21.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        bodyLarge = t.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = t.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = t.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    )
}

/** Lesetext: Serifenschrift, etwas größer und mit mehr Zeilenabstand. */
val LeseStil = TextStyle(fontFamily = LeseSchrift, fontSize = 16.sp, lineHeight = 26.sp)

@Composable
fun LernenTheme(darstellung: Darstellung = Darstellung.SYSTEM, content: @Composable () -> Unit) {
    val dunkel = when (darstellung) {
        Darstellung.SYSTEM -> isSystemInDarkTheme()
        Darstellung.HELL -> false
        Darstellung.DUNKEL -> true
    }
    // Bewusst kein Dynamic Color: eigene, gedeckte Fachfarben.
    CompositionLocalProvider(LocalLernenFarben provides if (dunkel) DunkleFarben else HelleFarben) {
        MaterialTheme(
            colorScheme = if (dunkel) DunklesSchema else HellesSchema,
            typography = Typo,
            content = content,
        )
    }
}

object Lernen {
    val farben: LernenFarben
        @Composable get() = LocalLernenFarben.current
}
