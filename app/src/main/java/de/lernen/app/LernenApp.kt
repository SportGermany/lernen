package de.lernen.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.lernen.core.design.komponenten.Symbole
import de.lernen.core.design.theme.Lernen
import de.lernen.feature.einstellungen.EinstellungenRoute
import de.lernen.feature.einstellungen.einstellungenBildschirm
import de.lernen.feature.faecher.FaecherRoute
import de.lernen.feature.faecher.faecherBildschirm
import de.lernen.feature.fragen.FragenRoute
import de.lernen.feature.fragen.fragenBildschirm
import de.lernen.feature.katalog.KatalogRoute
import de.lernen.feature.katalog.katalogBildschirm
import de.lernen.feature.start.StartRoute
import de.lernen.feature.start.startBildschirm
import de.lernen.feature.thema.ThemaTab
import de.lernen.feature.thema.oeffneThema
import de.lernen.feature.thema.themaBildschirm
import de.lernen.feature.werkzeuge.WerkzeugeRoute
import de.lernen.feature.werkzeuge.werkzeugeBildschirm
import kotlin.reflect.KClass

/** Die fünf Punkte der unteren Leiste (auf dem Tablet quer: seitliche Leiste). */
private enum class Ziel(val titel: String, val symbol: ImageVector, val route: Any, val klasse: KClass<*>) {
    START("Start", Icons.Outlined.Home, StartRoute, StartRoute::class),
    FAECHER("Fächer", Icons.AutoMirrored.Outlined.List, FaecherRoute(), FaecherRoute::class),
    FRAGEN("Fragen", Symbole.Frage, FragenRoute, FragenRoute::class),
    WERKZEUGE("Werkzeuge", Symbole.Kacheln, WerkzeugeRoute, WerkzeugeRoute::class),
    KATALOG("Katalog", Symbole.Katalog, KatalogRoute, KatalogRoute::class),
}

private fun NavHostController.oben(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun LernenApp() {
    val nav = rememberNavController()
    val eintrag by nav.currentBackStackEntryAsState()
    val ziel = eintrag?.destination

    NavigationSuiteScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Lernen.farben.karte,
            navigationRailContainerColor = Lernen.farben.karte,
        ),
        navigationSuiteItems = {
            Ziel.entries.forEach { z ->
                val an = ziel?.hierarchy?.any { it.hasRoute(z.klasse) } == true
                item(
                    selected = an,
                    onClick = { nav.oben(z.route) },
                    icon = { Icon(z.symbol, contentDescription = null) },
                    label = { Text(z.titel) },
                )
            }
        },
    ) {
        NavHost(nav, startDestination = StartRoute) {
            startBildschirm(
                onThema = { nav.oeffneThema(it) },
                onWiederholen = { nav.oeffneThema(it, ThemaTab.UEBEN) },
                onFach = { nav.oben(FaecherRoute(it)) },
                onEinstellungen = { nav.navigate(EinstellungenRoute) },
            )
            faecherBildschirm(onThema = { nav.oeffneThema(it) }, onKatalog = { nav.oben(KatalogRoute) })
            themaBildschirm(onZurueck = { nav.popBackStack() })
            katalogBildschirm(onThema = { nav.oeffneThema(it) })
            fragenBildschirm()
            werkzeugeBildschirm()
            einstellungenBildschirm(BuildConfig.VERSION_NAME, onZurueck = { nav.popBackStack() })
        }
    }
}
