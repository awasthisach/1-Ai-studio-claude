package com.vvf.smartmanager.domain.repository

import com.vvf.smartmanager.domain.model.DuplicateGroup
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.model.StorageSummary
import com.vvf.smartmanager.domain.model.VaultItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun listFiles(directoryPath: String): Result<List<FileItem>>
    suspend fun copy(source: FileItem, destinationDir: String): Result<Unit>
    suspend fun move(source: FileItem, destinationDir: String): Result<Unit>
    suspend fun rename(source: FileItem, newName: String): Result<Unit>
    suspend fun moveToTrash(source: FileItem): Result<Unit>
    suspend fun restoreFromTrash(source: FileItem): Result<Unit>
    suspend fun permanentlyDelete(source: FileItem): Result<Unit>
    suspend fun listTrash(): Result<List<FileItem>>

    /** Full re-scan used by the background indexing worker (Phase 12). */
    suspend fun rescanAndIndex(rootPath: String): Result<Int>
}

interface VaultRepository {
    val isUnlocked: Flow<Boolean>
    suspend fun unlockWithPin(pin: String): Result<Boolean>
    fun unlockWithBiometric()
    suspend fun setPin(pin: String): Result<Unit>
    suspend fun hasPinConfigured(): Boolean
    fun lock()
    suspend fun addToVault(file: FileItem): Result<Unit>
    suspend fun removeFromVault(item: VaultItem, restoreTo: String): Result<Unit>
    fun observeVaultItems(): Flow<List<VaultItem>>
}

interface SearchRepository {
    /** Layered search: filename → metadata → OCR text. Never blocks on unavailable AI. */
    suspend fun search(query: String): Result<List<FileItem>>
}

interface StorageRepository {
    suspend fun getStorageSummary(): Result<StorageSummary>
}

interface DuplicateRepository {
    /** Level 1 (hash) + Level 2 (metadata) — always available offline, cheap. */
    suspend fun findExactAndMetadataDuplicates(rootPath: String): Result<List<DuplicateGroup>>

    /** Level 3 — perceptual hash visual similarity for images. */
    suspend fun findVisuallySimilar(rootPath: String, threshold: Float): Result<List<DuplicateGroup>>
}

interface OcrRepository {
    /** Extracts text from an image file using on-device ML Kit (Latin + Devanagari). */
    suspend fun extractText(imagePath: String): Result<String>
}
