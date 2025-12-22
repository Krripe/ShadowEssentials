package org.krripe.shadowessentials.modules.warp

import com.google.gson.GsonBuilder
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

data class WarpData(
    val id: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    var permission: String? = null,
    var displayName: String? = null,
    var lore: List<String> = emptyList(),
    var iconMaterial: String? = null
)

object WarpManager {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val baseDir = File("config/ShadowEssentials/data/warps")
    private val warpCache: MutableMap<String, WarpData> = mutableMapOf()
    @Volatile
    private var loaded = false

    private val perms = PermissionsSystem()

    private fun normalizeId(raw: String): String =
        raw.trim().lowercase()

    private fun fileFor(id: String): File =
        File(baseDir, "${normalizeId(id)}.json")

    private fun ensureLoaded() {
        if (loaded) return
        baseDir.mkdirs()
        val files = baseDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        for (file in files) {
            runCatching {
                val w = gson.fromJson(file.readText(), WarpData::class.java)
                if (w != null && w.id.isNotBlank()) {
                    val idNorm = normalizeId(w.id)
                    warpCache[idNorm] = w.copy(id = idNorm)
                }
            }
        }
        loaded = true
    }

    fun all(): List<WarpData> {
        ensureLoaded()
        return warpCache.values.sortedBy { it.id }
    }

    fun get(rawId: String): WarpData? {
        ensureLoaded()
        return warpCache[normalizeId(rawId)]
    }

    fun exists(rawId: String): Boolean = get(rawId) != null

    private fun saveWarp(warp: WarpData) {
        baseDir.mkdirs()
        val normId = normalizeId(warp.id)
        val fixed = warp.copy(id = normId)
        warpCache[normId] = fixed
        val file = fileFor(normId)
        file.writeText(gson.toJson(fixed))
    }

    private fun deleteWarpFile(id: String) {
        val file = fileFor(id)
        if (file.exists()) file.delete()
    }

    private fun isBlacklistedWorld(worldId: String, blacklist: List<String>): Boolean =
        blacklist.isNotEmpty() && worldId in blacklist

    private fun canSetInWorld(worldId: String, allowed: List<String>): Boolean =
        allowed.isEmpty() || worldId in allowed

    private fun worldIdOf(player: ServerPlayerEntity): String =
        player.world.registryKey.value.toString()

