package de.lernen.feature.fragen

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
data object FragenRoute

fun NavGraphBuilder.fragenBildschirm() {
    composable<FragenRoute> { FragenScreen() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragenScreen() {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Fragen") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            KommtSpaeter("Kommt in Meilenstein 4", "Hier kannst du später eine Frage an Claude stellen. Die App legt sie als Datei ab und gibt dir einen Befehl zum Kopieren für einen neuen Claude-Chat.", Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp))
        }
    }
}
