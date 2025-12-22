package org.krripe.shadowessentials.modules.misc

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.krripe.shadowessentials.core.lang.LangManager
import kotlin.math.ceil

object MiscManager {

    fun setEnchantInHand(p: ServerPlayerEntity, enchantId: Identifier, level: Int): Text {
        val stack = p.mainHandStack
        if (stack.isEmpty) return LangManager.msg("enchantNoItem")

        val enchRegistry = p.server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val key: RegistryKey<Enchantment> = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchantId)
        val entry: RegistryEntry.Reference<Enchantment> =
            enchRegistry.getEntry(key).orElse(null)
                ?: return LangManager.msg("enchantInvalid", "enchant" to enchantId.toString())

        if (!entry.value().isAcceptableItem(stack)) {
            return LangManager.msg("enchantNotApplicable")
        }

        stack.addEnchantment(entry, level)

        val comp = stack.getOrDefault(
            DataComponentTypes.ENCHANTMENTS,
            ItemEnchantmentsComponent.DEFAULT
        )
        stack.set(DataComponentTypes.ENCHANTMENTS, comp)

        return LangManager.msg(
            "enchantSet",
            "enchant" to enchantId.toString(),
            "level" to level.toString()
        )
    }

    fun heal(p: ServerPlayerEntity) {
        p.health = p.maxHealth
        p.clearStatusEffects()
        p.hungerManager.foodLevel = 20
        p.hungerManager.saturationLevel = 20f
    }

    fun feed(p: ServerPlayerEntity) {
        p.hungerManager.foodLevel = 20
        p.hungerManager.saturationLevel = 20f
    }

    fun clearInventory(p: ServerPlayerEntity): Int {
        var c = 0
        p.inventory.main.forEach { if (!it.isEmpty) { it.count = 0; c++ } }
        p.inventory.armor.forEach { if (!it.isEmpty) { it.count = 0; c++ } }
        p.inventory.offHand.forEach { if (!it.isEmpty) { it.count = 0; c++ } }
        return c
    }

    fun nearList(center: ServerPlayerEntity, radius: Double): Text {
        val list = center.server.playerManager.playerList
            .filter { it != center && it.world == center.world && it.blockPos.isWithinDistance(center.blockPos, radius) }
            .sortedBy { it.squaredDistanceTo(center) }

        if (list.isEmpty()) return LangManager.msg("nearNone")

        val sb = StringBuilder()
        list.forEachIndexed { i, p ->
            val dist = ceil(center.distanceTo(p) * 10.0) / 10.0
            if (i > 0) sb.append(", ")
            sb.append("${p.name.string}(${dist}m)")
        }
        return LangManager.msg("nearList", "list" to sb.toString())
    }
}