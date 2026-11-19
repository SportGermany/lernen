package de.lernen.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Ein hinzugefügtes Paket. Das komplette JSON wird gespeichert, damit alles offline funktioniert. */
@Serializable
@Entity(tableName = "paket")
data class PaketEntity(
    @PrimaryKey val id: String,
    val typ: String,
    val fach: String,
    val titel: String,
    val version: String,
    val json: String,
    val hinzugefuegtAm: Long,
    /** "start" (mitgeliefert) oder "katalog" */
    val herkunft: String,
)

@Serializable
@Entity(tableName = "haken", primaryKeys = ["paketId", "schrittId", "index"], indices = [Index("paketId")])
data class HakenEntity(
    val paketId: String,
    val schrittId: String,
    val index: Int,
    val zeit: Long,
)

/** Wie viele Tipps in einem Schritt schon aufgedeckt sind. */
@Serializable
@Entity(tableName = "tipp_stand", primaryKeys = ["paketId", "schrittId"])
data class TippStandEntity(
    val paketId: String,
    val schrittId: String,
    val aufgedeckt: Int,
)

/** Letzte Antwort auf die Prüffrage eines Schritts. */
@Serializable
@Entity(tableName = "pruef_antwort", primaryKeys = ["paketId", "schrittId"])
data class PruefAntwortEntity(
    val paketId: String,
    val schrittId: String,
    val gewaehlt: Int,
    val richtig: Boolean,
)

/** Wo der Schüler im Thema zuletzt war (für "Weitermachen"). */
@Serializable
@Entity(tableName = "position")
data class PositionEntity(
    @PrimaryKey val paketId: String,
    val schrittId: String?,
    val tab: String,
    val zeit: Long,
)

/** Ergebnis einer Übungsaufgabe. status: "richtig" | "falsch" | "unsicher" */
@Serializable
@Entity(tableName = "uebung_ergebnis", primaryKeys = ["paketId", "aufgabeId"])
data class UebungErgebnisEntity(
    val paketId: String,
    val aufgabeId: String,
    val stufe: String,
    val status: String,
    val zeit: Long,
)

/** Frage an Claude. status: "wartet" | "hochgeladen" | "geloescht" */
@Serializable
@Entity(tableName = "frage")
data class FrageEntity(
    @PrimaryKey val id: String,
    val erstellt: Long,
    val fach: String?,
    val paketId: String?,
    val paketTitel: String?,
    val schrittId: String?,
    val schrittTitel: String?,
    val text: String,
    val antwortart: String,
    val status: String,
    val pfad: String?,
    /** FrageKontext als JSON (seit DB-Version 2). */
    @ColumnInfo(defaultValue = "NULL") val kontextJson: String? = null,
    /** Letzter Fehler beim Hochladen/Löschen (seit DB-Version 2). */
    @ColumnInfo(defaultValue = "NULL") val fehler: String? = null,
)
