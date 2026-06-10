package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pokyx.gollections.data.dao.ItemAttachmentDao
import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.ItemType
import com.pokyx.gollections.data.model.DisplayFormat
import com.pokyx.gollections.data.model.CollectionPropertyTemplate
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.ItemAttachment
import com.pokyx.gollections.data.repository.CollectionPropertyTemplateRepository
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.TagRepository
import com.pokyx.gollections.data.model.CollectionItemWithTags
import com.pokyx.gollections.data.model.Tag
import com.pokyx.gollections.domain.usecase.DeleteItemUseCase
import com.pokyx.gollections.domain.usecase.GetCollectionPathUseCase
import com.pokyx.gollections.domain.usecase.InsertItemUseCase
import com.pokyx.gollections.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ItemFormState(
    val title: String = "",
    val itemType: ItemType = ItemType.OTHER,
    val displayFormat: DisplayFormat = DisplayFormat.LANDSCAPE,
    val selectedPath: List<Long> = emptyList(),
    val purchaseDate: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",
    val status: String = "Non commencé",
    val selectedTags: Set<Tag> = emptySet(),
    val properties: List<ItemProperty> = emptyList(),
    val customSections: List<String> = emptyList(),
    val attachments: List<String> = emptyList()
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: CollectionPropertyTemplateRepository,
    private val attachmentDao: ItemAttachmentDao,
    private val imageProcessor: ImageProcessorRepository,
    private val insertItemUseCase: InsertItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getCollectionPathUseCase: GetCollectionPathUseCase
) : ViewModel() {

    private val gson = Gson()
    private var templateJob: Job? = null
    private var isFormInitialized = false

    val collections = collectionRepository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(
        savedStateHandle.get<String>("form_state_json")?.let {
            try { gson.fromJson(it, ItemFormState::class.java) } catch (e: Exception) { ItemFormState() }
        } ?: ItemFormState()
    )
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _formState.update { oldState ->
            val newState = transform(oldState)
            try { savedStateHandle["form_state_json"] = gson.toJson(newState) } catch (e: Exception) { e.printStackTrace() }
            newState
        }
    }

    private fun detectItemTypeFromPath(path: List<Long>, currentCollections: List<Collection>): ItemType {
        val pathNames = currentCollections.filter { it.id in path }.joinToString(" ") { it.name.lowercase() }
        return when {
            pathNames.contains("jeu") || pathNames.contains("console") || pathNames.contains("playstation") || pathNames.contains("nintendo") || pathNames.contains("xbox") -> ItemType.GAME
            pathNames.contains("film") || pathNames.contains("cinéma") || pathNames.contains("dvd") || pathNames.contains("bluray") -> ItemType.MOVIE
            pathNames.contains("musique") || pathNames.contains("vinyle") || pathNames.contains("cd") || pathNames.contains("album") -> ItemType.MUSIC
            pathNames.contains("livre") || pathNames.contains("manga") || pathNames.contains("roman") || pathNames.contains("bd") || pathNames.contains("comics") -> ItemType.BOOK
            else -> ItemType.OTHER
        }
    }

    fun resetFormState(preSelectedCollectionId: Long? = null, collectionsList: List<Collection> = emptyList(), scannedTitle: String? = null, scannedImageUrl: String? = null) {
        if (isFormInitialized) return
        isFormInitialized = true
        val initialPath = getCollectionPathUseCase(preSelectedCollectionId, collectionsList)
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val autoType = detectItemTypeFromPath(initialPath, collectionsList)
        updateForm {
            ItemFormState(
                selectedPath = initialPath, title = scannedTitle ?: "", imageUrl = scannedImageUrl ?: "",
                purchaseDate = currentDate, loanDate = currentDate, itemType = autoType,
                displayFormat = DisplayFormat.LANDSCAPE, attachments = emptyList(), properties = emptyList(), customSections = emptyList()
            )
        }
        initialPath.lastOrNull()?.let { fetchTemplatesForCollection(it) }
    }

    fun updateSelectedPath(path: List<Long>) {
        val autoType = detectItemTypeFromPath(path, collections.value)
        updateForm { oldState ->
            oldState.copy(selectedPath = path, itemType = if (oldState.itemType == ItemType.OTHER) autoType else oldState.itemType)
        }
        path.lastOrNull()?.let { fetchTemplatesForCollection(it) }
    }

    fun fetchTemplatesForCollection(collectionId: Long) {
        templateJob?.cancel()
        templateJob = viewModelScope.launch {
            templateRepository.getTemplatesForCollection(collectionId).collect { templates ->
                updateForm { oldState ->
                    val currentProps = oldState.properties.toMutableList()
                    templates.forEach { template ->
                        if (currentProps.none { it.label == template.propertyName }) {
                            // ✅ NOUVEAU: Récupère le type du template ou TEXT par défaut
                            val type = template.propertyType.ifBlank { "TEXT" }
                            currentProps.add(ItemProperty(itemId = 0, label = template.propertyName, value = "", sectionName = "Informations générales", type = type))
                        }
                    }
                    oldState.copy(properties = currentProps)
                }
            }
        }
    }

    fun addAttachment(uri: String) { updateForm { it.copy(attachments = it.attachments + uri) } }
    fun removeAttachment(uri: String) { updateForm { it.copy(attachments = it.attachments - uri) } }

    fun addSection(sectionName: String) {
        updateForm { it.copy(customSections = (it.customSections + sectionName).distinct()) }
    }

    // ✅ NOUVEAU: La méthode accepte maintenant le paramètre 'type'
    fun addProperty(sectionName: String, label: String, type: String) {
        updateForm { state ->
            val newProp = ItemProperty(itemId = 0, label = label, value = "", sectionName = sectionName, type = type)
            state.copy(properties = state.properties + newProp)
        }
    }

    fun updatePropertyValue(label: String, sectionName: String, newValue: String) {
        updateForm { state ->
            val updated = state.properties.map {
                if (it.label == label && it.sectionName == sectionName) it.copy(value = newValue) else it
            }
            state.copy(properties = updated)
        }
    }

    fun removeProperty(label: String, sectionName: String) {
        updateForm { state ->
            state.copy(properties = state.properties.filterNot { it.label == label && it.sectionName == sectionName })
        }
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        isFormInitialized = true
        val item = itemWithTags.item
        viewModelScope.launch {
            val fetchedAttachments = attachmentDao.getAttachmentsForItemDirect(item.id).map { it.uri }
            val priceString = if (item.price > 0.0) item.price.toString().replace(".", ",") else ""
            val path = com.pokyx.gollections.utils.buildPathBottomUp(item.collectionId, collectionsList)

            updateForm { oldState ->
                oldState.copy(
                    title = item.title, price = priceString, purchaseDate = item.purchaseDate, imageUrl = item.imageUrl,
                    status = item.status, isLoaned = item.isLoaned, loanTo = item.loanTo, loanDate = item.loanDate,
                    itemType = item.itemType, displayFormat = item.displayFormat, selectedPath = path,
                    selectedTags = itemWithTags.tags.toSet(), attachments = fetchedAttachments,
                    properties = itemWithTags.properties,
                    customSections = itemWithTags.properties.map { it.sectionName }.distinct()
                )
            }
            fetchTemplatesForCollection(item.collectionId)
        }
    }

    fun changeItemType(newType: ItemType) {
        val defaultPropsForType = when (newType) {
            ItemType.MOVIE -> listOf("Réalisateur", "Année de sortie", "Format (DVD, Blu-Ray, Démat)", "Synopsis")
            ItemType.BOOK -> listOf("Auteur", "Date de publication", "Résumé", "Nombre de pages")
            ItemType.GAME -> listOf("Studio", "Plateforme", "Année de sortie", "Description")
            ItemType.MUSIC -> listOf("Artiste", "Album", "Format (Vinyle, CD, Cassette)", "Année de sortie")
            ItemType.OTHER -> emptyList()
        }
        val mappedProps = defaultPropsForType.map {
            // ✅ NOUVEAU: Auto-détection du type pour les champs par défaut
            val type = when {
                it.contains("Date") -> "DATE"
                it.contains("Année") || it.contains("pages") -> "NUMBER"
                else -> "TEXT"
            }
            ItemProperty(itemId = 0, label = it, value = "", sectionName = "Informations générales", type = type)
        }
        updateForm { oldState -> oldState.copy(itemType = newType, properties = mappedProps) }
    }

    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>, properties: List<ItemProperty>, attachments: List<String>) {
        viewModelScope.launch {
            val itemAttachments = attachments.map { uri -> ItemAttachment(itemId = 0, uri = uri) }
            insertItemUseCase(item, tags, properties, itemAttachments)
        }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>, properties: List<ItemProperty>, attachments: List<String>) {
        viewModelScope.launch {
            val itemAttachments = attachments.map { uri -> ItemAttachment(itemId = item.id, uri = uri) }
            updateItemUseCase(item, tags, properties, itemAttachments)
        }
    }

    fun deleteItem(item: CollectionItem) { viewModelScope.launch { deleteItemUseCase(item) } }
    fun getAttachmentsStream(itemId: Int): Flow<List<ItemAttachment>> = attachmentDao.getAttachmentsForItem(itemId)
    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = itemRepository.getItemByIdWithTags(id)
    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) flowOf(emptyList()) else tagRepository.getTagsByCollectionIds(collectionIds)
    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { tagRepository.insertTag(Tag(name = name, collectionId = collectionId)) } }
    suspend fun loadBitmap(uri: Uri): android.graphics.Bitmap? = imageProcessor.loadScaledBitmap(uri)

    fun processAndSaveBitmap(bitmap: android.graphics.Bitmap, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val uri = imageProcessor.processAndSaveBitmap(bitmap, shouldCutout)
            onResult(uri?.toString())
        }
    }
}