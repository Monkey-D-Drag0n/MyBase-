package com.example.mybase.feature.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopwatchSessionCodecTest {
    @Test
    fun roundTripsRunningAndPausedSessionAnchors() {
        val running = StoredStopwatchSession(12_345L, true, 67_890L, 123_456L)
        val paused = StoredStopwatchSession(12_345L, false, 0L, 0L)

        assertEquals(running, StopwatchSessionCodec.decode(StopwatchSessionCodec.encode(running)))
        assertEquals(paused, StopwatchSessionCodec.decode(StopwatchSessionCodec.encode(paused)))
    }

    @Test
    fun malformedOrUnknownVersionDataIsIgnored() {
        assertNull(StopwatchSessionCodec.decode(""))
        assertNull(StopwatchSessionCodec.decode("v2|0|1|10|20"))
        assertNull(StopwatchSessionCodec.decode("v1|-1|1|10|20"))
        assertNull(StopwatchSessionCodec.decode("v1|1|3|10|20"))
    }
}
