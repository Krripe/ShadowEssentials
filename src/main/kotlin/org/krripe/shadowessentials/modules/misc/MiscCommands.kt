package org.krripe.shadowessentials.modules.misc

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.EntityArgumentType.player
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.IdentifierArgumentType.identifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.registry.RegistryKeys
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

object MiscCommands {

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

    private val ENCH_SUGGEST: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, b ->
            val reg = ctx.source.server.registryManager.get(RegistryKeys.ENCHANTMENT)
            reg.forEach { ench -> b.suggest(reg.getId(ench).toString()) }
            b.buildFuture()
        }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            // /enchant set <enchant> <level>
            dispatcher.register(
                literal("enchant")
                    .then(
                        literal("set")
                            .then(
                                argument("enchant", identifier())
                                    .suggests(ENCH_SUGGEST)
                                    .then(
                                        argument("level", IntegerArgumentType.integer(1))
                                            .executes { ctx ->
                                                val src = ctx.source

                                                if (!has(src, "shadowessentials.enchant")) {
                                                    src.sendMessage(
                                                        LangManager.msg(
                                                            "noPermission",
                                                            "perm" to "shadowessentials.enchant"
                                                        )
                                                    )
                                                    return@executes 0
                                                }

                                                val player = requirePlayer(src)
                                                val cfg = ConfigManager.config.misc
                                                if (!cfg.enchantEnabled) {
                                                    player.sendMessage(LangManager.msg("commandDisabled"))
                                                    return@executes 0
                                                }

                                                val worldId = player.world.registryKey.value.toString()
                                                if (!isWorldAllowed(worldId, cfg.enchantAllowWorlds)) {
                                                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                                    return@executes 0
                                                }

                                                val id = IdentifierArgumentType.getIdentifier(ctx, "enchant")
                                                val lvl = IntegerArgumentType.getInteger(ctx, "level")

                                                val msg = MiscManager.setEnchantInHand(player, id, lvl)
                                                player.sendMessage(msg)
                                                1
                                            }
                                    )
                            )
                    )
            )

            // /speed

            dispatcher.register(
                literal("speed")
                    .then(
                        literal("walk")
                            .then(
                                argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                                    .executes { ctx ->
                                        val src = ctx.source

                                        if (!has(src, "shadowessentials.speed")) {
                                            src.sendMessage(
                                                LangManager.msg(
                                                    "noPermission",
                                                    "perm" to "shadowessentials.speed"
                                                )
                                            )
                                            return@executes 0
                                        }

                                        val player = requirePlayer(src)
                                        val cfg = ConfigManager.config.misc
                                        if (!cfg.speedEnabled) {
                                            player.sendMessage(LangManager.msg("commandDisabled"))
                                            return@executes 0
                                        }

                                        val worldId = player.world.registryKey.value.toString()
                                        if (!isWorldAllowed(worldId, cfg.speedAllowWorlds)) {
                                            player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                            return@executes 0
                                        }

                                        val v = FloatArgumentType.getFloat(ctx, "value").coerceIn(0.1f, 10f)
                                        player.abilities.walkSpeed = 0.1f * v
                                        player.sendAbilitiesUpdate()
                                        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.baseValue =
                                            0.1 * v
                                        player.sendMessage(
                                            LangManager.msg("speedWalk", "value" to v.toString())
                                        )
                                        1
                                    }
                            )
                    )
                    .then(
                        literal("fly")
                            .then(
                                argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                                    .executes { ctx ->
                                        val src = ctx.source

                                        if (!has(src, "shadowessentials.speed")) {
                                            src.sendMessage(
                                                LangManager.msg(
                                                    "noPermission",
                                                    "perm" to "shadowessentials.speed"
                                                )
                                            )
                                            return@executes 0
                                        }

                                        val player = requirePlayer(src)
                                        val cfg = ConfigManager.config.misc
                                        if (!cfg.speedEnabled) {
                                            player.sendMessage(LangManager.msg("commandDisabled"))
                                            return@executes 0
                                        }

                                        val worldId = player.world.registryKey.value.toString()
                                        if (!isWorldAllowed(worldId, cfg.speedAllowWorlds)) {
                                            player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                            return@executes 0
                                        }

                                        val v = FloatArgumentType.getFloat(ctx, "value").coerceIn(0.1f, 10f)
                                        player.abilities.flySpeed = 0.05f * v
                                        player.sendAbilitiesUpdate()
                                        player.sendMessage(
                                            LangManager.msg("speedFly", "value" to v.toString())
                                        )
                                        1
                                    }
                            )
                    )
                    .then(
                        literal("reset")
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.speed")) {
                                    src.sendMessage(
                                        LangManager.msg(
                                            "noPermission",
                                            "perm" to "shadowessentials.speed"
                                        )
                                    )
                                    return@executes 0
                                }

                                val player = requirePlayer(src)
                                val cfg = ConfigManager.config.misc
                                if (!cfg.speedEnabled) {
                                    player.sendMessage(LangManager.msg("commandDisabled"))
                                    return@executes 0
                                }

                                val worldId = player.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.speedAllowWorlds)) {
                                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                    return@executes 0
                                }

                                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1
                                player.abilities.walkSpeed = 0.1f
                                player.abilities.flySpeed = 0.05f
                                player.sendAbilitiesUpdate()
                                player.sendMessage(LangManager.msg("speedReset"))
                                1
                            }
                    )
            )

            // /repair

            fun canUseRepairHere(player: ServerPlayerEntity): Boolean {
                val cfg = ConfigManager.config.misc
                if (!cfg.repairEnabled) {
                    player.sendMessage(LangManager.msg("commandDisabled"))
                    return false
                }
                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.repairAllowWorlds)) {
                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                    return false
                }
                return true
            }

            dispatcher.register(
                literal("repair")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.repair")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.repair")
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)
                        if (!canUseRepairHere(player)) return@executes 0

                        val stack = player.mainHandStack
                        if (stack.isEmpty || !stack.isDamageable) {
                            player.sendMessage(LangManager.msg("repairNoItem"))
                            return@executes 0
                        }
                        stack.damage = 0
                        player.sendMessage(LangManager.msg("repairDone"))
                        1
                    }
                    .then(
                        literal("all")
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.repair.all")) {
                                    src.sendMessage(
                                        LangManager.msg(
                                            "noPermission",
                                            "perm" to "shadowessentials.repair.all"
                                        )
                                    )
                                    return@executes 0
                                }

                                val player = requirePlayer(src)
                                if (!canUseRepairHere(player)) return@executes 0

                                var fixed = 0
                                player.inventory.main.forEach { s ->
                                    if (!s.isEmpty && s.isDamageable) {
                                        s.damage = 0; fixed++
                                    }
                                }
                                player.inventory.armor.forEach { s ->
                                    if (!s.isEmpty && s.isDamageable) {
                                        s.damage = 0; fixed++
                                    }
                                }
                                player.inventory.offHand.forEach { s ->
                                    if (!s.isEmpty && s.isDamageable) {
                                        s.damage = 0; fixed++
                                    }
                                }
                                player.sendMessage(
                                    LangManager.msg("repairAllDone", "count" to fixed.toString())
                                )
                                1
                            }
                    )
            )

            // /heal

            fun canUseHealHere(player: ServerPlayerEntity): Boolean {
                val cfg = ConfigManager.config.misc
                if (!cfg.healEnabled) {
                    player.sendMessage(LangManager.msg("commandDisabled"))
                    return false
                }
                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.healAllowWorlds)) {
                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                    return false
                }
                return true
            }

            dispatcher.register(
                literal("heal")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.heal")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.heal")
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)
                        if (!canUseHealHere(player)) return@executes 0

                        MiscManager.heal(player)
                        player.sendMessage(LangManager.msg("healSelf"))
                        1
                    }
                    .then(
                        argument("player", player())
                            .suggests(Suggestions.onlinePlayers)
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.heal.others")) {
                                    src.sendMessage(
                                        LangManager.msg(
                                            "noPermission",
                                            "perm" to "shadowessentials.heal.others"
                                        )
                                    )
                                    return@executes 0
                                }

                                val executor = requirePlayer(src)
                                if (!canUseHealHere(executor)) return@executes 0

                                val target = EntityArgumentType.getPlayer(ctx, "player")
                                MiscManager.heal(target)

                                executor.sendMessage(
                                    LangManager.msg("healOther", "player" to target.name.string)
                                )
                                target.sendMessage(LangManager.msg("healSelf"))
                                1
                            }
                    )
            )

            // /feed

            fun canUseFeedHere(player: ServerPlayerEntity): Boolean {
                val cfg = ConfigManager.config.misc
                if (!cfg.feedEnabled) {
                    player.sendMessage(LangManager.msg("commandDisabled"))
                    return false
                }
                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.feedAllowWorlds)) {
                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                    return false
                }
                return true
            }

            dispatcher.register(
                literal("feed")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.feed")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.feed")
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)
                        if (!canUseFeedHere(player)) return@executes 0

                        MiscManager.feed(player)
                        player.sendMessage(LangManager.msg("feedSelf"))
                        1
                    }
                    .then(
                        argument("player", player())
                            .suggests(Suggestions.onlinePlayers)
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.feed.others")) {
                                    src.sendMessage(
                                        LangManager.msg(
                                            "noPermission",
                                            "perm" to "shadowessentials.feed.others"
                                        )
                                    )
                                    return@executes 0
                                }

                                val executor = requirePlayer(src)
                                if (!canUseFeedHere(executor)) return@executes 0

                                val target = EntityArgumentType.getPlayer(ctx, "player")
                                MiscManager.feed(target)

                                executor.sendMessage(
                                    LangManager.msg("feedOther", "player" to target.name.string)
                                )
                                target.sendMessage(LangManager.msg("feedSelf"))
                                1
                            }
                    )
            )

            // /clear & /clearinventory

            fun canUseClearHere(player: ServerPlayerEntity): Boolean {
                val cfg = ConfigManager.config.misc
                if (!cfg.clearEnabled) {
                    player.sendMessage(LangManager.msg("commandDisabled"))
                    return false
                }
                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.clearAllowWorlds)) {
                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                    return false
                }
                return true
            }

            val clearRoots = listOf("clear", "clearinventory")

            for (label in clearRoots) {
                dispatcher.register(
                    literal(label)
                        .executes { ctx ->
                            val src = ctx.source

                            if (!has(src, "shadowessentials.clear")) {
                                src.sendMessage(
                                    LangManager.msg("noPermission", "perm" to "shadowessentials.clear")
                                )
                                return@executes 0
                            }

                            val player = requirePlayer(src)
                            if (!canUseClearHere(player)) return@executes 0

                            val c = MiscManager.clearInventory(player)
                            player.sendMessage(
                                LangManager.msg("clearSelf", "count" to c.toString())
                            )
                            1
                        }
                        .then(
                            argument("player", player())
                                .suggests(Suggestions.onlinePlayers)
                                .executes { ctx ->
                                    val src = ctx.source

                                    if (!has(src, "shadowessentials.clear.others")) {
                                        src.sendMessage(
                                            LangManager.msg(
                                                "noPermission",
                                                "perm" to "shadowessentials.clear.others"
                                            )
                                        )
                                        return@executes 0
                                    }

                                    val executor = requirePlayer(src)
                                    if (!canUseClearHere(executor)) return@executes 0

                                    val target = EntityArgumentType.getPlayer(ctx, "player")
                                    val c = MiscManager.clearInventory(target)

                                    executor.sendMessage(
                                        LangManager.msg(
                                            "clearOther",
                                            "player" to target.name.string,
                                            "count" to c.toString()
                                        )
                                    )
                                    1
                                }
                        )
                )
            }

            // /near

            fun canUseNearHere(player: ServerPlayerEntity): Boolean {
                val cfg = ConfigManager.config.misc
                if (!cfg.nearEnabled) {
                    player.sendMessage(LangManager.msg("commandDisabled"))
                    return false
                }
                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.nearAllowWorlds)) {
                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                    return false
                }
                return true
            }

            dispatcher.register(
                literal("near")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.near")) {
                            src.sendMessage(
                                LangManager.msg("noPermission", "perm" to "shadowessentials.near")
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)
                        if (!canUseNearHere(player)) return@executes 0

                        player.sendMessage(MiscManager.nearList(player, 50.0))
                        1
                    }
                    .then(
                        argument("radius", DoubleArgumentType.doubleArg(1.0, 512.0))
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.near")) {
                                    src.sendMessage(
                                        LangManager.msg("noPermission", "perm" to "shadowessentials.near")
                                    )
                                    return@executes 0
                                }

                                val player = requirePlayer(src)
                                if (!canUseNearHere(player)) return@executes 0

                                val r = DoubleArgumentType.getDouble(ctx, "radius")
                                player.sendMessage(MiscManager.nearList(player, r))
                                1
                            }
                    )
            )

            // /seen

            dispatcher.register(
                literal("seen")
                    .then(
                        argument("player", StringArgumentType.word())
                            .executes { ctx ->
                                val src = ctx.source

                                if (!has(src, "shadowessentials.seen")) {
                                    src.sendMessage(
                                        LangManager.msg("noPermission", "perm" to "shadowessentials.seen")
                                    )
                                    return@executes 0
                                }

                                val player = requirePlayer(src)
                                val cfg = ConfigManager.config.misc
                                if (!cfg.seenEnabled) {
                                    player.sendMessage(LangManager.msg("commandDisabled"))
                                    return@executes 0
                                }

                                val worldId = player.world.registryKey.value.toString()
                                if (!isWorldAllowed(worldId, cfg.seenAllowWorlds)) {
                                    player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
                                    return@executes 0
                                }

                                val targetName = StringArgumentType.getString(ctx, "player")
                                val msg = SeenManager.formatSeen(player, targetName)
                                player.sendMessage(msg)
                                1
                            }
                    )
            )
        }
    }
}