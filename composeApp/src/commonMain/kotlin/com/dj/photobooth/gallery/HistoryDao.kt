package com.dj.photobooth.gallery

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [HistoryEntry]. Newest-first ordering matches design/handoff/README.md §4's
 * "gallery: array of ..., newest first" - the Gallery grid never re-sorts client-side.
 */
@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("SELECT * FROM history_entry ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntry>>
}
