package de.lernen.core.design.komponenten

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Eigene Symbole, die in material-icons-core fehlen. */
object Symbole {
    /** Sonne für „Bildschirm anlassen“. */
    val Sonne: ImageVector by lazy {
        ImageVector.Builder("Sonne", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                // Kreis Mitte (12,12), r = 4.5
                moveTo(16.5f, 12f)
                arcTo(4.5f, 4.5f, 0f, true, true, 7.5f, 12f)
                arcTo(4.5f, 4.5f, 0f, true, true, 16.5f, 12f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 2f); lineTo(12f, 4f)
                moveTo(12f, 20f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(4f, 12f)
                moveTo(20f, 12f); lineTo(22f, 12f)
                moveTo(4.9f, 4.9f); lineTo(6.3f, 6.3f)
                moveTo(17.7f, 17.7f); lineTo(19.1f, 19.1f)
                moveTo(4.9f, 19.1f); lineTo(6.3f, 17.7f)
                moveTo(17.7f, 6.3f); lineTo(19.1f, 4.9f)
            }
        }.build()
    }

    /** Stapel für „Katalog“. */
    val Katalog: ImageVector by lazy {
        ImageVector.Builder("Katalog", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 10f); lineTo(4f, 10f); close()
                moveTo(4f, 14f); lineTo(20f, 14f); lineTo(20f, 19f); lineTo(4f, 19f); close()
            }
        }.build()
    }

    /** Werkzeug-Kacheln. */
    val Kacheln: ImageVector by lazy {
        ImageVector.Builder("Kacheln", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
                moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
                moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
                moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
            }
        }.build()
    }

    /** Sprechblase mit Fragezeichen für „Fragen“. */
    val Frage: ImageVector by lazy {
        ImageVector.Builder("Frage", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(10f, 16f); lineTo(6f, 20f); lineTo(6f, 16f); lineTo(4f, 16f); close()
                moveTo(10f, 9f); quadTo(10f, 7.5f, 12f, 7.5f); quadTo(14f, 7.5f, 14f, 9f); quadTo(14f, 10.3f, 12f, 11f); lineTo(12f, 12f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.9f, 14f); arcTo(0.9f, 0.9f, 0f, true, true, 11.1f, 14f); arcTo(0.9f, 0.9f, 0f, true, true, 12.9f, 14f); close()
            }
        }.build()
    }
}
