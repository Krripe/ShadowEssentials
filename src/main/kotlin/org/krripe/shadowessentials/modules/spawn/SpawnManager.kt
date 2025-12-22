package org.krripe.shadowessentials.modules.spawn

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.teleport.TeleportDelayManager
import org.krripe.shadowessentials.core.teleport.TeleportUtils
import org.krripe.shadowessentials.modules.back.BackManager
import java.io.File

object SpawnManager {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val file = File("config/ShadowEssentials/data/spawn.json")

    @Volatile
    private var spawnLocation: SpawnLocation? = null

    init {
        load()
    }

    private fun load() {
        if (!file.exists()) {
            spawnLocation = null
            return
        }

        runCatching {
            val type = object : TypeToken<SpawnLocation>() {}.type
            spawnLocation = gson.fromJson<SpawnLocation>(file.readText(), type)
        }.onFailure {
            it.printStackTrace()
            spawnLocation = null
        }
    }

    private fun save() {
        val loc = spawnLocation ?: return
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(loc))
    }

    fun setSpawnFromPlayer(player: ServerPlayerEntity) {
        val worldId = player.world.registryKey.value.toString()
        spawnLocation = SpawnLocation(
            world = worldId,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yaw,
            pitch = player.pitch
        )
        save()
    }

    fun hasSpawn(): Boolean = spawnLocation != null
    fun getSpawnLocation(): SpawnLocation? = spawnLocation

    fun teleportToSpawn(
        player: ServerPlayerEntity,
        useDelay: Boolean,
        delaySeconds: Int
    ): Boolean {
        val loc = spawnLocation ?: return false
        val server = player.server ?: return false

        val worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(loc.world))
        val world = server.getWorld(worldKey) ?: return false

        val tpCfg = ConfigManager.config.teleport
        val sound = tpCfg.sound
        val particles = tpCfg.particles

        BackManager.saveCurrentLocation(player)

        if (useDelay && delaySeconds > 0) {
            TeleportDelayManager.start(
                player,
                delaySeconds
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
                    p.sendMessage(LangManager.msg("spawnTeleport"), false)
                } else {
                    p.sendMessage(LangManager.msg("teleportFailed"), false)
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
            player.sendMessage(LangManager.msg("spawnTeleport"), false)
        } else {
            player.sendMessage(LangManager.msg("teleportFailed"), false)
        }
        return ok
    }
}