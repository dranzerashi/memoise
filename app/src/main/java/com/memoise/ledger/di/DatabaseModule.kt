package com.memoise.ledger.di

import android.content.Context
import androidx.room.Room
import com.memoise.ledger.data.database.EntryDao
import com.memoise.ledger.data.database.EntryActivityDao
import com.memoise.ledger.data.database.EntryChecklistDao
import com.memoise.ledger.data.database.EntryDescriptionBlockDao
import com.memoise.ledger.data.database.EntryNoteDao
import com.memoise.ledger.data.database.EntryReminderDao
import com.memoise.ledger.data.database.RecurrenceSeriesDao
import com.memoise.ledger.data.database.EntryRelationshipDao
import com.memoise.ledger.data.database.MemoiseDatabase
import com.memoise.ledger.data.repository.EntryRepository
import com.memoise.ledger.data.repository.RoomEntryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMemoiseDatabase(
        @ApplicationContext context: Context,
    ): MemoiseDatabase = Room.databaseBuilder(
        context,
        MemoiseDatabase::class.java,
        "memoise-ledger.db",
    ).addMigrations(
        MemoiseDatabase.MIGRATION_1_2,
        MemoiseDatabase.MIGRATION_2_3,
        MemoiseDatabase.MIGRATION_3_4,
        MemoiseDatabase.MIGRATION_4_5,
        MemoiseDatabase.MIGRATION_5_6,
        MemoiseDatabase.MIGRATION_6_7,
        MemoiseDatabase.MIGRATION_7_8,
        MemoiseDatabase.MIGRATION_8_9,
        MemoiseDatabase.MIGRATION_9_10,
    ).build()

    @Provides
    fun provideEntryDao(database: MemoiseDatabase): EntryDao = database.entryDao()

    @Provides
    fun provideEntryRelationshipDao(database: MemoiseDatabase): EntryRelationshipDao =
        database.entryRelationshipDao()

    @Provides
    fun provideEntryChecklistDao(database: MemoiseDatabase): EntryChecklistDao =
        database.entryChecklistDao()

    @Provides
    fun provideEntryActivityDao(database: MemoiseDatabase): EntryActivityDao =
        database.entryActivityDao()

    @Provides
    fun provideEntryDescriptionBlockDao(database: MemoiseDatabase): EntryDescriptionBlockDao =
        database.entryDescriptionBlockDao()

    @Provides
    fun provideEntryNoteDao(database: MemoiseDatabase): EntryNoteDao = database.entryNoteDao()

    @Provides
    fun provideEntryReminderDao(database: MemoiseDatabase): EntryReminderDao = database.entryReminderDao()

    @Provides
    fun provideRecurrenceSeriesDao(database: MemoiseDatabase): RecurrenceSeriesDao = database.recurrenceSeriesDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEntryRepository(implementation: RoomEntryRepository): EntryRepository
}
