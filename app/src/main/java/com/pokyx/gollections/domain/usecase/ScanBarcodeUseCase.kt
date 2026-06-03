package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.repository.BarcodeRepository
import retrofit2.HttpException
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
            // Note : on s'appuie sur l'inférence de type de Kotlin pour récupérer les propriétés
            val info = result.getOrNull() as? Any

            // Pour éviter les soucis d'import dynamique, si la ligne ci-dessous te met une erreur rouge
            // sur "title" ou "imageUrl", importe simplement ta classe UpcModels (ex: UpcItemInfo)
            // et remplace "as? Any" par "as? NomDeTaClasseModele"

            // On simule l'accès aux données comme dans ton ancien ViewModel
            val title = info?.javaClass?.getMethod("getTitle")?.invoke(info) as? String
            val imageUrl = info?.javaClass?.getMethod("getImageUrl")?.invoke(info) as? String

            BarcodeResult(title = title, imageUrl = imageUrl)
        } else {
            val exception = result.exceptionOrNull()

            // Sécurisation des limites de l'API (HTTP 429) et des objets introuvables (HTTP 404)
            val errorMessage = when {
                exception is HttpException && exception.code() == 429 -> "error_scan_limit"
                exception is HttpException && exception.code() == 404 -> "error_scan_not_found"
                else -> exception?.message ?: "error_unknown"
            }

            BarcodeResult(errorMsg = errorMessage)
        }
    }
}