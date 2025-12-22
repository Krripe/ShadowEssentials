package org.krripe.shadowessentials.core.teleport

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object CooldownManager {

    private data class Key(val playerId: UUID, val id: String)

    private val cooldowns = ConcurrentHashMap<Key, Long>()

    fun set(player: ServerPlayerEntity, id: String, seconds: Long) {
        if (seconds <= 0) {
            cooldowns.remove(Key(player.uuid, id))
            return
        }
        val expireAt = System.currentTimeMillis() + seconds * 1000L
        cooldowns[Key(player.uuid, id)] = expireAt
    }

    fun clear(player: ServerPlayerEntity, id: String) {
        cooldowns.remove(Key(player.uuid, id))
    }

    fun clearAll(player: ServerPlayerEntity) {
        cooldowns.keys.removeIf { it.playerId == player.uuid }
    }

    fun isOnCooldown(player: ServerPlayerEntity, id: String): Boolean {
        val key = Key(player.uuid, id)
        val expireAt = cooldowns[key] ?: return false
        if (System.currentTimeMillis() >= expireAt) {
            cooldowns.remove(key)
            return false
        }
        return true
    }

    fun remaining(player: ServerPlayerEntity, id: String): Long {
        val key = Key(player.uuid, id)
        val expireAt = cooldowns[key] ?: return 0L
        val left = (expireAt - System.currentTimeMillis()) / 1000L
        if (left <= 0L) {
            cooldowns.remove(key)
            return 0L
        }
        return max(0L, left)
    }
}