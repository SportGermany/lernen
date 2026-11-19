package de.lernen.feature.thema

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.model.Fach
import de.lernen.core.data.paket.Fortschritt
import de.lernen.core.data.paket.HakenSchluessel
import de.lernen.core.data.paket.PaketParser
import de.lernen.core.design.theme.LernenTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Rendert Überblick, Begleiter und Merkzettel als Bilder (build/reports/paparazzi).
 * Dient zur Sichtkontrolle von Handy- und Tablet-Layout ohne Emulator.
 */
class ThemaBilderTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        maxPercentDifference = 0.1,
    )

    private val deutsch = Fach("deutsch", "Deutsch", "#C0532F", "#F0937A", "schreiben")

    private fun zustand(datei: String, tab: ThemaTab, schritt: Int = 0, mitFortschritt: Boolean = true, stufe: String = "einstieg", index: Int = 0): ThemaZustand {
        val p = PaketParser.liesOderWirf((listOf("../core-data/src/main/assets/startpakete/", "../core-data/src/test/resources/").map { File(it + datei) }.first { it.exists() }).readText())
        val haken = if (mitFortschritt) setOf(HakenSchluessel(p.begleiter[0].id, 0), HakenSchluessel(p.begleiter[0].id, 1)) +
            p.begleiter[0].muss.indices.map { HakenSchluessel(p.begleiter[0].id, it) } else emptySet()
        return ThemaZustand(
            p, deutsch, Fortschritt(p, haken),
            tipps = p.begleiter.associate { it.id to 2 },
            antworten = mapOf(p.begleiter[schritt].id to 0),
            schritt = schritt, tab = tab,
            ergebnisse = mapOf("einstieg-0" to de.lernen.core.data.paket.ErgebnisStatus.FALSCH),
            uebenStufe = stufe, uebenIndex = mapOf(stufe to index),
        )
    }

    @Composable
    private fun Rahmen(dunkel: Boolean = false, inhalt: @Composable (Boolean) -> Unit) {
        LernenTheme(if (dunkel) Darstellung.DUNKEL else Darstellung.HELL) {
            Surface(color = MaterialTheme.colorScheme.background) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val breit = maxWidth >= BREIT
                    Column(Modifier.padding(14.dp)) { inhalt(breit) }
                }
            }
        }
    }

    private fun begleiter(z: ThemaZustand, breit: Boolean) =
        @Composable { BegleiterTab(z, breit, {}, { _, _, _ -> }, { _, _, _ -> }, { _, _, _ -> }, {}, {}, {}) }

    @Test fun begleiterArbeitsheftHandy() {
        val z = zustand("deutsch-arbeitsheft-s31-klassensprecherfahrt.json", ThemaTab.BEGLEITER, schritt = 0)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 4200))
        paparazzi.snapshot { Rahmen { begleiter(z, it)() } }
    }

    @Test fun begleiterSchreibenTabletQuer() {
        val z = zustand("deutsch-schilderung-schreiben.json", ThemaTab.BEGLEITER, schritt = 4)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_C.copy(orientation = ScreenOrientation.LANDSCAPE, screenWidth = 2560, screenHeight = 2400))
        paparazzi.snapshot { Rahmen { begleiter(z, it)() } }
    }

    @Test fun begleiterDunkel() {
        val z = zustand("deutsch-schilderung-ueberarbeiten.json", ThemaTab.BEGLEITER, schritt = 1)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 3600, nightMode = NightMode.NIGHT))
        paparazzi.snapshot { Rahmen(dunkel = true) { begleiter(z, it)() } }
    }

    @Test fun ueberblickSchreiben() {
        val z = zustand("deutsch-schilderung-schreiben.json", ThemaTab.UEBERBLICK)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 7000))
        paparazzi.snapshot { Rahmen { UeberblickTab(z, it, {}, {}) } }
    }

    @Test fun ueberblickArbeitsheft() {
        val z = zustand("deutsch-arbeitsheft-s31-klassensprecherfahrt.json", ThemaTab.UEBERBLICK)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 5600))
        paparazzi.snapshot { Rahmen { UeberblickTab(z, it, {}, {}) } }
    }

    @Test fun merkzettelTablet() {
        val z = zustand("deutsch-schilderung-ueberarbeiten.json", ThemaTab.MERKZETTEL)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_C.copy(orientation = ScreenOrientation.LANDSCAPE, screenWidth = 2560, screenHeight = 1800))
        paparazzi.snapshot { Rahmen { MerkzettelTab(z, it) } }
    }

    @Test fun uebenTippen() {
        val z = zustand("deutsch-arbeitsheft-s31-klassensprecherfahrt.json", ThemaTab.UEBEN, stufe = "sicher", index = 0)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 2000))
        paparazzi.snapshot { Rahmen { UebenTab(z, it, {}, {}, { _, _ -> }, {}) } }
    }

    @Test fun uebenTextTablet() {
        val z = zustand("deutsch-schilderung-ueberarbeiten.json", ThemaTab.UEBEN, stufe = "schulaufgabe", index = 1)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_C.copy(orientation = ScreenOrientation.LANDSCAPE, screenWidth = 2560, screenHeight = 1900))
        paparazzi.snapshot { Rahmen { UebenTab(z, it, {}, {}, { _, _ -> }, {}) } }
    }

    @Test fun begleiterEnglisch() {
        val z = zustand("englisch-formal-email.json", ThemaTab.BEGLEITER, schritt = 1, mitFortschritt = false)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 3000))
        paparazzi.snapshot { Rahmen { begleiter(z, it)() } }
    }

    @Test fun begleiterMathe() {
        val z = zustand("mathe-lgs-einsetzungsverfahren.json", ThemaTab.BEGLEITER, schritt = 2, mitFortschritt = false)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenHeight = 3000))
        paparazzi.snapshot { Rahmen { begleiter(z, it)() } }
    }
}
