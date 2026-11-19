package de.lernen.feature.katalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.lernen.core.design.komponenten.KommtSpaeter
import kotlinx.serialization.Serializable

@Serializable
data object KatalogRoute

fun NavGraphBuilder.katalogBildschirm() {
    composable<KatalogRoute> { KatalogScreen() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KatalogScreen() {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Katalog") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            KommtSpaeter("Kommt in Meilenstein 2", "Hier erscheinen alle Themen aus dem Paket-Repository auf GitHub. Du fügst sie einzeln hinzu, Updates werden angezeigt. Bis dahin sind drei Deutsch-Themen schon eingebaut.", Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp))
        }
    }
}
