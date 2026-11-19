package de.lernen.core.data.paket

import de.lernen.core.data.model.Fach
import de.lernen.core.data.model.Paket
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Liest Pakete und faecher.json. Prüft jedes Paket mit [PaketPruefer]. */
object PaketParser {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
        encodeDefaults = false
    }

    sealed interface Ergebnis {
        data class Ok(val paket: Paket, val warnungen: List<PaketPruefer.Problem>) : Ergebnis
        data class Fehler(val meldungen: List<String>) : Ergebnis
    }

    /** Liest ein Paket. Bei Schema-Fehlern (nicht Warnungen) kommt [Ergebnis.Fehler]. */
    fun lies(text: String): Ergebnis {
        val paket = try {
            json.decodeFromString(Paket.serializer(), text)
        } catch (e: SerializationException) {
            return Ergebnis.Fehler(listOf("JSON passt nicht zum Paketformat: ${e.message?.lineSequence()?.firstOrNull()}"))
        } catch (e: IllegalArgumentException) {
            return Ergebnis.Fehler(listOf("JSON passt nicht zum Paketformat: ${e.message}"))
        }
        val probleme = PaketPruefer.pruefe(paket)
        val fehler = probleme.filter { it.schwer }
        return if (fehler.isEmpty()) Ergebnis.Ok(paket, probleme)
        else Ergebnis.Fehler(fehler.map { it.toString() })
    }

    /** Wie [lies], wirft aber bei Fehlern. */
    fun liesOderWirf(text: String): Paket = when (val e = lies(text)) {
        is Ergebnis.Ok -> e.paket
        is Ergebnis.Fehler -> throw IllegalArgumentException(e.meldungen.joinToString("\n"))
    }

    fun liesFaecher(text: String): List<Fach> =
        json.decodeFromString(ListSerializer(Fach.serializer()), text)
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
}
