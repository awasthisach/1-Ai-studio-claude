package com.vvf.smartmanager.domain.model

data class VaultItem(
    val id: Long,
    val originalPath: String,
    val encryptedPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val addedEpochMillis: Long,
    val mimeType: String?
)

data class StorageSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val byCategory: Map<FileCategory, Long>
)

/**
 * Result of the Duplicate Cleaner engine (Master Specification v2.0, Section 9).
 * [level] tells the UI which detection level found the group, so it can show a
 * confidence label ("Exact", "Same metadata", "Visually similar").
 */
data class DuplicateGroup(
    val groupId: String,
    val level: DuplicateLevel,
    val items: List<FileItem>,
    val potentialSavingsBytes: Long
)

enum class DuplicateLevel {
    EXACT_HASH,       // Level 1 — SHA-256 match
    METADATA_MATCH,   // Level 2 — size + timestamp + resolution match
    VISUAL_SIMILAR    // Level 3 — perceptual hash within threshold
    // Level 4 (semantic AI) intentionally not modeled yet — see docs/PENDING_WORK.md
}
