package com.pokyx.gollections.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpcResponse(
    @SerialName("code")
    val code: String,

    @SerialName("items")
    val items: List<UpcItem> = emptyList()
)

@Serializable
data class UpcItem(
    @SerialName("title")
    val title: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("images")
    val images: List<String> = emptyList()
)