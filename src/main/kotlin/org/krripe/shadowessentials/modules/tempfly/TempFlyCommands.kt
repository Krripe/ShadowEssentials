package org.krripe.shadowessentials.modules.tempfly

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
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

private fun parseTimeToSeconds(raw: String): Long {
    val s = raw.trim().lowercase()
    if (s.isEmpty()) return 0

    val regex = Regex("(\\d+)([smhd])")
    var total = 0L
    for (m in regex.findAll(s)) {
        val value = m.groupValues[1].toLongOrNull() ?: continue
        val unit = m.groupValues[2]
        val part = when (unit) {
            "s" -> value
            "m" -> value * 60
            "h" -> value * 3600
            "d" -> value * 86400
            else -> 0L
        }
        total += part
    }

    if (total == 0L) {
        return s.toLongOrNull() ?: 0
    }
    return total
}

private fun fmt(seconds: Long) = TempFlyManager.formatSeconds(seconds)

object TempFlyCommands {

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

            val root = literal("tempfly")

            // /tempfly  (toggle self)
            root.executes { ctx ->
                val src = ctx.source

                if (!has(src, "shadowessentials.tempfly")) {
                    src.sendMessage(
                        LangManager.msg(
                            "noPermission",
                            "perm" to "shadowessentials.tempfly"
                        )
                    )
                    return@executes 0
                }

                val player = requirePlayer(src)
                val cfg = ConfigManager.config.tempfly

                if (!cfg.enabled) {
                    player.sendMessage(LangManager.msg("tempflyNotAllowed"))
                    return@executes 0
                }

                val worldId = player.world.registryKey.value.toString()
                if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                    player.sendMessage(LangManager.msg("tempflyWorldNotAllowed"))
                    return@executes 0
                }

