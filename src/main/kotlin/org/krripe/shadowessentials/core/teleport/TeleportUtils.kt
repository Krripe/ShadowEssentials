package org.krripe.shadowessentials.core.teleport

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object TeleportUtils {

    fun teleportPlayer(
        player: ServerPlayerEntity,
        world: ServerWorld,
        pos: Vec3d,
        yaw: Float = player.yaw,
        pitch: Float = player.pitch,
        sound: String? = null,
        particles: String? = null
    ): Boolean {
        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            val chunkPos = ChunkPos(blockPos)

            world.chunkManager.addTicket(ChunkTicketType.PORTAL, chunkPos, 5, blockPos)

            world.server.execute {
                player.teleport(world, pos.x, pos.y, pos.z, yaw, pitch)

                sound?.let {
                    val id = Identifier.of(it)
                    val soundEvent = Registries.SOUND_EVENT.get(id)
                    if (soundEvent != null) {
                        val entry: RegistryEntry<SoundEvent> =
                            Registries.SOUND_EVENT.getEntry(soundEvent)

                        player.networkHandler.sendPacket(
                            PlaySoundS2CPacket(
                                entry,
                                SoundCategory.PLAYERS,
                                player.x, player.y, player.z,
                                1.0f, 1.0f,
                                world.random.nextLong()
                            )
                        )
                    }
                }

                particles?.let {
                    val particleType = world.registryManager
                        .get(RegistryKeys.PARTICLE_TYPE)[Identifier.of(it)] ?: ParticleTypes.PORTAL
                    val particleEffect = particleType as? ParticleEffect ?: ParticleTypes.PORTAL
                    world.spawnParticles(
                        player,
                        particleEffect,
                        false,
                        pos.x, pos.y + 1, pos.z,
                        50,
                        0.5, 0.5, 0.5,
                        0.1
                    )
                }
            }


            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
