package de.lernen.core.data

import java.io.File

object TestDaten {
    val startpakete: Map<String, String> =
        File("src/main/assets/startpakete").listFiles { f -> f.extension == "json" }!!
            .sortedBy { it.name }
            .associate { it.name to it.readText() }

    fun paket(name: String) = startpakete.getValue(name)

    fun ressource(name: String) = File("src/test/resources/$name").readText()

    val SCHREIBEN = "deutsch-schilderung-schreiben.json"
    val UEBERARBEITEN = "deutsch-schilderung-ueberarbeiten.json"
    val ARBEITSHEFT = "deutsch-arbeitsheft-s31-klassensprecherfahrt.json"

    /** Kleines, gültiges Paket als Grundlage für Fehlertests. */
    const val MINI = """
    {
      "format": "lernen-paket/2", "typ": "thema", "id": "deutsch.mini-test", "version": "1.0",
      "fach": "deutsch", "titel": "Mini", "stufe": "9",
      "ueberblick": { "aufgabe": "Text" },
      "begleiter": [
        { "id": "a", "kurz": "A", "titel": "Schritt A", "ziel": "z", "muss": ["eins", "zwei"],
          "frage": { "frage": "?", "optionen": ["x", "y"], "richtig": 1, "warum": "w" } },
        { "id": "b", "kurz": "B", "titel": "Schritt B", "ziel": "z", "muss": ["drei"] }
      ],
      "uebungen": { "einstieg": [ { "typ": "auswahl", "frage": "?", "optionen": ["a","b"], "richtig": 0, "warum": "" } ] }
    }
    """
}
