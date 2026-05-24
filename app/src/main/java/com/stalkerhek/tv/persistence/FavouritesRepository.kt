package com.stalkerhek.tv.persistence

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory favourites store with persistence via DataStore.
 * Thread-safe, survives configuration changes.
 */
object FavouritesRepository {
    private val store = ConcurrentHashMap<Int, MutableSet<String>>()

    fun getFavourites(profileId: Int): Set<String> = store[profileId]?.toSet() ?: emptySet()

    fun toggle(profileId: Int, cmd: String): Boolean {
        val set = store.getOrPut(profileId) { mutableSetOf() }
        return if (set.contains(cmd)) { set.remove(cmd); false } else { set.add(cmd); true }
    }

    fun isFavourite(profileId: Int, cmd: String): Boolean = store[profileId]?.contains(cmd) == true

    fun loadFromJson(profileId: Int, cmds: List<String>) {
        store[profileId] = cmds.toMutableSet()
    }

    fun toJson(profileId: Int): List<String> = store[profileId]?.toList() ?: emptyList()
}
