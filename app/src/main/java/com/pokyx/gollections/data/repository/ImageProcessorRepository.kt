package com.pokyx.gollections.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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
            val maxSize = 1024

            val processedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.isMutableRequired = true
                    val width = info.size.width
                    val height = info.size.height

                    if (width > maxSize || height > maxSize) {
                        val scale = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
                        decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
                    }
                }
            } else {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                var inSampleSize = 1
                if (options.outHeight > maxSize || options.outWidth > maxSize) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while (halfHeight / inSampleSize >= maxSize && halfWidth / inSampleSize >= maxSize) {
                        inSampleSize *= 2
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                options.inMutable = true

                val roughBitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: throw Exception("Impossible de décoder l'image")

                resizeBitmap(roughBitmap, maxSize)
            }

            val finalBitmapToSave: Bitmap
            val isTransparent: Boolean

            if (shouldCutout) {
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .build()
                val segmenter = SubjectSegmentation.getClient(options)

                val cutoutBitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
                    segmenter.process(InputImage.fromBitmap(processedBitmap, 0))
                        .addOnSuccessListener { result ->
                            continuation.resume(result.foregroundBitmap)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                        .addOnCompleteListener {
                            segmenter.close()
                        }
                }

                if (cutoutBitmap != null) {
                    finalBitmapToSave = cutoutBitmap
                    isTransparent = true
                } else {
                    finalBitmapToSave = processedBitmap
                    isTransparent = false
                }
            } else {
                finalBitmapToSave = processedBitmap
                isTransparent = false
            }

            val savedUri = saveBitmapToInternalStorage(finalBitmapToSave, isTransparent)

            if (finalBitmapToSave != processedBitmap) {
                finalBitmapToSave.recycle()
            }
            processedBitmap.recycle()

            // OPTIMISATION CACHE CRITIQUE : Suppression du fichier brut (JPG) après traitement !
            clearTemporaryCameraFiles()

            return@withContext savedUri

        } catch (e: Exception) {
            Log.e("ImageProcessor", "Erreur lors du traitement de l'image", e)
            null
        }
    }

    private fun clearTemporaryCameraFiles() {
        try {
            // Nettoie tous les fichiers qui commencent par "cam_" dans le cache
            context.cacheDir.listFiles { file ->
                file.name.startsWith("cam_") || file.name.endsWith(".tmp")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Erreur lors du nettoyage du cache", e)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newBitmap = Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)

        if (newBitmap != bitmap) {
            bitmap.recycle()
        }

        return newBitmap
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap, isTransparent: Boolean): Uri {
        val file = File(context.filesDir, "gollections_img_${UUID.randomUUID()}.webp")
        FileOutputStream(file).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (isTransparent) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                }
            } else {
                @Suppress("DEPRECATION")
                val quality = if (isTransparent) 100 else 80
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out)
            }
        }
        return Uri.fromFile(file)
    }

    fun deleteImageFile(imageUrl: String) {
        try {
            if (imageUrl.startsWith("file://") && imageUrl.contains("gollections_img_")) {
                val path = Uri.parse(imageUrl).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Erreur lors de la suppression de l'image", e)
        }
    }
}