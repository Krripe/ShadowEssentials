package org.krripe.shadowessentials.core.command

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowessentials.core.perms.PermissionsSystem

object CommandUtil {

    private val perms = PermissionsSystem()

    fun has(src: ServerCommandSource, node: String?): Boolean {
        if (node == null) return true
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    fun requirePlayer(src: ServerCommandSource): ServerPlayerEntity {
        return src.player ?: throw IllegalStateException("This command can only be used by players.")
    }

    fun playerOrNull(src: ServerCommandSource): ServerPlayerEntity? =
        src.player
}
