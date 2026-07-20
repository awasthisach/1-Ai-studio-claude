package com.vvf.smartmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Indexed representation of a file on disk. Populated by the background indexing worker
 * (Phase 12) and updated immediately by file-manager operations for responsiveness.
 *
 * `path` has a UNIQUE index (audit finding, 18 July 2026): without it, every findByPath/
 * deleteByPath query did a full table scan, AND — more seriously — since fresh index entries
 * are always created with `id = 0` (autogenerate), nothing stopped the same physical file
 * from accumulating duplicate rows across separate indexSingle()/rescanAndIndex() calls.
 * The unique constraint makes Room's REPLACE conflict strategy correctly collapse to one
 * row per path even for fresh (id=0) inserts, not just for updates that already know the id.
 */
@Entity(tableName = "file_entries", indices = [Index(value = ["path"], unique = true)])
data class FileEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
    val mimeType: String?,
    val sha256Hash: String? = null,
    val perceptualHash: Long? = null,
    val extractedOcrText: String? = null,
    val isInTrash: Boolean = false,
    val originalPathBeforeTrash: String? = null
)

/**
 * FTS4 shadow table over [FileEntryEntity] — powers Search Engine (Phase 7) and
 * the OCR-text layer of the search pipeline (Phase 8). `rowid` is kept in sync with
 * [FileEntryEntity.id] by Room automatically because this is a content-linked FTS table.
 */
@Fts4(contentEntity = FileEntryEntity::class)
@Entity(tableName = "file_entries_fts")
data class FileEntryFts(
    val name: String,
    val extractedOcrText: String?
)
