package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.repository.BarcodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend operator fun invoke(barcode: String): BarcodeResult = withContext(Dispatchers.IO) {
        // OPTIMISATION : Utilisation de 'fold' pour gérer proprement le Result natif
        barcodeRepository.getInfoFromBarcode(barcode).fold(
            onSuccess = { info ->
                // Cas de succès : 'info' est directement du type ScannedItemInfo (non nullable si le repo le renvoie ainsi)
                BarcodeResult(
                    title = info?.title,
                    imageUrl = info?.imageUrl
                )
            },
            onFailure = { exception ->
                // Cas d'erreur : on extrait directement le message
                val errorMessage = exception.message ?: "error_unknown"
                BarcodeResult(errorMsg = errorMessage)
            }
        )
    }
}