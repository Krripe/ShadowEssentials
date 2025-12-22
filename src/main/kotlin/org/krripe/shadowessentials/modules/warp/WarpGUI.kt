package org.krripe.shadowessentials.modules.warp

import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import org.krripe.shadowessentials.core.gui.*
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.text.CustomColorParser

object WarpGUI {

    private val perms = PermissionsSystem()
    private const val EDIT_PERMISSION = "shadowessentials.editor.warp"

    init {
        WarpGuiConfig.load()
    }

    fun reload() = WarpGuiConfig.reload()

    private fun applyWarpBasePlaceholders(template: String, warp: WarpData): String {
        val display = warp.displayName?.takeIf { it.isNotBlank() } ?: warp.id
        return template
            .replace("%id%", warp.id)
            .replace("%display_name%", display)
            .replace("%display%", display)
            .replace("%world%", warp.world)
            .replace("%x%", warp.x.toInt().toString())
            .replace("%y%", warp.y.toInt().toString())
            .replace("%z%", warp.z.toInt().toString())
            .replace("%permission%", warp.permission ?: "-")
    }

    private fun applyWarpPlaceholders(template: String, warp: WarpData): String {
        val desc = warp.lore.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: ""
        return applyWarpBasePlaceholders(template, warp)
            .replace("%description%", desc)
    }

    private fun buildWarpLore(cfg: WarpMenuScreenCfg, warp: WarpData): List<String> {
        val descLines = warp.lore.takeIf { it.isNotEmpty() } ?: emptyList()
        if (cfg.warpItemLore.isEmpty()) {
            return descLines
        }

        val out = mutableListOf<String>()
        for (raw in cfg.warpItemLore) {
            if ("%description%" !in raw) {
                out += applyWarpBasePlaceholders(raw, warp)
            } else {
                if (descLines.isEmpty()) continue
                val before = raw.substringBefore("%description%")
                val after = raw.substringAfter("%description%", "")
                for (dl in descLines) {
                    var line = before + dl + after
                    line = applyWarpBasePlaceholders(line, warp)
                    out += line
                }
            }
        }
        return out
    }

    private fun makeWarpItem(cfg: WarpMenuScreenCfg, warp: WarpData): ItemStack =
        GuiUtils.makeItem(
            material = warp.iconMaterial?.takeIf { it.isNotBlank() } ?: cfg.warpItemIcon,
            name = applyWarpPlaceholders(cfg.warpItemName, warp),
            lore = buildWarpLore(cfg, warp)
        )

    private fun makeButton(b: WarpMenuButtonCfg) =
        GuiUtils.makeItem(
            material = b.icon,
            name = b.name,
            lore = b.lore,
            modelData = b.modelData,
            glow = b.glow,
            hideAdditionalTooltip = b.hideAdditionalTooltip,
            hideTooltip = b.hideTooltip
        )

    private fun makeFiller(f: WarpMenuFillerCfg) =
        GuiUtils.makeItem(
            material = f.icon,
            name = f.name,
            lore = f.lore,
            modelData = f.modelData,
            glow = f.glow,
            hideAdditionalTooltip = f.hideAdditionalTooltip,
            hideTooltip = f.hideTooltip
        )

    fun open(player: ServerPlayerEntity, page: Int = 0) {
        val screenCfg = WarpGuiConfig.cfg.screen
        val warps = WarpManager.all()

        if (warps.isEmpty()) {
            player.sendMessage(LangManager.msg("warpsEmpty"))
            return
        }

        val rawSize = screenCfg.size.coerceIn(9, 54)
        val guiSize = if (rawSize % 9 == 0) rawSize else ((rawSize / 9) + 1) * 9

        val warpSlots = screenCfg.warpSlots
            .filter { it in 0 until guiSize }
            .ifEmpty { (0 until guiSize).toList() }

        val perPage = warpSlots.size.coerceAtLeast(1)
        val pageCount = ((warps.size + perPage - 1) / perPage).coerceAtLeast(1)
        val currentPage = page.coerceIn(0, pageCount - 1)

        val from = currentPage * perPage
        val to = (from + perPage).coerceAtMost(warps.size)
        val pageWarps = warps.subList(from, to)

        val items = mutableListOf<GuiItem>()

        if (screenCfg.fillersEnabled) {
            screenCfg.fillers.forEach { f ->
                val fillerItem = makeFiller(f)
                f.slots.forEach { slot ->
                    if (slot in 0 until guiSize) {
                        items += GuiItem(fillerItem.copy(), slot, null)
                    }
                }
            }
        }

        pageWarps.forEachIndexed { idx, warp ->
            if (idx >= warpSlots.size) return@forEachIndexed
            val slot = warpSlots[idx]
            if (slot !in 0 until guiSize) return@forEachIndexed

            val id = "warp:${warp.id}"
            items += GuiItem(makeWarpItem(screenCfg, warp), slot, id)
        }

        screenCfg.closeButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "close")
            }
        }

        screenCfg.prevPageButton?.let { b ->
            if (currentPage > 0 && b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "prev")
            }
        }

        screenCfg.nextPageButton?.let { b ->
            if (currentPage < pageCount - 1 && b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "next")
            }
        }

        val title = CustomColorParser
            .processAllFormats(screenCfg.title)
            .setStyle(Style.EMPTY.withItalic(false))

        openGui(
            player,
            title,
            items,
            guiSize,
            GuiTypes.fromSize(guiSize),
            object : GuiInteraction {
                override fun onItemClick(slot: Int, player: ServerPlayerEntity, rightClick: Boolean) {
                    val clicked = items.lastOrNull { it.slot == slot } ?: return

                    when (clicked.id) {
                        "close" -> player.closeHandledScreen()

                        "prev" -> if (currentPage > 0) {
                            player.closeHandledScreen()
                            open(player, currentPage - 1)
                        }

                        "next" -> if (currentPage < pageCount - 1) {
                            player.closeHandledScreen()
                            open(player, currentPage + 1)
                        }

                        else -> {
                            val id = clicked.id ?: return
                            if (id.startsWith("warp:")) {
                                val warpId = id.substringAfter("warp:")

                                val canEdit = perms.hasPermission(
                                    player.commandSource,
                                    EDIT_PERMISSION
                                )

                                if (rightClick) {
                                    if (canEdit) {
                                        WarpEditorGUI.open(player, warpId)
                                    } else {
                                        player.sendMessage(
                                            LangManager.msg(
                                                "noPermission",
                                                "perm" to EDIT_PERMISSION
                                            )
                                        )
                                    }
                                } else {
                                    val ok = WarpManager.teleport(player, warpId)
                                    if (ok) {
                                        player.closeHandledScreen()
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onGuiClose(player: ServerPlayerEntity) {}
            }
        )
    }
}