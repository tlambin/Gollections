package com.pokyx.gollections.domain.usecase

import android.graphics.Bitmap
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import javax.inject.Inject

class ProcessImageUseCase @Inject constructor(
    private val imageProcessor: ImageProcessorRepository
) {
    // On prend désormais un Bitmap (l'image recadrée) au lieu d'une Uri
    suspend operator fun invoke(bitmap: Bitmap, shouldCutout: Boolean): String? =
        imageProcessor.processAndSaveBitmap(bitmap, shouldCutout)?.toString()
}