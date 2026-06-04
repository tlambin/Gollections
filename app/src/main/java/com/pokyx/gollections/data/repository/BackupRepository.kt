package com.pokyx.gollections.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val collections: List<Collection>,
    val items: List<CollectionItem>,
    val tags: List<Tag>,
    val properties: List<ItemProperty>,
    val crossRefs: List<CollectionItemTagCrossRef>
)

@Singleton
class BackupRepository @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val backupDao = database.backupDao()

    suspend fun exportDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                collections = backupDao.getAllCollections(),
                items = backupDao.getAllItems(),
                tags = backupDao.getAllTags(),
                properties = backupDao.getAllProperties(),
                crossRefs = backupDao.getAllCrossRefs()
            )

            val json = Gson().toJson(data)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRepository", "Erreur lors de l'exportation de la base de données", e)
            Result.failure(e)
        }
    }

    suspend fun importDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var json = ""
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    json = reader.readText()
                }
            }

            val data = Gson().fromJson(json, BackupData::class.java)

            // CORRECTION : Trier les collections par ID pour créer les dossiers parents AVANT les enfants
            val sortedCollections = data.collections?.sortedBy { it.id } ?: emptyList()

            backupDao.restoreAll(
                collections = sortedCollections,
                items = data.items ?: emptyList(),
                tags = data.tags ?: emptyList(),
                properties = data.properties ?: emptyList(),
                crossRefs = data.crossRefs ?: emptyList()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRepository", "Erreur lors de l'importation de la base de données", e)
            Result.failure(e)
        }
    }
}