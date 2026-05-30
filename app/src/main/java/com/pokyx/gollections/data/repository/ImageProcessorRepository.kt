package com.pokyx.gollections.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.min

@Singleton
class ImageProcessorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun processImage(sourceUri: Uri, shouldCutout: Boolean): Uri? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, sourceUri)
            }

            // 1. Redimensionner l'image pour éviter de saturer la mémoire et le stockage
            val resizedBitmap = resizeBitmap(originalBitmap, maxSize = 1024)

            if (!shouldCutout) {
                // Pas de détourage = Pas de transparence requise
                return@withContext saveBitmapToInternalStorage(resizedBitmap, isTransparent = false)
            }

            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
            val segmenter = SubjectSegmentation.getClient(options)

            suspendCancellableCoroutine { continuation ->
                segmenter.process(InputImage.fromBitmap(resizedBitmap, 0))
                    .addOnSuccessListener { result ->
                        val cutoutBitmap = result.foregroundBitmap
                        if (cutoutBitmap != null) {
                            // Détourage réussi = Transparence requise
                            continuation.resume(saveBitmapToInternalStorage(cutoutBitmap, isTransparent = true))
                        } else {
                            // Échec du détourage (aucun sujet trouvé), on sauvegarde l'image redimensionnée normale
                            continuation.resume(saveBitmapToInternalStorage(resizedBitmap, isTransparent = false))
                        }
                    }
                    .addOnFailureListener {
                        // Erreur de ML Kit, on sauvegarde l'image redimensionnée normale
                        continuation.resume(saveBitmapToInternalStorage(resizedBitmap, isTransparent = false))
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Redimensionnement proportionnel ---
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Si l'image est déjà plus petite que la limite, on ne fait rien
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // --- Sauvegarde optimisée en WebP ---
    private fun saveBitmapToInternalStorage(bitmap: Bitmap, isTransparent: Boolean): Uri {
        val file = File(context.filesDir, "gollections_img_${System.currentTimeMillis()}.webp")

        FileOutputStream(file).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) et plus
                if (isTransparent) {
                    // WEBP_LOSSLESS = Sans perte (parfait pour le détourage transparent)
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    // WEBP_LOSSY = Avec perte (comme le JPEG, on garde à 80%)
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                }
            } else { // Pour les anciens appareils
                @Suppress("DEPRECATION")
                // Sur les vieilles API, le format WEBP unique gérait la transparence automatiquement si la qualité était à 100
                val quality = if (isTransparent) 100 else 80
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out)
            }
        }
        return Uri.fromFile(file)
    }
}