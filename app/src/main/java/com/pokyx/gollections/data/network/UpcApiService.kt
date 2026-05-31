package com.pokyx.gollections.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface UpcApiService {
    // Appel gratuit de test chez UPCitemdb
    @GET("prod/trial/lookup")
    suspend fun lookupBarcode(@Query("upc") barcode: String): UpcResponse
}