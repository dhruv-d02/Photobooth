package com.dj.photobooth.gallery

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved strip/grid, per architecture.md's "Core domain model" § HistoryEntry - the Room
 * row backing the Gallery ("Past strips") screen. Deliberately small (no sync/account
 * fields), and uncapped - CLAUDE.md explicitly forbids an eviction limit unlike the
 * prototype's 12-item localStorage cap; deletion is manual only (see [GalleryRepo.delete]).
 *
 * [filmTreatmentId] stores [com.dj.photobooth.filter.FilmTreatment.code] (e.g. "F01"), not
 * the enum itself - Room entities should hold plain persistable data, and a String survives
 * a future rename/reorder of the FilmTreatment enum without a migration.
 */
@Entity(tableName = "history_entry")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finalImagePath: String,
    val thumbnailPath: String,
    val filmTreatmentId: String,
    val createdAt: Long,
    val stamp: String,
)
