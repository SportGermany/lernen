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
