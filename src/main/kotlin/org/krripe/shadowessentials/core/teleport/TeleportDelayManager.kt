package org.krripe.shadowessentials.core.teleport

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object TeleportDelayManager {

    private data class Pending(
        val playerId: UUID,
        var endTick: Long,
        var lastLeftSec: Int,
        val customOnTick: ((ServerPlayerEntity, Int) -> Unit)?,
        val onFinish: (ServerPlayerEntity) -> Unit
    )

    private val pending = ConcurrentHashMap<UUID, Pending>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            if (pending.isEmpty()) return@EndTick
            tick(server)
        })
    }

    fun start(
        player: ServerPlayerEntity,
        seconds: Int,
        onTick: ((ServerPlayerEntity, Int) -> Unit)? = null,
        onFinish: (ServerPlayerEntity) -> Unit
    ) {
        val sec = max(1, seconds)
        val currentTick = player.server?.overworld?.time ?: 0L
        val endTick = currentTick + sec * 20L

        pending[player.uuid] = Pending(
            playerId = player.uuid,
            endTick = endTick,
            lastLeftSec = sec,
            customOnTick = onTick,
            onFinish = onFinish
        )

        if (onTick != null) {
            onTick(player, sec)
        } else {
            sendStartMessage(player, sec)
        }
    }

    fun cancel(player: ServerPlayerEntity) {
        pending.remove(player.uuid)
    }

    private fun tick(server: MinecraftServer) {
        val nowTick = server.overworld?.time ?: return
        val toFinish = ArrayList<UUID>()

        pending.values.forEach { p ->
            val player = server.playerManager.getPlayer(p.playerId) ?: run {
                toFinish += p.playerId
                return@forEach
            }

            val ticksLeft = (p.endTick - nowTick).coerceAtLeast(0L)
            val secLeft = ((ticksLeft + 19) / 20).toInt()

            if (secLeft <= 0) {
                toFinish += p.playerId
            } else if (secLeft < p.lastLeftSec) {
                p.lastLeftSec = secLeft
                if (p.customOnTick != null) {
                    p.customOnTick.invoke(player, secLeft)
                } else {
                    sendTickMessage(player, secLeft)
                }
            }
        }

        toFinish.forEach { id ->
            val entry = pending.remove(id) ?: return@forEach
            val player = server.playerManager.getPlayer(id) ?: return@forEach
            entry.onFinish(player)
        }
    }

    private fun sendStartMessage(player: ServerPlayerEntity, seconds: Int) {
        player.sendMessage(
            LangManager.msg("tpDelayStart", "time" to seconds),
            true
        )
        playDing(player)
    }

    private fun sendTickMessage(player: ServerPlayerEntity, left: Int) {
        player.sendMessage(
            LangManager.msg("tpDelayTick", "left" to left),
            true
        )
        playDing(player)
    }

    private fun playDing(player: ServerPlayerEntity) {
        val cfg = ConfigManager.config.teleport
        val soundId = cfg.dingSound ?: return

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