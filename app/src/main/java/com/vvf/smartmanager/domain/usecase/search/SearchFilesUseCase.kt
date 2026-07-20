package com.vvf.smartmanager.domain.usecase.search

import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.repository.SearchRepository
import javax.inject.Inject

class SearchFilesUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): Result<List<FileItem>> = repository.search(query)
}
