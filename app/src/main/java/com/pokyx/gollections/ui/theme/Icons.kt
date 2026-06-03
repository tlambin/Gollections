package com.pokyx.gollections.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Objet de centralisation pour toutes les icônes vectorielles personnalisées
 * dessinées à la main pour l'application Gollections.
 */
object GollectionsIcons {

    // 1. Icône Planète (Internet / URL)
    val Planet: ImageVector
        get() = ImageVector.Builder(
            name = "CustomPlanet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
            curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            // Méridiens
            moveTo(12f, 4f)
            curveToRelative(1.66f, 0f, 3.45f, 2.83f, 3.92f, 6f)
            horizontalLineTo(8.08f)
            curveTo(8.55f, 6.83f, 10.34f, 4f, 12f, 4f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-1.66f, 0f, -3.45f, -2.83f, -3.92f, -6f)
            horizontalLineToRelative(7.84f)
            curveToRelative(-0.47f, 3.17f, -2.26f, 6f, -1.92f, 6f)
            close()
            // Équateur / Horizons
            moveTo(4.17f, 12f)
            curveToRelative(0.12f, -2.71f, 1.48f, -5.11f, 3.49f, -6.61f)
            curveTo(6.46f, 7.15f, 5.86f, 9.47f, 5.8f, 12f)
            horizontalLineTo(4.17f)
            close()
            moveTo(19.83f, 12f)
            horizontalLineToRelative(-1.63f)
            curveToRelative(-0.06f, -2.53f, -0.66f, -4.85f, -1.86f, -6.61f)
            curveToRelative(2.01f, 1.5f, 3.37f, 3.9f, 3.49f, 6.61f)
            close()
            moveTo(7.66f, 18.61f)
            curveToRelative(-2.01f, -1.5f, -3.37f, -3.9f, -3.49f, -6.61f)
            horizontalLineToRelative(1.63f)
            curveToRelative(0.06f, 2.53f, 0.66f, 4.85f, 1.86f, 6.61f)
            close()
            moveTo(16.34f, 18.61f)
            curveToRelative(1.2f, -1.76f, 1.8f, -4.08f, 1.86f, -6.61f)
            horizontalLineToRelative(1.63f)
            curveToRelative(-0.12f, 2.71f, -1.48f, 5.11f, -3.49f, 6.61f)
            close()
        }.build()

    // 2. Icône Photo avec montagnes (Galerie)
    val RoundedGallery: ImageVector
        get() = ImageVector.Builder(
            name = "CustomRoundedGallery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
            close()
            moveTo(19f, 17f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(12f)
            close()
            moveTo(14f, 11.4f)
            lineToRelative(-3.5f, 4.5f)
            lineToRelative(-2.5f, -3f)
            lineTo(5f, 17f)
            horizontalLineToRelative(14f)
            lineToRelative(-5f, -5.6f)
            close()
        }.build()

    // 3. Icône Appareil Photo Reflex
    val Camera: ImageVector
        get() = ImageVector.Builder(
            name = "CustomCamera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(12f, 18f)
            curveToRelative(-2.21f, 0f, -4f, -1.79f, -4f, -4f)
            curveToRelative(0f, -2.21f, 1.79f, -4f, 4f, -4f)
            curveToRelative(2.21f, 0f, 4f, 1.79f, 4f, 4f)
            curveToRelative(0f, 2.21f, -1.79f, 4f, -4f, 4f)
            close()
            moveTo(9f, 2f)
            lineTo(7.17f, 4.002f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4.002f, 2f, 4.9f, 2f, 6.002f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-12f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineToRelative(-3.17f)
            lineTo(15f, 2f)
            horizontalLineTo(9f)
            close()
        }.build()

    // 4. Icône Smiley (Emoji)
    val Smile: ImageVector
        get() = ImageVector.Builder(
            name = "CustomSmile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
            curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
            curveToRelative(0f, -4.41f, 3.59f, -8f, 8f, -8f)
            curveToRelative(4.41f, 0f, 8f, 3.59f, 8f, 8f)
            curveToRelative(0f, 4.41f, -3.59f, 8f, -8f, 8f)
            close()
            moveTo(15.5f, 11f)
            curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
            curveTo(17f, 8.67f, 16.33f, 8f, 15.5f, 8f)
            curveTo(14.67f, 8f, 14f, 8.67f, 14f, 9.5f)
            curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
            close()
            moveTo(8.5f, 11f)
            curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
            curveTo(10f, 8.67f, 9.33f, 8f, 8.5f, 8f)
            curveTo(7.67f, 8f, 7f, 8.67f, 7f, 9.5f)
            curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
            close()
            moveTo(12f, 17.5f)
            curveToRelative(2.33f, 0f, 4.31f, -1.46f, 5.11f, -3.5f)
            horizontalLineTo(6.89f)
            curveToRelative(0.8f, 2.04f, 2.78f, 3.5f, 5.21f, 3.5f)
            close()
        }.build()
}