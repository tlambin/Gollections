package com.pokyx.gollections.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class DisplayFormat {
    PORTRAIT,
    LANDSCAPE
}

enum class ItemType(val label: String, val emoji: String) {
    MOVIE("Film", "🎬"),
    BOOK("Livre", "📚"),
    GAME("Jeu", "🎮"),
    MUSIC("Musique", "🎵"),
    OTHER("Autre", "📦")
}

class Converters {
    @TypeConverter
    fun toItemType(value: String): ItemType =
        ItemType.entries.find { it.name == value } ?: ItemType.OTHER

    @TypeConverter
    fun fromItemType(value: ItemType): String = value.name

    @TypeConverter
    fun toDisplayFormat(value: String): DisplayFormat =
        DisplayFormat.entries.find { it.name == value } ?: DisplayFormat.PORTRAIT

    @TypeConverter
    fun fromDisplayFormat(value: DisplayFormat): String = value.name
}

@Entity(
    tableName = "collection_items",
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectionId"])]
)
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val collectionId: Long,

    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",

    val purchaseDate: String = "",
    val price: Double = 0.0,
    val status: String = "Non commencé",
    val comment: String = "",
    val imageUrl: String = "",
    val itemType: ItemType = ItemType.OTHER,

    @ColumnInfo(name = "display_format", defaultValue = "LANDSCAPE")
    val displayFormat: DisplayFormat = DisplayFormat.LANDSCAPE
)