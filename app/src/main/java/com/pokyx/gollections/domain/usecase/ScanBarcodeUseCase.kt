package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.repository.BarcodeRepository
import javax.inject.Inject

// Data class pour structurer proprement la réponse envoyée au ViewModel
data class BarcodeResult(
    val title: String? = null,
    val imageUrl: String? = null,
    val errorMsg: String? = null
)

class ScanBarcodeUseCase @Inject constructor(
    private val barcodeRepository: BarcodeRepository
) {
    suspend operator fun invoke(barcode: String): BarcodeResult {
        val result = barcodeRepository.getInfoFromBarcode(barcode)

        return if (result.isSuccess) {
            // Typage fort et sécurisé : on récupère directement l'objet ScannedItemInfo
            val info = result.getOrNull()

            BarcodeResult(
                title = info?.title,
                imageUrl = info?.imageUrl
            )
        } else {
            val exception = result.exceptionOrNull()
            // L'exception contient déjà la clé d'erreur formatée par le Repository
            // (ex: "error_scan_limit", "error_network", "error_scan_not_found")
            val errorMessage = exception?.message ?: "error_unknown"

            BarcodeResult(errorMsg = errorMessage)
        }
    }
}