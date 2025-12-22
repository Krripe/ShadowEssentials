package org.krripe.shadowessentials.modules.gamemode

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import org.krripe.shadowessentials.core.command.Suggestions
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem

private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
    return allowed.isEmpty() || worldId in allowed
}

private fun parseMode(input: String): GameMode {
    return when (input.lowercase()) {
        "0", "s", "survival"   -> GameMode.SURVIVAL
        "1", "c", "creative"   -> GameMode.CREATIVE
        "2", "a", "adventure"  -> GameMode.ADVENTURE
        "3", "sp", "spectator" -> GameMode.SPECTATOR
        else                   -> GameMode.SURVIVAL
    }
}

private fun changeGamemode(
    source: ServerPlayerEntity,
    target: ServerPlayerEntity,
    mode: GameMode
): Int {
    target.changeGameMode(mode)
    val modeName = mode.name.lowercase()

    if (source.uuid == target.uuid) {
        source.sendMessage(
            LangManager.msg("gmChangeSuccessSelf", "mode" to modeName)
        )
    } else {
        source.sendMessage(
            LangManager.msg(
                "gmChangeSuccessOther",
                "mode" to modeName,
                "target" to target.name.string
            )
        )
        target.sendMessage(
            LangManager.msg("gmChangeSuccessSelf", "mode" to modeName)
        )
    }
    return Command.SINGLE_SUCCESS
}

object GamemodeCommands {

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

            fun registerGamemodeRoot(name: String) {
                dispatcher.register(
                    literal(name)
                        // /gamemode <mode>
                        .then(
                            argument("mode", StringArgumentType.word())
                                .executes { ctx ->
                                    val src = ctx.source

                                    if (!has(src, "shadowessentials.gamemode")) {
                                        src.sendMessage(
                                            LangManager.msg(
                                                "noPermission",
                                                "perm" to "shadowessentials.gamemode"
                                            )
                                        )
                                        return@executes 0
                                    }

                                    val player = requirePlayer(src) ?: return@executes 0
                                    val cfg = ConfigManager.config.gamemode
                                    if (!cfg.enabled) {
                                        player.sendMessage(LangManager.msg("gamemodeDisabled"))
                                        return@executes 0
                                    }

                                    val worldId = player.world.registryKey.value.toString()
                                    if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                                        player.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                                        return@executes 0
                                    }

                                    val modeArg = StringArgumentType.getString(ctx, "mode")
                                    val mode = parseMode(modeArg)

                                    changeGamemode(player, player, mode)
                                }
                                // /gamemode <mode> <target>
                                .then(
                                    argument("target", EntityArgumentType.player())
                                        .suggests(Suggestions.onlinePlayers)
                                        .executes { ctx ->
                                            val src = ctx.source

                                            if (!has(src, "shadowessentials.gamemode.others")) {
                                                src.sendMessage(
                                                    LangManager.msg(
                                                        "noPermission",
                                                        "perm" to "shadowessentials.gamemode.others"
                                                    )
                                                )
                                                return@executes 0
                                            }

                                            val sender = requirePlayer(src) ?: return@executes 0
                                            val cfg = ConfigManager.config.gamemode
                                            if (!cfg.enabled) {
                                                sender.sendMessage(LangManager.msg("gamemodeDisabled"))
                                                return@executes 0
                                            }

                                            val modeArg = StringArgumentType.getString(ctx, "mode")
                                            val mode = parseMode(modeArg)
                                            val target = EntityArgumentType.getPlayer(ctx, "target")

                                            val allowed = cfg.allowWorlds
                                            val senderWorldId = sender.world.registryKey.value.toString()
                                            val targetWorldId = target.world.registryKey.value.toString()
                                            if (!isWorldAllowed(senderWorldId, allowed) ||
                                                !isWorldAllowed(targetWorldId, allowed)
                                            ) {
                                                sender.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                                                return@executes 0
                                            }

                                            changeGamemode(sender, target, mode)
                                        }
                                )
                        )
                )
            }

            // /gamemode and /gm
            registerGamemodeRoot("gamemode")
            registerGamemodeRoot("gm")

            // /gmc
            dispatcher.register(
                literal("gmc")
                    .requires { has(it, "shadowessentials.gmc") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = requirePlayer(src) ?: return@executes 0

                        val cfg = ConfigManager.config.gamemode
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("gamemodeDisabled"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                            return@executes 0
                        }

                        changeGamemode(player, player, GameMode.CREATIVE)
                    }
            )

            // /gms
            dispatcher.register(
                literal("gms")
                    .requires { has(it, "shadowessentials.gms") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = requirePlayer(src) ?: return@executes 0

                        val cfg = ConfigManager.config.gamemode
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("gamemodeDisabled"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                            return@executes 0
                        }

                        changeGamemode(player, player, GameMode.SURVIVAL)
                    }
            )

            // /gma
            dispatcher.register(
                literal("gma")
                    .requires { has(it, "shadowessentials.gma") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = requirePlayer(src) ?: return@executes 0

                        val cfg = ConfigManager.config.gamemode
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("gamemodeDisabled"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                            return@executes 0
                        }

                        changeGamemode(player, player, GameMode.ADVENTURE)
                    }
            )

            // /gmsp
            dispatcher.register(
                literal("gmsp")
                    .requires { has(it, "shadowessentials.gmsp") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = requirePlayer(src) ?: return@executes 0

                        val cfg = ConfigManager.config.gamemode
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("gamemodeDisabled"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("gamemodeWorldNotAllowed"))
                            return@executes 0
                        }

                        changeGamemode(player, player, GameMode.SPECTATOR)
                    }
            )
        }
    }
}