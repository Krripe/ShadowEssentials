package org.krripe.shadowessentials.modules.tpa

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.command.Suggestions
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.teleport.TeleportUtils

private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
    return allowed.isEmpty() || worldId in allowed
}

object TpaCommands {

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

            // /tpa <player>
            val tpaRoot = literal("tpa")
            tpaRoot.then(
                argument("player", StringArgumentType.word())
                    .suggests(Suggestions.onlinePlayers)
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.tpa")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.tpa")
                            )
                            return@executes 0
                        }

                        val sender = requirePlayer(src)
                        val targetName = StringArgumentType.getString(ctx, "player")

                        val target = sender.server.playerManager.getPlayer(targetName)
                        if (target == null) {
                            sender.sendMessage(LangManager.msg("tpaTargetOffline"))
                            return@executes 0
                        }

                        TpaManager.requestTo(sender, target)
                        1
                    }
            )
            dispatcher.register(tpaRoot)

            // /tpahere <player>
            val tpahereRoot = literal("tpahere")
            tpahereRoot.then(
                argument("player", StringArgumentType.word())
                    .suggests(Suggestions.onlinePlayers)
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.tpahere")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.tpahere")
                            )
                            return@executes 0
                        }

                        val sender = requirePlayer(src)
                        val targetName = StringArgumentType.getString(ctx, "player")

                        val target = sender.server.playerManager.getPlayer(targetName)
                        if (target == null) {
                            sender.sendMessage(LangManager.msg("tpaTargetOffline"))
                            return@executes 0
                        }

                        TpaManager.requestHere(sender, target)
                        1
                    }
            )
            dispatcher.register(tpahereRoot)

            // /tpaccept
            val tpacceptRoot = literal("tpaccept")
            tpacceptRoot.executes { ctx ->
                val src = ctx.source

                if (!has(src, "shadowessentials.tpaccept")) {
                    src.sendMessage(
                        LangManager.msg("noPermission", "perm" to "shadowessentials.tpaccept")
                    )
                    return@executes 0
                }

                val player = requirePlayer(src)
                TpaManager.accept(player)
                1
            }
            dispatcher.register(tpacceptRoot)

            // /tpdeny
            val tpdenyRoot = literal("tpdeny")
            tpdenyRoot.executes { ctx ->
                val src = ctx.source

                if (!has(src, "shadowessentials.tpdeny")) {
                    src.sendMessage(
                        LangManager.msg("noPermission", "perm" to "shadowessentials.tpdeny")
                    )
                    return@executes 0
                }

                val player = requirePlayer(src)
                TpaManager.deny(player)
                1
            }
            dispatcher.register(tpdenyRoot)

            // /tpcancel
            val tpcancelRoot = literal("tpcancel")
            tpcancelRoot.executes { ctx ->
                val src = ctx.source

                if (!has(src, "shadowessentials.tpcancel")) {
                    src.sendMessage(
                        LangManager.msg("noPermission", "perm" to "shadowessentials.tpcancel")
                    )
                    return@executes 0
                }

                val player = requirePlayer(src)
                TpaManager.cancel(player)
                1
            }
            dispatcher.register(tpcancelRoot)

            // /tpall
            val tpallRoot = literal("tpall")
            tpallRoot.executes { ctx ->
                val src = ctx.source

                if (!has(src, "shadowessentials.tpall")) {
                    src.sendMessage(
                        LangManager.msg("noPermission", "perm" to "shadowessentials.tpall")
                    )
                    return@executes 0
                }

                val sender = requirePlayer(src)
                val server = sender.server

                val cfg = ConfigManager.config.tpa
                if (!cfg.enabled) {
                    sender.sendMessage(LangManager.msg("tpaDisabled"))
                    return@executes 0
                }

                val allowedWorlds = cfg.allowWorlds
                val senderWorldId = sender.world.registryKey.value.toString()
                if (!isWorldAllowed(senderWorldId, allowedWorlds)) {
                    sender.sendMessage(LangManager.msg("tpaWorldNotAllowed"))
                    return@executes 0
                }

                val targets = server.playerManager.playerList
                    .filterIsInstance<ServerPlayerEntity>()
                    .filter { it.uuid != sender.uuid }
                    .filter {
                        val w = it.world.registryKey.value.toString()
                        isWorldAllowed(w, allowedWorlds)
                    }

                if (targets.isEmpty()) {
                    sender.sendMessage(LangManager.msg("tpallNoTargets"))
                    return@executes 0
                }

                sender.sendMessage(
                    LangManager.msg("tpallTeleporting", "count" to targets.size)
                )

                val teleCfg = ConfigManager.config.teleport
                val sound = teleCfg.sound
                val particles = teleCfg.particles

                val destWorld = sender.world as ServerWorld
                val destPos = sender.pos
                val destYaw = sender.yaw
                val destPitch = sender.pitch

                targets.forEach { target ->
                    val ok = TeleportUtils.teleportPlayer(
                        target,
                        destWorld,
                        destPos,
                        destYaw,
                        destPitch,
                        sound,
                        particles
                    )
                    if (ok) {
                        target.sendMessage(
                            LangManager.msg(
                                "tpallTeleportedTarget",
                                "player" to sender.name.string
                            )
                        )
                    } else {
                        target.sendMessage(LangManager.msg("teleportFailed"))
                    }
                }

                sender.sendMessage(LangManager.msg("tpallDone"))
                1
            }
            dispatcher.register(tpallRoot)
        }
    }
}