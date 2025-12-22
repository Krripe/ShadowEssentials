package org.krripe.shadowessentials.modules.tempfly

import com.google.gson.GsonBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.config.TempFlyDailyTier
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.modules.fly.FlyManager
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class TempFlyData(
    var secondsLeft: Long = 0,
    var active: Boolean = false,
    var lastDailyKey: String? = null
)

object TempFlyManager {

    enum class StackMode { RESET, ADD, MAX }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val baseDir = File("config/ShadowEssentials/data/tempfly")
    private val data: MutableMap<UUID, TempFlyData> = ConcurrentHashMap()
    private val perms = PermissionsSystem()

    private fun fileFor(uuid: UUID): File =
        File(baseDir, "$uuid.json")

    private fun load(uuid: UUID): TempFlyData? {
        val cached = data[uuid]
        if (cached != null) return cached

        val f = fileFor(uuid)
        if (!f.exists()) return null

        return runCatching {
            gson.fromJson(f.readText(), TempFlyData::class.java)
        }.getOrNull()?.also { data[uuid] = it }
    }

    private fun save(uuid: UUID, state: TempFlyData) {
        baseDir.mkdirs()
        val f = fileFor(uuid)
        if (state.secondsLeft <= 0 && !state.active && state.lastDailyKey == null) {
            if (f.exists()) f.delete()
        } else {
            f.writeText(gson.toJson(state))
        }
    }

