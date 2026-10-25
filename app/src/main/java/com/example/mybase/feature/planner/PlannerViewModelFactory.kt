package com.example.mybase.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mybase.data.repository.EntryRepository

class PlannerViewModelFactory(
    private val entryRepository: EntryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PlannerViewModel::class.java))
        return PlannerViewModel(entryRepository) as T
    }
}
