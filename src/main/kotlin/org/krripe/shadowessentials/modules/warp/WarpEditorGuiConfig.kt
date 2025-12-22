package org.krripe.shadowessentials.modules.warp

import com.google.gson.GsonBuilder
import java.io.File

data class WarpEditorFillerCfg(
    val name: String = " ",
    val lore: List<String> = emptyList(),
    val icon: String = "minecraft:gray_stained_glass_pane",
    val slots: List<Int> = emptyList(),
    val modelData: Int? = null,
    val glow: Boolean = false,
    val hideAdditionalTooltip: Boolean = true,
    val hideTooltip: Boolean = false
)

data class WarpEditorButtonCfg(
    val slot: Int = 0,
    val icon: String = "minecraft:barrier",
    val name: String = "&cClose",
    val lore: List<String> = emptyList(),
    val modelData: Int? = null,
    val glow: Boolean = false,
    val hideAdditionalTooltip: Boolean = true,
    val hideTooltip: Boolean = false
)

data class WarpEditorInfoCfg(
    val slot: Int = 4,
    val icon: String = "minecraft:ender_pearl",
    val name: String = "&e%id%",
    val lore: List<String> = listOf(
        "&7World: &f%world%",
        "&7Position: &f%x%, %y%, %z%",
        "&7Permission: &f%permission%"
    ),
    val modelData: Int? = null,
    val glow: Boolean = false,
    val hideAdditionalTooltip: Boolean = true,
    val hideTooltip: Boolean = false
)

data class WarpEditorScreenCfg(
    val title: String = "&6Warp &8» &e%id%",
    val size: Int = 36,

    val fillersEnabled: Boolean = true,
    val fillers: MutableList<WarpEditorFillerCfg> = mutableListOf(
        WarpEditorFillerCfg(
            icon = "minecraft:light_blue_stained_glass_pane",
            slots = listOf(0,1,2,3,4,5,6,7,8,9,17,18,26,27,28,29,30,31,32,33,34,35)
        )
    ),

    val info: WarpEditorInfoCfg = WarpEditorInfoCfg(),
    val teleportButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 22,
        icon = "minecraft:ender_pearl",
        name = "&aTeleport"
    ),
    val renameButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 10,
        icon = "minecraft:name_tag",
        name = "&eRename display"
    ),
    val setPermissionButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 14,
        icon = "minecraft:paper",
        name = "&eSet permission"
    ),
    val clearPermissionButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 15,
        icon = "minecraft:barrier",
        name = "&cClear permission"
    ),
    val editLoreButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 11,
        icon = "minecraft:book",
        name = "&eEdit lore"
    ),
    val changeIconButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 12,
        icon = "minecraft:item_frame",
        name = "&eChange icon",
        lore = listOf("&7Uses the item in your main hand.")
    ),
    val relocateButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 13,
        icon = "minecraft:end_crystal",
        name = "&eRelocate",
        lore = listOf("&7Set to your current position.")
    ),
    val deleteButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 16,
        icon = "minecraft:lava_bucket",
        name = "&4Delete warp"
    ),
    val backButton: WarpEditorButtonCfg? = WarpEditorButtonCfg(
        slot = 31,
        icon = "minecraft:arrow",
        name = "&cBack"
    ),

    val loreMaxLines: Int = 6
)

data class WarpEditorRootCfg(
    val screen: WarpEditorScreenCfg = WarpEditorScreenCfg()
)

object WarpEditorGuiConfig {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val file = File("config/ShadowEssentials/gui/warp_editor.json")

    @Volatile
    var cfg: WarpEditorRootCfg = WarpEditorRootCfg()
        private set

    fun load() {
        if (!file.exists()) {
            cfg = WarpEditorRootCfg()
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(cfg))
            return
        }

        val loaded = runCatching {
            gson.fromJson(file.readText(), WarpEditorRootCfg::class.java)
        }.getOrNull()

        cfg = loaded ?: WarpEditorRootCfg()

        file.parentFile.mkdirs()
        file.writeText(gson.toJson(cfg))
    }

    fun reload() = load()
}