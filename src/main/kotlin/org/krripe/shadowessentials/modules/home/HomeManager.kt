package org.krripe.shadowessentials.modules.home

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.teleport.CooldownManager
import org.krripe.shadowessentials.core.teleport.TeleportDelayManager
import org.krripe.shadowessentials.core.teleport.TeleportUtils
import org.krripe.shadowessentials.modules.back.BackManager
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object HomeManager {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val baseDir = File("config/ShadowEssentials/data/homes")
    private val homesCache: MutableMap<UUID, MutableMap<String, HomeLocation>> = ConcurrentHashMap()
    private val perms = PermissionsSystem()
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun fileFor(uuid: UUID): File =
        File(baseDir, "$uuid.json")

    private fun loadHomes(uuid: UUID): MutableMap<String, HomeLocation> {
        homesCache[uuid]?.let { return it }

        val file = fileFor(uuid)
        if (!file.exists()) {
            val empty = mutableMapOf<String, HomeLocation>()
            homesCache[uuid] = empty
            return empty
        }

        return runCatching {
            val type = object : TypeToken<Map<String, HomeLocation>>() {}.type
            val map: Map<String, HomeLocation> = gson.fromJson(file.readText(), type) ?: emptyMap()
            map.toMutableMap()
        }.getOrElse { mutableMapOf() }
            .also { homesCache[uuid] = it }
    }

    private fun saveHomes(uuid: UUID, homes: MutableMap<String, HomeLocation>) {
        baseDir.mkdirs()
        val file = fileFor(uuid)
        if (homes.isEmpty()) {
            if (file.exists()) file.delete()
            homesCache.remove(uuid)
            return
        }
        file.writeText(gson.toJson(homes))
        homesCache[uuid] = homes
    }

    fun getHomes(player: ServerPlayerEntity): Map<String, HomeLocation> =
        loadHomes(player.uuid).toMap()

    private fun worldId(player: ServerPlayerEntity): String =
        player.world.registryKey.value.toString()

    private fun isBlacklistedWorld(worldId: String, blacklist: List<String>): Boolean {
        return blacklist.isNotEmpty() && worldId in blacklist
    }

    private fun canSetHomeInWorld(worldId: String, allowed: List<String>): Boolean {
        return allowed.isEmpty() || worldId in allowed
    }

    private fun maxHomesFor(player: ServerPlayerEntity): Int {
        val cfg = ConfigManager.config.home
        val src = player.commandSource
        val tiers = cfg.perPermissionLimits
        val defaultFromTiers = if (tiers.isNotEmpty()) {
            tiers.minOf { it.max }
        } else {
            cfg.defaultMaxHomes
        }

        var best: Int? = null

        for (tier in tiers) {
            if (tier.max < 0) continue
            if (!perms.hasPermission(src, tier.permission)) continue

            if (best == null || tier.max > best) {
                best = tier.max
            }
        }

        return (best ?: defaultFromTiers).coerceAtLeast(0)
    }

    fun setHome(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.home
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("homeDisabled"))
            return false
        }

        val currentWorldId = worldId(player)
        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("homeWorldBlacklisted"))
            return false
        }

        if (!canSetHomeInWorld(currentWorldId, cfg.allowWorlds)) {
            player.sendMessage(LangManager.msg("homeWorldNotAllowed"))
            return false
        }

        val name = (rawName ?: "home").lowercase()
        val uuid = player.uuid
        val homes = loadHomes(uuid)

        val existing = homes[name]
        if (existing == null) {
            val limit = maxHomesFor(player)
            if (homes.size >= limit) {
                player.sendMessage(
                    LangManager.msg("homeLimitReached", "limit" to limit)
                )
                return false
            }
        }

        val createdAt = existing?.createdAt ?: System.currentTimeMillis()
        val loc = HomeLocation(
            world = currentWorldId,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yaw,
            pitch = player.pitch,
            createdAt = createdAt
        )

        homes[name] = loc
        saveHomes(uuid, homes)

        player.sendMessage(
            LangManager.msg("homeSet", "home" to name)
        )
        return true
    }

    fun deleteHome(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.home
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("homeDisabled"))
            return false
        }

        val currentWorldId = worldId(player)
        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("homeWorldBlacklisted"))
            return false
        }

        val name = (rawName ?: "home").lowercase()
        val uuid = player.uuid
        val homes = loadHomes(uuid)

        if (!homes.containsKey(name)) {
            player.sendMessage(
                LangManager.msg("homeNotFound", "home" to name)
            )
            return false
        }

        homes.remove(name)
        saveHomes(uuid, homes)

        player.sendMessage(
            LangManager.msg("homeDeleted", "home" to name)
        )
        return true
    }

    fun teleportToHome(player: ServerPlayerEntity, rawName: String?): Boolean {
        val name = (rawName?.takeIf { it.isNotBlank() } ?: "home").lowercase()

        val cfg = ConfigManager.config.home
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("homeDisabled"))
            return false
        }

        val currentWorldId = worldId(player)
        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("homeWorldBlacklisted"))
            return false
        }

        val uuid = player.uuid
        val homes = loadHomes(uuid)
        val loc = homes[name]

        if (loc == null) {
            player.sendMessage(
                LangManager.msg("homeNotFound", "home" to name)
            )
            return false
        }

        if (isBlacklistedWorld(loc.world, cfg.blacklistWorlds)) {
            player.sendMessage(
                LangManager.msg("homeTargetBlacklisted", "home" to name)
            )
            return false
        }

        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
            CooldownManager.isOnCooldown(player, "home")
        ) {
            val left = CooldownManager.remaining(player, "home")
            player.sendMessage(
                LangManager.msg("homeOnCooldown", "time" to left)
            )
            return false
        }

        val server = player.server ?: return false

        val worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(loc.world))
        val world = server.getWorld(worldKey) ?: run {
            player.sendMessage(LangManager.msg("teleportFailed"))
            return false
        }

        val tpCfg = ConfigManager.config.teleport
        val sound = tpCfg.sound
        val particles = tpCfg.particles

        BackManager.saveCurrentLocation(player)

        val useDelay = cfg.delayEnabled && cfg.delaySeconds > 0
        val sec = cfg.delaySeconds

        fun applyCooldown() {
            if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                CooldownManager.set(player, "home", cfg.cooldownSeconds)
            }
        }

        if (useDelay) {
            player.sendMessage(
                LangManager.msg("tpDelayStart", "time" to sec)
            )

            TeleportDelayManager.start(
                player,
                sec
            ) { p ->
                val ok = TeleportUtils.teleportPlayer(
                    p,
                    world,
                    Vec3d(loc.x, loc.y, loc.z),
                    loc.yaw,
                    loc.pitch,
                    sound,
                    particles
                )
                if (ok) {
                    p.sendMessage(
                        LangManager.msg("homeTeleport", "home" to name)
                    )
                    applyCooldown()
                } else {
                    p.sendMessage(LangManager.msg("teleportFailed"))
                }
            }

            return true
        }

        val ok = TeleportUtils.teleportPlayer(
            player,
            world,
            Vec3d(loc.x, loc.y, loc.z),
            loc.yaw,
            loc.pitch,
            sound,
            particles
        )
        if (ok) {
            player.sendMessage(
                LangManager.msg("homeTeleport", "home" to name)
            )
            applyCooldown()
        } else {
            player.sendMessage(LangManager.msg("teleportFailed"))
        }
        return ok
    }

    fun formatCreatedAt(createdAt: Long): String {
        if (createdAt <= 0) return "-"
        val time = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
        return time.format(dateFormatter)
    }
}