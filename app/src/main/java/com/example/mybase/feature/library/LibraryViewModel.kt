package com.example.mybase.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybase.data.document.DocumentAccess
import com.example.mybase.data.document.DocumentLocation
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.data.repository.LibraryResourceRepository
import com.example.mybase.data.repository.DocumentReadingStateRepository
import com.example.mybase.data.repository.PdfHighlightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val resources: List<LibraryResource> = emptyList(),
    val query: String = "",
    val selectedType: ResourceType? = null,
    val selectedCategory: String? = null,
    val selectedTag: String? = null,
    val isEditorVisible: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val type: ResourceType = ResourceType.Link,
    val reference: String = "",
    val description: String = "",
    val category: String = "",
    val tagsText: String = "",
    val originalResource: LibraryResource? = null,
    val showDiscardConfirmation: Boolean = false,
    val pendingDeleteId: String? = null,
    val isBrowserVisible: Boolean = false,
    val authorizedFolders: List<DocumentLocation> = emptyList(),
    val folderStack: List<DocumentLocation> = emptyList(),
    val folderItems: List<DocumentLocation> = emptyList(),
    val folderLoading: Boolean = false,
    val folderError: String? = null,
    val unavailableDocumentIds: Set<String> = emptySet(),
    val documentNames: Map<String, String> = emptyMap(),
    val infoResource: LibraryResource? = null,
    val infoLocation: DocumentLocation? = null,
    val infoAvailable: Boolean? = null,
    val infoLoading: Boolean = false
) {
    val categories: List<String>
        get() = resources.map { it.category }.filter(String::isNotBlank).distinct().sorted()
    val tags: List<String>
        get() = resources.flatMap { it.tags }.filter(String::isNotBlank).distinct().sorted()
    val filteredResources: List<LibraryResource>
        get() = resources.filter { resource ->
            val matchesSearch = query.isBlank() || listOf(
                resource.title,
                resource.description,
                resource.reference,
                resource.category,
                resource.tags.joinToString(" ")
            ).any { it.contains(query, true) }
            matchesSearch && (selectedType == null || resource.type == selectedType) &&
                    (selectedCategory == null || resource.category == selectedCategory) && (selectedTag == null || selectedTag in resource.tags)
        }
    val urlError: String?
        get() = if (type == ResourceType.Link && reference.isNotBlank() && !LibraryViewModel.isValidWebUrl(
                reference
            )
        ) "Enter a full web address, such as https://example.com" else null
    val canSave: Boolean get() = title.isNotBlank() && reference.isNotBlank() && urlError == null
    val hasUnsavedChanges: Boolean
        get() = if (originalResource == null) {
            type != ResourceType.Link || title.isNotBlank() || reference.isNotBlank() || description.isNotBlank() || category.isNotBlank() || tagsText.isNotBlank()
        } else {
            title != originalResource.title || type != originalResource.type || reference != originalResource.reference ||
                    description != originalResource.description || category != originalResource.category || tagsText != originalResource.tags.joinToString(
                ", "
            )
        }
}

