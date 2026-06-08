package com.pokyx.gollections.utils

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BarcodeScanner(context: Context) {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .enableAutoZoom()
        .build()

    // Sécurisation du contexte pour éviter les fuites de mémoire
    private val scanner = GmsBarcodeScanning.getClient(context.applicationContext, options)

    /**
     * Lance l'interface native de scan.
     * @return La valeur textuelle du code-barres.
     * @throws Exception en cas d'erreur ou d'annulation par l'utilisateur.
     */
    suspend fun startScan(): String = suspendCancellableCoroutine { continuation ->
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let { code ->
                    continuation.resume(code)
                } ?: continuation.resumeWithException(Exception("Code-barres illisible ou vide"))
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }

        // Si la coroutine est annulée (ex: fermeture brutale de l'écran), on peut optionnellement annuler la Task
        // (Bien que GMS s'en charge généralement tout seul)
    }
}