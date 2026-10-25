package com.example.mybase.feature.focus

import com.example.mybase.data.local.StringPreferenceStore

data class StoredStopwatchSession(
    val elapsedMillis: Long,
    val isRunning: Boolean,
    val startedElapsedRealtime: Long,
    val startedWallClockMillis: Long,
    val bootCount: Long = -1L
)

interface StopwatchSessionStorage {
    fun read(): StoredStopwatchSession?
    fun write(session: StoredStopwatchSession)
    fun clear()
}

class PreferenceStopwatchSessionStorage(
    private val preferences: StringPreferenceStore,
    private val key: String = "focus.stopwatch.v1"
) : StopwatchSessionStorage {
    override fun read(): StoredStopwatchSession? = StopwatchSessionCodec.decode(preferences.getString(key, ""))
    override fun write(session: StoredStopwatchSession) = preferences.putString(key, StopwatchSessionCodec.encode(session))
    override fun clear() = preferences.putString(key, "")
}

object StopwatchSessionCodec {
    fun encode(session: StoredStopwatchSession): String = listOf(
        "v1", session.elapsedMillis.coerceAtLeast(0L), if (session.isRunning) 1 else 0,
        session.startedElapsedRealtime.coerceAtLeast(0L), session.startedWallClockMillis.coerceAtLeast(0L), session.bootCount
    ).joinToString("|")

    fun decode(value: String): StoredStopwatchSession? = runCatching {
        val fields = value.split('|')
        require(fields.size == 6 && fields[0] == "v1")
        StoredStopwatchSession(
            elapsedMillis = fields[1].toLong().also { require(it >= 0L) },
            isRunning = fields[2].toInt().also { require(it == 0 || it == 1) } == 1,
            startedElapsedRealtime = fields[3].toLong().also { require(it >= 0L) },
            startedWallClockMillis = fields[4].toLong().also { require(it >= 0L) },
            bootCount = fields[5].toLong()
        )
    }.getOrNull()
}

class InMemoryStopwatchSessionStorage : StopwatchSessionStorage {
    private var value: StoredStopwatchSession? = null
    override fun read(): StoredStopwatchSession? = value
    override fun write(session: StoredStopwatchSession) { value = session }
    override fun clear() { value = null }
}
