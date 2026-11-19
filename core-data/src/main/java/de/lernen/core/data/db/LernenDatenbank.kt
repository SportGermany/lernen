package de.lernen.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PaketEntity::class,
        HakenEntity::class,
        TippStandEntity::class,
        PruefAntwortEntity::class,
        PositionEntity::class,
        UebungErgebnisEntity::class,
        FrageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LernenDatenbank : RoomDatabase() {
    abstract fun paketDao(): PaketDao
    abstract fun fortschrittDao(): FortschrittDao

    companion object {
        const val NAME = "lernen.db"
    }
}
