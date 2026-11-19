package de.lernen.feature.thema

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/** Route zu einem Thema. [tab] und [schrittId] sind optional (sonst: letzte Position). */
@Serializable
data class ThemaRoute(val paketId: String, val tab: String? = null, val schrittId: String? = null, val stufe: String? = null)

enum class ThemaTab(val schluessel: String, val titel: String) {
    UEBERBLICK("ueberblick", "Überblick"),
    BEGLEITER("begleiter", "Begleiter"),
    UEBEN("ueben", "Üben"),
    MERKZETTEL("merkzettel", "Merkzettel");

    companion object {
        fun von(s: String?) = entries.firstOrNull { it.schluessel == s }
    }
}

fun NavController.oeffneThema(paketId: String, tab: ThemaTab? = null, stufe: String? = null) =
    navigate(ThemaRoute(paketId, tab?.schluessel, stufe = stufe)) { launchSingleTop = true }

/** Üben-Stufe „Wiederholen“. */
const val STUFE_WIEDERHOLEN = "wdh"

fun NavGraphBuilder.themaBildschirm(
    onZurueck: () -> Unit,
    onFrage: (paketId: String, schrittId: String?) -> Unit,
    onMitrechnen: (paketId: String, schrittId: String?, aufgabe: String?) -> Unit,
) {
    composable<ThemaRoute> { e ->
        val id = e.toRoute<ThemaRoute>().paketId
        ThemaScreen(onZurueck = onZurueck, onFrage = { s -> onFrage(id, s) }, onMitrechnen = { s, a -> onMitrechnen(id, s, a) })
    }
}
