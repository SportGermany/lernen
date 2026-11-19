package de.lernen.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import de.lernen.core.data.model.Fach

/** Zusätzliche Farben, die Material 3 nicht kennt (aus der Konzept-HTML). */
@Immutable
data class LernenFarben(
    val hintergrund: Color,
    val karte: Color,
    val ton: Color,
    val rand: Color,
    val text: Color,
    val text2: Color,
    val ok: Color,
    val okTon: Color,
    val schlecht: Color,
    val schlechtTon: Color,
    val warnung: Color,
    val warnungTon: Color,
    val infoTon: Color,
    val markeSinne: Color,
    val markeGefuehl: Color,
    val markeWortwahl: Color,
    val markeSatzbau: Color,
    val dunkel: Boolean,
)

val HelleFarben = LernenFarben(
    hintergrund = Color(0xFFF6F7F9), karte = Color(0xFFFFFFFF), ton = Color(0xFFEEF1F5), rand = Color(0xFFE0E4EA),
    text = Color(0xFF1A1F26), text2 = Color(0xFF5D6570),
    ok = Color(0xFF2E8656), okTon = Color(0xFFE8F4ED), schlecht = Color(0xFFB3261E), schlechtTon = Color(0xFFFBE9E7),
    warnung = Color(0xFF7A5200), warnungTon = Color(0xFFFFF4E0), infoTon = Color(0xFFE9F0FA),
    markeSinne = Color(0xFFDCE9FB), markeGefuehl = Color(0xFFFBE1DA), markeWortwahl = Color(0xFFDCF1E3), markeSatzbau = Color(0xFFF8EBC9),
    dunkel = false,
)

val DunkleFarben = LernenFarben(
    hintergrund = Color(0xFF111418), karte = Color(0xFF1A1F25), ton = Color(0xFF252C35), rand = Color(0xFF2F3742),
    text = Color(0xFFE6E9EE), text2 = Color(0xFF9AA3AE),
    ok = Color(0xFF7FCF9E), okTon = Color(0xFF1B2E23), schlecht = Color(0xFFF2A49C), schlechtTon = Color(0xFF3A1F1D),
    warnung = Color(0xFFE9C77F), warnungTon = Color(0xFF33291A), infoTon = Color(0xFF1E2A3A),
    markeSinne = Color(0xFF203247), markeGefuehl = Color(0xFF3D2621), markeWortwahl = Color(0xFF1F3A2A), markeSatzbau = Color(0xFF3B311C),
    dunkel = true,
)

internal val HellesSchema: ColorScheme = lightColorScheme(
    primary = Color(0xFF22364D), onPrimary = Color.White,
    primaryContainer = HelleFarben.ton, onPrimaryContainer = Color(0xFF22364D),
    secondary = Color(0xFF22364D), onSecondary = Color.White,
    secondaryContainer = HelleFarben.ton, onSecondaryContainer = Color(0xFF22364D),
    background = HelleFarben.hintergrund, onBackground = HelleFarben.text,
    surface = HelleFarben.hintergrund, onSurface = HelleFarben.text,
    surfaceVariant = HelleFarben.ton, onSurfaceVariant = HelleFarben.text2,
    surfaceContainerLowest = HelleFarben.karte, surfaceContainerLow = HelleFarben.karte,
    surfaceContainer = HelleFarben.karte, surfaceContainerHigh = HelleFarben.karte, surfaceContainerHighest = HelleFarben.ton,
    outline = HelleFarben.rand, outlineVariant = HelleFarben.ton,
    error = HelleFarben.schlecht, onError = Color.White,
    inverseSurface = HelleFarben.text, inverseOnSurface = HelleFarben.hintergrund,
)

internal val DunklesSchema: ColorScheme = darkColorScheme(
    primary = Color(0xFFA9C4E6), onPrimary = Color(0xFF0F1C2B),
    primaryContainer = DunkleFarben.ton, onPrimaryContainer = Color(0xFFA9C4E6),
    secondary = Color(0xFFA9C4E6), onSecondary = Color(0xFF0F1C2B),
    secondaryContainer = DunkleFarben.ton, onSecondaryContainer = Color(0xFFA9C4E6),
    background = DunkleFarben.hintergrund, onBackground = DunkleFarben.text,
    surface = DunkleFarben.hintergrund, onSurface = DunkleFarben.text,
    surfaceVariant = DunkleFarben.ton, onSurfaceVariant = DunkleFarben.text2,
    surfaceContainerLowest = DunkleFarben.karte, surfaceContainerLow = DunkleFarben.karte,
    surfaceContainer = DunkleFarben.karte, surfaceContainerHigh = DunkleFarben.karte, surfaceContainerHighest = DunkleFarben.ton,
    outline = DunkleFarben.rand, outlineVariant = DunkleFarben.ton,
    error = DunkleFarben.schlecht, onError = Color(0xFF3A1F1D),
    inverseSurface = DunkleFarben.text, inverseOnSurface = DunkleFarben.hintergrund,
)

val LocalLernenFarben = staticCompositionLocalOf { HelleFarben }

/** Wandelt "#RRGGBB" in eine Farbe; bei Unsinn Grau. */
fun farbeAusHex(hex: String): Color = runCatching {
    val h = hex.trim().removePrefix("#")
    val v = h.toLong(16)
    if (h.length == 6) Color(0xFF000000 or v) else Color(v)
}.getOrDefault(Color(0xFF888888))

/** Kräftige Fachfarbe (Text, Balken, Rand). */
@Composable
fun fachFarbe(fach: Fach?): Color {
    val f = LocalLernenFarben.current
    if (fach == null) return f.text2
    return farbeAusHex(if (f.dunkel) fach.farbeDunkel else fach.farbe)
}

/** Heller Ton der Fachfarbe (Hintergrund für Punkt und Pille). */
@Composable
fun fachTon(fach: Fach?): Color {
    val f = LocalLernenFarben.current
    return lerp(f.karte, fachFarbe(fach), if (f.dunkel) 0.18f else 0.11f)
}
