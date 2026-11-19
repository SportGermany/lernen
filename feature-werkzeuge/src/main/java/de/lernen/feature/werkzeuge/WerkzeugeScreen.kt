package de.lernen.feature.werkzeuge

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
data object WerkzeugeRoute

fun NavGraphBuilder.werkzeugeBildschirm() {
    composable<WerkzeugeRoute> { WerkzeugeScreen() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WerkzeugeScreen() {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Werkzeuge") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            KommtSpaeter("Kommt in Meilenstein 3 und 5", "Wortkiste und Korrekturzeichen kommen mit Meilenstein 3, Taschenrechner, Formelsammlung, Umrechner, Plotter und Mitrechnen mit Meilenstein 5. Die Wortkiste gibt es schon jetzt im Begleiter.", Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp))
        }
    }
}
