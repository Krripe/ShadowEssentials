package org.krripe.shadowessentials.modules.blocks

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.*
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem

private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
    return allowed.isEmpty() || worldId in allowed
}

object BlocksCommands {

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

    private fun canUseBlockCommand(
        player: ServerPlayerEntity,
        enabled: Boolean,
        allowedWorlds: List<String>
    ): Boolean {
        if (!enabled) {
            player.sendMessage(LangManager.msg("commandDisabled"))
            return false
        }
        val worldId = player.world.registryKey.value.toString()
        if (!isWorldAllowed(worldId, allowedWorlds)) {
            player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
            return false
        }
        return true
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            fun registerEnder(label: String) {
                dispatcher.register(
                    literal(label)
                        .requires { has(it, "shadowessentials.enderchest") }
                        .executes { ctx ->
                            val src = ctx.source
                            val player = try {
                                requirePlayer(src)
                            } catch (_: IllegalStateException) {
                                return@executes 0
                            }

                            val cfg = ConfigManager.config.blocks
                            if (!cfg.enderchestEnabled) {
                                player.sendMessage(LangManager.msg("commandDisabled"))
                                return@executes 0
                            }

                            val worldId = player.world.registryKey.value.toString()
                            if (!isWorldAllowed(worldId, cfg.enderchestAllowWorlds)) {
                                player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                return@executes 0
                            }

                            player.openHandledScreen(
                                SimpleNamedScreenHandlerFactory(
                                    { syncId, inv, _ ->
                                        GenericContainerScreenHandler.createGeneric9x3(
                                            syncId,
                                            inv,
                                            player.enderChestInventory
                                        )
                                    },
                                    Text.translatable("container.enderchest")
                                )
                            )
                            player.sendMessage(LangManager.msg("enderOpened"))
                            1
                        }
                )
            }

            // /enderchest, /ec
            registerEnder("enderchest")
            registerEnder("ec")

            // /anvil
            dispatcher.register(
                literal("anvil")
                    .requires { has(it, "shadowessentials.anvil") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.anvilEnabled, cfg.anvilAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : AnvilScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.repair"))
                        )
                        1
                    }
            )

            // /cartography
            dispatcher.register(
                literal("cartography")
                    .requires { has(it, "shadowessentials.cartography") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.cartographyEnabled, cfg.cartographyAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : CartographyTableScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.cartography_table"))
                        )
                        1
                    }
            )

            // /stonecutter
            dispatcher.register(
                literal("stonecutter")
                    .requires { has(it, "shadowessentials.stonecutter") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.stonecutterEnabled, cfg.stonecutterAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : StonecutterScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.stonecutter"))
                        )
                        1
                    }
            )

            // /grindstone
            dispatcher.register(
                literal("grindstone")
                    .requires { has(it, "shadowessentials.grindstone") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.grindstoneEnabled, cfg.grindstoneAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : GrindstoneScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.grindstone_title"))
                        )
                        1
                    }
            )

            // /loom
            dispatcher.register(
                literal("loom")
                    .requires { has(it, "shadowessentials.loom") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.loomEnabled, cfg.loomAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : LoomScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.loom"))
                        )
                        1
                    }
            )

            // /craft
            dispatcher.register(
                literal("craft")
                    .requires { has(it, "shadowessentials.craft") }
                    .executes { ctx ->
                        val src = ctx.source
                        val player = try {
                            requirePlayer(src)
                        } catch (_: IllegalStateException) {
                            return@executes 0
                        }

                        val cfg = ConfigManager.config.blocks
                        if (!canUseBlockCommand(player, cfg.craftEnabled, cfg.craftAllowWorlds)) {
                            return@executes 0
                        }

                        player.openHandledScreen(
                            SimpleNamedScreenHandlerFactory({ id, inv, _ ->
                                object : CraftingScreenHandler(
                                    id,
                                    inv,
                                    ScreenHandlerContext.create(player.world, player.blockPos)
                                ) {
                                    override fun canUse(p: PlayerEntity) = true
                                }
                            }, Text.translatable("container.crafting"))
                        )
                        1
                    }
            )
        }
    }
}