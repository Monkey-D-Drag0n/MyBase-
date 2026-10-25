package com.example.mybase.feature.home

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.DocumentReadingState
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.feature.focus.StopwatchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyDashboardMapperTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun mapsDueTodayAndOverdueTasksWithoutCountingUnscheduledTasks() {
        val dueCompleted = task("done", today.toEpochDay(), completed = true)
        val dueOpen = task("today", today.toEpochDay())
        val yesterday = task("late", today.minusDays(1).toEpochDay())
        val completedOverdue = task("finished late", today.minusDays(2).toEpochDay(), completed = true)
        val unscheduled = task("whenever", null)

        val state = DailyDashboardMapper.map(
            listOf(dueCompleted, dueOpen, yesterday, completedOverdue, unscheduled),
            emptyList(), emptyList(), today
        )

        assertEquals(listOf("today", "done"), state.todayTasks.map { it.title })
        assertEquals(listOf("late"), state.overdueTasks.map { it.title })
        assertEquals(1, state.completedDueToday)
        assertEquals(.5f, state.todayProgress, 0f)
    }

    @Test
    fun usesExactLocalEpochDayBoundaryForDueToday() {
        val before = task("yesterday", today.minusDays(1).toEpochDay())
        val after = task("tomorrow", today.plusDays(1).toEpochDay())
        val state = DailyDashboardMapper.map(listOf(before, after), emptyList(), emptyList(), today)

        assertTrue(state.todayTasks.isEmpty())
        assertEquals(listOf("yesterday"), state.overdueTasks.map { it.title })
    }

    @Test
    fun emptySourcesProduceAnEmptyDashboardAndZeroProgress() {
        val state = DailyDashboardMapper.map(emptyList(), emptyList(), emptyList(), today)

        assertTrue(state.isEmpty)
        assertEquals(0f, state.todayProgress, 0f)
    }

    @Test
    fun recentNotesUseCreationTimestampAndReadingUsesSavedPageWithoutInventingRecency() {
        val old = CapturedEntry("Old", "Note", 10L)
        val recent = CapturedEntry("New", "Note", 20L, body = "A saved idea")
        val pdf = LibraryResource("pdf-id", "Guide", ResourceType.Pdf, "content://docs/guide", createdAtMillis = 5L)
        val state = DailyDashboardMapper.map(
            entries = listOf(old, recent, task("task", today.toEpochDay())),
            resources = listOf(pdf),
            readingStates = listOf(DocumentReadingState("pdf-id", lastPage = 4)),
            today = today
        )

        assertEquals(listOf("New", "Old"), state.recentNotes.map { it.title })
        assertEquals("A saved idea", state.recentNotes.first().body)
        assertEquals("Guide", state.continueReading.single().resource.title)
        assertEquals(5, state.continueReading.single().pageNumber)
        assertFalse(state.isEmpty)
    }

    @Test
    fun doesNotShowLibraryLinksAsResumeDocuments() {
        val link = LibraryResource("link-id", "Article", ResourceType.Link, "https://example.com", createdAtMillis = 1L)
        val state = DailyDashboardMapper.map(emptyList(), listOf(link), listOf(DocumentReadingState("link-id", 2)), today)

        assertTrue(state.continueReading.isEmpty())
    }

    @Test
    fun dashboardFocusMappingUsesSharedTimerStateAndReadableTime() {
        val ready = mapDashboardFocus(StopwatchUiState())
        val paused = mapDashboardFocus(StopwatchUiState(elapsedMillis = 65_000L))
        val running = mapDashboardFocus(StopwatchUiState(elapsedMillis = 3_661_000L, isRunning = true))

        assertEquals(DashboardFocusAction.Start, ready.primaryAction)
        assertEquals("00:00", ready.elapsedText)
        assertEquals(DashboardFocusAction.Resume, paused.primaryAction)
        assertEquals("01:05", paused.elapsedText)
        assertEquals(DashboardFocusAction.Pause, running.primaryAction)
        assertEquals("01:01:01", running.elapsedText)
    }

    private fun task(title: String, due: Long?, completed: Boolean = false) = CapturedEntry(
        title = title,
        kind = "Task",
        createdAtMillis = 1L,
        completed = completed,
        dueDateEpochDay = due
    )
}
