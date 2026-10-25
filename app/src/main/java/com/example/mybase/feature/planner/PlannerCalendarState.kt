package com.example.mybase.feature.planner

import com.example.mybase.core.model.CapturedEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarTaskCounts(val pending: Int = 0, val completed: Int = 0) {
    val hasTasks get() = pending + completed > 0
    val hasPending get() = pending > 0
}

object PlannerCalendarMapper {
    fun countsForDay(tasks: List<CapturedEntry>, epochDay: Long): CalendarTaskCounts {
        val matching = tasks.filter { it.kind == "Task" && it.dueDateEpochDay == epochDay }
        return CalendarTaskCounts(
            pending = matching.count { !it.completed },
            completed = matching.count { it.completed }
        )
    }

    /** Six rows keep the calendar stable while YearMonth handles year boundaries safely. */
    fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val first = month.atDay(1)
        val offset = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return buildList(42) {
            repeat(offset) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            while (size < 42) add(null)
        }
    }
}
