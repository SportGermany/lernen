package de.lernen.core.data.model

import kotlinx.serialization.Serializable

/** katalog.json aus dem Paket-Repo (wird dort per GitHub Action erzeugt). */
@Serializable
data class Katalog(
    val format: String = "lernen-katalog/1",
    val pakete: List<KatalogEintrag> = emptyList(),
)

@Serializable
data class KatalogEintrag(
    val id: String,
    val typ: String,
    val fach: String,
    val titel: String,
    val stufe: String = "",
    val version: String,
    val pfad: String,
    val groesse: Long = 0,
    val geaendert: String = "",
)

/** Woher die App Pakete liest. Vorbelegt mit dem öffentlichen Repo. */
data class PaketQuelle(
    val owner: String = "SportGermany",
    val repo: String = "lernen-pakete",
    val branch: String = "main",
) {
    fun rawUrl(pfad: String) = "https://raw.githubusercontent.com/$owner/$repo/$branch/${pfad.trimStart('/')}"
    val anzeige: String get() = "$owner/$repo" + if (branch != "main") " ($branch)" else ""
}
