package com.dj.photobooth.gallery

import kotlinx.coroutines.flow.Flow

/**
 * Local history/archive of saved strips (architecture.md: "Gallery Repository - Room KMP,
 * uncapped"). An interface, not a direct Room dependency for callers - GalleryViewModel and
 * StripPreviewViewModel depend on this, not on AppDatabase/HistoryDao directly, the same way
 * CaptureViewModel depends on the CameraController interface rather than AndroidCameraController.
 * That's what lets tests use a fake in-memory implementation instead of a real database
 * (see GalleryViewModelTest), matching CaptureViewModelTest's FakeCameraController pattern.
 */
interface GalleryRepo {
    /** All saved strips, newest first. Uncapped - see [HistoryEntry]'s doc comment. */
    val entries: Flow<List<HistoryEntry>>

    /** Archives a newly-saved strip. Returns the assigned row id. */
    suspend fun save(entry: HistoryEntry): Long

    /** Manual deletion only - CLAUDE.md forbids automatic eviction. */
    suspend fun delete(entry: HistoryEntry)
}

/** [GalleryRepo] backed by the real [AppDatabase]/[HistoryDao]. */
class RoomGalleryRepo(database: AppDatabase) : GalleryRepo {
    private val dao = database.historyDao()

    override val entries: Flow<List<HistoryEntry>> = dao.observeAll()

    override suspend fun save(entry: HistoryEntry): Long = dao.insert(entry)

    override suspend fun delete(entry: HistoryEntry) = dao.delete(entry)
}