    fun setWarp(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.warp
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("warpDisabled"))
            return false
        }

        val name = rawName?.trim()
        if (name.isNullOrEmpty()) return false
        val id = normalizeId(name)

        val currentWorldId = worldIdOf(player)

        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("warpWorldBlacklisted"))
            return false
        }

        if (!canSetInWorld(currentWorldId, cfg.allowWorlds)) {
            player.sendMessage(LangManager.msg("warpWorldNotAllowed"))
            return false
        }

        ensureLoaded()
        val existing = warpCache[id]

        val data = WarpData(
            id = id,
            world = currentWorldId,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yaw,
            pitch = player.pitch,
            permission = existing?.permission,
            displayName = existing?.displayName,
            lore = existing?.lore ?: emptyList(),
            iconMaterial = existing?.iconMaterial
        )

        saveWarp(data)

        player.sendMessage(LangManager.msg("warpSet", "warp" to id))
        return true
    }

    fun deleteWarp(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.warp
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("warpDisabled"))
            return false
        }

        val name = rawName?.trim()
        if (name.isNullOrEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        if (!warpCache.containsKey(id)) {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        warpCache.remove(id)
        deleteWarpFile(id)

        player.sendMessage(LangManager.msg("warpDeleted", "warp" to id))
        return true
    }

    fun relocateWarp(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.warp
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("warpDisabled"))
            return false
        }

        val name = rawName?.trim()
        if (name.isNullOrEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        val existing = warpCache[id] ?: run {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        val currentWorldId = worldIdOf(player)

        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("warpWorldBlacklisted"))
            return false
        }

        if (!canSetInWorld(currentWorldId, cfg.allowWorlds)) {
            player.sendMessage(LangManager.msg("warpWorldNotAllowed"))
            return false
        }

        val updated = existing.copy(
            world = currentWorldId,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yaw,
            pitch = player.pitch
        )

        saveWarp(updated)

        player.sendMessage(LangManager.msg("warpEditorRelocated", "warp" to id))
        return true
    }

    fun teleport(player: ServerPlayerEntity, rawName: String?): Boolean {
        val cfg = ConfigManager.config.warp
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("warpDisabled"))
            return false
        }

        val name = rawName?.trim()
        if (name.isNullOrEmpty()) return false
        val id = normalizeId(name)

        val currentWorldId = worldIdOf(player)
        if (isBlacklistedWorld(currentWorldId, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("warpWorldBlacklisted"))
            return false
        }

        val warp = get(id)
        if (warp == null) {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        if (isBlacklistedWorld(warp.world, cfg.blacklistWorlds)) {
            player.sendMessage(LangManager.msg("warpTargetBlacklisted", "warp" to id))
            return false
        }

        val permNode = warp.permission?.takeIf { it.isNotBlank() }
        if (permNode != null && !perms.hasPermission(player.commandSource, permNode)) {
            player.sendMessage(
                LangManager.msg(
                    "warpNoPermissionForWarp",
                    "warp" to id,
                    "perm" to permNode
                )
            )
            return false
        }

        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
            CooldownManager.isOnCooldown(player, "warp")
        ) {
            val left = CooldownManager.remaining(player, "warp")
            player.sendMessage(
                LangManager.msg("warpOnCooldown", "time" to left)
            )
            return false
        }

        val server = player.server ?: return false
        val worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(warp.world))
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
                CooldownManager.set(player, "warp", cfg.cooldownSeconds)
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
                    Vec3d(warp.x, warp.y, warp.z),
                    warp.yaw,
                    warp.pitch,
                    sound,
                    particles
                )
                if (ok) {
                    p.sendMessage(
                        LangManager.msg("warpTeleport", "warp" to id)
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
            Vec3d(warp.x, warp.y, warp.z),
            warp.yaw,
            warp.pitch,
            sound,
            particles
        )

        if (ok) {
            player.sendMessage(
                LangManager.msg("warpTeleport", "warp" to id)
            )
            applyCooldown()
        } else {
            player.sendMessage(LangManager.msg("teleportFailed"))
        }

        return ok
    }

    fun setPermission(player: ServerPlayerEntity, rawName: String, newPerm: String?): Boolean {
        val name = rawName.trim()
        if (name.isEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        val existing = warpCache[id]
        if (existing == null) {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        val normPerm = newPerm?.trim()?.takeIf { it.isNotEmpty() }

        existing.permission = normPerm
        saveWarp(existing)

        if (normPerm == null) {
            player.sendMessage(LangManager.msg("warpPermissionCleared", "warp" to id))
        } else {
            player.sendMessage(
                LangManager.msg(
                    "warpPermissionSet",
                    "warp" to id,
                    "perm" to normPerm
                )
            )
        }
        return true
    }

    // --- metode noi pentru editor ---

    fun setDisplayName(player: ServerPlayerEntity, rawName: String, newDisplayRaw: String?): Boolean {
        val name = rawName.trim()
        if (name.isEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        val existing = warpCache[id] ?: run {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        val clean = newDisplayRaw?.trim()?.takeIf { it.isNotEmpty() }
        existing.displayName = clean
        saveWarp(existing)

        if (clean == null) {
            player.sendMessage(LangManager.msg("warpEditorNameCleared", "warp" to id))
        } else {
            player.sendMessage(
                LangManager.msg(
                    "warpEditorNameSet",
                    "warp" to id,
                    "name" to clean
                )
            )
        }
        return true
    }

    fun setLore(player: ServerPlayerEntity, rawName: String, lines: List<String>): Boolean {
        val name = rawName.trim()
        if (name.isEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        val existing = warpCache[id] ?: run {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        val normalized = lines.map { it.trim() }.filter { it.isNotEmpty() }
        existing.lore = normalized
        saveWarp(existing)

        player.sendMessage(
            LangManager.msg(
                "warpEditorLoreSet",
                "warp" to id,
                "lines" to normalized.size.toString()
            )
        )
        return true
    }

    fun setIcon(player: ServerPlayerEntity, rawName: String, materialId: String?): Boolean {
        val name = rawName.trim()
        if (name.isEmpty()) return false
        val id = normalizeId(name)

        ensureLoaded()
        val existing = warpCache[id] ?: run {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to id))
            return false
        }

        val clean = materialId?.trim()?.takeIf { it.isNotEmpty() }
        existing.iconMaterial = clean
        saveWarp(existing)

        if (clean == null) {
            player.sendMessage(LangManager.msg("warpEditorIconCleared", "warp" to id))
        } else {
            player.sendMessage(
                LangManager.msg(
                    "warpEditorIconSet",
                    "warp" to id,
                    "icon" to clean
                )
            )
        }
        return true
    }
}