package org.krripe.shadowessentials.modules.invsee

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.InventoryChangedListener
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import org.krripe.shadowessentials.core.lang.LangManager
import java.nio.file.Files
import java.util.UUID

private fun ItemStack.setCustomName(name: Text) {
    this.set(DataComponentTypes.ITEM_NAME, name)
}

object InvseeManager {

    private val SLOT_POS = Array(54) { i -> 8 + (i % 9) * 18 to 18 + (i / 9) * 18 }

    fun open(viewer: ServerPlayerEntity, target: ServerPlayerEntity, editable: Boolean) {
        val factory = object : NamedScreenHandlerFactory {
            override fun createMenu(
                syncId: Int,
                inv: PlayerInventory,
                player: PlayerEntity
            ): ScreenHandler {
                return PlayerInventoryScreenHandler(syncId, inv, target, editable)
            }

            override fun getDisplayName(): Text =
                LangManager.msg("invseeTitle", "player" to target.name.string)
        }
        viewer.openHandledScreen(factory)
    }

    fun openOffline(viewer: ServerPlayerEntity, targetName: String): Boolean {
        val server = viewer.server
        val userCache = server.userCache ?: return false
        val profile = userCache.findByName(targetName).orElse(null) ?: return false
        val uuid = profile.id

        val nbt = loadOfflineInventoryNbt(server, uuid) ?: return false
        val registries: RegistryWrapper.WrapperLookup = server.registryManager
        val items = buildOfflineItemsFromNbt(registries, nbt)
        val offlineInv = OfflineInventory(server, uuid, registries, nbt, items)

        val factory = object : NamedScreenHandlerFactory {
            override fun createMenu(
                syncId: Int,
                inv: PlayerInventory,
                player: PlayerEntity
            ): ScreenHandler {
                return OfflineInventoryScreenHandler(syncId, inv, offlineInv)
            }

            override fun getDisplayName(): Text =
                LangManager.msg("invseeTitle", "player" to profile.name)
        }

        viewer.openHandledScreen(factory)
        return true
    }

