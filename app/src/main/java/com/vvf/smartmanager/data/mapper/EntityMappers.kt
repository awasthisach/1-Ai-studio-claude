package com.vvf.smartmanager.data.mapper

import com.vvf.smartmanager.data.local.entity.FileEntryEntity
import com.vvf.smartmanager.data.local.entity.VaultItemEntity
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.model.VaultItem

fun FileEntryEntity.toDomain(): FileItem = FileItem(
    path = path,
    name = name,
    isDirectory = isDirectory,
    sizeBytes = sizeBytes,
    lastModifiedEpochMillis = lastModifiedEpochMillis,
    mimeType = mimeType,
    isInTrash = isInTrash,
    originalPathBeforeTrash = originalPathBeforeTrash
)

fun VaultItemEntity.toDomain(): VaultItem = VaultItem(
    id = id,
    originalPath = originalPath,
    encryptedPath = encryptedPath,
    displayName = displayName,
    sizeBytes = sizeBytes,
    addedEpochMillis = addedEpochMillis,
    mimeType = mimeType
)
