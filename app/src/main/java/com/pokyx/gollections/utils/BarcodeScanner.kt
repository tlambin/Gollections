package com.pokyx.gollections.utils

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning


class BarcodeScanner(context: Context) {

    // Configuration du scanner pour cibler uniquement les types de codes-barres standards (EAN, UPC...)
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .enableAutoZoom() // Permet de zoomer automatiquement si le code-barres est loin
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    /**
     * Lance l'interface native de scan.
     * @param onScanSuccess Callback invoqué avec la chaîne de caractères du code-barres trouvé
     * @param onScanFailure Callback invoqué en cas d'erreur ou d'annulation
     */
    fun startScan(
        onScanSuccess: (String) -> Unit,
        onScanFailure: (Exception) -> Unit
    ) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                // Récupère la valeur brute textuelle du code-barres (ex: "9782205043754")
                barcode.rawValue?.let { code ->
                    onScanSuccess(code)
                }
            }
            .addOnFailureListener { exception ->
                onScanFailure(exception)
            }
    }
}