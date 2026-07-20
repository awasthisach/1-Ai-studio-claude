package com.vvf.smartmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val encryptedPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val addedEpochMillis: Long,
    val mimeType: String?
)
