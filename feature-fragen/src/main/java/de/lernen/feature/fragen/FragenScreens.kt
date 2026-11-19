package de.lernen.feature.fragen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.lernen.core.data.fragen.Antwortart
import de.lernen.core.data.fragen.FrageKontext
import de.lernen.core.design.komponenten.Abzeichen
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.komponenten.Ueberschrift
import de.lernen.core.design.komponenten.ZeileZwischen
import de.lernen.core.design.theme.Lernen
import de.lernen.core.sync.FrageEintrag
import de.lernen.core.sync.FrageStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable data object FragenRoute
@Serializable data class FrageNeuRoute(val paketId: String? = null, val schrittId: String? = null)
@Serializable data class FrageRoute(val id: String)

const val TOKEN_SEITE = "https://github.com/settings/personal-access-tokens/new"
private val DATUM = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.GERMANY)

fun NavController.neueFrage(paketId: String? = null, schrittId: String? = null) = navigate(FrageNeuRoute(paketId, schrittId))

fun NavGraphBuilder.fragenBildschirm(nav: NavController, onEinstellungen: () -> Unit) {
    composable<FragenRoute> { FragenScreen(onNeu = { nav.neueFrage() }, onFrage = { nav.navigate(FrageRoute(it)) }, onEinstellungen = onEinstellungen) }
    composable<FrageNeuRoute> {
        FrageNeuScreen(
            onZurueck = { nav.popBackStack() },
            onGesendet = { id -> nav.navigate(FrageRoute(id)) { popUpTo<FrageNeuRoute> { inclusive = true } } },
            onEinstellungen = onEinstellungen,
        )
    }
    composable<FrageRoute> { FrageDetailScreen(onZurueck = { nav.popBackStack() }) }
}

