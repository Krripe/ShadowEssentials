package org.krripe.shadowessentials.modules.invsee

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
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

private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
    return allowed.isEmpty() || worldId in allowed
}

object InvseeCommands {

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

            // /invsee <player> (read-only)
            dispatcher.register(
                literal("invsee")
                    .requires { has(it, "shadowessentials.invsee") }
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .executes { ctx ->
                                val viewer = requirePlayer(ctx.source) ?: return@executes 0
                                val cfg = ConfigManager.config.invsee
                                if (!cfg.enabled) {
                                    viewer.sendMessage(LangManager.msg("invseeDisabled"))
                                    return@executes 0
                                }

                                val target = EntityArgumentType.getPlayer(ctx, "player")

                                val allowed = cfg.allowWorlds
                                val vWorld = viewer.world.registryKey.value.toString()
                                val tWorld = target.world.registryKey.value.toString()
                                if (!isWorldAllowed(vWorld, allowed) || !isWorldAllowed(tWorld, allowed)) {
                                    viewer.sendMessage(LangManager.msg("invseeWorldNotAllowed"))
                                    return@executes 0
                                }

                                InvseeManager.open(viewer, target, editable = false)
                                1
                            }
                    )
            )

            // /invseeedit <player> (editable)
            dispatcher.register(
                literal("invseeedit")
                    .requires { has(it, "shadowessentials.invsee.edit") }
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .executes { ctx ->
                                val viewer = requirePlayer(ctx.source) ?: return@executes 0
                                val cfg = ConfigManager.config.invsee
                                if (!cfg.enabled) {
                                    viewer.sendMessage(LangManager.msg("invseeDisabled"))
                                    return@executes 0
                                }

                                val target = EntityArgumentType.getPlayer(ctx, "player")

                                val allowed = cfg.allowWorlds
                                val vWorld = viewer.world.registryKey.value.toString()
                                val tWorld = target.world.registryKey.value.toString()
                                if (!isWorldAllowed(vWorld, allowed) || !isWorldAllowed(tWorld, allowed)) {
                                    viewer.sendMessage(LangManager.msg("invseeWorldNotAllowed"))
                                    return@executes 0
                                }

                                InvseeManager.open(viewer, target, editable = true)
                                1
                            }
                    )
            )

            // /invseeoffline <name>  (offline inventory preview/edit)
            dispatcher.register(
                literal("invseeoffline")
                    .requires { has(it, "shadowessentials.invsee.offline") }
                    .then(
                        argument("player", StringArgumentType.word())
                            .executes { ctx ->
                                val viewer = requirePlayer(ctx.source) ?: return@executes 0
                                val cfg = ConfigManager.config.invsee
                                if (!cfg.enabled) {
                                    viewer.sendMessage(LangManager.msg("invseeDisabled"))
                                    return@executes 0
                                }

                                val worldId = viewer.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                                    viewer.sendMessage(LangManager.msg("invseeWorldNotAllowed"))
                                    return@executes 0
                                }

                                val targetName = getString(ctx, "player")

                                val ok = InvseeManager.openOffline(viewer, targetName)
                                if (!ok) {
                                    viewer.sendMessage(
                                        LangManager.msg(
                                            "invseeOfflineNoData",
                                            "player" to targetName
                                        )
                                    )
                                    return@executes 0
                                }

                                1
                            }
                    )
            )
        }
    }
}