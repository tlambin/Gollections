package com.pokyx.gollections.data.network

import kotlinx.serialization.Serializable

@Serializable
data class UpcResponse(
    val code: String,
    val items: List<UpcItem> = emptyList()
)

@Serializable
data class UpcItem(
    val title: String = "",
    val description: String = "",
    val images: List<String> = emptyList()
)