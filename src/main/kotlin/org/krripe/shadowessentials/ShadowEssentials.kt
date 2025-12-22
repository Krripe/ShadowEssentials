package org.krripe.shadowessentials

import net.fabricmc.api.ModInitializer
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.teleport.TeleportDelayManager
import org.krripe.shadowessentials.modules.back.BackCommands
import org.krripe.shadowessentials.modules.blocks.BlocksCommands
import org.krripe.shadowessentials.modules.fly.FlyCommands
import org.krripe.shadowessentials.modules.gamemode.GamemodeCommands
import org.krripe.shadowessentials.modules.home.HomeCommands
import org.krripe.shadowessentials.modules.home.HomeGUI
import org.krripe.shadowessentials.modules.invsee.InvseeCommands
import org.krripe.shadowessentials.modules.itemeditor.ItemEditorCommands
import org.krripe.shadowessentials.modules.misc.CoreCommands
import org.krripe.shadowessentials.modules.misc.MiscCommands
import org.krripe.shadowessentials.modules.spawn.SpawnCommands
import org.krripe.shadowessentials.modules.tempfly.TempFlyCommands
import org.krripe.shadowessentials.modules.tempfly.TempFlyManager
import org.krripe.shadowessentials.modules.tpa.TpaCommands
import org.krripe.shadowessentials.modules.warp.WarpCommands
import org.krripe.shadowessentials.modules.warp.WarpEditorGUI
import org.krripe.shadowessentials.modules.warp.WarpGUI

class ShadowEssentials : ModInitializer {
    override fun onInitialize() {
        ConfigManager.load()

        LangManager.reload()
        HomeGUI.reload()
        WarpGUI.reload()
        WarpEditorGUI.reload()

        LangManager.init(ConfigManager.config.language)
        TempFlyManager.init()
        TeleportDelayManager.init()

        BackCommands.register()
        BlocksCommands.register()
        FlyCommands.register()
        GamemodeCommands.register()
        HomeCommands.register()
        InvseeCommands.register()
        ItemEditorCommands.register()
        CoreCommands.register()
        MiscCommands.register()
        SpawnCommands.register()
        TempFlyCommands.register()
        TpaCommands.register()
        WarpCommands.register()
    }
}
