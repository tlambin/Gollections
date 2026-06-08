package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.network.UpcApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedItemInfo(val title: String, val imageUrl: String)

@Singleton
class BarcodeRepository @Inject constructor(
    private val apiService: UpcApiService
) {
    suspend fun getInfoFromBarcode(barcode: String): Result<ScannedItemInfo> {
        return try {
            val response = apiService.lookupBarcode(barcode)

            // OPTIMISATION : Utilisation de 'when' pour une lecture beaucoup plus claire des conditions
            when {
                response.code == "OK" && response.items.isNotEmpty() -> {
                    val item = response.items.first()
                    Result.success(
                        ScannedItemInfo(
                            title = item.title,
                            imageUrl = item.images.firstOrNull() ?: ""
                        )
                    )
                }
                response.code == "TOO_FAST" || response.code == "EXCEEDED" -> {
                    Result.failure(Exception("error_scan_limit"))
                }
                else -> {
                    Result.failure(Exception("error_scan_not_found"))
                }
            }
        } catch (e: HttpException) {
            if (e.code() == 429 || e.code() == 400) {
                Result.failure(Exception("error_scan_limit"))
            } else {
                Result.failure(Exception("error_scan_not_found"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("error_network"))
        } catch (e: Exception) {
            Result.failure(Exception("error_scan_not_found"))
        }
    }
}