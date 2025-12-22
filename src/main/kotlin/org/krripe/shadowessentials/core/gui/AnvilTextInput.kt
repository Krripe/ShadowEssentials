package org.krripe.shadowessentials.core.gui

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.screen.AnvilScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.text.CustomColorParser

object AnvilTextInput {

    private fun noItalics(t: Text): Text =
        t.copy().setStyle(Style.EMPTY.withItalic(false))

    fun open(
        player: ServerPlayerEntity,
        titleRaw: String,
        initial: String = "",
        onCancel: () -> Unit = {},
        resetName: String? = null,
        resetLore: List<String> = emptyList(),
        confirmName: String? = null,
        confirmLore: List<String> = emptyList(),
        onSubmit: (String) -> Unit
    ) {
        val title = CustomColorParser.processAllFormats(titleRaw)
            .setStyle(Style.EMPTY.withItalic(false))
        open(player, title, initial, onCancel, resetName, resetLore, confirmName, confirmLore, onSubmit)
    }

    fun open(
        player: ServerPlayerEntity,
        title: Text,
        initial: String = "",
        onCancel: () -> Unit = {},
        resetName: String? = null,
        resetLore: List<String> = emptyList(),
        confirmName: String? = null,
        confirmLore: List<String> = emptyList(),
        onSubmit: (String) -> Unit
    ) {
        val factory = SimpleNamedScreenHandlerFactory({ syncId, inv, _ ->
            object : AnvilScreenHandler(syncId, inv, ScreenHandlerContext.EMPTY) {
                private var submitted = false
                private var currentText: String = initial
                private val virtual = arrayOf(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)

                private val dummyInv: Inventory = object : Inventory {
                    override fun clear() {}
                    override fun size() = 3
                    override fun isEmpty() = virtual.all { it.isEmpty }
                    override fun getStack(slot: Int) = virtual.getOrNull(slot) ?: ItemStack.EMPTY
                    override fun removeStack(slot: Int, amount: Int) = ItemStack.EMPTY
                    override fun removeStack(slot: Int) = ItemStack.EMPTY
                    override fun setStack(slot: Int, stack: ItemStack) {}
                    override fun markDirty() {}
                    override fun canPlayerUse(player: PlayerEntity) = true
                }

                private inner class LockedSlot(index: Int, x: Int, y: Int) :
                    Slot(dummyInv, index, x, y) {
                    override fun canInsert(stack: ItemStack) = false
                    override fun canTakeItems(player: PlayerEntity) = false
                }

                init {
                    virtual[0] = buildResetItem(resetName, resetLore)
                    virtual[1] = ItemStack.EMPTY
                    virtual[2] = ItemStack.EMPTY

                    this.slots[0] = LockedSlot(0, this.slots[0].x, this.slots[0].y)
                    this.slots[1] = LockedSlot(1, this.slots[1].x, this.slots[1].y)
                    this.slots[2] = LockedSlot(2, this.slots[2].x, this.slots[2].y)

                    setNewItemName(initial)
                    refreshPreview(confirmName, confirmLore)
                    pushSlot(0); pushSlot(1); pushSlot(2)
                }

                private fun buildResetItem(
                    name: String?,
                    lore: List<String>
                ): ItemStack {
                    if (name == null && lore.isEmpty()) {
                        return ItemStack(Items.PAPER)
                    }
                    val it = ItemStack(Items.PAPER)
                    val nm = name ?: "&cReset"
                    it.set(
                        DataComponentTypes.CUSTOM_NAME,
                        CustomColorParser.processAllFormats(nm)
                            .setStyle(Style.EMPTY.withItalic(false))
                    )
                    if (lore.isNotEmpty()) {
                        val loreTexts = lore.map {
                            CustomColorParser.processAllFormats(it)
                                .setStyle(Style.EMPTY.withItalic(false))
                        }
                        it.set(DataComponentTypes.LORE, LoreComponent(loreTexts))
                    }
                    return it
                }

                private fun confirmItemStatic(
                    name: String?,
                    lore: List<String>
                ): ItemStack {
                    val it = ItemStack(Items.PAPER)
                    val nm = name ?: ""
                    if (nm.isNotBlank()) {
                        it.set(
                            DataComponentTypes.CUSTOM_NAME,
                            CustomColorParser.processAllFormats(nm)
                                .setStyle(Style.EMPTY.withItalic(false))
                        )
                    }
                    if (lore.isNotEmpty()) {
                        val loreTexts = lore.map {
                            CustomColorParser.processAllFormats(it)
                                .setStyle(Style.EMPTY.withItalic(false))
                        }
                        it.set(DataComponentTypes.LORE, LoreComponent(loreTexts))
                    }
                    return it
                }

                private fun refreshPreview(
                    confirmName: String?,
                    confirmLore: List<String>
                ) {
                    virtual[2] =
                        if (confirmName != null && currentText.isNotBlank())
                            confirmItemStatic(confirmName, confirmLore)
                        else
                            ItemStack.EMPTY

                    pushSlot(2)
                    sendContentUpdates()
                }

                private fun pushSlot(idx: Int) {
                    player.networkHandler.sendPacket(
                        ScreenHandlerSlotUpdateS2CPacket(
                            syncId,
                            nextRevision(),
                            idx,
                            virtual[idx]
                        )
                    )
                }

                override fun setNewItemName(newName: String): Boolean {
                    val changed = super.setNewItemName(newName)
                    if (newName != currentText) {
                        currentText = newName
                        refreshPreview(confirmName, confirmLore)
                    }
                    return changed
                }

                override fun updateResult() {}
                override fun getLevelCost() = 0

                override fun onSlotClick(
                    slotIndex: Int,
                    button: Int,
                    actionType: SlotActionType,
                    p: PlayerEntity
                ) {
                    if (slotIndex in 0..2 && actionType != SlotActionType.PICKUP) {
                        pushSlot(slotIndex)
                        return
                    }

                    when (slotIndex) {
                        0 -> {
                            if (actionType == SlotActionType.PICKUP) {
                                currentText = ""
                                setNewItemName("")
                                refreshPreview(confirmName, confirmLore)
                            }
                            return
                        }

                        2 -> {
                            if (actionType == SlotActionType.PICKUP &&
                                (confirmName == null || currentText.isNotBlank())
                            ) {
                                val value = currentText.trim()
                                submitted = true
                                onSubmit(value)
                                if (p is ServerPlayerEntity) p.closeHandledScreen()
                            }
                            return
                        }
                    }

                    super.onSlotClick(slotIndex, button, actionType, p)
                }

                override fun quickMove(player: PlayerEntity, index: Int) = ItemStack.EMPTY
                override fun canUse(player: PlayerEntity) = true

                override fun onClosed(p: PlayerEntity) {
                    super.onClosed(p)
                    if (!submitted) onCancel()
                }
            }
        }, title)

        player.openHandledScreen(factory)
    }

