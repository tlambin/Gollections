package com.pokyx.gollections

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlin.concurrent.thread

@HiltAndroidApp
class GollectionsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Nettoyage de sécurité en arrière-plan au lancement de l'application
        thread(start = true) {
            clearOldCacheFiles()
        }
    }

    private fun clearOldCacheFiles() {
        try {
            // Supprime les photos orphelines des sessions précédentes
            cacheDir.listFiles { file ->
                file.name.startsWith("cam_") || file.name.endsWith(".tmp")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}