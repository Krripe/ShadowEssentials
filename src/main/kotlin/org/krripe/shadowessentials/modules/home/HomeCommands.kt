package org.krripe.shadowessentials.modules.home

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.perms.PermissionsSystem

private val HOME_NAME_SUGGESTIONS: SuggestionProvider<ServerCommandSource> =
    SuggestionProvider { context, builder ->
        val player = context.source.player
        if (player != null) {
            val names = HomeManager.getHomes(player).keys
            for (name in names) {
                builder.suggest(name)
            }
        }
        builder.buildFuture()
    }

object HomeCommands {

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

            // /home [name]
            dispatcher.register(
                literal("home")
                    .requires { has(it, "shadowessentials.home") }
                    .executes { ctx ->
                        val player = requirePlayer(ctx.source) ?: return@executes 0
                        val ok = HomeManager.teleportToHome(player, null)
                        if (!ok) 0 else Command.SINGLE_SUCCESS
                    }
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(HOME_NAME_SUGGESTIONS)
                            .executes { ctx ->
                                val player = requirePlayer(ctx.source) ?: return@executes 0
                                val name = getString(ctx, "name")
                                val ok = HomeManager.teleportToHome(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /sethome [name]
            dispatcher.register(
                literal("sethome")
                    .requires { has(it, "shadowessentials.sethome") }
                    .executes { ctx ->
                        val player = requirePlayer(ctx.source) ?: return@executes 0
                        val ok = HomeManager.setHome(player, null)
                        if (!ok) 0 else Command.SINGLE_SUCCESS
                    }
                    .then(
                        argument("name", StringArgumentType.word())
                            .executes { ctx ->
                                val player = requirePlayer(ctx.source) ?: return@executes 0
                                val name = getString(ctx, "name")
                                val ok = HomeManager.setHome(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /delhome [name]
            dispatcher.register(
                literal("delhome")
                    .requires { has(it, "shadowessentials.delhome") }
                    .executes { ctx ->
                        val player = requirePlayer(ctx.source) ?: return@executes 0
                        val ok = HomeManager.deleteHome(player, null)
                        if (!ok) 0 else Command.SINGLE_SUCCESS
                    }
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(HOME_NAME_SUGGESTIONS)
                            .executes { ctx ->
                                val player = requirePlayer(ctx.source) ?: return@executes 0
                                val name = getString(ctx, "name")
                                val ok = HomeManager.deleteHome(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /deletehome [name]
            dispatcher.register(
                literal("deletehome")
                    .requires { has(it, "shadowessentials.delhome") }
                    .executes { ctx ->
                        val player = requirePlayer(ctx.source) ?: return@executes 0
                        val ok = HomeManager.deleteHome(player, null)
                        if (!ok) 0 else Command.SINGLE_SUCCESS
                    }
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(HOME_NAME_SUGGESTIONS)
                            .executes { ctx ->
                                val player = requirePlayer(ctx.source) ?: return@executes 0
                                val name = getString(ctx, "name")
                                val ok = HomeManager.deleteHome(player, name)
                                if (!ok) 0 else Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /homes
            dispatcher.register(
                literal("homes")
                    .requires { has(it, "shadowessentials.homes") }
                    .executes { ctx ->
                        val player = requirePlayer(ctx.source) ?: return@executes 0
                        HomeGUI.open(player)
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }
}
