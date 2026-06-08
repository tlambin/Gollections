package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_properties",
    foreignKeys = [
        ForeignKey(
            entity = CollectionItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE // Si on supprime l'objet, on supprime ses propriétés
        )
    ],
    // OPTIMISATION : Index indispensable pour des suppressions en cascade instantanées
    indices = [
        Index(value = ["itemId"])
    ]
)
data class ItemProperty(
    @PrimaryKey(autoGenerate = true) val propertyId: Int = 0,
    val itemId: Int,
    val label: String, // ex: "Réalisateur", "Éditeur", "Nombre de joueurs"
    val value: String  // ex: "Christopher Nolan", "Nintendo", "1-4"
)