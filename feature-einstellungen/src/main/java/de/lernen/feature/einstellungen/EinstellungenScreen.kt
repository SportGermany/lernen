package de.lernen.feature.einstellungen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Darstellung
import de.lernen.core.data.model.Einstellungen
import de.lernen.core.data.model.PaketQuelle
import de.lernen.core.data.repo.EinstellungenRepository
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.data.repo.Sicherung
import de.lernen.core.data.repo.SicherungInfo
import de.lernen.core.data.repo.SicherungRepository
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Segmente
import de.lernen.core.design.komponenten.ZeileZwischen
import de.lernen.core.design.theme.Lernen
import de.lernen.core.sync.AppUpdate
import de.lernen.core.sync.GitHubApi
import de.lernen.core.sync.Hintergrund
import de.lernen.core.sync.TokenSpeicher
import de.lernen.core.sync.UpdateZustand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Serializable
data object EinstellungenRoute

@HiltViewModel
class EinstellungenViewModel @Inject constructor(
    private val repo: EinstellungenRepository,
    faecherRepo: FachRepository,
    private val token: TokenSpeicher,
    private val github: GitHubApi,
    private val sicherung: SicherungRepository,
    private val hintergrund: Hintergrund,
    private val update: AppUpdate,
) : ViewModel() {
    val einstellungen = repo.einstellungen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Einstellungen())
    val faecher = faecherRepo.faecherAlle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hatToken = token.vorhanden
    val updateZustand = update.zustand
    private val meldungen = Channel<String>(Channel.BUFFERED)
    val meldung = meldungen.receiveAsFlow()

    fun darstellung(d: Darstellung) = viewModelScope.launch { repo.setzeDarstellung(d) }
    fun sichtbar(id: String, an: Boolean) = viewModelScope.launch { repo.setzeFachSichtbar(id, an) }
    fun reihenfolge(ids: List<String>) = viewModelScope.launch { repo.setzeReihenfolge(ids) }
    fun farbe(id: String, hex: String?) = viewModelScope.launch { repo.setzeFachFarbe(id, hex) }
    fun quelle(q: PaketQuelle) = viewModelScope.launch { repo.setzeQuelle(q) }

    fun erinnerung(an: Boolean, minuten: Int) = viewModelScope.launch {
        repo.setzeErinnerung(an, minuten)
        hintergrund.erinnerungPlanen(an, minuten)
    }

    fun tokenSpeichern(t: String) {
        token.speichere(t)
        hintergrund.fragenHochladen()
    }

    fun tokenLoeschen() = viewModelScope.launch { token.loesche(); meldungen.send("Token gelöscht.") }


    suspend fun exportText(): String = sicherung.exportieren()

    fun liesSicherung(text: String): Pair<Sicherung, SicherungInfo>? =
        runCatching { sicherung.lies(text) }.onFailure { viewModelScope.launch { meldungen.send(it.message ?: "Ungültige Datei.") } }.getOrNull()

    fun importieren(s: Sicherung) = viewModelScope.launch {
        sicherung.importieren(s)
        val e = repo.aktuell()
        hintergrund.erinnerungPlanen(e.erinnerungAn, e.erinnerungMinuten)
        meldungen.send("Sicherung eingespielt.")
    }

    fun zuruecksetzen() = viewModelScope.launch { sicherung.fortschrittZuruecksetzen(); meldungen.send("Fortschritt zurückgesetzt.") }

    fun nachUpdateSuchen(version: String) = viewModelScope.launch { update.pruefen(version) }
    fun installieren(z: UpdateZustand.Verfuegbar) = viewModelScope.launch { update.installieren(z.neu) }
    fun melde(t: String) = viewModelScope.launch { meldungen.send(t) }
}

fun NavGraphBuilder.einstellungenBildschirm(version: String, onZurueck: () -> Unit, onDiagnose: () -> Unit) {
    composable<EinstellungenRoute> { EinstellungenScreen(version, onZurueck, onDiagnose) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinstellungenScreen(version: String, onZurueck: () -> Unit, onDiagnose: () -> Unit = {}, vm: EinstellungenViewModel = hiltViewModel()) {
    val e by vm.einstellungen.collectAsStateWithLifecycle()
    val faecher by vm.faecher.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.meldung.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it) } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = { IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innen ->
        Box(Modifier.fillMaxSize().padding(innen), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Abschnitt("Fächer")
                FaecherAbschnitt(faecher, e.ausgeblendeteFaecher, e.fachFarben, vm::sichtbar, vm::reihenfolge, vm::farbe)

                Abschnitt("Erinnerung")
                ErinnerungAbschnitt(e, vm::erinnerung)

                Abschnitt("Darstellung")
                val optionen = listOf(Darstellung.SYSTEM to "System", Darstellung.HELL to "Hell", Darstellung.DUNKEL to "Dunkel")
                Segmente(optionen.map { it.second }, optionen.indexOfFirst { it.first == e.darstellung }, onWahl = { vm.darstellung(optionen[it].first) }, modifier = Modifier.padding(vertical = 8.dp))

                Abschnitt("GitHub")
                QuelleZeile(e.quelle, vm::quelle)
                TokenAbschnitt(vm, onDiagnose)

                Abschnitt("Daten")
                DatenAbschnitt(vm)

                Abschnitt("App")
                AppAbschnitt(version, vm)
            }
        }
    }
}

