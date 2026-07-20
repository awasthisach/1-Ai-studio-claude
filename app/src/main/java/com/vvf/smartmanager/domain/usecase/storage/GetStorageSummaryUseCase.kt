package com.vvf.smartmanager.domain.usecase.storage

import com.vvf.smartmanager.domain.model.StorageSummary
import com.vvf.smartmanager.domain.repository.StorageRepository
import javax.inject.Inject

class GetStorageSummaryUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(): Result<StorageSummary> = repository.getStorageSummary()
}