class LibraryViewModel(
    private val repository: LibraryResourceRepository,
    private val readingStateRepository: DocumentReadingStateRepository,
    private val highlightRepository: PdfHighlightRepository,
    private val documentAccess: DocumentAccess
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(LibraryUiState(resources = repository.resources.value))
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.resources.collect { list ->
                mutableState.update {
                    it.copy(
                        resources = list
                    )
                }; refreshAvailability(list)
            }
        }
    }

    fun setSearchQuery(query: String) = mutableState.update { it.copy(query = query) }
    fun selectType(type: ResourceType?) =
        mutableState.update { it.copy(selectedType = if (it.selectedType == type) null else type) }

    fun selectCategory(category: String?) =
        mutableState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }

    fun selectTag(tag: String?) =
        mutableState.update { it.copy(selectedTag = if (it.selectedTag == tag) null else tag) }

    fun createResource() = mutableState.update {
        it.copy(
            isEditorVisible = true,
            editingId = null,
            title = "",
            type = ResourceType.Link,
            reference = "",
            description = "",
            category = "",
            tagsText = "",
            originalResource = null,
            showDiscardConfirmation = false
        )
    }

    fun editResource(resource: LibraryResource) = mutableState.update {
        it.copy(
            isEditorVisible = true,
            editingId = resource.id,
            title = resource.title,
            type = resource.type,
            reference = resource.reference,
            description = resource.description,
            category = resource.category,
            tagsText = resource.tags.joinToString(", "),
            originalResource = resource,
            showDiscardConfirmation = false
        )
    }

    fun updateTitle(value: String) = mutableState.update { it.copy(title = value) }
    fun updateType(value: ResourceType) = mutableState.update { it.copy(type = value) }
    fun updateReference(value: String) = mutableState.update { it.copy(reference = value) }
    fun updateDescription(value: String) = mutableState.update { it.copy(description = value) }
    fun updateCategory(value: String) = mutableState.update { it.copy(category = value) }
    fun updateTags(value: String) = mutableState.update { it.copy(tagsText = value) }

    fun saveResource() {
        val state = mutableState.value
        if (!state.canSave) return
        val now = System.currentTimeMillis()
        val tags = state.tagsText.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        val id = state.editingId
        if (id == null) {
            repository.add(
                LibraryResource(
                    UUID.randomUUID().toString(),
                    state.title.trim(),
                    state.type,
                    state.reference.trim(),
                    state.description.trim(),
                    state.category.trim(),
                    tags,
                    now,
                    now
                )
            )
        } else {
            val newReference = state.reference.trim()
            val changedDocument = state.originalResource?.let { it.reference != newReference || it.type != state.type } == true
            repository.update(id) { current ->
                current.copy(
                    title = state.title.trim(),
                    type = state.type,
                    reference = newReference,
                    mimeType = if (current.reference == newReference) current.mimeType else "",
                    description = state.description.trim(),
                    category = state.category.trim(),
                    tags = tags,
                    updatedAtMillis = now
                )
            }
            if (changedDocument) readingStateRepository.delete(id)
            if (changedDocument) highlightRepository.removeDocument(id)
        }
        mutableState.update { closeEditor(it) }
    }

    fun requestDismissEditor() = mutableState.update {
        if (it.hasUnsavedChanges) it.copy(showDiscardConfirmation = true) else closeEditor(it)
    }

    fun keepEditing() = mutableState.update { it.copy(showDiscardConfirmation = false) }
    fun discardChanges() = mutableState.update { closeEditor(it) }
    fun requestDelete(resource: LibraryResource) =
        mutableState.update { it.copy(pendingDeleteId = resource.id) }

    fun showInfo(resource: LibraryResource) {
        mutableState.update { it.copy(infoResource = resource, infoLocation = null, infoAvailable = null, infoLoading = resource.type != ResourceType.Link) }
        if (resource.type == ResourceType.Link) {
            mutableState.update { it.copy(infoAvailable = true, infoLoading = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val location = runCatching { documentAccess.queryLocation(resource.reference) }.getOrNull()
            val available = runCatching { documentAccess.isReadable(resource.reference) }.getOrDefault(false)
            mutableState.update { current ->
                if (current.infoResource?.id != resource.id) current else current.copy(
                    infoLocation = location,
                    infoAvailable = available,
                    infoLoading = false,
                    documentNames = if (location?.name.isNullOrBlank()) current.documentNames else current.documentNames + (resource.id to location!!.name)
                )
            }
        }
    }

    fun dismissInfo() = mutableState.update { it.copy(infoResource = null, infoLocation = null, infoAvailable = null, infoLoading = false) }

    fun cancelDelete() = mutableState.update { it.copy(pendingDeleteId = null) }
    fun confirmDelete() {
        val id = mutableState.value.pendingDeleteId ?: return
        repository.delete(id)
        readingStateRepository.delete(id)
        highlightRepository.removeDocument(id)
        mutableState.update { it.copy(pendingDeleteId = null) }
    }

    fun addDocumentReference(uri: String, title: String, mimeType: String): Boolean {
        val normalized = normalizeUri(uri)
        if (mutableState.value.resources.any { it.type != ResourceType.Link && normalizeUri(it.reference) == normalized }) return false
        val type = documentAccess.resourceType(mimeType, title)
        val id = UUID.nameUUIDFromBytes(normalized.toByteArray(Charsets.UTF_8)).toString()
        repository.add(
            LibraryResource(
                id,
                title.ifBlank { "Untitled document" },
                type,
                normalized,
                createdAtMillis = System.currentTimeMillis(),
                mimeType = mimeType
            )
        )
        return true
    }

    fun showFolderBrowser() {
        mutableState.update {
            it.copy(
                isBrowserVisible = true,
                folderError = null,
                folderStack = emptyList(),
                folderItems = emptyList(),
                folderLoading = true
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val folders =
                runCatching { documentAccess.persistedFolders() }.getOrDefault(emptyList())
            mutableState.update {
                it.copy(
                    authorizedFolders = folders,
                    folderLoading = false,
                    folderError = if (folders.isEmpty()) "Choose a folder to browse files it contains." else null
                )
            }
        }
    }

    fun folderPermissionGranted(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folders =
                runCatching { documentAccess.persistedFolders() }.getOrDefault(emptyList())
            val chosen = documentAccess.queryLocation(uri) ?: DocumentLocation(uri, "Authorized folder", "", true)
            mutableState.update {
                it.copy(
                    isBrowserVisible = true,
                    authorizedFolders = folders,
                    folderStack = listOf(chosen),
                    folderLoading = false,
                    folderError = null
                )
            }
            chosen?.let { loadFolder(it.uri) }
        }
    }

    fun browseFolder(folder: DocumentLocation) {
        loadFolder(folder.uri, listOf(folder))
    }

    fun enterFolder(folder: DocumentLocation) {
        if (!folder.isDirectory) return
        loadFolder(folder.uri, mutableState.value.folderStack + folder)
    }

    fun backFolder() {
        val stack = mutableState.value.folderStack
        if (stack.size > 1) loadFolder(stack[stack.lastIndex - 1].uri, stack.dropLast(1))
        else mutableState.update {
            it.copy(
                folderStack = emptyList(),
                folderItems = emptyList(),
                folderError = null
            )
        }
    }

    fun closeFolderBrowser() = mutableState.update {
        it.copy(
            isBrowserVisible = false,
            folderStack = emptyList(),
            folderItems = emptyList(),
            folderError = null
        )
    }

    fun addFolderItem(item: DocumentLocation) {
        if (item.isDirectory) return
        addDocumentReference(item.uri, item.name, item.mimeType)
        closeFolderBrowser()
    }

    private fun loadFolder(
        uri: String,
        stack: List<DocumentLocation> = mutableState.value.folderStack
    ) {
        mutableState.update {
            it.copy(
                folderStack = stack,
                folderLoading = true,
                folderError = null,
                folderItems = emptyList()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val items = runCatching { documentAccess.children(uri) }.getOrElse {
                mutableState.update { state ->
                    state.copy(
                        folderLoading = false,
                        folderError = "This folder could not be read. Its provider may not support browsing."
                    )
                }
                return@launch
            }
            mutableState.update {
                it.copy(
                    folderLoading = false,
                    folderItems = items,
                    folderError = if (items.isEmpty()) "This folder is empty or its provider does not expose its contents." else null
                )
            }
        }
    }

    private fun refreshAvailability(resources: List<LibraryResource>) {
        viewModelScope.launch(Dispatchers.IO) {
            val missing = resources.filter {
                it.type != ResourceType.Link && !runCatching { documentAccess.isReadable(it.reference) }.getOrDefault(false)
            }.map { it.id }.toSet()
            val names = resources.filter { it.type != ResourceType.Link }.mapNotNull { resource ->
                runCatching { documentAccess.queryLocation(resource.reference) }.getOrNull()?.name?.takeIf(String::isNotBlank)?.let { resource.id to it }
            }.toMap()
            mutableState.update { it.copy(unavailableDocumentIds = missing, documentNames = it.documentNames + names) }
        }
    }

    private fun normalizeUri(value: String): String {
        val trimmed = value.trim()
        val colon = trimmed.indexOf(':')
        return if (colon > 0) trimmed.substring(0, colon).lowercase() + trimmed.substring(colon) else trimmed
    }

    private fun closeEditor(state: LibraryUiState) = state.copy(
        isEditorVisible = false,
        editingId = null,
        title = "",
        reference = "",
        description = "",
        category = "",
        tagsText = "",
        originalResource = null,
        showDiscardConfirmation = false
    )

    companion object {
        fun isValidWebUrl(value: String): Boolean = runCatching {
            if (value.isBlank() || value.any(Char::isWhitespace)) return false
            val uri = URI(value.trim())
            (uri.scheme.equals("http", true) || uri.scheme.equals(
                "https",
                true
            )) && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }
}

class LibraryViewModelFactory(
    private val repository: LibraryResourceRepository,
    private val readingStateRepository: DocumentReadingStateRepository,
    private val highlightRepository: PdfHighlightRepository,
    private val documentAccess: DocumentAccess
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LibraryViewModel(repository, readingStateRepository, highlightRepository, documentAccess) as T
}
