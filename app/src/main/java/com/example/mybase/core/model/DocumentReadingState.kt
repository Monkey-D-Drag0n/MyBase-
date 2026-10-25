package com.example.mybase.core.model

data class DocumentReadingState(
    val documentId: String,
    val lastPage: Int = 0,
    val bookmarks: List<Int> = emptyList()
)
