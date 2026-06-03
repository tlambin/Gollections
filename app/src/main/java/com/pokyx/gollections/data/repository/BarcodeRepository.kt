package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.network.UpcApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
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

            // Si la requête réussit et renvoie un JSON valide
            if (response.code == "OK" && response.items.isNotEmpty()) {
                val item = response.items.first()
                Result.success(
                    ScannedItemInfo(
                        title = item.title,
                        imageUrl = item.images.firstOrNull() ?: ""
                    )
                )
            } else if (response.code == "TOO_FAST" || response.code == "EXCEEDED") {
                Result.failure(Exception("error_scan_limit"))
            } else {
                Result.failure(Exception("error_scan_not_found"))
            }

        } catch (e: HttpException) {
            // NOUVEAU : Intercepte les erreurs HTTP (ex: 429 Too Many Requests)
            if (e.code() == 429 || e.code() == 400) {
                Result.failure(Exception("error_scan_limit"))
            } else {
                Result.failure(Exception("error_scan_not_found"))
            }
        } catch (e: IOException) {
            // NOUVEAU : Intercepte les problèmes de connexion (Mode avion, pas de wifi)
            Result.failure(Exception("error_network"))
        } catch (e: Exception) {
            // Sécurité pour toute autre erreur inattendue
            Result.failure(Exception("error_scan_not_found"))
        }
    }
}