    fun openMultiline(
        player: ServerPlayerEntity,
        titleRaw: String,
        initialLines: List<String> = emptyList(),
        maxLines: Int = 6,
        onCancel: () -> Unit = {},
        onSubmit: (List<String>) -> Unit
    ) {
        val title = CustomColorParser.processAllFormats(titleRaw)
            .setStyle(Style.EMPTY.withItalic(false))

        val factory = SimpleNamedScreenHandlerFactory({ syncId, inv, _ ->
            object : AnvilScreenHandler(syncId, inv, ScreenHandlerContext.EMPTY) {
                private var submitted = false
                private var currentText = ""
                private val lines =
                    initialLines.map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                private var selectedIdx: Int = if (lines.isNotEmpty()) 0 else -1
                private var editIdx: Int = -1

                private val virtual = arrayOf(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)

                private val dummyInv: Inventory = object : Inventory {
                    override fun clear() {}
                    override fun size() = 3
                    override fun isEmpty() = virtual.all { it.isEmpty }
                    override fun getStack(slot: Int) = virtual.getOrNull(slot) ?: ItemStack.EMPTY
                    override fun removeStack(slot: Int, amount: Int) = ItemStack.EMPTY
                    override fun removeStack(slot: Int) = ItemStack.EMPTY
                    override fun setStack(slot: Int, stack: ItemStack) {}
                    override fun markDirty() {}
                    override fun canPlayerUse(player: PlayerEntity) = true
                }

                private inner class LockedSlot(index: Int, x: Int, y: Int) :
                    Slot(dummyInv, index, x, y) {
                    override fun canInsert(stack: ItemStack) = false
                    override fun canTakeItems(player: PlayerEntity) = false
                }

                init {
                    this.slots[0] = LockedSlot(0, this.slots[0].x, this.slots[0].y)
                    this.slots[1] = LockedSlot(1, this.slots[1].x, this.slots[1].y)
                    this.slots[2] = LockedSlot(2, this.slots[2].x, this.slots[2].y)

                    setNewItemName("")
                    refreshAll()
                }

                private fun push(idx: Int) {
                    player.networkHandler.sendPacket(
                        ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), idx, virtual[idx])
                    )
                }

                private fun paperLines(): ItemStack {
                    val it = ItemStack(Items.PAPER)
                    it.set(
                        DataComponentTypes.CUSTOM_NAME,
                        noItalics(
                            LangManager.msg(
                                "anvilLinesTitle",
                                "count" to lines.size.toString(),
                                "max" to maxLines.toString()
                            )
                        )
                    )

                    val lore = buildList {
                        if (lines.isEmpty()) {
                            add(noItalics(LangManager.msg("anvilLinesEmpty")))
                        } else {
                            lines.forEachIndexed { i, s ->
                                val entry =
                                    if (i == selectedIdx)
                                        LangManager.msg(
                                            "anvilLinesEntrySelected",
                                            "text" to s
                                        )
                                    else
                                        LangManager.msg(
                                            "anvilLinesEntryUnselected",
                                            "index" to (i + 1).toString(),
                                            "text" to s
                                        )
                                add(noItalics(entry))
                            }
                        }
                        add(Text.literal(""))
                        add(noItalics(LangManager.msg("anvilLClickSelectEdit")))
                        add(noItalics(LangManager.msg("anvilRClickRemoveSelected")))
                    }

                    it.set(DataComponentTypes.LORE, LoreComponent(lore))
                    return it
                }

                private fun doneButton(): ItemStack {
                    val it = ItemStack(Items.LIME_DYE)
                    val key =
                        if (lines.isEmpty()) "anvilDoneTitleDisabled" else "anvilDoneTitleEnabled"
                    it.set(
                        DataComponentTypes.CUSTOM_NAME,
                        noItalics(LangManager.msg(key))
                    )
                    it.set(
                        DataComponentTypes.LORE,
                        LoreComponent(listOf(noItalics(LangManager.msg("anvilDoneLore"))))
                    )
                    return it
                }

                private fun addOrSaveButton(): ItemStack {
                    val saving = editIdx >= 0
                    val it = ItemStack(if (saving) Items.YELLOW_DYE else Items.PAPER)

                    it.set(
                        DataComponentTypes.CUSTOM_NAME,
                        noItalics(
                            LangManager.msg(
                                if (saving) "anvilSaveTitle" else "anvilAddTitle"
                            )
                        )
                    )

                    val lore = buildList {
                        if (currentText.isBlank()) {
                            add(noItalics(LangManager.msg("anvilTypeAbove")))
                        } else {
                            add(
                                noItalics(
                                    LangManager.msg(
                                        "anvilCurrentText",
                                        "text" to currentText
                                    )
                                )
                            )
                        }
                        add(Text.literal(""))
                        add(
                            noItalics(
                                LangManager.msg(
                                    if (saving) "anvilClickToSave" else "anvilClickToAdd"
                                )
                            )
                        )
                        if (saving) add(noItalics(LangManager.msg("anvilBlankRemoves")))
                    }

                    it.set(DataComponentTypes.LORE, LoreComponent(lore))
                    return it
                }

                private fun refreshAll() {
                    virtual[0] = paperLines()
                    virtual[1] = doneButton()
                    virtual[2] = addOrSaveButton()
                    push(0); push(1); push(2)
                    sendContentUpdates()
                }

                override fun setNewItemName(newName: String): Boolean {
                    val changed = super.setNewItemName(newName)
                    if (newName != currentText) {
                        currentText = newName
                        virtual[2] = addOrSaveButton()
                        push(2)
                    }
                    return changed
                }

                override fun updateResult() {}
                override fun getLevelCost() = 0

                override fun onSlotClick(
                    slotIndex: Int,
                    button: Int,
                    actionType: SlotActionType,
                    p: PlayerEntity
                ) {
                    if (slotIndex in 0..2 && actionType != SlotActionType.PICKUP) {
                        push(slotIndex)
                        return
                    }

                    when (slotIndex) {
                        0 -> {
                            if (lines.isEmpty()) {
                                push(0); return
                            }
                            if (button == 1) {
                                // remove selected
                                if (selectedIdx in lines.indices) {
                                    lines.removeAt(selectedIdx)
                                    if (lines.isEmpty()) {
                                        selectedIdx = -1
                                        editIdx = -1
                                        setNewItemName(""); currentText = ""
                                    } else {
                                        selectedIdx = selectedIdx.coerceAtMost(lines.lastIndex)
                                        editIdx = -1
                                        setNewItemName(""); currentText = ""
                                    }
                                    refreshAll()
                                } else {
                                    push(0)
                                }
                                return
                            } else {
                                selectedIdx =
                                    if (selectedIdx < 0) 0 else (selectedIdx + 1) % lines.size
                                editIdx = selectedIdx
                                val txt = lines[selectedIdx]
                                setNewItemName(txt)
                                currentText = txt
                                refreshAll()
                                return
                            }
                        }

                        1 -> if (actionType == SlotActionType.PICKUP) {
                            submitted = true
                            val out =
                                lines.map { it.trim() }.filter { it.isNotEmpty() }
                            (p as? ServerPlayerEntity)?.closeHandledScreen()
                            onSubmit(out)
                            return
                        }

                        2 -> if (actionType == SlotActionType.PICKUP) {
                            val v = currentText.trim()
                            if (editIdx >= 0) {
                                // save / remove
                                if (v.isEmpty()) {
                                    if (editIdx in lines.indices) lines.removeAt(editIdx)
                                    selectedIdx =
                                        if (lines.isEmpty()) -1 else editIdx.coerceAtMost(lines.lastIndex)
                                } else if (editIdx in lines.indices) {
                                    lines[editIdx] = v
                                    selectedIdx = editIdx
                                }
                                editIdx = -1
                                setNewItemName(""); currentText = ""
                                refreshAll()
                                return
                            } else {
                                if (v.isNotEmpty() && lines.size < maxLines) {
                                    lines += v
                                    selectedIdx = lines.lastIndex
                                    setNewItemName(""); currentText = ""
                                    refreshAll()
                                } else {
                                    push(2)
                                }
                                return
                            }
                        }
                    }

                    super.onSlotClick(slotIndex, button, actionType, p)
                }

                override fun quickMove(player: PlayerEntity, index: Int) = ItemStack.EMPTY
                override fun canUse(player: PlayerEntity) = true

                override fun onClosed(p: PlayerEntity) {
                    super.onClosed(p)
                    if (!submitted) onCancel()
                }
            }
        }, title)

        player.openHandledScreen(factory)
    }
}