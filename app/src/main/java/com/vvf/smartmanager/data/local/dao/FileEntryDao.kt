package com.vvf.smartmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vvf.smartmanager.data.local.entity.FileEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FileEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<FileEntryEntity>)

    @Update
    suspend fun update(entry: FileEntryEntity)

    @Delete
    suspend fun delete(entry: FileEntryEntity)

    @Query("DELETE FROM file_entries WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT * FROM file_entries WHERE path = :path LIMIT 1")
    suspend fun findByPath(path: String): FileEntryEntity?

    @Query("SELECT * FROM file_entries WHERE isInTrash = 0 AND path LIKE :directoryPrefix || '%' AND path NOT LIKE :directoryPrefix || '%/%'")
    suspend fun listDirectChildren(directoryPrefix: String): List<FileEntryEntity>

    @Query("SELECT * FROM file_entries WHERE isInTrash = 1 ORDER BY lastModifiedEpochMillis DESC")
    suspend fun listTrash(): List<FileEntryEntity>

    @Query("SELECT * FROM file_entries WHERE isDirectory = 0 AND isInTrash = 0")
    suspend fun listAllFiles(): List<FileEntryEntity>

    @Query("SELECT * FROM file_entries WHERE isDirectory = 0 AND isInTrash = 0 AND sha256Hash IS NOT NULL")
    suspend fun listHashedFiles(): List<FileEntryEntity>

    @Query("SELECT * FROM file_entries WHERE isDirectory = 0 AND isInTrash = 0 AND perceptualHash IS NOT NULL")
    suspend fun listPerceptualHashedFiles(): List<FileEntryEntity>

    @Query(
        """
        SELECT file_entries.* FROM file_entries
        JOIN file_entries_fts ON file_entries.id = file_entries_fts.rowid
        WHERE file_entries_fts MATCH :ftsQuery AND file_entries.isInTrash = 0
        LIMIT 200
        """
    )
    suspend fun searchByFtsMatch(ftsQuery: String): List<FileEntryEntity>

    @Query("SELECT * FROM file_entries WHERE isInTrash = 0 AND name LIKE '%' || :fragment || '%' LIMIT 200")
    suspend fun searchByNameFallback(fragment: String): List<FileEntryEntity>

    fun observeStorageTotals(): Flow<List<FileEntryEntity>> = observeAllNonTrashFiles()

    @Query("SELECT * FROM file_entries WHERE isInTrash = 0 AND isDirectory = 0")
    fun observeAllNonTrashFiles(): Flow<List<FileEntryEntity>>
}