@Composable
internal fun Abschnitt(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 22.dp, bottom = 4.dp))
}

@Composable
private fun Zeile(links: String, rechts: String, onClick: (() -> Unit)? = null) {
    ZeileZwischen((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 13.dp)) {
        Text(links, modifier = Modifier.weight(1f))
        Grau(rechts)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErinnerungAbschnitt(e: Einstellungen, onAendern: (Boolean, Int) -> Unit) {
    var uhrOffen by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val erlaubnis = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> onAendern(ok, e.erinnerungMinuten) }
    ZeileZwischen(Modifier.padding(vertical = 6.dp)) {
        Text("Täglich erinnern", modifier = Modifier.weight(1f))
        Switch(checked = e.erinnerungAn, onCheckedChange = { an ->
            if (an && Build.VERSION.SDK_INT >= 33 &&
                ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) erlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
            else onAendern(an, e.erinnerungMinuten)
        })
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Zeile("Uhrzeit", "%02d:%02d ›".format(e.erinnerungMinuten / 60, e.erinnerungMinuten % 60)) { uhrOffen = true }
    Grau("Text: „Zeit für eine kleine Lerneinheit“", Modifier.padding(top = 6.dp))
    if (uhrOffen) {
        val st = rememberTimePickerState(e.erinnerungMinuten / 60, e.erinnerungMinuten % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { uhrOffen = false },
            title = { Text("Uhrzeit der Erinnerung") },
            text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { onAendern(e.erinnerungAn, st.hour * 60 + st.minute); uhrOffen = false }) { Text("Übernehmen") } },
            dismissButton = { TextButton(onClick = { uhrOffen = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun TokenAbschnitt(vm: EinstellungenViewModel, onDiagnose: () -> Unit) {
    val hat by vm.hatToken.collectAsStateWithLifecycle()
    val e by vm.einstellungen.collectAsStateWithLifecycle()
    var eingabe by remember { mutableStateOf("") }
    var loeschen by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(Modifier.padding(vertical = 10.dp)) {
        ZeileZwischen {
            Text("Token für Fragen", modifier = Modifier.weight(1f))
            Grau(if (hat) "gespeichert (verschlüsselt)" else "nicht eingerichtet")
        }
        Grau(
            "Fine-grained Token, angemeldet als ${e.quelle.owner}: „Repository access“ → Only select repositories → ${e.quelle.repo}; " +
                "„Contents“ → Read and write. Die App schreibt und löscht nur im Ordner fragen/offen/.",
            Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            value = eingabe, onValueChange = { eingabe = it.trim() }, singleLine = true,
            label = { Text(if (hat) "Neuen Token einfügen (ersetzt den alten)" else "Token einfügen") },
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.tokenSpeichern(eingabe); eingabe = ""; onDiagnose() }, enabled = eingabe.length >= 20) { Text("Speichern + prüfen") }
            OutlinedButton(onClick = { uri.openUri(GitHubApi.tokenSeite(e.quelle)) }) { Text("Token erstellen") }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDiagnose) { Text("Fehlerdiagnose") }
            if (hat) TextButton(onClick = { loeschen = true }) { Text("Token löschen") }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    if (loeschen) Bestaetigen("Token löschen?", "Danach kann die App keine Fragen mehr hochladen oder löschen, bis du einen neuen Token einfügst.", "Löschen", { loeschen = false }) { vm.tokenLoeschen() }
}

@Composable
private fun DatenAbschnitt(vm: EinstellungenViewModel) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var vorschau by remember { mutableStateOf<Pair<Sicherung, SicherungInfo>?>(null) }
    var reset by remember { mutableStateOf(false) }
    val dateiname = "lernen-sicherung-" + SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date()) + ".json"

    val speichern = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching { schreibe(ctx, uri, vm.exportText()) }.isSuccess
            vm.melde(if (ok) "Sicherung gespeichert." else "Speichern fehlgeschlagen.")
        }
    }
    val oeffnen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }.getOrNull() }
            if (text == null) vm.melde("Datei konnte nicht gelesen werden.") else vorschau = vm.liesSicherung(text)
        }
    }
    Zeile("Sicherung als Datei speichern", "›") { speichern.launch(dateiname) }
    Zeile("Sicherung teilen", "›") {
        scope.launch {
            val f = withContext(Dispatchers.IO) {
                File(ctx.cacheDir, "sicherung").apply { mkdirs() }.let { File(it, dateiname) }.apply { writeText(vm.exportText()) }
            }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", f)
            ctx.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Sicherung teilen",
            ))
        }
    }
    Zeile("Sicherung importieren", "›") { oeffnen.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    Zeile("Fortschritt zurücksetzen", "›") { reset = true }
    Grau("Die Sicherung enthält Haken, Tipps, Übungsergebnisse, Fragen-Verlauf und Einstellungen, aber keinen Token.", Modifier.padding(top = 6.dp))

    vorschau?.let { (s, info) ->
        Bestaetigen(
            "Sicherung einspielen?",
            "Sicherung vom ${SimpleDateFormat("d.M.yyyy, HH:mm", Locale.GERMANY).format(Date(info.erstellt))}: ${info.haken} Haken, ${info.ergebnisse} Übungsergebnisse. " +
                "Dein jetziger Fortschritt wird dabei ersetzt." +
                if (info.pakete.isNotEmpty()) " Füge danach im Katalog die Themen hinzu, falls sie fehlen." else "",
            "Einspielen", { vorschau = null },
        ) { vm.importieren(s) }
    }
    if (reset) Bestaetigen("Fortschritt zurücksetzen?", "Alle Haken, aufgedeckten Tipps und Übungsergebnisse werden gelöscht. Themen und Einstellungen bleiben.", "Zurücksetzen", { reset = false }) { vm.zuruecksetzen() }
}

