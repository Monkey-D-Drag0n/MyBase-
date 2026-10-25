package com.example.mybase.data.repository

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.data.local.LibraryResourceLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface LibraryResourceRepository {
    val resources: StateFlow<List<LibraryResource>>
    fun add(resource: LibraryResource)
    fun update(id: String, transform: (LibraryResource) -> LibraryResource)
    fun delete(id: String)
}

class LocalLibraryResourceRepository(private val dataSource: LibraryResourceLocalDataSource) : LibraryResourceRepository {
    private val mutableResources = MutableStateFlow(dataSource.readResources())
    override val resources: StateFlow<List<LibraryResource>> = mutableResources.asStateFlow()

    override fun add(resource: LibraryResource) = save(listOf(resource) + mutableResources.value)
    override fun update(id: String, transform: (LibraryResource) -> LibraryResource) {
        val current = mutableResources.value
        if (current.any { it.id == id }) save(current.map { if (it.id == id) transform(it) else it })
    }
    override fun delete(id: String) {
        val current = mutableResources.value
        if (current.any { it.id == id }) save(current.filterNot { it.id == id })
    }
    private fun save(resources: List<LibraryResource>) {
        dataSource.writeResources(resources)
        mutableResources.value = resources
    }
}
