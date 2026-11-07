package com.memoise.ledger.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.memoise.ledger.domain.model.DatePurpose
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.EntryType
import com.memoise.ledger.domain.model.RelationshipType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index(value = ["scheduledDate"]),
        Index(value = ["status"]),
        Index(value = ["recurrenceSeriesId"]),
    ],
)
@TypeConverters(EntryConverters::class)
data class EntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "entryType") val type: EntryType?,
    val status: EntryStatus,
    val scheduledDate: LocalDate?,
    val scheduledTime: LocalTime?,
    val descriptiveEndDate: LocalDate?,
    val datePurpose: DatePurpose,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val recurrenceSeriesId: String?,
    val sourceEntryId: String?,
    val sourceContentId: String?,
)

class EntryConverters {
    @TypeConverter
    fun relationshipTypeToString(value: RelationshipType): String = value.name

    @TypeConverter
    fun stringToRelationshipType(value: String): RelationshipType = RelationshipType.valueOf(value)

    @TypeConverter
    fun entryTypeToString(value: EntryType?): String? = value?.name

    @TypeConverter
    fun stringToEntryType(value: String?): EntryType? = value?.let { EntryType.valueOf(it) }

    @TypeConverter
    fun statusToString(value: EntryStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): EntryStatus = EntryStatus.valueOf(value)

    @TypeConverter
    fun datePurposeToString(value: DatePurpose): String = value.name

    @TypeConverter
    fun stringToDatePurpose(value: String): DatePurpose = DatePurpose.valueOf(value)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun localTimeToMinuteOfDay(value: LocalTime?): Int? = value?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun minuteOfDayToLocalTime(value: Int?): LocalTime? = value?.let { LocalTime.of(it / 60, it % 60) }
}
