package org.krripe.shadowessentials.modules.fly

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FlyManager {

    private data class State(
        var toggled: Boolean = false,
        var temp: Boolean = false
    )

    private val states = ConcurrentHashMap<UUID, State>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            tick(server)
        })
    }

    private fun tick(server: MinecraftServer) {
        val overworld = server.overworld ?: return
        if (overworld.time % 20L != 0L) return

        val cfg = ConfigManager.config.fly

        if (!cfg.enabled) {
            server.playerManager.playerList.forEach { player ->
                val st = states[player.uuid] ?: return@forEach
                if (st.toggled || st.temp) {
                    st.toggled = false
                    st.temp = false
                    apply(player)
                }
            }
            return
        }

        val allowedWorlds = cfg.allowWorlds

        server.playerManager.playerList.forEach { player ->
            val worldId = player.world.registryKey.value.toString()
            val worldAllowed = allowedWorlds.isEmpty() || worldId in allowedWorlds

            if (!worldAllowed) {
                val st = states[player.uuid] ?: return@forEach
                if (st.toggled) {
                    st.toggled = false
                    apply(player)
                    player.sendMessage(LangManager.msg("flyWorldNotAllowed"))
                }
            }
        }
    }

    private fun state(player: ServerPlayerEntity): State =
        states.computeIfAbsent(player.uuid) { State() }

    private fun apply(player: ServerPlayerEntity) {
        val st = state(player)
        val enable = st.toggled || st.temp

        val abilities = player.abilities
        if (enable) {
            abilities.allowFlying = true
            if (!player.isSpectator) {
                abilities.flying = true
            }
        } else {
            if (!player.isCreative && !player.isSpectator) {
                abilities.allowFlying = false
                abilities.flying = false
            }
        }
        player.sendAbilitiesUpdate()
    }

    fun isFlyEnabled(player: ServerPlayerEntity): Boolean =
        state(player).let { it.toggled || it.temp }

    fun toggle(player: ServerPlayerEntity): Boolean {
        val st = state(player)
        st.toggled = !st.toggled
        apply(player)
        return st.toggled
    }

    fun setToggled(player: ServerPlayerEntity, enabled: Boolean) {
        val st = state(player)
        st.toggled = enabled
        apply(player)
    }

    internal fun setTemp(player: ServerPlayerEntity, enabled: Boolean) {
        val st = state(player)
        st.temp = enabled
        apply(player)
    }

    fun clearAll(player: ServerPlayerEntity) {
        states.remove(player.uuid)
        apply(player)
    }
}