package com.vvf.smartmanager.domain.usecase.filemanager

import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.repository.FileRepository
import javax.inject.Inject

class ListFilesUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(directoryPath: String): Result<List<FileItem>> =
        repository.listFiles(directoryPath)
}

class CopyFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem, destinationDir: String): Result<Unit> =
        repository.copy(source, destinationDir)
}

class MoveFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem, destinationDir: String): Result<Unit> =
        repository.move(source, destinationDir)
}

class RenameFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem, newName: String): Result<Unit> {
        if (newName.isBlank()) return Result.failure(IllegalArgumentException("नाम खाली नहीं हो सकता"))
        if (newName.any { it in ILLEGAL_CHARS }) {
            return Result.failure(IllegalArgumentException("नाम में / \\ : * ? \" < > | जैसे characters नहीं हो सकते"))
        }
        return repository.rename(source, newName)
    }

    private companion object {
        val ILLEGAL_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    }
}

class DeleteToTrashUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem): Result<Unit> = repository.moveToTrash(source)
}

class RestoreFromTrashUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem): Result<Unit> = repository.restoreFromTrash(source)
}

class PermanentDeleteUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(source: FileItem): Result<Unit> = repository.permanentlyDelete(source)
}

class ListTrashUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(): Result<List<FileItem>> = repository.listTrash()
}
