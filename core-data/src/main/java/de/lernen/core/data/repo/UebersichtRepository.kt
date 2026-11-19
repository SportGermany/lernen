package de.lernen.core.data.repo

import de.lernen.core.data.model.Fach
import de.lernen.core.data.model.Paket
import de.lernen.core.data.paket.Fortschritt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class ThemaKurz(val paket: Paket, val fortschritt: Fortschritt)

data class FachMitThemen(val fach: Fach, val themen: List<ThemaKurz>)

data class Weitermachen(
    val thema: ThemaKurz,
    val fach: Fach?,
    val schrittTitel: String?,
    val begonnen: Boolean,
)

data class Uebersicht(
    val faecher: List<FachMitThemen>,
    val weitermachen: Weitermachen?,
    val offeneWiederholungen: Int,
    /** Thema mit den meisten offenen Wiederholungen (sonst das Weitermachen-Thema). */
    val wiederholenPaketId: String?,
)

/** Fasst Fächer, Pakete und Fortschritt für Start und Fächer zusammen. */
@Singleton
class UebersichtRepository @Inject constructor(
    pakete: PaketRepository,
    faecher: FachRepository,
    fortschritt: FortschrittRepository,
) {
    val uebersicht: Flow<Uebersicht> = combine(
        pakete.pakete, faecher.faecher, fortschritt.alleHaken, fortschritt.positionen, fortschritt.offeneWiederholungen,
    ) { pl, fl, haken, pos, wdh ->
        val themen = pl.filter { it.typ != Paket.TYP_FORMELSAMMLUNG }
            .map { ThemaKurz(it, Fortschritt(it, haken[it.id].orEmpty())) }
        val proFach = fl.map { f -> FachMitThemen(f, themen.filter { it.paket.fach == f.id }) }
        val sichtbar = fl.map { it.id }.toSet()

        val letzte = pos.firstNotNullOfOrNull { p -> themen.firstOrNull { it.paket.id == p.paketId && it.paket.fach in sichtbar }?.let { it to p } }
        val weiter = when {
            letzte != null -> {
                val (t, p) = letzte
                Weitermachen(t, fl.firstOrNull { it.id == t.paket.fach }, t.paket.begleiter.firstOrNull { it.id == p.schrittId }?.titel, true)
            }
            else -> themen.firstOrNull { it.paket.fach in sichtbar }?.let {
                Weitermachen(it, fl.firstOrNull { f -> f.id == it.paket.fach }, it.paket.begleiter.firstOrNull()?.titel, false)
            }
        }
        val offen = wdh.filterKeys { id -> themen.any { it.paket.id == id } }
        Uebersicht(proFach, weiter, offen.values.sum(), offen.maxByOrNull { it.value }?.key ?: weiter?.thema?.paket?.id)
    }
}
