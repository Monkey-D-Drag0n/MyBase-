package com.example.mybase.core.model

/** A locally captured note or task shown in the dashboard's recent activity. */
data class CapturedEntry(
    val title: String,
    val kind: String,
    val createdAtMillis: Long,
    val completed: Boolean = false,
    /** LocalDate epoch day; null keeps legacy tasks without a due date unchanged. */
    val dueDateEpochDay: Long? = null,
    val priority: TaskPriority = TaskPriority.Normal,
    val body: String = "",
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val id: String = "$kind:$createdAtMillis:${title.hashCode()}"
)

enum class TaskPriority(val storedValue: String, val label: String) {
    Low("low", "Low"),
    Normal("normal", "Normal"),
    High("high", "High");

    companion object {
        fun fromStored(value: String?): TaskPriority = entries.firstOrNull { it.storedValue == value } ?: Normal
    }
}
