package de.lernen.feature.thema

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Route zu einem Thema. [tab] und [schrittId] sind optional (sonst: letzte Position). */
@Serializable
data class ThemaRoute(val paketId: String, val tab: String? = null, val schrittId: String? = null)

enum class ThemaTab(val schluessel: String, val titel: String) {
    UEBERBLICK("ueberblick", "Überblick"),
    BEGLEITER("begleiter", "Begleiter"),
    UEBEN("ueben", "Üben"),
    MERKZETTEL("merkzettel", "Merkzettel");

    companion object {
        fun von(s: String?) = entries.firstOrNull { it.schluessel == s }
    }
}

fun NavController.oeffneThema(paketId: String, tab: ThemaTab? = null) =
    navigate(ThemaRoute(paketId, tab?.schluessel)) { launchSingleTop = true }

fun NavGraphBuilder.themaBildschirm(onZurueck: () -> Unit) {
    composable<ThemaRoute> { ThemaScreen(onZurueck = onZurueck) }
}
