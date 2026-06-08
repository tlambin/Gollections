package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag

@Dao
interface BackupDao {

    // --- LECTURE (Export) ---
    @Query("SELECT * FROM collections")
    suspend fun getAllCollections(): List<Collection>

    @Query("SELECT * FROM collection_items")
    suspend fun getAllItems(): List<CollectionItem>

    @Query("SELECT * FROM Tag")
    suspend fun getAllTags(): List<Tag>

    @Query("SELECT * FROM item_properties")
    suspend fun getAllProperties(): List<ItemProperty>

    @Query("SELECT * FROM collection_item_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<CollectionItemTagCrossRef>

    // --- ECRITURE (Import) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<Collection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CollectionItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<ItemProperty>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<CollectionItemTagCrossRef>)

    // --- SUPPRESSION (Nettoyage) ---
    @Query("DELETE FROM collection_item_tag_cross_ref")
    suspend fun clearCrossRefs()

    @Query("DELETE FROM item_properties")
    suspend fun clearProperties()

    @Query("DELETE FROM Tag")
    suspend fun clearTags()

    @Query("DELETE FROM collection_items")
    suspend fun clearItems()

    @Query("DELETE FROM collections")
    suspend fun clearCollections()

    // --- TRANSACTION GLOBALE DE RESTAURATION ---
    @Transaction
    suspend fun restoreAll(
        collections: List<Collection>,
        items: List<CollectionItem>,
        tags: List<Tag>,
        properties: List<ItemProperty>,
        crossRefs: List<CollectionItemTagCrossRef>
    ) {
        // 1. On vide tout dans le bon ordre (Enfants -> Parents) pour respecter les contraintes
        clearCrossRefs()
        clearProperties()
        clearTags()
        clearItems()
        clearCollections()

        // 2. On réinsère tout avec les IDs d'origine (Parents -> Enfants)
        insertCollections(collections)
        insertItems(items)
        insertTags(tags)
        insertProperties(properties)
        insertCrossRefs(crossRefs)
    }
}