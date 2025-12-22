package org.krripe.shadowessentials.modules.back

import com.google.gson.GsonBuilder
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.teleport.TeleportDelayManager
import org.krripe.shadowessentials.core.teleport.TeleportUtils
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BackLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)

object BackManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val baseDir = File("config/ShadowEssentials/data/back")
    private val locations: MutableMap<UUID, BackLocation> = ConcurrentHashMap()
    private data class DailyState(var day: Long, var used: Int)
    private val dailyUsage = ConcurrentHashMap<UUID, DailyState>()

    private fun fileFor(uuid: UUID): File =
        File(baseDir, "$uuid.json")

    private fun save(uuid: UUID, loc: BackLocation) {
        baseDir.mkdirs()
        val f = fileFor(uuid)
        f.writeText(gson.toJson(loc))
    }

    private fun load(uuid: UUID): BackLocation? {
        val cached = locations[uuid]
        if (cached != null) return cached

        val f = fileFor(uuid)
        if (!f.exists()) return null

        return runCatching {
            gson.fromJson(f.readText(), BackLocation::class.java)
        }.getOrNull()?.also { locations[uuid] = it }
    }

    fun saveCurrentLocation(player: ServerPlayerEntity) {
        val worldId = player.world.registryKey.value.toString()
        val loc = BackLocation(
            world = worldId,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yaw,
            pitch = player.pitch
        )
        locations[player.uuid] = loc
        save(player.uuid, loc)
    }

    fun getLocation(player: ServerPlayerEntity): BackLocation? =
        load(player.uuid)

    fun clear(player: ServerPlayerEntity) {
        locations.remove(player.uuid)
        val f = fileFor(player.uuid)
        if (f.exists()) f.delete()
    }

    fun consumeDailyBack(player: ServerPlayerEntity, maxPerDay: Int): Boolean {
        if (maxPerDay <= 0) return true

        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val state = dailyUsage.compute(player.uuid) { _, old ->
            if (old == null || old.day != today) {
                DailyState(today, 0)
            } else {
                old
            }
        }!!

        if (state.used >= maxPerDay) {
            return false
        }
        state.used++
        return true
    }

    fun teleportBack(
        player: ServerPlayerEntity,
        useDelay: Boolean,
        delaySeconds: Int
    ): Boolean {
        val loc = getLocation(player) ?: return false
        val server = player.server ?: return false

        val worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(loc.world))
        val world = server.getWorld(worldKey) ?: return false

        val tpCfg = ConfigManager.config.teleport
        val sound = tpCfg.sound
        val particles = tpCfg.particles
        val dingSound = tpCfg.dingSound

        if (useDelay && delaySeconds > 0) {
            val sec = delaySeconds.coerceAtLeast(1)

            player.sendMessage(
                LangManager.msg("tpDelayStart", "time" to sec),
                true
            )

            TeleportDelayManager.start(
                player,
                sec,
                onTick = { p, left ->
                    dingSound?.let { playDing(p, it) }
                    p.sendMessage(
                        LangManager.msg("tpDelayTick", "left" to left),
                        true
                    )
                }
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
                if (!ok) {
                    p.sendMessage(LangManager.msg("teleportFailed"), false)
                }
            }
            return true
        }

        return TeleportUtils.teleportPlayer(
            player,
            world,
            Vec3d(loc.x, loc.y, loc.z),
            loc.yaw,
            loc.pitch,
            sound,
            particles
        )
    }

    private fun playDing(player: ServerPlayerEntity, soundId: String) {
        val id = Identifier.of(soundId)
        val soundEvent = Registries.SOUND_EVENT.get(id) ?: return
        val entry: RegistryEntry<SoundEvent> =
            Registries.SOUND_EVENT.getEntry(soundEvent)

        player.networkHandler.sendPacket(
            PlaySoundS2CPacket(
                entry,
                SoundCategory.PLAYERS,
                player.x, player.y, player.z,
                1.0f,
                1.0f,
                player.world.random.nextLong()
            )
        )
    }
}