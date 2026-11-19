package de.lernen.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaketDao {
    @Query("SELECT * FROM paket ORDER BY fach, titel")
    fun alle(): Flow<List<PaketEntity>>

    @Query("SELECT * FROM paket WHERE id = :id")
    fun eines(id: String): Flow<PaketEntity?>

    @Query("SELECT COUNT(*) FROM paket")
    suspend fun anzahl(): Int

    @Upsert
    suspend fun speichere(p: PaketEntity)

    @Query("DELETE FROM paket WHERE id = :id")
    suspend fun loesche(id: String)
}

@Dao
interface FortschrittDao {
    @Query("SELECT * FROM haken WHERE paketId = :paketId")
    fun haken(paketId: String): Flow<List<HakenEntity>>

    @Query("SELECT * FROM haken WHERE paketId = :paketId")
    suspend fun hakenListe(paketId: String): List<HakenEntity>

    @Query("SELECT * FROM haken")
    fun alleHaken(): Flow<List<HakenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setzeHaken(h: HakenEntity)

    @Query("DELETE FROM haken WHERE paketId = :paketId AND schrittId = :schrittId AND `index` = :index")
    suspend fun entferneHaken(paketId: String, schrittId: String, index: Int)

    @Query("SELECT * FROM tipp_stand WHERE paketId = :paketId")
    fun tipps(paketId: String): Flow<List<TippStandEntity>>

    @Upsert
    suspend fun setzeTipps(t: TippStandEntity)

    @Query("SELECT * FROM pruef_antwort WHERE paketId = :paketId")
    fun pruefAntworten(paketId: String): Flow<List<PruefAntwortEntity>>

    @Upsert
    suspend fun setzePruefAntwort(a: PruefAntwortEntity)

    @Query("DELETE FROM pruef_antwort WHERE paketId = :paketId AND schrittId = :schrittId")
    suspend fun entfernePruefAntwort(paketId: String, schrittId: String)

    @Query("SELECT * FROM position ORDER BY zeit DESC")
    fun positionen(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM position WHERE paketId = :paketId")
    suspend fun position(paketId: String): PositionEntity?

    @Upsert
    suspend fun setzePosition(p: PositionEntity)

    @Query("SELECT * FROM uebung_ergebnis")
    fun ergebnisse(): Flow<List<UebungErgebnisEntity>>

    @Query("SELECT * FROM uebung_ergebnis WHERE paketId = :paketId")
    fun ergebnisse(paketId: String): Flow<List<UebungErgebnisEntity>>

    @Upsert
    suspend fun setzeErgebnis(e: UebungErgebnisEntity)

    @Query("DELETE FROM haken WHERE paketId = :paketId")
    suspend fun loescheHaken(paketId: String)

    @Query("DELETE FROM tipp_stand WHERE paketId = :paketId")
    suspend fun loescheTipps(paketId: String)

    @Query("DELETE FROM pruef_antwort WHERE paketId = :paketId")
    suspend fun loeschePruefAntworten(paketId: String)

    @Query("DELETE FROM position WHERE paketId = :paketId")
    suspend fun loeschePosition(paketId: String)

    @Query("DELETE FROM uebung_ergebnis WHERE paketId = :paketId")
    suspend fun loescheErgebnisse(paketId: String)
}

@Dao
interface FrageDao {
    @Query("SELECT * FROM frage ORDER BY erstellt DESC")
    fun alle(): Flow<List<FrageEntity>>

    @Query("SELECT * FROM frage WHERE status = :status ORDER BY erstellt")
    suspend fun mitStatus(status: String): List<FrageEntity>

    @Query("SELECT * FROM frage WHERE id = :id")
    suspend fun eine(id: String): FrageEntity?

    @Query("SELECT * FROM frage WHERE id = :id")
    fun beobachte(id: String): Flow<FrageEntity?>

    @Upsert
    suspend fun speichere(f: FrageEntity)

    @Query("UPDATE frage SET status = :status, fehler = :fehler WHERE id = :id")
    suspend fun setzeStatus(id: String, status: String, fehler: String?)

    @Query("UPDATE frage SET fehler = :fehler WHERE id = :id")
    suspend fun setzeFehler(id: String, fehler: String?)
}

/** Alles für Datensicherung (Export/Import) und „Fortschritt zurücksetzen“. */
@Dao
interface SicherungDao {
    @Query("SELECT * FROM haken") suspend fun haken(): List<HakenEntity>
    @Query("SELECT * FROM tipp_stand") suspend fun tipps(): List<TippStandEntity>
    @Query("SELECT * FROM pruef_antwort") suspend fun pruefAntworten(): List<PruefAntwortEntity>
    @Query("SELECT * FROM position") suspend fun positionen(): List<PositionEntity>
    @Query("SELECT * FROM uebung_ergebnis") suspend fun ergebnisse(): List<UebungErgebnisEntity>
    @Query("SELECT * FROM frage") suspend fun fragen(): List<FrageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun haken(l: List<HakenEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun tipps(l: List<TippStandEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun pruefAntworten(l: List<PruefAntwortEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun positionen(l: List<PositionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun ergebnisse(l: List<UebungErgebnisEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun fragen(l: List<FrageEntity>)

    @Query("DELETE FROM haken") suspend fun loescheHaken()
    @Query("DELETE FROM tipp_stand") suspend fun loescheTipps()
    @Query("DELETE FROM pruef_antwort") suspend fun loeschePruefAntworten()
    @Query("DELETE FROM position") suspend fun loeschePositionen()
    @Query("DELETE FROM uebung_ergebnis") suspend fun loescheErgebnisse()

    @androidx.room.Transaction
    suspend fun fortschrittLoeschen() {
        loescheHaken(); loescheTipps(); loeschePruefAntworten(); loeschePositionen(); loescheErgebnisse()
    }
}
