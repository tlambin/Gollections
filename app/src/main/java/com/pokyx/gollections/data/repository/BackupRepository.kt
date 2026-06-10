package com.pokyx.gollections.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef
import com.pokyx.gollections.data.model.Tag
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
    database: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val backupDao = database.backupDao()

    private val gson = Gson()

    suspend fun exportDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                collections = backupDao.getAllCollections(),
                items = backupDao.getAllItems(),
                tags = backupDao.getAllTags(),
                properties = backupDao.getAllProperties(),
                crossRefs = backupDao.getAllCrossRefs()
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    gson.toJson(data, writer)
                }
            } ?: throw Exception("Impossible d'ouvrir le fichier de destination")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRepository", "Erreur lors de l'exportation de la base de données", e)
            Result.failure(e)
        }
    }

    suspend fun importDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, BackupData::class.java)
                }
            } ?: throw Exception("Impossible de lire le fichier de sauvegarde")

            val rawCollections = data.collections ?: emptyList()

            val sortedCollections = mutableListOf<Collection>()
            val remainingCollections = rawCollections.toMutableList()

            while (remainingCollections.isNotEmpty()) {
                val insertable = remainingCollections.filter { col ->
                    col.parentId == null || sortedCollections.any { it.id == col.parentId }
                }

                if (insertable.isEmpty()) {
                    sortedCollections.addAll(remainingCollections)
                    break
                }

                sortedCollections.addAll(insertable)
                remainingCollections.removeAll(insertable)
            }

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