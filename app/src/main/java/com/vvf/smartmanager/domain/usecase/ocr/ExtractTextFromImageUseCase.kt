package com.vvf.smartmanager.domain.usecase.ocr

import com.vvf.smartmanager.domain.repository.OcrRepository
import javax.inject.Inject

class ExtractTextFromImageUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(imagePath: String): Result<String> = repository.extractText(imagePath)
}
