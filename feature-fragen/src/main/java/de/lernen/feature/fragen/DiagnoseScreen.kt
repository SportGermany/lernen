package de.lernen.feature.fragen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.theme.Lernen
import de.lernen.core.sync.GitHubApi
import de.lernen.core.sync.Hintergrund
import de.lernen.core.sync.TokenSpeicher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object DiagnoseRoute

@HiltViewModel
class DiagnoseViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: GitHubApi,
    private val token: TokenSpeicher,
    private val einstellungen: EinstellungenRepository,
    private val hintergrund: Hintergrund,
) : ViewModel() {
    val ergebnis = MutableStateFlow<GitHubApi.Diagnose?>(null)
    val laeuft = MutableStateFlow(false)
    val quelle get() = kotlinx.coroutines.runBlocking { einstellungen.aktuell().quelle }

    init { starten() }

    fun starten() = viewModelScope.launch {
        laeuft.value = true
        val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
        val d = api.diagnose(einstellungen.aktuell().quelle, token.lies(), version)
        ergebnis.value = d
        laeuft.value = false
        if (d.ok) hintergrund.fragenHochladen() // wartende Fragen gleich hochladen
    }

    fun neuerToken(t: String) {
        token.speichere(t)
        starten()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnoseScreen(onZurueck: () -> Unit, vm: DiagnoseViewModel = hiltViewModel()) {
    val d by vm.ergebnis.collectAsStateWithLifecycle()
    val laeuft by vm.laeuft.collectAsStateWithLifecycle()
    val clip = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var eingabe by remember { mutableStateOf("") }
    val f = Lernen.farben
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Fehlerdiagnose") },
                navigationIcon = { IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innen ->
        Box(Modifier.fillMaxSize().padding(innen), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
                Grau("Die App prüft Schritt für Schritt, ob sie Fragen auf GitHub ablegen darf. Dabei wird eine kleine Testdatei angelegt und sofort wieder gelöscht.", Modifier.padding(bottom = 10.dp))
                if (laeuft) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Prüfe …")
                }
                d?.let { dd ->
                    Karte {
                        dd.schritte.forEach { s ->
                            Row(Modifier.padding(vertical = 6.dp)) {
                                Text(if (s.ok) "✓" else "✗", color = if (s.ok) f.ok else f.schlecht, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                Column {
                                    Text(s.titel, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    Grau(s.text)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HinweisBox(if (dd.ok) BoxArt.OK else BoxArt.SCHLECHT, if (dd.ok) "Alles in Ordnung" else "So behebst du es") {
                        Text(dd.loesung, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!dd.ok) {
                        OutlinedButton(onClick = { uri.openUri("https://github.com/settings/personal-access-tokens") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Meine Tokens auf GitHub öffnen")
                        }
                        OutlinedButton(onClick = { uri.openUri(GitHubApi.tokenSeite(vm.quelle)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Neuen Token erstellen (vorausgefüllt)")
                        }
                        Karte(Modifier.padding(top = 10.dp)) {
                            KartenTitel("Neuen Token einfügen")
                            OutlinedTextField(
                                value = eingabe, onValueChange = { eingabe = it.trim() }, singleLine = true, label = { Text("github_pat_…") },
                                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(onClick = { vm.neuerToken(eingabe); eingabe = "" }, enabled = eingabe.length >= 20 && !laeuft, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Speichern und prüfen")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.starten() }, enabled = !laeuft, modifier = Modifier.weight(1f)) { Text("Nochmal prüfen") }
                        OutlinedButton(onClick = {
                            clip.setText(AnnotatedString(dd.bericht))
                            scope.launch { snackbar.showSnackbar("Bericht kopiert – enthält keinen Token") }
                        }, modifier = Modifier.weight(1f)) { Text("Bericht kopieren") }
                    }
                    Text(
                        dd.bericht, style = MaterialTheme.typography.bodySmall, color = f.text2,
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(f.ton).padding(10.dp),
                    )
                }
            }
        }
    }
}
