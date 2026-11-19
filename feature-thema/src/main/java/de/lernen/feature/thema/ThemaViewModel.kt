package de.lernen.feature.thema

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Fach
import de.lernen.core.data.model.Paket
import de.lernen.core.data.paket.Fortschritt
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.data.repo.FortschrittRepository
import de.lernen.core.data.repo.PaketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemaZustand(
    val paket: Paket,
    val fach: Fach?,
    val fortschritt: Fortschritt,
    val tipps: Map<String, Int>,
    val antworten: Map<String, Int>,
    val schritt: Int,
    val tab: ThemaTab,
)

private data class Ansicht(val schritt: Int, val tab: ThemaTab, val bereit: Boolean)

@HiltViewModel
class ThemaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    pakete: PaketRepository,
    faecher: FachRepository,
    private val fortschritt: FortschrittRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ThemaRoute>()
    private val paketId = route.paketId
    private val ansicht = MutableStateFlow(Ansicht(0, ThemaTab.UEBERBLICK, bereit = false))

    val zustand: StateFlow<ThemaZustand?> = combine(
        pakete.paket(paketId).filterNotNull(),
        faecher.faecherAlle,
        fortschritt.haken(paketId),
        combine(fortschritt.tipps(paketId), fortschritt.pruefAntworten(paketId)) { t, a -> t to a },
        ansicht,
    ) { p, f, h, (t, a), v ->
        if (!v.bereit) null
        else ThemaZustand(p, f.firstOrNull { it.id == p.fach }, Fortschritt(p, h), t, a, v.schritt.coerceIn(0, (p.begleiter.size - 1).coerceAtLeast(0)), v.tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Paket existiert nicht (mehr), z. B. nach dem Entfernen. */
    val fehlt = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val p = pakete.paket(paketId).first()
            if (p == null) { fehlt.value = true; return@launch }
            val pos = fortschritt.position(paketId)
            val schrittId = route.schrittId ?: pos?.schrittId
            val idx = p.begleiter.indexOfFirst { it.id == schrittId }.coerceAtLeast(0)
            val tab = ThemaTab.von(route.tab) ?: ThemaTab.von(pos?.tab) ?: ThemaTab.UEBERBLICK
            ansicht.value = Ansicht(idx, tab, bereit = true)
            merke()
        }
    }

    fun waehleTab(t: ThemaTab) { ansicht.value = ansicht.value.copy(tab = t); merke() }

    fun waehleSchritt(i: Int) { ansicht.value = ansicht.value.copy(schritt = i, tab = ThemaTab.BEGLEITER); merke() }

    fun umschalten(schrittId: String, index: Int, an: Boolean) = viewModelScope.launch {
        fortschritt.setzeHaken(paketId, schrittId, index, an)
    }

    fun naechsterTipp(schrittId: String, bisher: Int, max: Int) = viewModelScope.launch {
        fortschritt.setzeTipps(paketId, schrittId, (bisher + 1).coerceAtMost(max))
    }

    fun antworte(schrittId: String, gewaehlt: Int, richtig: Boolean) = viewModelScope.launch {
        fortschritt.beantworte(paketId, schrittId, gewaehlt, richtig)
    }

    fun nochmal(schrittId: String) = viewModelScope.launch { fortschritt.nochmalVersuchen(paketId, schrittId) }

    private fun merke() = viewModelScope.launch {
        val p = zustand.value?.paket ?: return@launch merkeOhnePaket()
        val v = ansicht.value
        fortschritt.merkePosition(paketId, p.begleiter.getOrNull(v.schritt)?.id, v.tab.schluessel)
    }

    private suspend fun merkeOhnePaket() {
        val v = ansicht.value
        val p = zustand.filterNotNull().first().paket
        fortschritt.merkePosition(paketId, p.begleiter.getOrNull(v.schritt)?.id, v.tab.schluessel)
    }
}
