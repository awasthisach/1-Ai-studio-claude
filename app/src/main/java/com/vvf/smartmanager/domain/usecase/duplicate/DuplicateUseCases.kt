package com.vvf.smartmanager.domain.usecase.duplicate

import com.vvf.smartmanager.domain.model.DuplicateGroup
import com.vvf.smartmanager.domain.repository.DuplicateRepository
import javax.inject.Inject

class FindExactDuplicatesUseCase @Inject constructor(
    private val repository: DuplicateRepository
) {
    suspend operator fun invoke(rootPath: String): Result<List<DuplicateGroup>> =
        repository.findExactAndMetadataDuplicates(rootPath)
}

class FindSimilarImagesUseCase @Inject constructor(
    private val repository: DuplicateRepository
) {
    /** [threshold] is 0f..1f — UI exposes this as a 70%-95% slider (Blueprint design). */
    suspend operator fun invoke(rootPath: String, threshold: Float): Result<List<DuplicateGroup>> {
        require(threshold in 0f..1f) { "threshold 0..1 के बीच होना चाहिए" }
        return repository.findVisuallySimilar(rootPath, threshold)
    }
}
