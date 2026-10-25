package com.example.mybase.feature.planner

import com.example.mybase.core.model.CapturedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class PlannerCalendarMapperTest {
    @Test
    fun pendingAndCompletedDatesHaveDistinctStatuses() {
        val day = LocalDate.of(2026, 10, 25).toEpochDay()
        val tasks = listOf(task("pending", day), task("done", day, true), task("another day", day + 1, true))

        val mixed = PlannerCalendarMapper.countsForDay(tasks, day)
        val other = PlannerCalendarMapper.countsForDay(tasks, day + 1)
        val empty = PlannerCalendarMapper.countsForDay(tasks, day - 1)

        assertEquals(1, mixed.pending)
        assertEquals(1, mixed.completed)
        assertTrue(mixed.hasPending) // Pending marker takes priority in the UI.
        assertEquals(0, other.pending)
        assertEquals(1, other.completed)
        assertFalse(other.hasPending)
        assertFalse(empty.hasTasks)
    }

    @Test
    fun monthGridUsesLocalDateEpochDaysAndCrossesYearBoundary() {
        val december = YearMonth.of(2026, 12)
        val cells = PlannerCalendarMapper.monthCells(december, DayOfWeek.MONDAY)
        val dates = cells.filterNotNull()

        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 12, 1), dates.first())
        assertEquals(LocalDate.of(2026, 12, 31), dates.last())
        assertEquals(YearMonth.of(2027, 1), december.plusMonths(1))
        assertEquals(LocalDate.of(2027, 1, 1).toEpochDay(), YearMonth.of(2027, 1).atDay(1).toEpochDay())
    }

    @Test
    fun monthGridHonorsFirstWeekdayAndLeapFebruary() {
        val february = YearMonth.of(2028, 2)
        val cells = PlannerCalendarMapper.monthCells(february, DayOfWeek.SUNDAY)

        assertEquals(null, cells[0]) // February 1, 2028 is Tuesday.
        assertEquals(LocalDate.of(2028, 2, 1), cells[2])
        assertEquals(LocalDate.of(2028, 2, 29), cells.filterNotNull().last())
    }

    private fun task(title: String, due: Long, completed: Boolean = false) = CapturedEntry(
        title = title, kind = "Task", createdAtMillis = 1L, dueDateEpochDay = due, completed = completed
    )
}
