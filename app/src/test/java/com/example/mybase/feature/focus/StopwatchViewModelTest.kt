package com.example.mybase.feature.focus

import com.example.mybase.app.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StopwatchViewModelTest {
    @Test
    fun startPauseResumeAndResetUseMonotonicElapsedTime() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val mono = FakeClock()
        val wall = FakeClock()
        val viewModel = StopwatchViewModel(mono, wall)
        try {
            viewModel.start()
            viewModel.start() // duplicate start cannot replace the original anchor
            runCurrent()
            assertTrue(viewModel.uiState.value.isRunning)
            mono.advanceBy(750L); wall.advanceBy(750L); advanceTimeBy(250L); viewModel.refreshElapsed()
            assertEquals(750L, viewModel.uiState.value.elapsedMillis)

            viewModel.pause()
            mono.advanceBy(5_000L); wall.advanceBy(5_000L); advanceTimeBy(1_000L)
            assertFalse(viewModel.uiState.value.isRunning)
            assertEquals(750L, viewModel.uiState.value.elapsedMillis)

            viewModel.start()
            mono.advanceBy(250L); wall.advanceBy(250L); advanceTimeBy(250L); viewModel.refreshElapsed()
            assertEquals(1_000L, viewModel.uiState.value.elapsedMillis)

            viewModel.reset()
            assertEquals(StopwatchUiState(), viewModel.uiState.value)
        } finally {
            viewModel.pause()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun hiddenAndReopenedFocusUiDoesNotPauseTheSharedSession() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val mono = FakeClock()
        val wall = FakeClock()
        val timer = StopwatchViewModel(mono, wall)
        val app = AppViewModel()
        try {
            timer.start()
            app.showStopwatch()
            app.hideStopwatch() // Closing only changes visibility, not timer state.
            mono.advanceBy(12_000L); wall.advanceBy(12_000L)
            app.showStopwatch()
            timer.refreshElapsed()

            assertTrue(timer.uiState.value.isRunning)
            assertEquals(12_000L, timer.uiState.value.elapsedMillis)
            assertTrue(app.uiState.value.isStopwatchVisible)
        } finally {
            timer.pause()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun delayedUiRefreshCatchesUpFromMonotonicAnchor() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val mono = FakeClock()
        val wall = FakeClock()
        val viewModel = StopwatchViewModel(mono, wall)
        try {
            viewModel.start()
            runCurrent()
            mono.advanceBy(12 * 60 * 1_000L); wall.advanceBy(12 * 60 * 1_000L)
            // No UI/ticker work occurs during this simulated background interval.
            viewModel.refreshElapsed()
            assertEquals(12 * 60 * 1_000L, viewModel.uiState.value.elapsedMillis)
            assertTrue(viewModel.uiState.value.isRunning)
        } finally {
            viewModel.pause()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun runningSessionRestoresAcrossProcessRecreationWhenClocksAgree() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val storage = InMemoryStopwatchSessionStorage()
        val first = StopwatchViewModel(FakeClock(1_000L), FakeClock(100_000L), storage)
        first.start()
        val restored = StopwatchViewModel(FakeClock(31_000L), FakeClock(130_000L), storage)
        try {
            assertTrue(restored.uiState.value.isRunning)
            assertEquals(30_000L, restored.uiState.value.elapsedMillis)
        } finally {
            restored.pause()
            first.pause()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun rebootOrWallClockJumpRecoversSafelyPausedAtKnownElapsedBaseline() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val storage = InMemoryStopwatchSessionStorage()
        val first = StopwatchViewModel(FakeClock(1_000L), FakeClock(100_000L), storage, BootSessionProvider { 3L })
        first.start()
        // Clock deltas look plausible, but Android's boot counter confirms a reboot.
        val restored = StopwatchViewModel(FakeClock(30_000L), FakeClock(129_000L), storage, BootSessionProvider { 4L })
        try {
            assertFalse(restored.uiState.value.isRunning)
            assertEquals(0L, restored.uiState.value.elapsedMillis)
        } finally {
            restored.pause(); first.pause(); Dispatchers.resetMain()
        }
    }

    private class FakeClock(start: Long = 0L) : MonotonicClock, WallClock {
        private var timeMillis = start
        override fun nowMillis(): Long = timeMillis
        fun advanceBy(millis: Long) { timeMillis += millis }
    }
}
