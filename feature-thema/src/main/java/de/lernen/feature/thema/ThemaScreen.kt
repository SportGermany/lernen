package de.lernen.feature.thema

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lernen.core.design.komponenten.Segmente
import de.lernen.core.design.komponenten.Symbole
import de.lernen.core.design.komponenten.WortkisteSheet
import de.lernen.core.design.theme.Lernen
import kotlinx.coroutines.launch

/** Ab dieser Breite: zweispaltig (Tablet quer). */
internal val BREIT = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemaScreen(
    onZurueck: () -> Unit,
    onFrage: (schrittId: String?) -> Unit = {},
    onMitrechnen: (schrittId: String?, aufgabe: String?) -> Unit = { _, _ -> },
    vm: ThemaViewModel = hiltViewModel(),
) {
    val z by vm.zustand.collectAsStateWithLifecycle()
    val fehlt by vm.fehlt.collectAsStateWithLifecycle()
    var wach by rememberSaveable { mutableStateOf(false) }
    var wortkiste by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // „Bildschirm anlassen“: gilt nur, solange das Thema offen ist.
    val view = LocalView.current
    DisposableEffect(wach) {
        view.keepScreenOn = wach
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(fehlt) { if (fehlt) onZurueck() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                title = { Text(z?.paket?.titel ?: "", fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(
                        onClick = {
                            wach = !wach
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(if (wach) "Bildschirm bleibt an" else "Bildschirm darf wieder ausgehen")
                            }
                        },
                        colors = if (wach) IconButtonDefaults.iconButtonColors(containerColor = Lernen.farben.ton, contentColor = MaterialTheme.colorScheme.primary)
                        else IconButtonDefaults.iconButtonColors(contentColor = Lernen.farben.text2),
                    ) { Icon(Symbole.Sonne, if (wach) "Bildschirm anlassen: an" else "Bildschirm anlassen: aus") }
                },
            )
        },
    ) { innen ->
        val zz = z
        if (zz == null) {
            Box(Modifier.fillMaxSize().padding(innen), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(innen), contentAlignment = Alignment.TopCenter) {
            val breit = maxWidth >= BREIT
            val scroll = rememberScrollState()
            LaunchedEffect(zz.tab, zz.schritt, zz.uebenStufe, zz.uebenIndex) { scroll.scrollTo(0) }
            Column(
                Modifier.widthIn(max = 1100.dp).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 14.dp).padding(bottom = 24.dp)
            ) {
                Segmente(
                    ThemaTab.entries.map { it.titel }, ThemaTab.entries.indexOf(zz.tab),
                    onWahl = { vm.waehleTab(ThemaTab.entries[it]) }, modifier = Modifier.padding(bottom = 12.dp),
                )
                when (zz.tab) {
                    ThemaTab.UEBERBLICK -> UeberblickTab(zz, breit, onSchritt = vm::waehleSchritt, onStart = { vm.waehleTab(ThemaTab.BEGLEITER) })
                    ThemaTab.BEGLEITER -> BegleiterTab(
                        zz, breit,
                        onSchritt = vm::waehleSchritt,
                        onHaken = vm::umschalten,
                        onTipp = vm::naechsterTipp,
                        onAntwort = vm::antworte,
                        onNochmal = vm::nochmal,
                        onWortkiste = { wortkiste = true },
                        onFertig = { vm.waehleTab(ThemaTab.UEBEN) },
                        onFrage = onFrage,
                        onMitrechnen = { onMitrechnen(it, null) },
                    )
                    ThemaTab.UEBEN -> UebenTab(
                        zz, breit,
                        onStufe = vm::waehleStufe,
                        onIndex = vm::geheZuAufgabe,
                        onErgebnis = vm::ergebnis,
                        onWortkiste = { wortkiste = true },
                        onMitrechnen = { onMitrechnen(null, it) },
                    )
                    ThemaTab.MERKZETTEL -> MerkzettelTab(zz, breit)
                }
            }
        }
        if (wortkiste) WortkisteSheet(zz.paket.wortkiste) { wortkiste = false }
    }
}
