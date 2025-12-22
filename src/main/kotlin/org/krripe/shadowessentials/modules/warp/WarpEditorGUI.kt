package org.krripe.shadowessentials.modules.warp

import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import org.krripe.shadowessentials.core.gui.*
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.text.CustomColorParser

object WarpEditorGUI {

    private val perms = PermissionsSystem()
    private const val EDIT_PERMISSION = "shadowessentials.editor.warp"

    init {
        WarpEditorGuiConfig.load()
    }

    fun reload() = WarpEditorGuiConfig.reload()

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

    private fun buildInfoLore(cfg: WarpEditorInfoCfg, warp: WarpData): List<String> {
        val descLines = warp.lore.takeIf { it.isNotEmpty() } ?: emptyList()
        if (cfg.lore.isEmpty()) return descLines

        val out = mutableListOf<String>()
        for (raw in cfg.lore) {
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

    private fun warpItemIconOrDefault(icon: String, warp: WarpData): String =
        warp.iconMaterial?.takeIf { it.isNotBlank() }
            ?: icon.ifBlank { "minecraft:ender_pearl" }

    private fun makeInfoItem(cfg: WarpEditorInfoCfg, warp: WarpData) =
        GuiUtils.makeItem(
            material = warpItemIconOrDefault(cfg.icon, warp),
            name = applyWarpBasePlaceholders(cfg.name, warp),
            lore = buildInfoLore(cfg, warp),
            modelData = cfg.modelData,
            glow = cfg.glow,
            hideAdditionalTooltip = cfg.hideAdditionalTooltip,
            hideTooltip = cfg.hideTooltip
        )

    private fun makeButton(b: WarpEditorButtonCfg) =
        GuiUtils.makeItem(
            material = b.icon,
            name = b.name,
            lore = b.lore,
            modelData = b.modelData,
            glow = b.glow,
            hideAdditionalTooltip = b.hideAdditionalTooltip,
            hideTooltip = b.hideTooltip
        )

    private fun makeFiller(f: WarpEditorFillerCfg) =
        GuiUtils.makeItem(
            material = f.icon,
            name = f.name,
            lore = f.lore,
            modelData = f.modelData,
            glow = f.glow,
            hideAdditionalTooltip = f.hideAdditionalTooltip,
            hideTooltip = f.hideTooltip
        )

    fun open(player: ServerPlayerEntity, warpId: String) {
        if (!perms.hasPermission(player.commandSource, EDIT_PERMISSION)) {
            player.sendMessage(
                LangManager.msg(
                    "noPermission",
                    "perm" to EDIT_PERMISSION
                )
            )
            return
        }

        val warp = WarpManager.get(warpId)
        if (warp == null) {
            player.sendMessage(LangManager.msg("warpNotFound", "warp" to warpId))
            return
        }

        val screenCfg = WarpEditorGuiConfig.cfg.screen

        val rawSize = screenCfg.size.coerceIn(9, 54)
        val guiSize = if (rawSize % 9 == 0) rawSize else ((rawSize / 9) + 1) * 9

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

        val info = screenCfg.info
        if (info.slot in 0 until guiSize) {
            items += GuiItem(makeInfoItem(info, warp), info.slot, "info")
        }

        screenCfg.teleportButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "tp")
            }
        }

        screenCfg.renameButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "rename")
            }
        }

        screenCfg.setPermissionButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "setPerm")
            }
        }

        screenCfg.clearPermissionButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "clearPerm")
            }
        }

        screenCfg.editLoreButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "editLore")
            }
        }

        screenCfg.changeIconButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "icon")
            }
        }

        screenCfg.relocateButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "relocate")
            }
        }

        screenCfg.deleteButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "delete")
            }
        }

        screenCfg.backButton?.let { b ->
            if (b.slot in 0 until guiSize) {
                items += GuiItem(makeButton(b), b.slot, "back")
            }
        }

        val title = CustomColorParser
            .processAllFormats(
                screenCfg.title.replace("%id%", warp.id)
            )
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
                        "tp" -> {
                            val ok = WarpManager.teleport(player, warp.id)
                            if (ok) player.closeHandledScreen()
                        }

                        "rename" -> {
                            val anvilTitle = LangManager
                                .msg("warpEditorEnterDisplayName", "warp" to warp.id)
                            val initialName = warp.displayName ?: warp.id

                            AnvilTextInput.open(
                                player = player,
                                title = anvilTitle,
                                initial = initialName,
                                onCancel = {},
                                resetName = "&cReset",
                                resetLore = listOf("&7Reset to default (&e%id%&7)."),
                                confirmName = "&aConfirm",
                                confirmLore = listOf("&7Click to save this name.")
                            ) { value ->
                                val trimmed = value.trim()
                                val ok2 = WarpManager.setDisplayName(
                                    player,
                                    warp.id,
                                    trimmed.ifEmpty { null }
                                )
                                if (ok2) {
                                    open(player, warp.id)
                                }
                            }
                        }

                        "setPerm" -> {
                            val anvilTitle = LangManager
                                .msg("warpEditorEnterPermission", "warp" to warp.id)
                            val initial = warp.permission ?: ""

                            AnvilTextInput.open(
                                player = player,
                                title = anvilTitle,
                                initial = initial,
                                onCancel = {},
                                resetName = "&cReset",
                                resetLore = listOf("&7Click to clear the text."),
                                confirmName = "&aConfirm",
                                confirmLore = listOf("&7Click to apply this permission.")
                            ) { value ->
                                val trimmed = value.trim()
                                val ok2 = if (trimmed.isEmpty()) {
                                    WarpManager.setPermission(player, warp.id, null)
                                } else {
                                    WarpManager.setPermission(player, warp.id, trimmed)
                                }
                                if (ok2) {
                                    open(player, warp.id)
                                }
                            }
                        }

                        "clearPerm" -> {
                            val ok2 = WarpManager.setPermission(player, warp.id, null)
                            if (ok2) {
                                open(player, warp.id)
                            }
                        }

                        "editLore" -> {
                            val titleRaw = LangManager
                                .msg("warpEditorEnterLoreTitle", "warp" to warp.id)
                                .string

                            AnvilTextInput.openMultiline(
                                player = player,
                                titleRaw = titleRaw,
                                initialLines = warp.lore,
                                maxLines = screenCfg.loreMaxLines,
                                onCancel = {}
                            ) { lines ->
                                val ok2 = WarpManager.setLore(player, warp.id, lines)
                                if (ok2) {
                                    open(player, warp.id)
                                }
                            }
                        }

                        "icon" -> {
                            val stack = player.mainHandStack
                            if (stack.isEmpty) {
                                player.sendMessage(
                                    LangManager.msg("warpEditorIconNoItem"),
                                    false
                                )
                                return
                            }
                            val id = Registries.ITEM.getId(stack.item).toString()
                            val ok2 = WarpManager.setIcon(player, warp.id, id)
                            if (ok2) {
                                open(player, warp.id)
                            }
                        }

                        "relocate" -> {
                            val ok2 = WarpManager.relocateWarp(player, warp.id)
                            if (ok2) {
                                open(player, warp.id)
                            }
                        }

                        "delete" -> {
                            val ok2 = WarpManager.deleteWarp(player, warp.id)
                            if (ok2) {
                                player.closeHandledScreen()
                                WarpGUI.open(player, 0)
                            }
                        }

                        "back" -> {
                            player.closeHandledScreen()
                            WarpGUI.open(player, 0)
                        }
                    }
                }

                override fun onGuiClose(player: ServerPlayerEntity) {}
            }
        )
    }
}