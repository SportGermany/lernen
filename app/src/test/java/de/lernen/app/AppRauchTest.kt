package de.lernen.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Startet die echte App (Hilt, Room, DataStore, Navigation) auf der JVM und klickt sich durch:
 * Start → Weitermachen → Thema → Begleiter → Haken setzen → Fächer → Merkzettel.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "de-rDE-w411dp-h891dp")
class AppRauchTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun warteAuf(text: String) = try {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    } catch (e: Throwable) {
        println(compose.onRoot().printToString(maxDepth = 30))
        throw AssertionError("Text nicht gefunden: $text", e)
    }

    @Test
    fun durchDieAppKlicken() {
        // Start: Startpakete wurden beim ersten Start eingerichtet
        warteAuf("Neu hinzugefügt")
        compose.onNodeWithText("Deine Fächer").assertIsDisplayed()
        compose.onNodeWithText("3 Themen").assertIsDisplayed()

        // Thema öffnen → Überblick
        compose.onAllNodesWithText("Neu hinzugefügt", substring = true).onFirst().performClick()
        warteAuf("Überblick")
        compose.onNodeWithText("Begleiter").performClick()

        // Begleiter: ersten Punkt abhaken
        warteAuf("Muss rein")
        compose.onNodeWithText("0/5").assertIsDisplayed()
        compose.onNodeWithText("Aufgabe genau gelesen", substring = true).performClick()
        warteAuf("1/5")

        // Merkzettel
        compose.onNodeWithText("Merkzettel").performClick()
        warteAuf("Einen einzigen Moment")

        // Zurück zum Start: jetzt „Weitermachen“ mit gespeichertem Haken
        compose.onNodeWithContentDescription("Zurück").performClick()
        warteAuf("Weitermachen")
        warteAuf("1 von 42 abgehakt")

        // Fächer: alle mitgelieferten Themen sind da
        compose.onAllNodes(hasText("Fächer")).onFirst().performClick()
        warteAuf("Gleichungssysteme")
        compose.onAllNodesWithText("Schilderung", substring = true).onFirst().assertIsDisplayed()
    }
}
