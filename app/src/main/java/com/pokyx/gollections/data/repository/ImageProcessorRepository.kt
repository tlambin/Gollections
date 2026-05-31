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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.min

@Singleton
class ImageProcessorRepository @Inject constructor(
    @ApplicationContext private val context: Context // <-- MODIFICATION: Injection propre, plus besoin de redéclarer la variable
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

            val resizedBitmap = resizeBitmap(originalBitmap, maxSize = 1024)

            if (!shouldCutout) {
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
                            continuation.resume(saveBitmapToInternalStorage(cutoutBitmap, isTransparent = true))
                        } else {
                            continuation.resume(saveBitmapToInternalStorage(resizedBitmap, isTransparent = false))
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(saveBitmapToInternalStorage(resizedBitmap, isTransparent = false))
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap, isTransparent: Boolean): Uri {
        // <-- MODIFICATION: Utilisation de UUID pour garantir l'unicité
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
}