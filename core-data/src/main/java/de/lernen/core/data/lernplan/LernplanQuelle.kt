package de.lernen.core.data.lernplan

import java.time.LocalDate

/**
 * Vorbereitet für später (Abschnitt 12): Anbindung an die Schulplaner-Web-App „Lernplan“.
 * Liefert anstehende Schulaufgaben, damit die App passende Themen nach vorne holen kann.
 * Noch nicht angebunden: [KeinLernplan] liefert eine leere Liste.
 */
interface LernplanQuelle {
    suspend fun anstehendeSchulaufgaben(): List<Schulaufgabe>
}

data class Schulaufgabe(
    val fach: String,
    val datum: LocalDate,
    val thema: String?,
    /** Paket-ids, die zu dieser Schulaufgabe passen (falls bekannt). */
    val paketIds: List<String> = emptyList(),
)

class KeinLernplan : LernplanQuelle {
    override suspend fun anstehendeSchulaufgaben(): List<Schulaufgabe> = emptyList()
}
