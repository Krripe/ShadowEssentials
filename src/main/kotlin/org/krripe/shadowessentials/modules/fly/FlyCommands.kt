package org.krripe.shadowessentials.modules.fly

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.command.Suggestions
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.teleport.CooldownManager

private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
    return allowed.isEmpty() || worldId in allowed
}

object FlyCommands {

    private val perms = PermissionsSystem()

    private fun has(src: ServerCommandSource, node: String): Boolean {
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    private fun requirePlayer(src: ServerCommandSource): ServerPlayerEntity? {
        val p = src.player
        if (p == null) {
            src.sendError(Text.literal("This command can only be used by players."))
            return null
        }
        return p
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            dispatcher.register(
                literal("fly")

                    // /fly  (toggle self)
                    .executes { ctx ->
                        val src = ctx.source
                        if (!has(src, "shadowessentials.fly")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.fly")
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src) ?: return@executes 0
                        val cfg = ConfigManager.config.fly

                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("flyNotAllowed"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("flyWorldNotAllowed"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                            CooldownManager.isOnCooldown(player, "fly")
                        ) {
                            val left = CooldownManager.remaining(player, "fly")
                            player.sendMessage(LangManager.msg("flyCooldown", "time" to left))
                            return@executes 0
                        }

                        val enabledNow = FlyManager.toggle(player)

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                            CooldownManager.set(player, "fly", cfg.cooldownSeconds)
                        }

                        player.sendMessage(
                            LangManager.msg(if (enabledNow) "flyEnabled" else "flyDisabled")
                        )
                        1
                    }

                    // /fly on  (self)
                    .then(
                        literal("on")
                            .requires { has(it, "shadowessentials.fly.on") }
                            .executes { ctx ->
                                val src = ctx.source
                                val player = requirePlayer(src) ?: return@executes 0
                                val cfg = ConfigManager.config.fly

                                if (!cfg.enabled) {
                                    player.sendMessage(LangManager.msg("flyNotAllowed"))
                                    return@executes 0
                                }

                                val worldId = player.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                                    player.sendMessage(LangManager.msg("flyWorldNotAllowed"))
                                    return@executes 0
                                }

                                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                                    CooldownManager.isOnCooldown(player, "fly")
                                ) {
                                    val left = CooldownManager.remaining(player, "fly")
                                    player.sendMessage(LangManager.msg("flyCooldown", "time" to left))
                                    return@executes 0
                                }

                                FlyManager.setToggled(player, true)

                                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                                    CooldownManager.set(player, "fly", cfg.cooldownSeconds)
                                }

                                player.sendMessage(LangManager.msg("flyEnabled"))
                                1
                            }
                    )

                    // /fly off  (self)
                    .then(
                        literal("off")
                            .requires { has(it, "shadowessentials.fly.off") }
                            .executes { ctx ->
                                val src = ctx.source
                                val player = requirePlayer(src) ?: return@executes 0
                                val cfg = ConfigManager.config.fly

                                if (!cfg.enabled) {
                                    player.sendMessage(LangManager.msg("flyNotAllowed"))
                                    return@executes 0
                                }

                                val worldId = player.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                                    player.sendMessage(LangManager.msg("flyWorldNotAllowed"))
                                    return@executes 0
                                }

                                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                                    CooldownManager.isOnCooldown(player, "fly")
                                ) {
                                    val left = CooldownManager.remaining(player, "fly")
                                    player.sendMessage(LangManager.msg("flyCooldown", "time" to left))
                                    return@executes 0
                                }

                                FlyManager.setToggled(player, false)

                                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                                    CooldownManager.set(player, "fly", cfg.cooldownSeconds)
                                }

                                player.sendMessage(LangManager.msg("flyDisabled"))
                                1
                            }
                    )

                    // /fly <player>  (toggle others)
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .requires { has(it, "shadowessentials.fly.others") }
                            .executes { ctx ->
                                val src = ctx.source
                                val cfg = ConfigManager.config.fly

                                if (!cfg.enabled) {
                                    src.sendMessage(LangManager.msg("flyNotAllowed"))
                                    return@executes 0
                                }

                                val target = EntityArgumentType.getPlayer(ctx, "player")
                                val worldId = target.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                                    src.sendMessage(LangManager.msg("flyWorldNotAllowed"))
                                    return@executes 0
                                }

                                val enabledNow = FlyManager.toggle(target)

                                src.sendMessage(
                                    LangManager.msg("flyToggledOther", "player" to target.name.string)
                                )
                                target.sendMessage(
                                    LangManager.msg(if (enabledNow) "flyEnabled" else "flyDisabled")
                                )
                                1
                            }
                    )
            )
        }
    }
}