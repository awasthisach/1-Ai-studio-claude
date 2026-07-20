package com.vvf.smartmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vvf.smartmanager.data.local.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {

    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("SELECT * FROM vault_items ORDER BY addedEpochMillis DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): VaultItemEntity?
}
