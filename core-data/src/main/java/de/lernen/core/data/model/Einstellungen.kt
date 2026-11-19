package de.lernen.core.data.model

enum class Darstellung { SYSTEM, HELL, DUNKEL }

data class Einstellungen(
    val darstellung: Darstellung = Darstellung.SYSTEM,
    val ausgeblendeteFaecher: Set<String> = emptySet(),
    val fachReihenfolge: List<String> = emptyList(),
    /** Eigene Farbe je Fach-id (#RRGGBB), überschreibt faecher.json. */
    val fachFarben: Map<String, String> = emptyMap(),
)
