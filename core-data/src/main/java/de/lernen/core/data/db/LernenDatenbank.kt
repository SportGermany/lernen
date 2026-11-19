package de.lernen.core.data.db

import androidx.room.AutoMigration
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
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class LernenDatenbank : RoomDatabase() {
    abstract fun paketDao(): PaketDao
    abstract fun fortschrittDao(): FortschrittDao
    abstract fun frageDao(): FrageDao
    abstract fun sicherungDao(): SicherungDao

    companion object {
        const val NAME = "lernen.db"
    }
}
