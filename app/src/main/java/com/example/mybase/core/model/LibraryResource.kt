package com.example.mybase.core.model

/** A locally saved learning resource. Document references are stored as URIs or paths; files are never copied. */
data class LibraryResource(
    val id: String,
    val title: String,
    val type: ResourceType,
    val reference: String,
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis,
    val mimeType: String = ""
)

enum class ResourceType(val storedValue: String, val label: String) {
    Link("link", "Link"), Pdf("pdf", "PDF"), Document("document", "Document"), Other("other", "Other");

    companion object {
        fun fromStored(value: String?): ResourceType = entries.firstOrNull { it.storedValue == value } ?: Other
    }
}