                if (TempFlyManager.isActive(player)) {
                    val ok = TempFlyManager.setActive(player, false)
                    if (ok) player.sendMessage(LangManager.msg("tempflyDisabled"))
                    return@executes if (ok) 1 else 0
                }

                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                    CooldownManager.isOnCooldown(player, "tempfly")
                ) {
                    val leftCd = CooldownManager.remaining(player, "tempfly")
                    player.sendMessage(
                        LangManager.msg("tempflyCooldown", "time" to fmt(leftCd))
                    )
                    return@executes 0
                }

                var left = TempFlyManager.remaining(player)
                if (left <= 0) {
                    val added = TempFlyManager.claimDailyIfAvailable(player)
                    if (added > 0) {
                        left = TempFlyManager.remaining(player)
                    }
                }

                if (left <= 0) {
                    player.sendMessage(LangManager.msg("tempflyNoTime"))
                    return@executes 0
                }

                val success = TempFlyManager.setActive(player, true)
                if (!success) {
                    player.sendMessage(LangManager.msg("tempflyNoTime"))
                    return@executes 0
                }

                if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                    CooldownManager.set(player, "tempfly", cfg.cooldownSeconds)
                }

                player.sendMessage(LangManager.msg("tempflyEnabled"))
                1
            }

            // /tempfly on
            root.then(
                literal("on")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.tempfly.on")) {
                            src.sendMessage(
                                LangManager.msg(
                                    "noPermission",
                                    "perm" to "shadowessentials.tempfly.on"
                                )
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)
                        val cfg = ConfigManager.config.tempfly

                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("tempflyNotAllowed"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("tempflyWorldNotAllowed"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
                            CooldownManager.isOnCooldown(player, "tempfly")
                        ) {
                            val leftCd = CooldownManager.remaining(player, "tempfly")
                            player.sendMessage(
                                LangManager.msg(
                                    "tempflyCooldown",
                                    "time" to fmt(leftCd)
                                )
                            )
                            return@executes 0
                        }

                        var left = TempFlyManager.remaining(player)
                        if (left <= 0) {
                            val added = TempFlyManager.claimDailyIfAvailable(player)
                            if (added > 0) {
                                left = TempFlyManager.remaining(player)
                            }
                        }

                        if (left <= 0) {
                            player.sendMessage(LangManager.msg("tempflyNoTime"))
                            return@executes 0
                        }

                        val success = TempFlyManager.setActive(player, true)
                        if (!success) {
                            player.sendMessage(LangManager.msg("tempflyNoTime"))
                            return@executes 0
                        }

                        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0) {
                            CooldownManager.set(player, "tempfly", cfg.cooldownSeconds)
                        }

                        player.sendMessage(LangManager.msg("tempflyEnabled"))
                        1
                    }
            )

            // /tempfly off
            root.then(
                literal("off")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.tempfly.off")) {
                            src.sendMessage(
                                LangManager.msg(
                                    "noPermission",
                                    "perm" to "shadowessentials.tempfly.off"
                                )
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)

                        val success = TempFlyManager.setActive(player, false)
                        if (!success) {
                            player.sendMessage(LangManager.msg("tempflyNoTime"))
                            return@executes 0
                        }

                        player.sendMessage(LangManager.msg("tempflyDisabled"))
                        1
                    }
            )

            // /tempfly give <player> <time>
            root.then(
                literal("give")
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .then(
                                argument("time", StringArgumentType.word())
                                    .executes { ctx ->
                                        val src = ctx.source
                                        val cfg = ConfigManager.config.tempfly

                                        if (!cfg.enabled) {
                                            src.sendMessage(LangManager.msg("tempflyNotAllowed"))
                                            return@executes 0
                                        }

                                        if (!has(src, "shadowessentials.tempfly.give")) {
                                            src.sendMessage(
                                                LangManager.msg(
                                                    "noPermission",
                                                    "perm" to "shadowessentials.tempfly.give"
                                                )
                                            )
                                            return@executes 0
                                        }

                                        val target = EntityArgumentType.getPlayer(ctx, "player")
                                        val timeStr = StringArgumentType.getString(ctx, "time")
                                        val sec = parseTimeToSeconds(timeStr)
                                        if (sec <= 0) {
                                            src.sendMessage(LangManager.msg("tempflyInvalidTime"))
                                            return@executes 0
                                        }

                                        val newTotal = TempFlyManager.give(target, sec)
                                        src.sendMessage(
                                            LangManager.msg(
                                                "tempflyGivenOther",
                                                "player" to target.name.string,
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        target.sendMessage(
                                            LangManager.msg(
                                                "tempflyGiven",
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        1
                                    }
                            )
                    )
            )

            // /tempfly set <player> <time>
            root.then(
                literal("set")
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .then(
                                argument("time", StringArgumentType.word())
                                    .executes { ctx ->
                                        val src = ctx.source

                                        if (!has(src, "shadowessentials.tempfly.set")) {
                                            src.sendMessage(
                                                LangManager.msg(
                                                    "noPermission",
                                                    "perm" to "shadowessentials.tempfly.set"
                                                )
                                            )
                                            return@executes 0
                                        }

                                        val target = EntityArgumentType.getPlayer(ctx, "player")
                                        val timeStr = StringArgumentType.getString(ctx, "time")
                                        val sec = parseTimeToSeconds(timeStr)
                                        if (sec < 0) {
                                            src.sendMessage(LangManager.msg("tempflyInvalidTime"))
                                            return@executes 0
                                        }

                                        val newTotal = TempFlyManager.setTime(target, sec)
                                        src.sendMessage(
                                            LangManager.msg(
                                                "tempflySetOther",
                                                "player" to target.name.string,
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        target.sendMessage(
                                            LangManager.msg(
                                                "tempflySet",
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        1
                                    }
                            )
                    )
            )

            // /tempfly extract <player> <time>
            root.then(
                literal("extract")
                    .then(
                        argument("player", EntityArgumentType.player())
                            .suggests(Suggestions.onlinePlayers)
                            .then(
                                argument("time", StringArgumentType.word())
                                    .executes { ctx ->
                                        val src = ctx.source

                                        if (!has(src, "shadowessentials.tempfly.extract")) {
                                            src.sendMessage(
                                                LangManager.msg(
                                                    "noPermission",
                                                    "perm" to "shadowessentials.tempfly.extract"
                                                )
                                            )
                                            return@executes 0
                                        }

                                        val target = EntityArgumentType.getPlayer(ctx, "player")
                                        val timeStr = StringArgumentType.getString(ctx, "time")
                                        val sec = parseTimeToSeconds(timeStr)
                                        if (sec <= 0) {
                                            src.sendMessage(LangManager.msg("tempflyInvalidTime"))
                                            return@executes 0
                                        }

                                        val newTotal = TempFlyManager.extractTime(target, sec)
                                        src.sendMessage(
                                            LangManager.msg(
                                                "tempflyExtractedOther",
                                                "player" to target.name.string,
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        target.sendMessage(
                                            LangManager.msg(
                                                "tempflyExtracted",
                                                "time" to fmt(newTotal)
                                            )
                                        )
                                        1
                                    }
                            )
                    )
            )

            // /tempfly left
            root.then(
                literal("left")
                    .executes { ctx ->
                        val src = ctx.source

                        if (!has(src, "shadowessentials.tempfly.left")) {
                            src.sendMessage(
                                LangManager.msg(
                                    "noPermission",
                                    "perm" to "shadowessentials.tempfly.left"
                                )
                            )
                            return@executes 0
                        }

                        val player = requirePlayer(src)

                        val cfg = ConfigManager.config.tempfly
                        if (!cfg.enabled) {
                            player.sendMessage(LangManager.msg("tempflyNotAllowed"))
                            return@executes 0
                        }

                        val worldId = player.world.registryKey.value.toString()
                        if (!isWorldAllowed(worldId, cfg.allowWorlds)) {
                            player.sendMessage(LangManager.msg("tempflyWorldNotAllowed"))
                            return@executes 0
                        }

                        val left = TempFlyManager.remaining(player)
                        val formatted = TempFlyManager.formatSeconds(left)

                        player.sendMessage(
                            LangManager.msg("tempflyLeft", "time" to formatted)
                        )
                        1
                    }
            )

            dispatcher.register(root)
        }
    }
}