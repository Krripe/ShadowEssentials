package org.krripe.shadowessentials.modules.tpa

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.teleport.CooldownManager
import org.krripe.shadowessentials.core.teleport.TeleportDelayManager
import org.krripe.shadowessentials.core.teleport.TeleportUtils
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TpaManager {

    enum class Type {
        TO_TARGET,
        HERE
    }

    data class Request(
        val from: UUID,
        val to: UUID,
        val type: Type,
        val createdAt: Long
    )

    private val requests = ConcurrentHashMap<UUID, Request>()

    private fun timeoutSeconds(): Int =
        ConfigManager.config.tpa.requestTimeoutSeconds.coerceAtLeast(5)

    private fun isExpired(req: Request): Boolean {
        val timeout = timeoutSeconds()
        if (timeout <= 0) return false
        return System.currentTimeMillis() - req.createdAt > timeout * 1000L
    }

    private fun isWorldAllowed(worldId: String, allowed: List<String>): Boolean {
        return allowed.isEmpty() || worldId in allowed
    }

    private fun checkWorldsAllowed(a: ServerPlayerEntity, b: ServerPlayerEntity): Boolean {
        val cfg = ConfigManager.config.tpa
        val allowed = cfg.allowWorlds
        val w1 = a.world.registryKey.value.toString()
        val w2 = b.world.registryKey.value.toString()
        return isWorldAllowed(w1, allowed) && isWorldAllowed(w2, allowed)
    }

    private fun getRequestFor(target: ServerPlayerEntity): Request? {
        val r = requests[target.uuid] ?: return null
        if (isExpired(r)) {
            requests.remove(target.uuid)

            val server = target.server
            val from = server?.playerManager?.getPlayer(r.from)
            val fromName = from?.name?.string ?: "unknown"

            from?.sendMessage(
                LangManager.msg(
                    "tpaRequestExpiredSender",
                    "player" to target.name.string
                )
            )

            target.sendMessage(
                LangManager.msg(
                    "tpaRequestExpiredTarget",
                    "player" to fromName
                )
            )

            return null
        }
        return r
    }

    fun requestTo(sender: ServerPlayerEntity, target: ServerPlayerEntity) {
        val cfg = ConfigManager.config.tpa
        if (!cfg.enabled) {
            sender.sendMessage(LangManager.msg("tpaDisabled"))
            return
        }

        if (sender.uuid == target.uuid) {
            sender.sendMessage(LangManager.msg("tpaSelf"))
            return
        }

        if (!checkWorldsAllowed(sender, target)) {
            sender.sendMessage(LangManager.msg("tpaWorldNotAllowed"))
            return
        }

        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
            CooldownManager.isOnCooldown(sender, "tpa")
        ) {
            val left = CooldownManager.remaining(sender, "tpa")
            sender.sendMessage(LangManager.msg("tpaOnCooldown", "time" to left))
            return
        }

        val timeout = timeoutSeconds()
        requests[target.uuid] = Request(
            from = sender.uuid,
            to = target.uuid,
            type = Type.TO_TARGET,
            createdAt = System.currentTimeMillis()
        )

        sender.sendMessage(
            LangManager.msg(
                "tpaRequestSent",
                "target" to target.name.string,
                "time" to timeout
            )
        )
        target.sendMessage(
            LangManager.msg(
                "tpaRequestReceived",
                "player" to sender.name.string,
                "time" to timeout
            )
        )
    }

    fun requestHere(sender: ServerPlayerEntity, target: ServerPlayerEntity) {
        val cfg = ConfigManager.config.tpa
        if (!cfg.enabled) {
            sender.sendMessage(LangManager.msg("tpaDisabled"))
            return
        }

        if (sender.uuid == target.uuid) {
            sender.sendMessage(LangManager.msg("tpaSelf"))
            return
        }

        if (!checkWorldsAllowed(sender, target)) {
            sender.sendMessage(LangManager.msg("tpaWorldNotAllowed"))
            return
        }

        if (cfg.cooldownEnabled && cfg.cooldownSeconds > 0 &&
            CooldownManager.isOnCooldown(sender, "tpa")
        ) {
            val left = CooldownManager.remaining(sender, "tpa")
            sender.sendMessage(LangManager.msg("tpaOnCooldown", "time" to left))
            return
        }

        val timeout = timeoutSeconds()
        requests[target.uuid] = Request(
            from = sender.uuid,
            to = target.uuid,
            type = Type.HERE,
            createdAt = System.currentTimeMillis()
        )

        sender.sendMessage(
            LangManager.msg(
                "tpaHereRequestSent",
                "target" to target.name.string,
                "time" to timeout
            )
        )
        target.sendMessage(
            LangManager.msg(
                "tpaHereRequestReceived",
                "player" to sender.name.string,
                "time" to timeout
            )
        )
    }

    fun accept(target: ServerPlayerEntity) {
        val req = getRequestFor(target)
        if (req == null) {
            target.sendMessage(LangManager.msg("tpaNoRequest"))
            return
        }

        requests.remove(target.uuid)

        val server = target.server ?: return
        val from = server.playerManager.getPlayer(req.from)
        val to = server.playerManager.getPlayer(req.to)

        if (from == null || to == null) {
            target.sendMessage(LangManager.msg("tpaNoRequest"))
            return
        }

        val cfg = ConfigManager.config
        val tpaCfg = cfg.tpa
        val teleCfg = cfg.teleport

        val (teleporter, dest) = when (req.type) {
            Type.TO_TARGET -> from to to
            Type.HERE      -> to   to from
        }

        if (!checkWorldsAllowed(teleporter, dest)) {
            teleporter.sendMessage(LangManager.msg("tpaWorldNotAllowed"))
            return
        }

        val sound = teleCfg.sound
        val particles = teleCfg.particles
        val destWorld = dest.world as ServerWorld
        val destPos = dest.pos
        val destYaw = dest.yaw
        val destPitch = dest.pitch

        val successMsgKey =
            if (req.type == Type.TO_TARGET) "tpaTeleportSuccess" else "tpaTeleportHereSuccess"

        fun applyCooldown() {
            if (tpaCfg.cooldownEnabled && tpaCfg.cooldownSeconds > 0) {
                CooldownManager.set(teleporter, "tpa", tpaCfg.cooldownSeconds)
            }
        }

        target.sendMessage(
            LangManager.msg("tpaAcceptedTarget", "player" to from.name.string)
        )
        from.sendMessage(
            LangManager.msg("tpaAcceptedSender")
        )

        val useDelay = tpaCfg.delayEnabled && tpaCfg.delaySeconds > 0
        val sec = tpaCfg.delaySeconds

        if (useDelay) {
            teleporter.sendMessage(
                LangManager.msg("tpDelayStart", "time" to sec)
            )

            TeleportDelayManager.start(
                teleporter,
                sec
            ) { p ->
                val ok = TeleportUtils.teleportPlayer(
                    p,
                    destWorld,
                    destPos,
                    destYaw,
                    destPitch,
                    sound,
                    particles
                )
                if (ok) {
                    p.sendMessage(
                        LangManager.msg(
                            successMsgKey,
                            "player" to dest.name.string
                        )
                    )
                    applyCooldown()
                } else {
                    p.sendMessage(LangManager.msg("teleportFailed"))
                }
            }
        } else {
            val ok = TeleportUtils.teleportPlayer(
                teleporter,
                destWorld,
                destPos,
                destYaw,
                destPitch,
                sound,
                particles
            )
            if (ok) {
                teleporter.sendMessage(
                    LangManager.msg(
                        successMsgKey,
                        "player" to dest.name.string
                    )
                )
                applyCooldown()
            } else {
                teleporter.sendMessage(LangManager.msg("teleportFailed"))
            }
        }
    }

    fun deny(target: ServerPlayerEntity) {
        val req = getRequestFor(target)
        if (req == null) {
            target.sendMessage(LangManager.msg("tpaNoRequest"))
            return
        }

        requests.remove(target.uuid)

        val server = target.server ?: return
        val from = server.playerManager.getPlayer(req.from)

        from?.sendMessage(
            LangManager.msg(
                "tpaDeniedSender",
                "player" to target.name.string
            )
        )

        target.sendMessage(
            LangManager.msg(
                "tpaDeniedTarget",
                "player" to (from?.name?.string ?: "unknown")
            )
        )
    }

    fun cancel(sender: ServerPlayerEntity) {
        val req = requests.values.firstOrNull { it.from == sender.uuid }
        if (req == null) {
            sender.sendMessage(LangManager.msg("tpaCancelNoRequest"))
            return
        }

        val server = sender.server

        if (isExpired(req)) {
            requests.remove(req.to)

            val target = server?.playerManager?.getPlayer(req.to)
            val targetName = target?.name?.string ?: "unknown"

            sender.sendMessage(
                LangManager.msg(
                    "tpaRequestExpiredSender",
                    "player" to targetName
                )
            )

            target?.sendMessage(
                LangManager.msg(
                    "tpaRequestExpiredTarget",
                    "player" to sender.name.string
                )
            )
            return
        }

        requests.remove(req.to)

        val target = server?.playerManager?.getPlayer(req.to)

        sender.sendMessage(
            LangManager.msg(
                "tpaCancelSuccessSender",
                "target" to (target?.name?.string ?: "unknown")
            )
        )

        target?.sendMessage(
            LangManager.msg(
                "tpaCancelSuccessTarget",
                "player" to sender.name.string
            )
        )
    }
}