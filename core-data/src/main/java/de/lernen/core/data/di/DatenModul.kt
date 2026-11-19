package de.lernen.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.lernen.core.data.db.FortschrittDao
import de.lernen.core.data.db.LernenDatenbank
import de.lernen.core.data.db.PaketDao
import de.lernen.core.data.lernplan.KeinLernplan
import de.lernen.core.data.lernplan.LernplanQuelle
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatenModul {
    @Provides
    @Singleton
    fun datenbank(@ApplicationContext context: Context): LernenDatenbank =
        Room.databaseBuilder(context, LernenDatenbank::class.java, LernenDatenbank.NAME).build()

    @Provides
    fun paketDao(db: LernenDatenbank): PaketDao = db.paketDao()

    @Provides
    fun fortschrittDao(db: LernenDatenbank): FortschrittDao = db.fortschrittDao()

    @Provides
    @Singleton
    fun lernplan(): LernplanQuelle = KeinLernplan()
}
