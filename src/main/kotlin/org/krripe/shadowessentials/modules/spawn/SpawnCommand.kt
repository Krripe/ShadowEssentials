package org.krripe.shadowessentials.modules.spawn

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.teleport.CooldownManager

object SpawnCommands {

    private val perms = PermissionsSystem()

    private fun has(src: ServerCommandSource, node: String): Boolean {
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    private val ONLY_PLAYERS = SimpleCommandExceptionType(
        Text.literal("This command can only be used by players.")
    )

    private fun requirePlayer(src: ServerCommandSource): ServerPlayerEntity =
        src.player ?: throw ONLY_PLAYERS.create()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            // /spawn
            dispatcher.register(
                literal("spawn")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.spawn")) {
                            src.sendMessage(
                                LangManager.msg(
                                    "noPermission",
                                    "perm" to "shadowessentials.spawn"
                                )
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)

                        val cfg = ConfigManager.config.spawn
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("spawnDisabled"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                            CooldownManager.isOnCooldown(player, "spawn")
                        ) {
                            val left = CooldownManager.remaining(player, "spawn")
                            player.sendMessage(
                                LangManager.msg("spawnOnCooldown", "time" to left)
                            )
                            return@executes 0
                        }

                        val useDelay = cfg.delayEnabled && cfg.delaySeconds > 0
                        val ok = SpawnManager.teleportToSpawn(
                            player,
                            useDelay,
                            cfg.delaySeconds
                        )

                        if (!ok) {
                            player.sendMessage(LangManager.msg("teleportFailed"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                            CooldownManager.set(player, "spawn", cfg.cooldownSeconds)
                        }

                        1
                    }
            )

            // /setspawn
            dispatcher.register(
                literal("setspawn")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.setspawn")) {
                            src.sendMessage(
                                LangManager.msg(
                                    "noPermission",
                                    "perm" to "shadowessentials.setspawn"
                                )
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)

                        SpawnManager.setSpawnFromPlayer(player)
                        player.sendMessage(LangManager.msg("spawnSet"))
                        1
                    }
            )
        }
    }
}