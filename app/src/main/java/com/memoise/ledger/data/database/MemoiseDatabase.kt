package com.memoise.ledger.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        EntryEntity::class,
        EntryRelationshipEntity::class,
        EntryChecklistEntity::class,
        EntryActivityEntity::class,
        EntryDescriptionBlockEntity::class,
        EntryNoteEntity::class,
        EntryReminderEntity::class,
        RecurrenceSeriesEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(EntryConverters::class)
abstract class MemoiseDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun entryRelationshipDao(): EntryRelationshipDao
    abstract fun entryChecklistDao(): EntryChecklistDao
    abstract fun entryActivityDao(): EntryActivityDao
    abstract fun entryDescriptionBlockDao(): EntryDescriptionBlockDao
    abstract fun entryNoteDao(): EntryNoteDao
    abstract fun entryReminderDao(): EntryReminderDao
    abstract fun recurrenceSeriesDao(): RecurrenceSeriesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_relationships` (
                        `id` TEXT NOT NULL,
                        `sourceEntryId` TEXT NOT NULL,
                        `targetEntryId` TEXT NOT NULL,
                        `relationshipType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceEntryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`targetEntryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_relationships_sourceEntryId` ON `entry_relationships` (`sourceEntryId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_relationships_targetEntryId` ON `entry_relationships` (`targetEntryId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_entry_relationships_sourceEntryId_targetEntryId_relationshipType` ON `entry_relationships` (`sourceEntryId`, `targetEntryId`, `relationshipType`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_checklist_items` (
                        `id` TEXT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `linkedEntryId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`entryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`linkedEntryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_checklist_items_entryId_sortOrder` ON `entry_checklist_items` (`entryId`, `sortOrder`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_checklist_items_linkedEntryId` ON `entry_checklist_items` (`linkedEntryId`)",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_activities` (
                        `id` TEXT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `details` TEXT,
                        `occurredAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`entryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_activities_entryId_occurredAt` ON `entry_activities` (`entryId`, `occurredAt`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_description_blocks` (
                        `id` TEXT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`entryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_description_blocks_entryId_sortOrder` ON `entry_description_blocks` (`entryId`, `sortOrder`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_notes` (
                        `id` TEXT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`entryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_notes_entryId_createdAt` ON `entry_notes` (`entryId`, `createdAt`)",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entry_reminders` (
                        `id` TEXT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `triggerAt` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`entryId`) REFERENCES `ledger_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_reminders_entryId_triggerAt` ON `entry_reminders` (`entryId`, `triggerAt`)",
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recurrence_series` (
                        `id` TEXT NOT NULL,
                        `ruleJson` TEXT NOT NULL,
                        `startsOn` INTEGER NOT NULL,
                        `endsOn` INTEGER,
                        `timeZoneId` TEXT NOT NULL,
                        `active` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry_reminders` ADD COLUMN `relativeOffsetDays` INTEGER")
                db.execSQL("ALTER TABLE `entry_reminders` ADD COLUMN `relativeMinuteOfDay` INTEGER")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry_reminders` ADD COLUMN `parentReminderId` TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """INSERT INTO `entry_relationships`
                        (`id`, `sourceEntryId`, `targetEntryId`, `relationshipType`, `createdAt`)
                        SELECT lower(hex(randomblob(16))), parent.`id`, child.`id`, 'RECURRENCE', child.`createdAt`
                        FROM `ledger_entries` AS child
                        JOIN `ledger_entries` AS parent
                          ON parent.`recurrenceSeriesId` = child.`recurrenceSeriesId`
                        WHERE child.`recurrenceSeriesId` IS NOT NULL
                          AND parent.`id` = (
                            SELECT candidate.`id` FROM `ledger_entries` AS candidate
                            WHERE candidate.`recurrenceSeriesId` = child.`recurrenceSeriesId`
                            ORDER BY candidate.`createdAt` ASC, candidate.`id` ASC LIMIT 1
                          )
                          AND parent.`id` <> child.`id`""".trimIndent(),
                )
            }
        }
    }
}
