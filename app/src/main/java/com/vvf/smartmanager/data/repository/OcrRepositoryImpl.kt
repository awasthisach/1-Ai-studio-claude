package com.vvf.smartmanager.data.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vvf.smartmanager.core.di.IoDispatcher
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.domain.repository.OcrRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Engine (Phase 8, plugin module). Uses on-device ML Kit — no server call, no API key.
 * Runs BOTH the Latin and Devanagari recognizers since VVF content is bilingual
 * (Hindi + English), and merges their output into the FTS search index.
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OcrRepository {

    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    override suspend fun extractText(imagePath: String): Result<String> =
        withContext(ioDispatcher) {
            runCatching {
                val file = File(imagePath)
                require(file.exists()) { "Image file नहीं मिली: $imagePath" }
                val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                val latinText = recognize(latinRecognizer, image)
                val devanagariText = recognize(devanagariRecognizer, image)

                val combined = listOf(latinText, devanagariText)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("\n")

                // Persist into the index so FTS search (Phase 7) can find it immediately.
                dao.findByPath(imagePath)?.let { entity ->
                    dao.update(entity.copy(extractedOcrText = combined))
                }
                combined
            }
        }

    private suspend fun recognize(
        recognizer: TextRecognizer,
        image: InputImage
    ): String = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { result -> continuation.resume(result.text) }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
}
