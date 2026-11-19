package de.lernen.core.data.model

import kotlinx.serialization.Serializable

/** Ein Fach aus faecher.json. Neue Fächer kommen ohne App-Update über diese Datei dazu. */
@Serializable
data class Fach(
    val id: String,
    val name: String,
    val farbe: String,
    val farbeDunkel: String = farbe,
    /** "schreiben" | "rechnen" | "physik" */
    val begleiterTyp: String = "schreiben",
) {
    /** Kürzel für den farbigen Punkt, z. B. "D", "M", "Ph". */
    val kuerzel: String
        get() = when (id) {
            "physik" -> "Ph"
            "chemie" -> "Ch"
            "geschichte" -> "G"
            else -> name.take(1).uppercase()
        }
}
