package org.krripe.shadowessentials.modules.warp

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import org.krripe.shadowessentials.core.command.CommandUtil
import org.krripe.shadowessentials.core.command.Suggestions

object WarpCommands {

    private val WARP_NAME_SUGGESTIONS: SuggestionProvider<ServerCommandSource> =
        Suggestions.dynamic { _ -> WarpManager.all().map { it.id } }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            dispatcher.register(
                literal("warp")
                    .requires { CommandUtil.has(it, "shadowessentials.warp") }
                    .executes { ctx ->
                        val player = CommandUtil.playerOrNull(ctx.source) ?: return@executes 0
                        WarpGUI.open(player, 0)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(WARP_NAME_SUGGESTIONS)
                            .executes { ctx ->
                                val player = CommandUtil.requirePlayer(ctx.source)
                                val name = getString(ctx, "name")
                                val ok = WarpManager.teleport(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            dispatcher.register(
                literal("setwarp")
                    .requires { CommandUtil.has(it, "shadowessentials.setwarp") }
                    .then(
                        argument("name", StringArgumentType.word())
                            .executes { ctx ->
                                val player = CommandUtil.requirePlayer(ctx.source)
                                val name = getString(ctx, "name")
                                val ok = WarpManager.setWarp(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            dispatcher.register(
                literal("delwarp")
                    .requires { CommandUtil.has(it, "shadowessentials.delwarp") }
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(WARP_NAME_SUGGESTIONS)
                            .executes { ctx ->
                                val player = CommandUtil.requirePlayer(ctx.source)
                                val name = getString(ctx, "name")
                                val ok = WarpManager.deleteWarp(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            dispatcher.register(
                literal("warps")
                    .requires { CommandUtil.has(it, "shadowessentials.warps") }
                    .executes { ctx ->
                        val player = CommandUtil.requirePlayer(ctx.source)
                        WarpGUI.open(player, 0)
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }
}