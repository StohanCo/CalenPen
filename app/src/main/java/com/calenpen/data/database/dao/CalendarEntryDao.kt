package com.calenpen.data.database.dao

import androidx.room.*
import com.calenpen.data.database.entities.CalendarEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: CalendarEntry)

    @Delete
    suspend fun delete(entry: CalendarEntry)

    @Query("SELECT * FROM calendar_entries WHERE dateKey = :dateKey")
    suspend fun getEntry(dateKey: String): CalendarEntry?

    @Query("SELECT * FROM calendar_entries WHERE dateKey = :dateKey")
    fun observeEntry(dateKey: String): Flow<CalendarEntry?>

    @Query("SELECT * FROM calendar_entries WHERE dateKey LIKE :monthPrefix || '%' ORDER BY dateKey ASC")
    fun observeEntriesForMonth(monthPrefix: String): Flow<List<CalendarEntry>>

    @Query("SELECT * FROM calendar_entries WHERE dateKey BETWEEN :startDate AND :endDate ORDER BY dateKey ASC")
    fun observeEntriesInRange(startDate: String, endDate: String): Flow<List<CalendarEntry>>

    @Query("SELECT * FROM calendar_entries WHERE isHighlighted = 1 ORDER BY dateKey ASC")
    fun observeHighlightedEntries(): Flow<List<CalendarEntry>>

    @Query("UPDATE calendar_entries SET noteCount = :count, previewText = :preview, updatedAt = :now WHERE dateKey = :dateKey")
    suspend fun updateCountAndPreview(dateKey: String, count: Int, preview: String, now: Long = System.currentTimeMillis())
}
