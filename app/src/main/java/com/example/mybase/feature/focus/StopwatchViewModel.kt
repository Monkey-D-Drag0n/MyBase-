package com.example.mybase.feature.focus

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

fun interface MonotonicClock { fun nowMillis(): Long }
fun interface WallClock { fun nowMillis(): Long }
fun interface BootSessionProvider { fun currentBootCount(): Long }

/** Monotonic time is authoritative; wall time is only used to detect reboot/clock discontinuity on recovery. */
class StopwatchViewModel(
    private val clock: MonotonicClock = MonotonicClock { SystemClock.elapsedRealtime() },
    private val wallClock: WallClock = WallClock { System.currentTimeMillis() },
    private val storage: StopwatchSessionStorage = InMemoryStopwatchSessionStorage(),
    private val bootSessionProvider: BootSessionProvider = BootSessionProvider { -1L }
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState = _uiState.asStateFlow()

    private var accumulatedMillis = 0L
    private var startedAtElapsedRealtime = 0L
    private var startedAtWallClock = 0L
    private var startedBootCount = -1L
    private var tickerJob: Job? = null

    init { restore() }

    fun start() {
        if (_uiState.value.isRunning) return
        startedAtElapsedRealtime = clock.nowMillis()
        startedAtWallClock = wallClock.nowMillis()
        startedBootCount = safeBootCount()
        persist(running = true)
        _uiState.value = StopwatchUiState(accumulatedMillis, isRunning = true)
        ensureTicker()
    }

    fun pause() {
        if (!_uiState.value.isRunning) return
        accumulatedMillis = currentElapsedMillis()
        _uiState.value = StopwatchUiState(accumulatedMillis, isRunning = false)
        tickerJob?.cancel()
        tickerJob = null
        persist(running = false)
    }

    fun reset() {
        accumulatedMillis = 0L
        startedAtElapsedRealtime = 0L
        startedAtWallClock = 0L
        startedBootCount = -1L
        tickerJob?.cancel()
        tickerJob = null
        storage.clear()
        _uiState.value = StopwatchUiState()
    }

    fun toggleRunning() { if (_uiState.value.isRunning) pause() else start() }

    /** Recompute from the monotonic anchor, so delayed UI/ticker work never loses elapsed time. */
    fun refreshElapsed() {
        if (_uiState.value.isRunning) _uiState.update { it.copy(elapsedMillis = currentElapsedMillis()) }
    }

    private fun restore() {
        val saved = storage.read() ?: return
        accumulatedMillis = saved.elapsedMillis.coerceAtLeast(0L)
        if (!saved.isRunning) {
            _uiState.value = StopwatchUiState(accumulatedMillis, false)
            return
        }
        val nowElapsed = clock.nowMillis()
        val nowWall = wallClock.nowMillis()
        val currentBootCount = safeBootCount()
        val monotonicDelta = nowElapsed - saved.startedElapsedRealtime
        val wallDelta = nowWall - saved.startedWallClockMillis
        val bootMatches = saved.bootCount < 0L || currentBootCount < 0L || saved.bootCount == currentBootCount
        val sameBootAndStableClock = bootMatches && monotonicDelta >= 0L && wallDelta >= 0L &&
            abs(monotonicDelta - wallDelta) <= RECOVERY_CLOCK_TOLERANCE_MILLIS
        if (sameBootAndStableClock) {
            startedAtElapsedRealtime = saved.startedElapsedRealtime
            startedAtWallClock = saved.startedWallClockMillis
            startedBootCount = saved.bootCount
            _uiState.value = StopwatchUiState(accumulatedMillis + monotonicDelta, true)
            ensureTicker()
        } else {
            // A reboot or wall-clock jump makes the old running interval ambiguous. Keep only known time.
            _uiState.value = StopwatchUiState(accumulatedMillis, false)
            persist(running = false)
        }
    }

    private fun currentElapsedMillis(): Long = (accumulatedMillis + (clock.nowMillis() - startedAtElapsedRealtime)).coerceAtLeast(accumulatedMillis)

    private fun persist(running: Boolean) {
        storage.write(
            StoredStopwatchSession(
                elapsedMillis = accumulatedMillis,
                isRunning = running,
                startedElapsedRealtime = if (running) startedAtElapsedRealtime else 0L,
                startedWallClockMillis = if (running) startedAtWallClock else 0L,
                bootCount = if (running) startedBootCount else -1L
            )
        )
    }

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(TICK_MILLIS)
                refreshElapsed()
            }
        }
    }

    private fun safeBootCount(): Long = runCatching { bootSessionProvider.currentBootCount() }.getOrDefault(-1L)

    override fun onCleared() {
        if (_uiState.value.isRunning) persist(running = true)
        tickerJob?.cancel()
        tickerJob = null
    }

    private companion object {
        const val TICK_MILLIS = 250L
        const val RECOVERY_CLOCK_TOLERANCE_MILLIS = 2 * 60 * 1000L
    }
}

class StopwatchViewModelFactory(
    private val storage: StopwatchSessionStorage,
    private val bootSessionProvider: BootSessionProvider = BootSessionProvider { -1L }
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StopwatchViewModel(storage = storage, bootSessionProvider = bootSessionProvider) as T
}
