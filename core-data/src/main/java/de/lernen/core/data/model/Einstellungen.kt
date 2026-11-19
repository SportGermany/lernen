package de.lernen.core.data.model

enum class Darstellung { SYSTEM, HELL, DUNKEL }

data class Einstellungen(
    val darstellung: Darstellung = Darstellung.SYSTEM,
    val ausgeblendeteFaecher: Set<String> = emptySet(),
    val fachReihenfolge: List<String> = emptyList(),
    /** Eigene Farbe je Fach-id (#RRGGBB), überschreibt faecher.json. */
    val fachFarben: Map<String, String> = emptyMap(),
    val quelle: PaketQuelle = PaketQuelle(),
    /** Zeitpunkt des letzten erfolgreichen Syncs (ms), 0 = nie. */
    val letzterSync: Long = 0,
    val erinnerungAn: Boolean = false,
    /** Minuten nach Mitternacht, Standard 19:00. */
    val erinnerungMinuten: Int = 19 * 60,
    /** Letzte Prüfung auf App-Update (ms). */
    val letzteUpdatePruefung: Long = 0,
)
