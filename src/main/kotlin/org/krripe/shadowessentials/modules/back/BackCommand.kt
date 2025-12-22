package org.krripe.shadowessentials.modules.back

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.teleport.CooldownManager

object BackCommands {

    private val perms = PermissionsSystem()

    private fun has(src: ServerCommandSource, node: String): Boolean {
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    private fun requirePlayer(src: ServerCommandSource): ServerPlayerEntity {
        return src.player ?: run {
            src.sendError(Text.literal("This command can only be used by players."))
            throw IllegalStateException("Command requires player")
        }
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("back")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.back")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.back")
                            )
                            return@executes 0
                        }

                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.back

                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("backDisabled"))
                            return@executes 0
                        }

                        val currentWorldId = player.world.registryKey.value.toString()
                        if (cfg.allowWorlds.isNotEmpty() && currentWorldId !in cfg.allowWorlds) {
                            player.sendMessage(LangManager.msg("backWorldNotAllowed"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                            CooldownManager.isOnCooldown(player, "back")
                        ) {
                            val left = CooldownManager.remaining(player, "back")
                            player.sendMessage(
                                LangManager.msg("backOnCooldown", "time" to left)
                            )
                            return@executes 0
                        }

                        val loc = BackManager.getLocation(player)
                        if (loc == null) {
                            player.sendMessage(LangManager.msg("backNoLocation"))
                            return@executes 0
                        }

                        if (cfg.allowWorlds.isNotEmpty() && loc.world !in cfg.allowWorlds) {
                            player.sendMessage(LangManager.msg("backWorldNotAllowed"))
                            return@executes 0
                        }

                        if (cfg.dailyBacksEnabled && cfg.dailyBacks > 0) {
                            if (!BackManager.consumeDailyBack(player, cfg.dailyBacks)) {
                                player.sendMessage(
                                    LangManager.msg("backDailyLimit", "max" to cfg.dailyBacks)
                                )
                                return@executes 0
                            }
                        }

                        val ok = BackManager.teleportBack(
                            player,
                            useDelay = cfg.delayEnabled,
                            delaySeconds = cfg.delaySeconds
                        )
                        if (!ok) {
                            player.sendMessage(LangManager.msg("teleportFailed"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                            CooldownManager.set(player, "back", cfg.cooldownSeconds)
                        }

                        player.sendMessage(LangManager.msg("backTeleport"))
                        1
                    }
            )
        }
    }
}
