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
    @param:ApplicationContext private val context: Context
) {

    // 1. Charge l'image depuis l'URI et la réduit pour éviter les crashs mémoire
    suspend fun loadScaledBitmap(sourceUri: Uri, maxSize: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.isMutableRequired = true
                    val scale = min(maxSize.toFloat() / info.size.width, maxSize.toFloat() / info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                    }
                }
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, options) }

                var inSampleSize = 1
                while (options.outHeight / inSampleSize >= maxSize || options.outWidth / inSampleSize >= maxSize) {
                    inSampleSize *= 2
                }

                options.apply {
                    inJustDecodeBounds = false
                    this.inSampleSize = inSampleSize
                    inMutable = true
                }
                val roughBitmap = context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: throw Exception("Decodage impossible")
                resizeBitmap(roughBitmap, maxSize)
            }
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Erreur chargement image", e)
            null
        }
    }

    // 2. Traite (ML Kit optionnel) et sauvegarde le Bitmap final
    suspend fun processAndSaveBitmap(bitmap: Bitmap, shouldCutout: Boolean): Uri? = withContext(Dispatchers.IO) {
        try {
            val finalBitmapToSave: Bitmap
            val isTransparent: Boolean

            if (shouldCutout) {
                val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
                val segmenter = SubjectSegmentation.getClient(options)

                val cutoutBitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
                    segmenter.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { result -> continuation.resume(result.foregroundBitmap) }
                        .addOnFailureListener { continuation.resume(null) }
                        .addOnCompleteListener { segmenter.close() }
                }

                if (cutoutBitmap != null) {
                    finalBitmapToSave = cutoutBitmap
                    isTransparent = true
                } else {
                    finalBitmapToSave = bitmap
                    isTransparent = false
                }
            } else {
                finalBitmapToSave = bitmap
                isTransparent = false
            }

            val savedUri = saveBitmapToInternalStorage(finalBitmapToSave, isTransparent)

            // Nettoyage mémoire très important
            if (finalBitmapToSave != bitmap) finalBitmapToSave.recycle()
            bitmap.recycle() // On libère le bitmap recadré de l'UI
            clearTemporaryCameraFiles()

            savedUri
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Erreur traitement final", e)
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newBitmap = Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
        if (newBitmap != bitmap) bitmap.recycle()
        return newBitmap
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap, isTransparent: Boolean): Uri {
        val file = File(context.filesDir, "gollections_img_${UUID.randomUUID()}.webp")
        FileOutputStream(file).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(if (isTransparent) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY, if (isTransparent) 100 else 80, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, if (isTransparent) 100 else 80, out)
            }
        }
        return Uri.fromFile(file)
    }

    private fun clearTemporaryCameraFiles() {
        try {
            context.cacheDir.listFiles { file -> file.name.startsWith("cam_") || file.name.endsWith(".tmp") }?.forEach { it.delete() }
        } catch (e: Exception) { Log.e("ImageProcessor", "Erreur nettoyage", e) }
    }

    fun deleteImageFile(imageUrl: String) {
        try {
            if (imageUrl.startsWith("file://") && imageUrl.contains("gollections_img_")) {
                Uri.parse(imageUrl).path?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
        } catch (e: Exception) { Log.e("ImageProcessor", "Erreur suppression", e) }
    }
}