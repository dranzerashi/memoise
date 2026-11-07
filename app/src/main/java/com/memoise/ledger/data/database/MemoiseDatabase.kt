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
        EntrySearchFtsEntity::class,
    ],
    version = 11,
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
    abstract fun entrySearchDao(): EntrySearchDao

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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE VIRTUAL TABLE IF NOT EXISTS `ledger_search_fts` USING FTS4(
                        `entryId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `checklist` TEXT NOT NULL,
                        `relatedTitles` TEXT NOT NULL
                    )""".trimIndent(),
                )
                SEARCH_INDEX_TRIGGERS.forEach(db::execSQL)
                db.execSQL(
                    """INSERT INTO `ledger_search_fts`
                        (`rowid`, `entryId`, `title`, `description`, `notes`, `checklist`, `relatedTitles`)
                        SELECT entry.`rowid`, entry.`id`, entry.`title`,
                            COALESCE((SELECT group_concat(block.`content`, ' ')
                                FROM `entry_description_blocks` AS block WHERE block.`entryId` = entry.`id`), ''),
                            COALESCE((SELECT group_concat(note.`content`, ' ')
                                FROM `entry_notes` AS note WHERE note.`entryId` = entry.`id`), ''),
                            COALESCE((SELECT group_concat(item.`title`, ' ')
                                FROM `entry_checklist_items` AS item WHERE item.`entryId` = entry.`id`), ''),
                            COALESCE((SELECT group_concat(other.`title`, ' ')
                                FROM `entry_relationships` AS relationship
                                JOIN `ledger_entries` AS other ON other.`id` = CASE
                                    WHEN relationship.`sourceEntryId` = entry.`id` THEN relationship.`targetEntryId`
                                    ELSE relationship.`sourceEntryId` END
                                WHERE relationship.`sourceEntryId` = entry.`id`
                                   OR relationship.`targetEntryId` = entry.`id`), '')
                        FROM `ledger_entries` AS entry""".trimIndent(),
                )
            }
        }

        private val SEARCH_INDEX_TRIGGERS = listOf(
            """CREATE TRIGGER `ledger_search_entry_insert` AFTER INSERT ON `ledger_entries` BEGIN
                INSERT OR REPLACE INTO `ledger_search_fts`
                    (`rowid`, `entryId`, `title`, `description`, `notes`, `checklist`, `relatedTitles`)
                VALUES (NEW.`rowid`, NEW.`id`, NEW.`title`,
                    COALESCE((SELECT group_concat(`content`, ' ') FROM `entry_description_blocks` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(`content`, ' ') FROM `entry_notes` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(other.`title`, ' ')
                        FROM `entry_relationships` AS relationship
                        JOIN `ledger_entries` AS other ON other.`id` = CASE
                            WHEN relationship.`sourceEntryId` = NEW.`id` THEN relationship.`targetEntryId`
                            ELSE relationship.`sourceEntryId` END
                        WHERE relationship.`sourceEntryId` = NEW.`id`
                           OR relationship.`targetEntryId` = NEW.`id`), ''));
            END""",
            """CREATE TRIGGER `ledger_search_entry_title_update` AFTER UPDATE OF `title` ON `ledger_entries` BEGIN
                INSERT OR REPLACE INTO `ledger_search_fts`
                    (`rowid`, `entryId`, `title`, `description`, `notes`, `checklist`, `relatedTitles`)
                VALUES (NEW.`rowid`, NEW.`id`, NEW.`title`,
                    COALESCE((SELECT group_concat(`content`, ' ') FROM `entry_description_blocks` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(`content`, ' ') FROM `entry_notes` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = NEW.`id`), ''),
                    COALESCE((SELECT group_concat(other.`title`, ' ')
                        FROM `entry_relationships` AS relationship
                        JOIN `ledger_entries` AS other ON other.`id` = CASE
                            WHEN relationship.`sourceEntryId` = NEW.`id` THEN relationship.`targetEntryId`
                            ELSE relationship.`sourceEntryId` END
                        WHERE relationship.`sourceEntryId` = NEW.`id`
                           OR relationship.`targetEntryId` = NEW.`id`), ''));
                UPDATE `ledger_search_fts` SET `relatedTitles` = COALESCE((
                    SELECT group_concat(other.`title`, ' ')
                    FROM `entry_relationships` AS relationship
                    JOIN `ledger_entries` AS other ON other.`id` = CASE
                        WHEN relationship.`sourceEntryId` = `ledger_search_fts`.`entryId` THEN relationship.`targetEntryId`
                        ELSE relationship.`sourceEntryId` END
                    WHERE relationship.`sourceEntryId` = `ledger_search_fts`.`entryId`
                       OR relationship.`targetEntryId` = `ledger_search_fts`.`entryId`), '')
                WHERE `entryId` IN (
                    SELECT `sourceEntryId` FROM `entry_relationships` WHERE `targetEntryId` = NEW.`id`
                    UNION SELECT `targetEntryId` FROM `entry_relationships` WHERE `sourceEntryId` = NEW.`id`
                );
            END""",
            """CREATE TRIGGER `ledger_search_entry_delete` AFTER DELETE ON `ledger_entries` BEGIN
                DELETE FROM `ledger_search_fts` WHERE `rowid` = OLD.`rowid`;
                UPDATE `ledger_search_fts` SET `relatedTitles` = COALESCE((
                    SELECT group_concat(other.`title`, ' ')
                    FROM `entry_relationships` AS relationship
                    JOIN `ledger_entries` AS other ON other.`id` = CASE
                        WHEN relationship.`sourceEntryId` = `ledger_search_fts`.`entryId` THEN relationship.`targetEntryId`
                        ELSE relationship.`sourceEntryId` END
                    WHERE relationship.`sourceEntryId` = `ledger_search_fts`.`entryId`
                       OR relationship.`targetEntryId` = `ledger_search_fts`.`entryId`), '')
                WHERE `entryId` IN (
                    SELECT `sourceEntryId` FROM `entry_relationships` WHERE `targetEntryId` = OLD.`id`
                    UNION SELECT `targetEntryId` FROM `entry_relationships` WHERE `sourceEntryId` = OLD.`id`
                );
            END""",
            """CREATE TRIGGER `ledger_search_description_insert` AFTER INSERT ON `entry_description_blocks` BEGIN
                UPDATE `ledger_search_fts` SET `description` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_description_blocks` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_description_update` AFTER UPDATE ON `entry_description_blocks` BEGIN
                UPDATE `ledger_search_fts` SET `description` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_description_blocks` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_description_delete` AFTER DELETE ON `entry_description_blocks` BEGIN
                UPDATE `ledger_search_fts` SET `description` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_description_blocks` WHERE `entryId` = OLD.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = OLD.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_note_insert` AFTER INSERT ON `entry_notes` BEGIN
                UPDATE `ledger_search_fts` SET `notes` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_notes` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_note_update` AFTER UPDATE ON `entry_notes` BEGIN
                UPDATE `ledger_search_fts` SET `notes` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_notes` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_note_delete` AFTER DELETE ON `entry_notes` BEGIN
                UPDATE `ledger_search_fts` SET `notes` = COALESCE((
                    SELECT group_concat(`content`, ' ') FROM `entry_notes` WHERE `entryId` = OLD.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = OLD.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_checklist_insert` AFTER INSERT ON `entry_checklist_items` BEGIN
                UPDATE `ledger_search_fts` SET `checklist` = COALESCE((
                    SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_checklist_update` AFTER UPDATE ON `entry_checklist_items` BEGIN
                UPDATE `ledger_search_fts` SET `checklist` = COALESCE((
                    SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = NEW.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = NEW.`entryId`);
                UPDATE `ledger_search_fts` SET `checklist` = COALESCE((
                    SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = OLD.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = OLD.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_checklist_delete` AFTER DELETE ON `entry_checklist_items` BEGIN
                UPDATE `ledger_search_fts` SET `checklist` = COALESCE((
                    SELECT group_concat(`title`, ' ') FROM `entry_checklist_items` WHERE `entryId` = OLD.`entryId`), '')
                WHERE `rowid` = (SELECT `rowid` FROM `ledger_entries` WHERE `id` = OLD.`entryId`);
            END""",
            """CREATE TRIGGER `ledger_search_relationship_insert` AFTER INSERT ON `entry_relationships` BEGIN
                UPDATE `ledger_search_fts` SET `relatedTitles` = COALESCE((
                    SELECT group_concat(other.`title`, ' ')
                    FROM `entry_relationships` AS relationship
                    JOIN `ledger_entries` AS other ON other.`id` = CASE
                        WHEN relationship.`sourceEntryId` = `ledger_search_fts`.`entryId` THEN relationship.`targetEntryId`
                        ELSE relationship.`sourceEntryId` END
                    WHERE relationship.`sourceEntryId` = `ledger_search_fts`.`entryId`
                       OR relationship.`targetEntryId` = `ledger_search_fts`.`entryId`), '')
                WHERE `entryId` IN (NEW.`sourceEntryId`, NEW.`targetEntryId`);
            END""",
            """CREATE TRIGGER `ledger_search_relationship_delete` AFTER DELETE ON `entry_relationships` BEGIN
                UPDATE `ledger_search_fts` SET `relatedTitles` = COALESCE((
                    SELECT group_concat(other.`title`, ' ')
                    FROM `entry_relationships` AS relationship
                    JOIN `ledger_entries` AS other ON other.`id` = CASE
                        WHEN relationship.`sourceEntryId` = `ledger_search_fts`.`entryId` THEN relationship.`targetEntryId`
                        ELSE relationship.`sourceEntryId` END
                    WHERE relationship.`sourceEntryId` = `ledger_search_fts`.`entryId`
                       OR relationship.`targetEntryId` = `ledger_search_fts`.`entryId`), '')
                WHERE `entryId` IN (OLD.`sourceEntryId`, OLD.`targetEntryId`);
            END""",
        )
    }
}
