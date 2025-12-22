package org.krripe.shadowessentials.modules.warp

import com.google.gson.GsonBuilder
import java.io.File

data class WarpMenuFillerCfg(
    val name: String = " ",
    val lore: List<String> = emptyList(),
    val icon: String = "minecraft:gray_stained_glass_pane",
    val slots: List<Int> = emptyList(),
    val modelData: Int? = null,
    val glow: Boolean = false,
    val hideAdditionalTooltip: Boolean = true,
    val hideTooltip: Boolean = false
)

data class WarpMenuButtonCfg(
    val slot: Int = 0,
    val icon: String = "minecraft:barrier",
    val name: String = "&cClose",
    val lore: List<String> = emptyList(),
    val modelData: Int? = null,
    val glow: Boolean = false,
    val hideAdditionalTooltip: Boolean = true,
    val hideTooltip: Boolean = false
)

data class WarpMenuScreenCfg(
    val title: String = "&6Warps",
    val size: Int = 54,

    val warpSlots: List<Int> = listOf(
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    ),

    val warpItemIcon: String = "minecraft:ender_pearl",
    val warpItemName: String = "&e%display%",
    val warpItemLore: List<String> = listOf(
        "&7World: &f%world%",
        "&7Pos: &f%x%&7, &f%y%&7, &f%z%",
        "&7Permission: &f%permission%",
        "",
        "&7Description:",
        "&f%description%"
    ),

    val fillersEnabled: Boolean = true,
    val fillers: MutableList<WarpMenuFillerCfg> = mutableListOf(
        WarpMenuFillerCfg(
            icon = "minecraft:light_blue_stained_glass_pane",
            slots = listOf(0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,56,47,48,49,50,51,52,53),
            hideAdditionalTooltip = true,
            hideTooltip = true
        )
    ),

    val closeButton: WarpMenuButtonCfg? = WarpMenuButtonCfg(
        slot = 49,
        icon = "minecraft:barrier",
        name = "&cClose"
    ),
    val nextPageButton: WarpMenuButtonCfg? = WarpMenuButtonCfg(
        slot = 53,
        icon = "minecraft:arrow",
        name = "&aNext"
    ),
    val prevPageButton: WarpMenuButtonCfg? = WarpMenuButtonCfg(
        slot = 45,
        icon = "minecraft:arrow",
        name = "&aPrevious"
    )
)

data class WarpMenuRootCfg(
    val screen: WarpMenuScreenCfg = WarpMenuScreenCfg()
)

object WarpGuiConfig {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val file = File("config/ShadowEssentials/gui/warps_menu.json")

    @Volatile
    var cfg: WarpMenuRootCfg = WarpMenuRootCfg()
        private set

    fun load() {
        if (!file.exists()) {
            cfg = WarpMenuRootCfg()
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(cfg))
            return
        }

        val loaded = runCatching {
            gson.fromJson(file.readText(), WarpMenuRootCfg::class.java)
        }.getOrNull()

        cfg = loaded ?: WarpMenuRootCfg()

        file.parentFile.mkdirs()
        file.writeText(gson.toJson(cfg))
    }

    fun reload() = load()
}
