package org.krripe.shadowessentials.modules.misc

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.modules.home.HomeGUI
import org.krripe.shadowessentials.modules.warp.WarpEditorGUI
import org.krripe.shadowessentials.modules.warp.WarpGUI

object CoreCommands {

    private val perms = PermissionsSystem()

    private fun has(src: ServerCommandSource, node: String): Boolean {
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val roots = listOf("sessentials", "se", "ses")

            for (label in roots) {
                dispatcher.register(
                    literal(label)
                        .requires { has(it, "shadowessentials.reload") }
                        .then(
                            literal("reload")
                                .executes { ctx ->
                                    ConfigManager.load()
                                    LangManager.setLanguage(ConfigManager.config.language)
                                    LangManager.reload()
                                    HomeGUI.reload()
                                    WarpGUI.reload()
                                    WarpEditorGUI.reload()

                                    ctx.source.sendMessage(LangManager.msg("reloadSuccess"))
                                    Command.SINGLE_SUCCESS
                                }
                        )
                )
            }
        }
    }
}