    fun init() {
        baseDir.mkdirs()
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            tick(server)
        })
    }

    private fun isWorldAllowed(player: ServerPlayerEntity): Boolean {
        val cfg = ConfigManager.config.tempfly
        val worldId = player.world.registryKey.value.toString()
        val allowed = cfg.allowWorlds
        return allowed.isEmpty() || worldId in allowed
    }

    private fun tick(server: MinecraftServer) {
        val overworld = server.overworld ?: return
        if (overworld.time % 20L != 0L) return

        val toRemove = mutableListOf<UUID>()

        server.playerManager.playerList.forEach { player ->
            val uuid = player.uuid
            val state = load(uuid) ?: return@forEach

            if (!isWorldAllowed(player) && state.active) {
                state.active = false
                FlyManager.setTemp(player, false)
                player.sendMessage(LangManager.msg("tempflyWorldNotAllowed"))
                save(uuid, state)
                return@forEach
            }

            if (!state.active || state.secondsLeft <= 0) {
                if (state.secondsLeft <= 0 && state.active) {
                    state.active = false
                    FlyManager.setTemp(player, false)
                    save(uuid, state)
                }
                return@forEach
            }

            val flyingNow = player.abilities.flying || player.isFallFlying
            if (flyingNow) {
                state.secondsLeft = max(0, state.secondsLeft - 1)
                save(uuid, state)
            }

            val left = state.secondsLeft

            if (left > 0) {
                val formatted = formatSeconds(left)
                player.sendMessageToClient(
                    LangManager.msg("tempflyBar", "time" to formatted),
                    true
                )

                if (ConfigManager.config.tempfly.notifyOnExpire && flyingNow) {
                    if (left == 60L || left == 30L || left == 10L || left == 5L ||
                        left == 3L || left == 2L || left == 1L
                    ) {
                        player.sendMessage(
                            LangManager.msg("tempflyExpireSoon", "time" to formatted)
                        )
                    }
                }
            }

            if (left <= 0) {
                state.active = false
                FlyManager.setTemp(player, false)

                if (ConfigManager.config.tempfly.notifyOnExpire) {
                    player.sendMessage(LangManager.msg("tempflyExpired"))
                }

                save(uuid, state)
                toRemove += uuid
            }
        }

        toRemove.forEach { id ->
            val st = data[id] ?: return@forEach
            if (st.secondsLeft <= 0 && !st.active) {
                data.remove(id)
            }
        }
    }

    fun remaining(player: ServerPlayerEntity): Long {
        val st = load(player.uuid) ?: return 0
        return max(0, st.secondsLeft)
    }

    private fun parseDurationToSeconds(input: String): Long {
        val s = input.trim().lowercase()
        if (s.isEmpty()) return 0

        val regex = Regex("(\\d+)([smhd])")
        var total = 0L
        for (m in regex.findAll(s)) {
            val value = m.groupValues[1].toLongOrNull() ?: continue
            val unit = m.groupValues[2]
            val part = when (unit) {
                "s" -> value
                "m" -> value * 60
                "h" -> value * 3600
                "d" -> value * 86400
                else -> 0L
            }
            total += part
        }

        if (total == 0L) {
            return s.toLongOrNull() ?: 0
        }
        return total
    }

    private fun maxTimeSeconds(): Long {
        val cfg = ConfigManager.config.tempfly
        return parseDurationToSeconds(cfg.maxTime).coerceAtLeast(0)
    }

    private fun clampToMax(seconds: Long): Long {
        val max = maxTimeSeconds()
        if (max <= 0) return seconds
        return seconds.coerceAtMost(max)
    }

    private fun stackMode(): StackMode {
        return when (ConfigManager.config.tempfly.stackMode.uppercase()) {
            "RESET" -> StackMode.RESET
            "MAX"   -> StackMode.MAX
            else    -> StackMode.ADD
        }
    }

    fun give(player: ServerPlayerEntity, seconds: Long, modeOverride: StackMode? = null): Long {
        val uuid = player.uuid
        val current = load(uuid) ?: TempFlyData(0, active = false, lastDailyKey = null)

        val baseMode = modeOverride ?: stackMode()
        val newSeconds = when (baseMode) {
            StackMode.RESET -> seconds
            StackMode.ADD   -> current.secondsLeft + seconds
            StackMode.MAX   -> max(current.secondsLeft, seconds)
        }.coerceAtLeast(0)

        val finalSeconds = clampToMax(newSeconds)
        current.secondsLeft = finalSeconds

        if (finalSeconds <= 0 && current.active) {
            current.active = false
            FlyManager.setTemp(player, false)
        }

        data[uuid] = current
        save(uuid, current)
        return finalSeconds
    }

    fun setTime(player: ServerPlayerEntity, seconds: Long): Long {
        val uuid = player.uuid
        val s = clampToMax(seconds.coerceAtLeast(0))
        val current = load(uuid) ?: TempFlyData(0, active = false, lastDailyKey = null)
        current.secondsLeft = s

        if (s <= 0 && current.active) {
            current.active = false
            FlyManager.setTemp(player, false)
        }

        data[uuid] = current
        save(uuid, current)
        return s
    }

    fun extractTime(player: ServerPlayerEntity, seconds: Long): Long {
        val uuid = player.uuid
        val current = load(uuid) ?: TempFlyData(0, active = false, lastDailyKey = null)
        val s = max(0, current.secondsLeft - seconds.coerceAtLeast(0))
        current.secondsLeft = s

        if (s <= 0 && current.active) {
            current.active = false
            FlyManager.setTemp(player, false)
        }

        data[uuid] = current
        save(uuid, current)
        return s
    }

    fun setActive(player: ServerPlayerEntity, active: Boolean): Boolean {
        val uuid = player.uuid
        val st = load(uuid) ?: TempFlyData(0, active = false, lastDailyKey = null)
        if (active && st.secondsLeft <= 0) {
            return false
        }

        st.active = active
        data[uuid] = st
        save(uuid, st)

        FlyManager.setTemp(player, active)
        return true
    }

    private fun computeDailySecondsFor(player: ServerPlayerEntity): Long {
        val cfg = ConfigManager.config.tempfly
        val src = player.commandSource
        val tiers: List<TempFlyDailyTier> = cfg.dailyTiers

        var bestSeconds = 0L

        for (tier in tiers) {
            if (tier.permission.isBlank()) continue
            if (tier.time.isBlank()) continue

            if (!perms.hasPermission(src, tier.permission)) continue

            val sec = parseDurationToSeconds(tier.time)
            if (sec > bestSeconds) {
                bestSeconds = sec
            }
        }

        return bestSeconds
    }

    fun claimDailyIfAvailable(player: ServerPlayerEntity): Long {
        val cfg = ConfigManager.config.tempfly
        if (!cfg.dailyEnabled) return 0

        val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        val resetHour = cfg.dailyResetHour.coerceIn(0, 23)

        val today = if (now.hour < resetHour) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
        val currentKey = today.toString()

        val uuid = player.uuid
        val state = load(uuid) ?: TempFlyData(
            secondsLeft = 0,
            active = false,
            lastDailyKey = null
        )

        if (state.lastDailyKey == currentKey) {
            return 0
        }

        val awarded = computeDailySecondsFor(player)
        if (awarded <= 0) {
            return 0
        }

        give(player, awarded, modeOverride = StackMode.ADD)
        val newState = load(uuid) ?: state
        newState.lastDailyKey = currentKey
        data[uuid] = newState
        save(uuid, newState)

        player.sendMessage(
            LangManager.msg(
                "tempflyDailyReceived",
                "time" to formatSeconds(awarded)
            )
        )
        return awarded
    }

    fun formatSeconds(seconds: Long): String {
        var s = seconds.coerceAtLeast(0)
        val d = s / 86400
        s %= 86400
        val h = s / 3600
        s %= 3600
        val m = s / 60
        s %= 60

        val parts = mutableListOf<String>()
        if (d > 0) parts += "${d}d"
        if (h > 0) parts += "${h}h"
        if (m > 0) parts += "${m}m"
        if (s > 0 || parts.isEmpty()) parts += "${s}s"

        return parts.joinToString(" ")
    }

    fun isActive(player: ServerPlayerEntity): Boolean {
        val st = load(player.uuid) ?: return false
        return st.active && st.secondsLeft > 0
    }
}