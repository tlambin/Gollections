package com.pokyx.gollections.domain.usecase

import android.net.Uri
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import javax.inject.Inject

class ProcessImageUseCase @Inject constructor(
    private val imageProcessor: ImageProcessorRepository
) {
    suspend operator fun invoke(sourceUri: Uri, shouldCutout: Boolean): String? {
        val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
        return resultUri?.toString()
    }
}