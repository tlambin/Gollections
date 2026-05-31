package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.network.UpcApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedItemInfo(val title: String, val imageUrl: String)

@Singleton
class BarcodeRepository @Inject constructor(
    private val apiService: UpcApiService
) {
    suspend fun getInfoFromBarcode(barcode: String): Result<ScannedItemInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.lookupBarcode(barcode)
            if (response.code == "OK" && response.items.isNotEmpty()) {
                val item = response.items.first()
                Result.success(
                    ScannedItemInfo(
                        title = item.title,
                        imageUrl = item.images.firstOrNull() ?: ""
                    )
                )
            } else {
                Result.failure(Exception("Produit non trouvé dans la base de données"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}