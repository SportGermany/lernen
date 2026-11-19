package de.lernen.feature.fragen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.fragen.Antwortart
import de.lernen.core.data.fragen.FrageFormat
import de.lernen.core.data.fragen.FrageKontext
import de.lernen.core.data.model.Paket
import de.lernen.core.data.repo.FachRepository
import de.lernen.core.data.repo.FortschrittRepository
import de.lernen.core.data.repo.PaketRepository
import de.lernen.core.sync.FragenRepository
import de.lernen.core.sync.Hintergrund
import de.lernen.core.sync.KatalogRepository
import de.lernen.core.sync.TokenSpeicher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragenViewModel @Inject constructor(
    repo: FragenRepository,
    token: TokenSpeicher,
    private val hintergrund: Hintergrund,
) : ViewModel() {
    val fragen = repo.fragen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hatToken = token.vorhanden
    fun nochmalHochladen() = hintergrund.fragenHochladen()
}

@HiltViewModel
class FrageNeuViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pakete: PaketRepository,
    private val faecher: FachRepository,
    private val fortschritt: FortschrittRepository,
    private val katalog: KatalogRepository,
    private val repo: FragenRepository,
    private val hintergrund: Hintergrund,
    token: TokenSpeicher,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<FrageNeuRoute>()
    val kontext = MutableStateFlow(FrageKontext())
    val hatToken = token.vorhanden
    val gesendet = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { kontext.value = baueKontext() }
    }

    private suspend fun baueKontext(): FrageKontext {
        val id = route.paketId ?: return FrageKontext()
        val p: Paket = pakete.paket(id).first() ?: return FrageKontext()
        val fach = faecher.faecherAlle.first().firstOrNull { it.id == p.fach }
        val s = p.begleiter.firstOrNull { it.id == route.schrittId }
        val tipps = s?.let { fortschritt.tipps(p.id).first()[it.id] } ?: 0
        val pfad = katalog.zeilen.first().firstOrNull { it.eintrag.id == p.id }?.eintrag?.pfad
        return FrageKontext(
            fachId = p.fach, fachName = fach?.name ?: p.fach, paketId = p.id, paketTitel = p.titel, paketPfad = pfad,
            schrittId = s?.id, schrittTitel = s?.titel, schrittZiel = s?.ziel,
            zeilen = if (p.istArbeitsheft && s != null) FrageFormat.zeilenAus(s.hinweise) else emptyList(),
            angezeigteTipps = tipps,
        )
    }

    fun senden(text: String, art: Antwortart) = viewModelScope.launch {
        val id = repo.erstelle(text, art, kontext.value)
        hintergrund.fragenHochladen()
        gesendet.value = id
    }
}

@HiltViewModel
class FrageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: FragenRepository,
    private val hintergrund: Hintergrund,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<FrageRoute>().id
    val frage = repo.frage(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun nochmal() = hintergrund.fragenHochladen()
}
