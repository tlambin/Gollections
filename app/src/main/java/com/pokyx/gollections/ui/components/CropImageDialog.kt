package com.pokyx.gollections.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

// 1. Définition des formes de l'overlay
enum class CropOverlayShape {
    CIRCLE,         // Pour les Collections (Icônes rondes)
    ROUNDED_SQUARE  // Pour les Items (Cartes avec bords arrondis)
}

@Composable
fun CropImageDialog(
    bitmap: Bitmap,
    overlayShape: CropOverlayShape = CropOverlayShape.ROUNDED_SQUARE, // Par défaut : Item
    onDismiss: () -> Unit,
    onConfirm: (croppedBitmap: Bitmap, performCutout: Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Plein écran
    ) {
        Scaffold(
            containerColor = Color.Black,
            bottomBar = {
                Surface(color = Color.DarkGray.copy(alpha = 0.5f)) {
                    Column(
                        modifier = Modifier.padding(16.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            OutlinedButton(
                                onClick = { onConfirm(createCroppedBitmap(bitmap, scale, offset), false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) { Text("Juste Recadrer") }

                            Button(
                                onClick = { onConfirm(createCroppedBitmap(bitmap, scale, offset), true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("✨ Détourage") }
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Annuler", color = Color.LightGray)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text("Ajustez l'image", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("La zone lumineuse sera visible dans l'application", color = Color.Gray, fontSize = 14.sp)

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // La zone de recadrage (Le Carré)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f) // Carré parfait
                            .clipToBounds()
                            .background(Color.DarkGray)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offset += pan
                                }
                            }
                    ) {
                        // 1. L'image en dessous
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Image à recadrer",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )

                        // 2. L'OVERLAY MAGIQUE (Le masque percé)
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val rect = Rect(0f, 0f, size.width, size.height)

                            // Création du chemin "troué" grâce à EvenOdd
                            val path = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(rect) // Le fond complet

                                // On soustrait la forme désirée au centre
                                if (overlayShape == CropOverlayShape.CIRCLE) {
                                    addOval(rect)
                                } else {
                                    // Bords arrondis (Dimens.cornerLarge est généralement 16.dp)
                                    addRoundRect(RoundRect(rect, CornerRadius(16.dp.toPx(), 16.dp.toPx())))
                                }
                            }

                            // Dessine le voile noir semi-transparent
                            drawPath(path, color = Color.Black.copy(alpha = 0.7f))

                            // Dessine une bordure blanche subtile pour guider l'œil
                            if (overlayShape == CropOverlayShape.CIRCLE) {
                                drawOval(color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                            } else {
                                drawRoundRect(color = Color.White.copy(alpha = 0.5f), cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createCroppedBitmap(original: Bitmap, scale: Float, offset: Offset, outputSize: Int = 1024): Bitmap {
    val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    val matrix = Matrix()

    val baseScale = max(outputSize.toFloat() / original.width, outputSize.toFloat() / original.height)
    val dx = (outputSize - original.width * baseScale) / 2f
    val dy = (outputSize - original.height * baseScale) / 2f
    matrix.postScale(baseScale, baseScale)
    matrix.postTranslate(dx, dy)

    matrix.postScale(scale, scale, outputSize / 2f, outputSize / 2f)
    matrix.postTranslate(offset.x, offset.y)

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    canvas.drawBitmap(original, matrix, paint)
    return result
}