@Composable
internal fun OeffentlichHinweis() {
    HinweisBox(BoxArt.WARNUNG, "Wichtig") {
        Text("Öffentlich sichtbar – keine Namen oder persönlichen Infos.", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun TokenAnleitung(onEinstellungen: () -> Unit) {
    val uri = LocalUriHandler.current
    Karte {
        KartenTitel("Einmal einrichten")
        Text(
            "Damit die App Fragen hochladen darf, braucht sie einen GitHub-Token:\n" +
                "1. Auf GitHub einen „Fine-grained Personal Access Token“ erstellen.\n" +
                "2. Repository access: nur „lernen-pakete“.\n" +
                "3. Permissions → Contents: „Read and write“.\n" +
                "4. Ein Ablaufdatum wählen (z. B. 90 Tage).\n" +
                "5. Den Token in den Einstellungen unter „GitHub“ einfügen und „Verbindung testen“.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { uri.openUri(TOKEN_SEITE) }) { Text("Token-Seite öffnen") }
            Button(onClick = onEinstellungen) { Text("Einstellungen") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Kopf(titel: String, onZurueck: (() -> Unit)?) {
    TopAppBar(
        title = { Text(titel) },
        navigationIcon = { if (onZurueck != null) IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun Seite(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 24.dp)) { content() }
    }
}

@Composable
fun FragenScreen(onNeu: () -> Unit, onFrage: (String) -> Unit, onEinstellungen: () -> Unit, vm: FragenViewModel = hiltViewModel()) {
    val fragen by vm.fragen.collectAsStateWithLifecycle()
    val hatToken by vm.hatToken.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Kopf("Fragen", null)
        Seite {
            Karte {
                KartenTitel("Frage an Claude")
                Grau(
                    "Du tippst deine Frage, die App legt sie als Datei auf GitHub ab und gibt dir einen Befehl. " +
                        "Den fügst du in einen neuen Claude-Chat ein. Die Antwort bekommst du dort. " +
                        "Nach 24 Stunden löscht die App die Datei wieder. Gelöschte Fragen sind in der GitHub-Versionsgeschichte weiterhin auffindbar."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onNeu, enabled = hatToken, modifier = Modifier.fillMaxWidth()) { Text("Neue Frage") }
            }
            Spacer(Modifier.height(10.dp))
            if (!hatToken) TokenAnleitung(onEinstellungen)
            if (fragen.isNotEmpty()) {
                Ueberschrift("Deine Fragen")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fragen.forEach { FrageZeile(it) { onFrage(it.frage.id) } }
                }
            }
        }
    }
}

@Composable
private fun StatusAbzeichen(s: FrageStatus) {
    val f = Lernen.farben
    Abzeichen(s.anzeige, when (s) { FrageStatus.WARTET -> f.warnung; FrageStatus.HOCHGELADEN -> f.ok; FrageStatus.GELOESCHT -> f.text2 })
}

@Composable
private fun FrageZeile(e: FrageEintrag, onClick: () -> Unit) {
    val clip = LocalClipboardManager.current
    Karte(onClick = onClick) {
        ZeileZwischen {
            Grau(e.frage.erstellt.format(DATUM) + (e.frage.kontext.paketTitel?.let { " · $it" } ?: ""), maxZeilen = 1, modifier = Modifier.weight(1f))
            StatusAbzeichen(e.status)
        }
        Text(e.frage.text, maxLines = 3, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        e.fehler?.takeIf { e.status != FrageStatus.GELOESCHT }?.let { Text(it, color = Lernen.farben.schlecht, style = MaterialTheme.typography.bodySmall) }
        e.befehl?.let { b ->
            TextButton(onClick = { clip.setText(AnnotatedString(b)) }) { Text("Befehl erneut kopieren") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrageNeuScreen(onZurueck: () -> Unit, onGesendet: (String) -> Unit, onEinstellungen: () -> Unit, vm: FrageNeuViewModel = hiltViewModel()) {
    val k by vm.kontext.collectAsStateWithLifecycle()
    val hatToken by vm.hatToken.collectAsStateWithLifecycle()
    val gesendet by vm.gesendet.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    var art by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(gesendet) { gesendet?.let(onGesendet) }
    Column(Modifier.fillMaxSize()) {
        Kopf("Frage an Claude", onZurueck)
        Seite {
            if (!hatToken) { TokenAnleitung(onEinstellungen); return@Seite }
            KontextKarte(k)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(2000) },
                label = { Text("Deine Frage") }, minLines = 4,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            Text("Antwortart", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Antwortart.entries.forEach { a -> FilterChip(selected = art == a.schluessel, onClick = { art = a.schluessel }, label = { Text(a.titel) }) }
            }
            art?.let { Grau(Antwortart.von(it).baustein, Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(12.dp))
            OeffentlichHinweis()
            Button(
                onClick = { vm.senden(text, Antwortart.von(art!!)) },
                enabled = text.isNotBlank() && art != null && gesendet == null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Hochladen") }
            Grau("Keine Fotos hier. Wenn Claude die Aufgabe sehen muss, schickst du das Foto direkt im Chat.", Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun KontextKarte(k: FrageKontext) {
    Karte(farbe = Lernen.farben.ton, rand = null) {
        Text("WIRD AUTOMATISCH MITGESCHICKT", style = MaterialTheme.typography.labelSmall, color = Lernen.farben.text2, modifier = Modifier.padding(bottom = 4.dp))
        if (k.paketTitel == null) Text("Allgemeine Frage, ohne Thema", style = MaterialTheme.typography.bodyMedium)
        k.fachName?.let { Text("Fach: $it", style = MaterialTheme.typography.bodyMedium) }
        k.paketTitel?.let { Text("Thema: $it", style = MaterialTheme.typography.bodyMedium) }
        k.schrittTitel?.let { Text("Schritt: $it", style = MaterialTheme.typography.bodyMedium) }
        if (k.zeilen.isNotEmpty()) Text("Zeilen: ${k.zeilen.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
        if (k.schrittId != null) Text("Aufgedeckte Tipps: ${k.angezeigteTipps}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FrageDetailScreen(onZurueck: () -> Unit, vm: FrageDetailViewModel = hiltViewModel()) {
    val e by vm.frage.collectAsStateWithLifecycle()
    val clip = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }, topBar = { Kopf("Deine Frage", onZurueck) }) { innen ->
        Box(Modifier.padding(innen)) {
            Seite {
                val ee = e ?: return@Seite
                when (ee.status) {
                    FrageStatus.WARTET -> Karte {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 12.dp).height(22.dp), strokeWidth = 2.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Wartet auf Netz", fontWeight = FontWeight.Medium)
                                Grau("Die Frage ist gespeichert und wird hochgeladen, sobald Internet da ist. Danach erscheint hier der Befehl.")
                            }
                        }
                        ee.fehler?.let {
                            Text(it, color = Lernen.farben.schlecht, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = vm::nochmal) { Text("Nochmal versuchen") }
                        }
                    }
                    FrageStatus.HOCHGELADEN -> {
                        Text("Hochgeladen. Kopiere den Befehl und füge ihn in einen neuen Claude-Chat ein.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 10.dp))
                        Karte(farbe = Lernen.farben.ton, rand = null) {
                            SelectionContainer { Text(ee.befehl ?: "", fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            clip.setText(AnnotatedString(ee.befehl ?: ""))
                            scope.launch { snackbar.showSnackbar("Befehl kopiert") }
                        }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Kopieren") }
                        Grau("Die Antwort bekommst du im Claude-Chat. Die Datei wird nach 24 Stunden gelöscht.", Modifier.padding(top = 8.dp))
                    }
                    FrageStatus.GELOESCHT -> HinweisBox(BoxArt.INFO) {
                        Text("Diese Frage wurde nach 24 Stunden von GitHub gelöscht. Hier bleibt sie als Verlauf.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Ueberschrift("Frage")
                Karte {
                    Grau(ee.frage.erstellt.format(DATUM) + " · " + ee.frage.antwortart.titel)
                    Text(ee.frage.text, modifier = Modifier.padding(top = 4.dp))
                }
                if (ee.status != FrageStatus.GELOESCHT) { Spacer(Modifier.height(10.dp)); KontextKarte(ee.frage.kontext) }
                Spacer(Modifier.height(10.dp))
                Grau("Gelöschte Fragen sind in der GitHub-Versionsgeschichte weiterhin auffindbar.")
            }
        }
    }
}