private suspend fun schreibe(ctx: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    ctx.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(text) }
}

@Composable
private fun AppAbschnitt(version: String, vm: EinstellungenViewModel) {
    val z by vm.updateZustand.collectAsStateWithLifecycle()
    Zeile("Version", version)
    Zeile("Nach Update suchen", "›") { vm.nachUpdateSuchen(version) }
    Grau("Die App prüft einmal am Tag selbst, ob es eine neue Version gibt.", Modifier.padding(top = 6.dp))
    when (val zz = z) {
        is UpdateZustand.Verfuegbar -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Neue Version ${zz.neu.version}", modifier = Modifier.weight(1f))
            Button(onClick = { vm.installieren(zz) }) { Text("Installieren") }
        }
        is UpdateZustand.Laedt -> Grau("Lade Version ${zz.neu.version} herunter …", Modifier.padding(top = 8.dp))
        is UpdateZustand.Fehler -> Text(zz.meldung, color = Lernen.farben.schlecht, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        UpdateZustand.Aktuell -> Grau("Du hast die neueste Version.", Modifier.padding(top = 8.dp))
        UpdateZustand.Nichts -> Unit
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun Bestaetigen(titel: String, text: String, ok: String, onAbbrechen: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text(titel) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onOk(); onAbbrechen() }) { Text(ok) } },
        dismissButton = { TextButton(onClick = onAbbrechen) { Text("Abbrechen") } },
    )
}

@Composable
private fun QuelleZeile(q: PaketQuelle, onSpeichern: (PaketQuelle) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    Zeile("Paket-Quelle", q.anzeige) { offen = true }
    if (offen) {
        var owner by remember { mutableStateOf(q.owner) }
        var repo by remember { mutableStateOf(q.repo) }
        var branch by remember { mutableStateOf(q.branch) }
        AlertDialog(
            onDismissRequest = { offen = false },
            title = { Text("Paket-Quelle (GitHub)") },
            text = {
                Column {
                    OutlinedTextField(owner, { owner = it }, label = { Text("Owner") }, singleLine = true)
                    OutlinedTextField(repo, { repo = it }, label = { Text("Repository") }, singleLine = true)
                    OutlinedTextField(branch, { branch = it }, label = { Text("Branch") }, singleLine = true)
                    Grau("Nach dem Ändern im Katalog auf ↻ tippen.", Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { onSpeichern(PaketQuelle(owner, repo, branch)); offen = false }, enabled = owner.isNotBlank() && repo.isNotBlank()) { Text("Speichern") } },
            dismissButton = { TextButton(onClick = { onSpeichern(PaketQuelle()); offen = false }) { Text("Zurücksetzen") } },
        )
    }
}
