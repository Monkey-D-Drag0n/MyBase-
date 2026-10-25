package com.example.mybase.feature.library

import androidx.lifecycle.ViewModelStore
import android.net.Uri
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.data.document.DocumentAccess
import com.example.mybase.data.document.DocumentLocation
import com.example.mybase.data.local.SharedPreferencesDocumentReadingStateLocalDataSource
import com.example.mybase.data.local.PdfHighlightLocalDataSource
import com.example.mybase.data.local.SharedPreferencesLibraryResourceLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import com.example.mybase.data.repository.LocalLibraryResourceRepository
import com.example.mybase.data.repository.LocalDocumentReadingStateRepository
import com.example.mybase.data.repository.LocalPdfHighlightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Test fun searchMatchesTitleDescriptionReferenceAndTagsAndTypeFilterWorks() = withViewModel { vm, _ ->
        val rows = listOf(
            resource("link", "Kotlin site", ResourceType.Link, description = "Language docs", reference = "https://kotlinlang.org", tags = listOf("compiler")),
            resource("pdf", "Design notes", ResourceType.Pdf, description = "Layout", reference = "file:///design.pdf")
        )
        rows.forEach { vm.addTestResource(it) }
        vm.setSearchQuery("KOTLINLANG")
        assertEquals(listOf("Kotlin site"), vm.uiState.value.filteredResources.map { it.title })
        vm.setSearchQuery("compiler")
        assertEquals(listOf("Kotlin site"), vm.uiState.value.filteredResources.map { it.title })
        vm.setSearchQuery("language docs")
        assertEquals(listOf("Kotlin site"), vm.uiState.value.filteredResources.map { it.title })
        vm.setSearchQuery("design notes")
        assertEquals(listOf("Design notes"), vm.uiState.value.filteredResources.map { it.title })
        vm.setSearchQuery(""); vm.selectType(ResourceType.Pdf)
        assertEquals(listOf("Design notes"), vm.uiState.value.filteredResources.map { it.title })
    }

    @Test fun createEditKeepsIdAndConfirmedDeleteRemovesOnlySelectedResource() = withViewModel { vm, repository ->
        vm.createResource(); vm.updateTitle("First link"); vm.updateReference("https://example.org"); vm.saveResource()
        val saved = repository.resources.value.single()
        vm.editResource(saved); vm.updateTitle("Edited link"); vm.saveResource()
        assertEquals(1, repository.resources.value.size)
        assertEquals(saved.id, repository.resources.value.single().id)
        repository.add(resource("second", "Second", ResourceType.Other))
        val edited = repository.resources.value.first { it.id == saved.id }
        vm.requestDelete(edited); vm.cancelDelete()
        assertEquals(2, repository.resources.value.size)
        vm.requestDelete(edited); vm.confirmDelete()
        assertEquals(listOf("Second"), repository.resources.value.map { it.title })
    }

    @Test fun dirtyEditorAsksBeforeDiscardingAndUrlValidationIsStrict() = withViewModel { vm, _ ->
        vm.createResource(); vm.updateTitle("link"); vm.requestDismissEditor()
        assertTrue(vm.uiState.value.showDiscardConfirmation)
        vm.keepEditing(); assertTrue(vm.uiState.value.isEditorVisible)
        vm.updateReference("www.example.com")
        assertFalse(vm.uiState.value.canSave)
        vm.updateReference("https://example.com/path?q=1")
        assertTrue(vm.uiState.value.canSave)
        assertFalse(LibraryViewModel.isValidWebUrl("javascript:alert(1)"))
        assertFalse(LibraryViewModel.isValidWebUrl("https://bad host"))
    }

    @Test fun filteringCanMatchCategoryAndTag() = withViewModel { vm, repository ->
        repository.add(resource("r", "Resource", ResourceType.Document, category = "Study", tags = listOf("read-later")))
        vm.selectCategory("Study"); vm.selectTag("read-later")
        assertEquals(1, vm.uiState.value.filteredResources.size)
        vm.setSearchQuery("missing")
        assertTrue(vm.uiState.value.filteredResources.isEmpty())
    }

    @Test fun pickedUriDeduplicatesAndUsesStableIdentity() = withViewModel { vm, repository ->
        assertTrue(vm.addDocumentReference("content://provider/document/42", "Paper.pdf", "application/pdf"))
        val saved = repository.resources.value.single()
        assertFalse(vm.addDocumentReference("CONTENT://provider/document/42", "Paper again.pdf", "application/pdf"))
        assertEquals(1, repository.resources.value.size)
        assertEquals(saved.id, repository.resources.value.single().id)
    }

    private fun withViewModel(block: suspend (LibraryViewModel, LocalLibraryResourceRepository) -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val repository = LocalLibraryResourceRepository(SharedPreferencesLibraryResourceLocalDataSource(object : StringPreferenceStore {
            private var value = ""
            override fun getString(key: String, defaultValue: String) = value.ifEmpty { defaultValue }
            override fun putString(key: String, value: String) { this.value = value }
        }))
        val readingStateRepository = LocalDocumentReadingStateRepository(SharedPreferencesDocumentReadingStateLocalDataSource(object : StringPreferenceStore {
            private var value = ""
            override fun getString(key: String, defaultValue: String) = value.ifEmpty { defaultValue }
            override fun putString(key: String, value: String) { this.value = value }
        }))
        val documentAccess = object : DocumentAccess {
            override fun retainFilePermission(uri: Uri) = true
            override fun retainTreePermission(uri: Uri) = true
            override fun persistedFolders() = emptyList<DocumentLocation>()
            override fun queryLocation(uri: String): DocumentLocation? = null
            override fun children(folderUri: String) = emptyList<DocumentLocation>()
            override fun resourceType(mimeType: String, displayName: String) = when {
                mimeType == "application/pdf" -> ResourceType.Pdf
                mimeType.startsWith("text/") -> ResourceType.Document
                else -> ResourceType.Other
            }
            override fun isReadable(uri: String) = true
        }
        val highlightRepository = LocalPdfHighlightRepository(object : PdfHighlightLocalDataSource {
            private var values = emptyList<PdfHighlight>()
            override fun read() = values
            override fun write(highlights: List<PdfHighlight>) { values = highlights }
        })
        val store = ViewModelStore()
        try {
            val vm = LibraryViewModel(repository, readingStateRepository, highlightRepository, documentAccess); store.put("library", vm)
            block(vm, repository)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    private fun resource(id: String, title: String, type: ResourceType, description: String = "", reference: String = "ref", category: String = "", tags: List<String> = emptyList()) =
        LibraryResource(id, title, type, reference, description, category, tags, 1L)

    private fun LibraryViewModel.addTestResource(resource: LibraryResource) {
        createResource(); updateTitle(resource.title); updateType(resource.type); updateReference(resource.reference)
        updateDescription(resource.description); updateCategory(resource.category); updateTags(resource.tags.joinToString(",")); saveResource()
    }
}