    private fun loadOfflineInventoryNbt(server: MinecraftServer, uuid: UUID): NbtCompound? {
        val path = server.getSavePath(WorldSavePath.PLAYERDATA).resolve("$uuid.dat")
        if (!Files.exists(path)) return null

        Files.newInputStream(path).use { input ->
            return NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes())
        }
    }

    private fun buildOfflineItemsFromNbt(
        registries: RegistryWrapper.WrapperLookup,
        nbt: NbtCompound
    ): MutableList<ItemStack> {
        val items = MutableList(54) { ItemStack.EMPTY }

        if (!nbt.contains("Inventory", NbtElement.LIST_TYPE.toInt())) {
            return items
        }

        val list = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE.toInt())

        for (i in 0 until list.size) {
            val stackNbt = list.getCompound(i)
            val slot = stackNbt.getByte("Slot").toInt() and 255
            val stack = ItemStack.fromNbt(registries, stackNbt).orElse(ItemStack.EMPTY)

            when (slot) {
                103 -> items[0] = stack
                102 -> items[1] = stack
                101 -> items[2] = stack
                100 -> items[3] = stack
                150 -> items[4] = stack

                in 9..35 -> {
                    if (slot in items.indices) {
                        items[slot] = stack
                    }
                }

                in 0..8 -> {
                    val idx = 45 + slot
                    if (idx in items.indices) {
                        items[idx] = stack
                    }
                }
            }
        }

        return items
    }

    private class FixedLayoutProxyInventory(
        private val target: ServerPlayerEntity,
        private val editable: Boolean
    ) : Inventory {

        private val targetInv: PlayerInventory = target.inventory
        private val craft: CraftingInventory =
            target.playerScreenHandler.craftingInput as CraftingInventory

        private val listeners = mutableListOf<InventoryChangedListener>()

        private val filler: ItemStack by lazy {
            ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
                setCustomName(Text.literal(" "))
            }
        }

        override fun size() = 54

        override fun isEmpty(): Boolean {
            for (i in 0..8) if (!targetInv.getStack(i).isEmpty) return false
            for (i in 9 until targetInv.size()) if (!targetInv.getStack(i).isEmpty) return false
            for (i in 0..3) if (!targetInv.armor[i].isEmpty) return false
            if (!targetInv.offHand[0].isEmpty) return false
            for (i in 0..3) if (!craft.getStack(i).isEmpty) return false
            return true
        }

        override fun getStack(slot: Int): ItemStack = when (slot) {
            in 0..4 -> when (slot) {
                0 -> targetInv.armor[3]
                1 -> targetInv.armor[2]
                2 -> targetInv.armor[1]
                3 -> targetInv.armor[0]
                else -> targetInv.offHand[0]
            }
            in 5..8 -> craft.getStack(slot - 5)
            in 36..44 -> filler
            in 9..35 -> targetInv.getStack(slot)
            in 45..53 -> targetInv.getStack(slot - 45)
            else -> ItemStack.EMPTY
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            if (!editable) return ItemStack.EMPTY
            val cur = getStack(slot)
            if (cur.isEmpty) return ItemStack.EMPTY
            val copy = cur.copy()
            val taken = copy.split(amount)
            setStack(slot, copy)
            return taken
        }

        override fun removeStack(slot: Int): ItemStack {
            if (!editable) return ItemStack.EMPTY
            val cur = getStack(slot)
            if (cur.isEmpty) return ItemStack.EMPTY
            setStack(slot, ItemStack.EMPTY)
            return cur
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            if (!editable) return
            when (slot) {
                in 0..4 -> when (slot) {
                    0 -> targetInv.armor[3] = stack
                    1 -> targetInv.armor[2] = stack
                    2 -> targetInv.armor[1] = stack
                    3 -> targetInv.armor[0] = stack
                    else -> targetInv.offHand[0] = stack
                }
                in 5..8 -> craft.setStack(slot - 5, stack)
                in 9..35 -> targetInv.setStack(slot, stack)
                in 36..44 -> {}
                in 45..53 -> targetInv.setStack(slot - 45, stack)
            }
            markDirty()
        }

        override fun markDirty() {
            targetInv.markDirty()
            craft.markDirty()
            target.playerScreenHandler.onContentChanged(craft)
            if (listeners.isNotEmpty()) {
                listeners.forEach { it.onInventoryChanged(this) }
            }
        }

        override fun canPlayerUse(player: PlayerEntity) = true
        override fun clear() {}

        fun addListener(l: InventoryChangedListener) {
            if (l !in listeners) listeners += l
        }
    }

    private fun saveOfflineInventoryNbt(
        server: MinecraftServer,
        uuid: UUID,
        registries: RegistryWrapper.WrapperLookup,
        rootNbt: NbtCompound,
        stacks: List<ItemStack>
    ) {
        val list = net.minecraft.nbt.NbtList()

        fun add(slotNum: Int, stack: ItemStack) {
            if (stack.isEmpty) return
            val stackNbt = stack.encode(registries) as NbtCompound
            stackNbt.put("Slot", net.minecraft.nbt.NbtByte.of(slotNum.toByte()))
            list.add(stackNbt)
        }

        if (stacks.size >= 54) {
            add(103, stacks[0])
            add(102, stacks[1])
            add(101, stacks[2])
            add(100, stacks[3])
            add(150, stacks[4])

            for (slot in 9..35) {
                add(slot, stacks[slot])
            }

            for (slot in 0..8) {
                val idx = 45 + slot
                if (idx < stacks.size) {
                    add(slot, stacks[idx])
                }
            }
        }

        rootNbt.put("Inventory", list)

        val path = server.getSavePath(WorldSavePath.PLAYERDATA).resolve("$uuid.dat")
        Files.newOutputStream(path).use { out ->
            NbtIo.writeCompressed(rootNbt, out)
        }
    }

    private class OfflineInventory(
        private val server: MinecraftServer,
        private val uuid: UUID,
        private val registries: RegistryWrapper.WrapperLookup,
        private val rootNbt: NbtCompound,
        private val stacks: MutableList<ItemStack>
    ) : Inventory {

        private val filler: ItemStack by lazy {
            ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
                setCustomName(Text.literal(" "))
            }
        }

        override fun size(): Int = 54

        override fun isEmpty(): Boolean = stacks.all { it.isEmpty }

        override fun getStack(slot: Int): ItemStack = when (slot) {
            in 0..4 -> stacks.getOrNull(slot) ?: ItemStack.EMPTY
            in 5..8 -> ItemStack.EMPTY
            in 9..35 -> stacks.getOrNull(slot) ?: ItemStack.EMPTY
            in 36..44 -> filler
            in 45..53 -> stacks.getOrNull(slot) ?: ItemStack.EMPTY
            else -> ItemStack.EMPTY
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            if (slot in 5..8 || slot in 36..44) return ItemStack.EMPTY
            val cur = getStack(slot)
            if (cur.isEmpty) return ItemStack.EMPTY
            val copy = cur.copy()
            val taken = copy.split(amount)
            setStack(slot, copy)
            return taken
        }

        override fun removeStack(slot: Int): ItemStack {
            if (slot in 5..8 || slot in 36..44) return ItemStack.EMPTY
            val cur = getStack(slot)
            if (cur.isEmpty) return ItemStack.EMPTY
            setStack(slot, ItemStack.EMPTY)
            return cur
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            if (slot in 5..8 || slot in 36..44) return
            if (slot !in 0 until 54) return
            stacks[slot] = stack
            markDirty()
        }

        override fun markDirty() {
            saveOfflineInventoryNbt(server, uuid, registries, rootNbt, stacks)
        }

        override fun canPlayerUse(player: PlayerEntity) = true

        override fun clear() {}
    }

    private class PlayerInventoryScreenHandler(
        syncId: Int,
        viewerInventory: PlayerInventory,
        target: ServerPlayerEntity,
        editable: Boolean
    ) : ScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId) {

        private val proxy = FixedLayoutProxyInventory(target, editable)

        init {
            for (i in 0 until 54) {
                val (x, y) = SLOT_POS[i]
                val slot = when (i) {
                    in 36..44 -> object : Slot(proxy, i, x, y) {
                        override fun canInsert(stack: ItemStack) = false
                        override fun canTakeItems(player: PlayerEntity) = false
                    }
                    else -> Slot(proxy, i, x, y)
                }
                addSlot(slot)
            }

            val invX = 8
            val invY = 140
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    val idx = col + row * 9 + 9
                    addSlot(Slot(viewerInventory, idx, invX + col * 18, invY + row * 18))
                }
            }
            val hotbarY = invY + 58
            for (col in 0 until 9) {
                addSlot(Slot(viewerInventory, col, invX + col * 18, hotbarY))
            }

            proxy.addListener { sendContentUpdates() }
        }

        override fun canUse(player: PlayerEntity) = true

        override fun onSlotClick(
            slotIndex: Int,
            button: Int,
            action: SlotActionType,
            player: PlayerEntity
        ) {
            if (slotIndex in 36..44) return
            super.onSlotClick(slotIndex, button, action, player)
        }

        override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY
    }

    private class OfflineInventoryScreenHandler(
        syncId: Int,
        viewerInventory: PlayerInventory,
        private val offlineInv: OfflineInventory
    ) : ScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId) {

        init {
            for (i in 0 until 54) {
                val (x, y) = SLOT_POS[i]
                val slot = when (i) {
                    in 36..44 -> object : Slot(offlineInv, i, x, y) {
                        override fun canInsert(stack: ItemStack) = false
                        override fun canTakeItems(player: PlayerEntity) = false
                    }
                    else -> Slot(offlineInv, i, x, y)
                }
                addSlot(slot)
            }

            val invX = 8
            val invY = 140
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    val idx = col + row * 9 + 9
                    addSlot(Slot(viewerInventory, idx, invX + col * 18, invY + row * 18))
                }
            }
            val hotbarY = invY + 58
            for (col in 0 until 9) {
                addSlot(Slot(viewerInventory, col, invX + col * 18, hotbarY))
            }
        }

        override fun canUse(player: PlayerEntity) = true

        override fun onSlotClick(
            slotIndex: Int,
            button: Int,
            action: SlotActionType,
            player: PlayerEntity
        ) {
            if (slotIndex in 36..44) return
            super.onSlotClick(slotIndex, button, action, player)
        }

        override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY
    }
}