package org.krripe.shadowessentials.modules.home

import com.google.gson.GsonBuilder
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.gui.GuiConfig
import org.krripe.shadowessentials.core.gui.GuiInteraction
import org.krripe.shadowessentials.core.gui.GuiItem
import org.krripe.shadowessentials.core.gui.GuiUtils
import org.krripe.shadowessentials.core.gui.openGui
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.text.CustomColorParser
import java.io.File

data class HomeIconConfig(
    val icon: String = "minecraft:red_bed",
    val name: String = "&e%home%",
    val lore: List<String> = listOf(
        "&7World: &e%world%",
        "&7Position: &e%x%&7, &e%y%&7, &e%z%",
        "&7Created: &e%date%"
    ),
    val modelData: Int? = null,
    val glow: Boolean = false
)

data class HomesGuiConfig(
    val title: String = "&8Homes",
    val size: Int = 54,
    val fillersEnabled: Boolean = true,
    val fillers: MutableList<GuiConfig.Filler> = mutableListOf(),
    val nextPageButton: GuiConfig.Button? = null,
    val prevPageButton: GuiConfig.Button? = null,
    val closeButton: GuiConfig.Button? = null,
    val homeSlots: List<String> = emptyList(),
    val homeIcon: HomeIconConfig = HomeIconConfig()
)

object HomeGUI {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val file = File("config/ShadowEssentials/gui/homes_menu.json")

    @Volatile
    private var cachedConfig: HomesGuiConfig? = null

    fun reload() {
        cachedConfig = null
    }

    private fun ensureDefaultFile() {
        if (file.exists()) return
        file.parentFile?.mkdirs()

        val default = HomesGuiConfig(
            title = "&8Homes",
            size = 54,
            fillersEnabled = true,
            fillers = mutableListOf(
                GuiConfig.Filler(
                    name = " ",
                    lore = emptyList(),
                    icon = "minecraft:light_blue_stained_glass_pane",
                    slots = listOf("0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,56,47,48,49,50,51,52,53"),
                    glow = false
                )
            ),
            closeButton = GuiConfig.Button(
                slot = 49,
                icon = "minecraft:barrier",
                name = "&cClose",
                lore = emptyList(),
                modelData = null,
                glow = false
            ),
            homeSlots = listOf("10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,28,30,31,32,33,34,37,38,39,40,41,42,43"),
            homeIcon = HomeIconConfig()
        )

        file.writeText(gson.toJson(default))
    }

    private fun loadConfig(): HomesGuiConfig {
        ensureDefaultFile()
        return runCatching {
            gson.fromJson(file.readText(), HomesGuiConfig::class.java)
        }.getOrElse { HomesGuiConfig() }
    }

    private fun config(): HomesGuiConfig {
        val cfg = cachedConfig
        if (cfg != null) return cfg
        val loaded = loadConfig()
        cachedConfig = loaded
        return loaded
    }

    fun open(player: ServerPlayerEntity) {
        val homeCfg = ConfigManager.config.home
        if (!homeCfg.enabled) {
            player.sendMessage(LangManager.msg("homeDisabled"))
            return
        }

        val currentWorldId = player.world.registryKey.value.toString()
        if (homeCfg.blacklistWorlds.isNotEmpty() && currentWorldId in homeCfg.blacklistWorlds) {
            player.sendMessage(LangManager.msg("homeWorldBlacklisted"))
            return
        }

        val homes = HomeManager.getHomes(player)
        if (homes.isEmpty()) {
            player.sendMessage(LangManager.msg("homeListEmpty"))
            return
        }

        val sorted: List<Pair<String, HomeLocation>> = homes.entries
            .sortedBy { it.key.lowercase() }
            .map { it.key to it.value }

        val guiCfg = config()
        var size = guiCfg.size.coerceIn(9, 54)
        if (size % 9 != 0) {
            size = ((size / 9).coerceAtLeast(1)).coerceAtMost(6) * 9
        }

        val homeSlots: List<Int> = if (guiCfg.homeSlots.isNotEmpty()) {
            GuiUtils.parseSlots(guiCfg.homeSlots, size)
        } else {
            (0 until size).toList()
        }

        val guiItems = mutableListOf<GuiItem>()
        val homeSlotsMap = mutableMapOf<Int, String>()

        val iconCfg = guiCfg.homeIcon
        val maxHomesShown = homeSlots.size.coerceAtMost(sorted.size)

        for (i in 0 until maxHomesShown) {
            val slot = homeSlots[i]
            val (name, loc) = sorted[i]

            val displayName = applyPlaceholders(
                iconCfg.name,
                name,
                loc
            )
            val loreLines = iconCfg.lore.map { line ->
                applyPlaceholders(line, name, loc)
            }

            val stack = GuiUtils.makeItem(
                material = iconCfg.icon,
                name = displayName,
                lore = loreLines,
                modelData = iconCfg.modelData,
                glow = iconCfg.glow
            )

            guiItems += GuiItem(
                itemStack = stack,
                slot = slot
            )
            homeSlotsMap[slot] = name
        }

        if (guiCfg.fillersEnabled && guiCfg.fillers.isNotEmpty()) {
            for (filler in guiCfg.fillers) {
                val fillerStack = GuiUtils.makeFiller(filler)
                val slots = GuiUtils.parseSlots(filler.slots, size)
                for (slot in slots) {
                    if (homeSlotsMap.containsKey(slot)) continue
                    if (guiCfg.closeButton != null && slot == guiCfg.closeButton.slot) continue
                    if (guiCfg.nextPageButton != null && slot == guiCfg.nextPageButton.slot) continue
                    if (guiCfg.prevPageButton != null && slot == guiCfg.prevPageButton.slot) continue

                    guiItems += GuiItem(
                        itemStack = fillerStack.copy(),
                        slot = slot
                    )
                }
            }
        }

        val closeBtn = guiCfg.closeButton
        val closeSlot = closeBtn?.slot
        if (closeBtn != null && closeSlot != null && closeSlot in 0 until size) {
            val closeItem = GuiUtils.makeButton(closeBtn)
            guiItems += GuiItem(
                itemStack = closeItem,
                slot = closeSlot
            )
        }

        val titleText = CustomColorParser.processAllFormats(guiCfg.title)

        openGui(
            player = player,
            title = titleText,
            guiItems = guiItems,
            inventorySize = size,
            interaction = object : GuiInteraction {
                override fun onItemClick(slot: Int, player: ServerPlayerEntity, rightClick: Boolean) {
                    if (closeSlot != null && slot == closeSlot) {
                        player.closeHandledScreen()
                        return
                    }

                    val homeName = homeSlotsMap[slot] ?: return
                    if (!player.world.isClient) {
                        player.closeHandledScreen()
                        HomeManager.teleportToHome(player, homeName)
                    }
                }

                override fun onGuiClose(player: ServerPlayerEntity) {
                }
            }
        )
    }

    private fun applyPlaceholders(
        template: String,
        homeName: String,
        loc: HomeLocation
    ): String {
        val date = HomeManager.formatCreatedAt(loc.createdAt)
        val xStr = String.format("%.1f", loc.x)
        val yStr = String.format("%.1f", loc.y)
        val zStr = String.format("%.1f", loc.z)

        return template
            .replace("%home%", homeName)
            .replace("%world%", loc.world)
            .replace("%x%", xStr)
            .replace("%y%", yStr)
            .replace("%z%", zStr)
            .replace("%date%", date)
    }
}