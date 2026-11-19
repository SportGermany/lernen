package de.lernen.feature.werkzeuge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.design.theme.LernenTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class WerkzeugBilderTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    @Composable
    private fun Rahmen(inhalt: @Composable () -> Unit) = LernenTheme(Darstellung.HELL) {
        Surface(color = MaterialTheme.colorScheme.background) { Box(Modifier.fillMaxSize().padding(14.dp)) { inhalt() } }
    }

    @Test fun mitrechnenMathe() = paparazzi.snapshot { Rahmen { MatheMitrechnen(listOf("y = 2x − 1", "3x + 2y = 12"), emptyList()) } }

    @Test fun plotter() = paparazzi.snapshot { Rahmen { Plotter("0,5x² − 2") } }

    @Test fun mitrechnenPhysik() {
        val p = PaketParser.liesOderWirf(File("../core-data/src/test/resources/physik-elektrische-leistung.json").readText())
        val fs = PaketParser.liesOderWirf(File("../core-data/src/main/assets/startpakete/werkzeuge-formelsammlung.json").readText())
        val d = p.begleiter.first { it.mitrechnen != null }.mitrechnen
        val fo = fs.gebiete.flatMap { it.formeln }.first { it.id == d!!.formel }
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 2600))
        paparazzi.snapshot { Rahmen { PhysikMitrechnen(MitrechnenVorgabe(d, fo, "Einsetzen und ausrechnen")) } }
    }
}
