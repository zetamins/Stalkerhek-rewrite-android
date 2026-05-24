package com.stalkerhek.tv.persistence

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class WatchHistoryEntry(
    val profileId: Int,
    val cmd: String,
    val title: String,
    val genre: String,
    val logo: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
)

object WatchHistoryRepository {
    private val history = ConcurrentHashMap<Int, CopyOnWriteArrayList<WatchHistoryEntry>>()
    private const val MAX_ENTRIES = 50

    fun record(entry: WatchHistoryEntry) {
        val list = history.getOrPut(entry.profileId) { CopyOnWriteArrayList() }
        // Remove existing entry for same channel to avoid duplicates
        list.removeIf { it.cmd == entry.cmd }
        list.add(0, entry)
        // Trim to max size
        while (list.size > MAX_ENTRIES) list.removeAt(list.size - 1)
    }

    fun getHistory(profileId: Int): List<WatchHistoryEntry> =
        history[profileId]?.toList() ?: emptyList()

    fun updatePosition(profileId: Int, cmd: String, positionMs: Long) {
        history[profileId]?.let { list ->
            val idx = list.indexOfFirst { it.cmd == cmd }
            if (idx >= 0) {
                val existing = list[idx]
                list[idx] = existing.copy(positionMs = positionMs, watchedAt = System.currentTimeMillis())
            }
        }
    }

    fun getLastPosition(profileId: Int, cmd: String): Long =
        history[profileId]?.find { it.cmd == cmd }?.positionMs ?: 0L

    fun clear(profileId: Int) { history[profileId]?.clear() }
}
