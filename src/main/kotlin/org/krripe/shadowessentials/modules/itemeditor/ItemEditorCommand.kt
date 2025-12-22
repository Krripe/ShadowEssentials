package org.krripe.shadowessentials.modules.itemeditor

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.IdentifierArgumentType.getIdentifier
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.component.type.CustomModelDataComponent
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.FireworksComponent
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.component.type.UnbreakableComponent
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation
import net.minecraft.item.Items
import net.minecraft.item.trim.ArmorTrim
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.krripe.shadowessentials.core.command.Suggestions
import org.krripe.shadowessentials.core.config.ConfigManager
import org.krripe.shadowessentials.core.lang.LangManager
import org.krripe.shadowessentials.core.perms.PermissionsSystem
import org.krripe.shadowessentials.core.text.CustomColorParser
import java.util.Optional
import java.util.UUID

object ItemEditorCommands {

    private val perms = PermissionsSystem()

    private fun has(src: ServerCommandSource, node: String): Boolean {
        if (src.entity == null) return true
        return perms.hasPermission(src, node)
    }

    private fun requirePlayer(src: ServerCommandSource): ServerPlayerEntity? {
        val p = src.player
        if (p == null) {
            src.sendError(Text.literal("This command can only be used by players."))
            return null
        }
        return p
    }

    private fun canUseHere(player: ServerPlayerEntity): Boolean {
        val cfg = ConfigManager.config.itemEditor
        if (!cfg.enabled) {
            player.sendMessage(LangManager.msg("commandDisabled"))
            return false
        }
        val worldId = player.world.registryKey.value.toString()
        if (cfg.allowWorlds.isNotEmpty() && worldId !in cfg.allowWorlds) {
            player.sendMessage(LangManager.msg("commandWorldNotAllowed"))
            return false
        }
        return true
    }

    private val ENCHANT_SUGGESTIONS = Suggestions.custom { _, builder ->
        Registries.ENCHANTMENT_PROVIDER_TYPE.ids.forEach { id ->
            builder.suggest(id.toString())
        }
    }

    private val ATTRIBUTE_SUGGESTIONS = Suggestions.custom { _, builder ->
        Registries.ATTRIBUTE.ids.forEach { id ->
            builder.suggest(id.toString())
        }
    }

    private val SLOT_SUGGESTIONS = Suggestions.fixed(
        "any", "mainhand", "offhand", "head", "chest", "legs", "feet"
    )

    private val OPERATION_SUGGESTIONS = Suggestions.fixed(
        "add", "add_value", "addition",
        "base_mult", "multiply_base",
        "total_mult", "add_mult", "multiply_total"
    )

    private val TRIM_PATTERN_SUGGESTIONS = Suggestions.custom { src, builder ->
        val reg = src.server.registryManager.get(RegistryKeys.TRIM_PATTERN)
        reg.keys.forEach { key ->
            builder.suggest(key.value.toString())
        }
    }

    private val TRIM_MATERIAL_SUGGESTIONS = Suggestions.custom { src, builder ->
        val reg = src.server.registryManager.get(RegistryKeys.TRIM_MATERIAL)
        reg.keys.forEach { key ->
            builder.suggest(key.value.toString())
        }
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = literal("itemeditor")

                // /itemeditor rename <name>
                .then(
                    literal("rename")
                        .requires { has(it, "shadowessentials.item.rename") }
                        .then(
                            argument("name", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }

                                    val rawName = getString(ctx, "name")
                                    val text = CustomColorParser.processAllFormats(rawName)
                                        .setStyle(Style.EMPTY.withItalic(false))
                                    stack.set(DataComponentTypes.CUSTOM_NAME, text)
                                    p.sendMessage(
                                        LangManager.msg("itemRenamed", "value" to rawName)
                                    )
                                    1
                                }
                        )
                )

                // /itemeditor name clear/reset
                .then(
                    literal("name")
                        .requires { has(it, "shadowessentials.item.rename") }
                        .then(
                            literal("clear")
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }
                                    stack.remove(DataComponentTypes.CUSTOM_NAME)
                                    p.sendMessage(LangManager.msg("itemNameCleared"))
                                    1
                                }
                        )
                        .then(
                            literal("reset")
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }
                                    stack.remove(DataComponentTypes.CUSTOM_NAME)
                                    p.sendMessage(LangManager.msg("itemNameCleared"))
                                    1
                                }
                        )
                )

                // /itemeditor lore
                .then(
                    literal("lore")
                        .requires { has(it, "shadowessentials.item.lore") }

                        // add
                        .then(
                            literal("add")
                                .then(
                                    argument("line", IntegerArgumentType.integer(1))
                                        .then(
                                            argument("text", StringArgumentType.greedyString())
                                                .executes { ctx ->
                                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                                    if (!canUseHere(p)) return@executes 0

                                                    val stack = p.mainHandStack
                                                    if (stack.isEmpty) {
                                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                        return@executes 0
                                                    }

                                                    val existing = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
                                                    val idx1 = getInteger(ctx, "line")
                                                    val idx0 = idx1 - 1
                                                    if (idx0 < 0 || idx0 > existing.size) {
                                                        p.sendMessage(LangManager.msg("itemLoreIndexInvalid"))
                                                        return@executes 0
                                                    }

                                                    val rawText = getString(ctx, "text")
                                                    val line = CustomColorParser.processAllFormats(rawText)
                                                        .setStyle(Style.EMPTY.withItalic(false))
                                                    val newLore = existing.toMutableList().apply { add(idx0, line) }
                                                    stack.set(DataComponentTypes.LORE, LoreComponent(newLore))
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemLoreAddedAt",
                                                            "index" to idx1.toString()
                                                        )
                                                    )
                                                    1
                                                }
                                        )
                                )
                        )

                        // remove
                        .then(
                            literal("remove")
                                .then(
                                    argument("line", IntegerArgumentType.integer(1))
                                        .executes { ctx ->
                                            val p = requirePlayer(ctx.source) ?: return@executes 0
                                            if (!canUseHere(p)) return@executes 0

                                            val stack = p.mainHandStack
                                            if (stack.isEmpty) {
                                                p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                return@executes 0
                                            }

                                            val existing = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
                                            val idx1 = getInteger(ctx, "line")
                                            val idx0 = idx1 - 1
                                            if (idx0 !in existing.indices) {
                                                p.sendMessage(LangManager.msg("itemLoreIndexInvalid"))
                                                return@executes 0
                                            }

                                            val newLore = existing.toMutableList().also { it.removeAt(idx0) }
                                            if (newLore.isEmpty()) stack.remove(DataComponentTypes.LORE)
                                            else stack.set(DataComponentTypes.LORE, LoreComponent(newLore))

                                            p.sendMessage(
                                                LangManager.msg(
                                                    "itemLoreRemovedAt",
                                                    "index" to idx1.toString()
                                                )
                                            )
                                            1
                                        }
                                )
                        )

                        // edit
                        .then(
                            literal("edit")
                                .then(
                                    argument("line", IntegerArgumentType.integer(1))
                                        .then(
                                            argument("text", StringArgumentType.greedyString())
                                                .executes { ctx ->
                                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                                    if (!canUseHere(p)) return@executes 0

                                                    val stack = p.mainHandStack
                                                    if (stack.isEmpty) {
                                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                        return@executes 0
                                                    }

                                                    val existing = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
                                                    val idx1 = getInteger(ctx, "line")
                                                    val idx0 = idx1 - 1
                                                    if (idx0 !in existing.indices) {
                                                        p.sendMessage(LangManager.msg("itemLoreIndexInvalid"))
                                                        return@executes 0
                                                    }

                                                    val rawText = getString(ctx, "text")
                                                    val line = CustomColorParser.processAllFormats(rawText)
                                                        .setStyle(Style.EMPTY.withItalic(false))
                                                    val newLore = existing.toMutableList().apply { this[idx0] = line }
                                                    stack.set(DataComponentTypes.LORE, LoreComponent(newLore))
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemLoreEdited",
                                                            "index" to idx1.toString()
                                                        )
                                                    )
                                                    1
                                                }
                                        )
                                )
                        )

                        // clear
                        .then(
                            literal("clear")
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }
                                    stack.remove(DataComponentTypes.LORE)
                                    p.sendMessage(LangManager.msg("itemLoreCleared"))
                                    1
                                }
                        )
                )

                // /itemeditor amount <count>
                .then(
                    literal("amount")
                        .requires { has(it, "shadowessentials.item.amount") }
                        .then(
                            argument("count", IntegerArgumentType.integer(1))
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }

                                    val cnt = getInteger(ctx, "count").coerceIn(1, stack.maxCount)
                                    stack.count = cnt
                                    p.sendMessage(
                                        LangManager.msg("itemAmountSet", "value" to cnt.toString())
                                    )
                                    1
                                }
                        )
                )

                // /itemeditor unbreakable <bool>
                .then(
                    literal("unbreakable")
                        .requires { has(it, "shadowessentials.item.unbreakable") }
                        .then(
                            argument("value", BoolArgumentType.bool())
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val stack = p.mainHandStack
                                    if (stack.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }

                                    val v = getBool(ctx, "value")
                                    if (v) stack.set(DataComponentTypes.UNBREAKABLE, UnbreakableComponent(true))
                                    else stack.remove(DataComponentTypes.UNBREAKABLE)

                                    p.sendMessage(
                                        LangManager.msg("itemUnbreakableSet", "value" to v.toString())
                                    )
                                    1
                                }
                        )
                )

                // /itemeditor durability <value>
                .then(
                    literal("durability")
                        .requires { has(it, "shadowessentials.item.durability") }
                        .then(
                            argument("value", IntegerArgumentType.integer(0))
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val s = p.mainHandStack
                                    if (s.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }
                                    if (!s.isDamageable) {
                                        p.sendMessage(LangManager.msg("itemNotDamageable"))
                                        return@executes 0
                                    }

                                    val max = s.maxDamage
                                    val remain = getInteger(ctx, "value").coerceIn(0, max)
                                    val dmg = (max - remain).coerceIn(0, max - 1)
                                    s.damage = dmg

                                    p.sendMessage(
                                        LangManager.msg("itemDurabilitySet", "remaining" to remain.toString())
                                    )
                                    1
                                }
                        )
                )

                // /itemeditor enchant <enchantment> <level>
                .then(
                    literal("enchant")
                        .requires { has(it, "shadowessentials.item.enchant") }
                        .then(
                            argument("enchantment", IdentifierArgumentType.identifier())
                                .suggests(ENCHANT_SUGGESTIONS)
                                .then(
                                    argument("level", IntegerArgumentType.integer(1, 255))
                                        .executes { ctx ->
                                            val p = requirePlayer(ctx.source) ?: return@executes 0
                                            if (!canUseHere(p)) return@executes 0

                                            val s = p.mainHandStack
                                            if (s.isEmpty) {
                                                p.sendMessage(LangManager.msg("itemEnchantNoItem"))
                                                return@executes 0
                                            }

                                            val enchId = getIdentifier(ctx, "enchantment")
                                            val reg = p.server.registryManager.get(RegistryKeys.ENCHANTMENT)
                                            val key: RegistryKey<Enchantment> =
                                                RegistryKey.of(RegistryKeys.ENCHANTMENT, enchId)
                                            val enchEntry = reg.getEntry(key).orElse(null)
                                                ?: run {
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemEnchantInvalid",
                                                            "enchant" to enchId.toString()
                                                        )
                                                    )
                                                    return@executes 0
                                                }

                                            val lvl = getInteger(ctx, "level")
                                            if (!enchEntry.value().isAcceptableItem(s)) {
                                                p.sendMessage(LangManager.msg("itemEnchantNotApplicable"))
                                                return@executes 0
                                            }

                                            s.addEnchantment(enchEntry, lvl)
                                            val comp = s.getOrDefault(
                                                DataComponentTypes.ENCHANTMENTS,
                                                ItemEnchantmentsComponent.DEFAULT
                                            )
                                            s.set(DataComponentTypes.ENCHANTMENTS, comp)

                                            p.sendMessage(
                                                LangManager.msg(
                                                    "itemEnchantSet",
                                                    "enchant" to enchId.toString(),
                                                    "level" to lvl.toString()
                                                )
                                            )
                                            1
                                        }
                                )
                        )
                )

                // /itemeditor attribute <attribute> <amount> <slot> <operation> <showTooltip>
                .then(
                    literal("attribute")
                        .requires { has(it, "shadowessentials.item.attribute") }
                        .then(
                            argument("attribute", IdentifierArgumentType.identifier())
                                .suggests(ATTRIBUTE_SUGGESTIONS)
                                .then(
                                    argument("amount", DoubleArgumentType.doubleArg())
                                        .then(
                                            argument("slot", StringArgumentType.word())
                                                .suggests(SLOT_SUGGESTIONS)
                                                .then(
                                                    argument("operation", StringArgumentType.word())
                                                        .suggests(OPERATION_SUGGESTIONS)
                                                        .then(
                                                            argument("showTooltip", BoolArgumentType.bool())
                                                                .executes { ctx ->
                                                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                                                    if (!canUseHere(p)) return@executes 0

                                                                    val s = p.mainHandStack
                                                                    if (s.isEmpty) {
                                                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                                        return@executes 0
                                                                    }

                                                                    val attrId = getIdentifier(ctx, "attribute")
                                                                    val attr: RegistryEntry<EntityAttribute> =
                                                                        Registries.ATTRIBUTE.getEntry(
                                                                            RegistryKey.of(
                                                                                RegistryKeys.ATTRIBUTE,
                                                                                attrId
                                                                            )
                                                                        ).orElse(null)
                                                                            ?: run {
                                                                                p.sendMessage(
                                                                                    LangManager.msg(
                                                                                        "itemAttrInvalid",
                                                                                        "attribute" to attrId.toString()
                                                                                    )
                                                                                )
                                                                                return@executes 0
                                                                            }

                                                                    val amount = getDouble(ctx, "amount")
                                                                    val slotStr = getString(ctx, "slot").lowercase()
                                                                    val opStr = getString(ctx, "operation").lowercase()
                                                                    val show = getBool(ctx, "showTooltip")

                                                                    val slot = parseSlot(slotStr)
                                                                        ?: run {
                                                                            p.sendMessage(
                                                                                LangManager.msg(
                                                                                    "itemAttrInvalidSlot",
                                                                                    "slot" to slotStr
                                                                                )
                                                                            )
                                                                            return@executes 0
                                                                        }

                                                                    val op = parseOperation(opStr)
                                                                        ?: run {
                                                                            p.sendMessage(
                                                                                LangManager.msg(
                                                                                    "itemAttrInvalidOperation",
                                                                                    "operation" to opStr
                                                                                )
                                                                            )
                                                                            return@executes 0
                                                                        }

                                                                    val id = Identifier.of(
                                                                        "shadowessentials",
                                                                        "itemeditor_" + UUID.randomUUID().toString()
                                                                            .replace("-", "")
                                                                    )

                                                                    val modifier = EntityAttributeModifier(
                                                                        id,
                                                                        amount,
                                                                        op
                                                                    )

                                                                    val entry = AttributeModifiersComponent.Entry(
                                                                        attr,
                                                                        modifier,
                                                                        slot
                                                                    )

                                                                    val cur = s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                                                                    val list = cur?.modifiers?.toMutableList() ?: mutableListOf()
                                                                    list += entry
                                                                    s.set(
                                                                        DataComponentTypes.ATTRIBUTE_MODIFIERS,
                                                                        AttributeModifiersComponent(list, show)
                                                                    )

                                                                    p.sendMessage(
                                                                        LangManager.msg(
                                                                            "itemAttrAdded",
                                                                            "attribute" to attrId.toString(),
                                                                            "amount" to amount.toString(),
                                                                            "slot" to slotStr,
                                                                            "operation" to opStr
                                                                        )
                                                                    )
                                                                    1
                                                                }
                                                        )
                                                )
                                        )
                                )
                        )
                )

                // /itemeditor leathercolor <r> <g> <b> <showTooltip>
                .then(
                    literal("leathercolor")
                        .requires { has(it, "shadowessentials.item.leathercolor") }
                        .then(
                            argument("r", IntegerArgumentType.integer(0, 255))
                                .then(
                                    argument("g", IntegerArgumentType.integer(0, 255))
                                        .then(
                                            argument("b", IntegerArgumentType.integer(0, 255))
                                                .then(
                                                    argument("showTooltip", BoolArgumentType.bool())
                                                        .executes { ctx ->
                                                            val p = requirePlayer(ctx.source) ?: return@executes 0
                                                            if (!canUseHere(p)) return@executes 0

                                                            val s = p.mainHandStack
                                                            if (s.isEmpty) {
                                                                p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                                return@executes 0
                                                            }

                                                            val dyeableLeather =
                                                                s.isOf(Items.LEATHER_HELMET) ||
                                                                        s.isOf(Items.LEATHER_CHESTPLATE) ||
                                                                        s.isOf(Items.LEATHER_LEGGINGS) ||
                                                                        s.isOf(Items.LEATHER_BOOTS) ||
                                                                        s.isOf(Items.WOLF_ARMOR)

                                                            if (!dyeableLeather) {
                                                                p.sendMessage(LangManager.msg("itemNotLeather"))
                                                                return@executes 0
                                                            }

                                                            val r = getInteger(ctx, "r")
                                                            val g = getInteger(ctx, "g")
                                                            val b = getInteger(ctx, "b")
                                                            val show = getBool(ctx, "showTooltip")

                                                            val rgb = ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)

                                                            s.set(
                                                                DataComponentTypes.DYED_COLOR,
                                                                DyedColorComponent(rgb, show)
                                                            )

                                                            p.sendMessage(
                                                                LangManager.msg(
                                                                    "itemLeatherColorSet",
                                                                    "hex" to String.format("#%06X", rgb)
                                                                )
                                                            )
                                                            1
                                                        }
                                                )
                                        )
                                )
                        )
                )

                // /itemeditor potioncolor <r> <g> <b>
                .then(
                    literal("potioncolor")
                        .requires { has(it, "shadowessentials.item.potioncolor") }
                        .then(
                            argument("r", IntegerArgumentType.integer(0, 255))
                                .then(
                                    argument("g", IntegerArgumentType.integer(0, 255))
                                        .then(
                                            argument("b", IntegerArgumentType.integer(0, 255))
                                                .executes { ctx ->
                                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                                    if (!canUseHere(p)) return@executes 0

                                                    val s = p.mainHandStack
                                                    if (s.isEmpty) {
                                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                        return@executes 0
                                                    }

                                                    val r = getInteger(ctx, "r")
                                                    val g = getInteger(ctx, "g")
                                                    val b = getInteger(ctx, "b")
                                                    val rgb = ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)

                                                    val cur = s.get(DataComponentTypes.POTION_CONTENTS)
                                                        ?: PotionContentsComponent.DEFAULT

                                                    val updated = PotionContentsComponent(
                                                        cur.potion(),
                                                        Optional.of(rgb),
                                                        cur.customEffects()
                                                    )

                                                    s.set(DataComponentTypes.POTION_CONTENTS, updated)
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemPotionColorSet",
                                                            "hex" to String.format("#%06X", rgb)
                                                        )
                                                    )
                                                    1
                                                }
                                        )
                                )
                        )
                )

                // /itemeditor fireworkpower <power>
                .then(
                    literal("fireworkpower")
                        .requires { has(it, "shadowessentials.item.fireworkpower") }
                        .then(
                            argument("power", IntegerArgumentType.integer(0, 3))
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val s = p.mainHandStack
                                    if (s.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }
                                    if (s.item != Items.FIREWORK_ROCKET) {
                                        p.sendMessage(LangManager.msg("itemNotFirework"))
                                        return@executes 0
                                    }

                                    val power = getInteger(ctx, "power").coerceIn(0, 3)
                                    val fw = s.get(DataComponentTypes.FIREWORKS)
                                    val explosions = fw?.explosions ?: listOf()
                                    s.set(DataComponentTypes.FIREWORKS, FireworksComponent(power, explosions))

                                    p.sendMessage(
                                        LangManager.msg(
                                            "itemFireworkPowerSet",
                                            "value" to power.toString()
                                        )
                                    )
                                    1
                                }
                        )
                )

                // /itemeditor trim <pattern> <material>
                .then(
                    literal("trim")
                        .requires { has(it, "shadowessentials.item.trim") }
                        .then(
                            argument("pattern", IdentifierArgumentType.identifier())
                                .suggests(TRIM_PATTERN_SUGGESTIONS)
                                .then(
                                    argument("material", IdentifierArgumentType.identifier())
                                        .suggests(TRIM_MATERIAL_SUGGESTIONS)
                                        .executes { ctx ->
                                            val p = requirePlayer(ctx.source) ?: return@executes 0
                                            if (!canUseHere(p)) return@executes 0

                                            val s = p.mainHandStack
                                            if (s.isEmpty) {
                                                p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                return@executes 0
                                            }
                                            if (s.item !is net.minecraft.item.ArmorItem) {
                                                p.sendMessage(LangManager.msg("itemNotArmor"))
                                                return@executes 0
                                            }

                                            val patId = getIdentifier(ctx, "pattern")
                                            val matId = getIdentifier(ctx, "material")

                                            val patternReg = p.server.registryManager.get(RegistryKeys.TRIM_PATTERN)
                                            val materialReg = p.server.registryManager.get(RegistryKeys.TRIM_MATERIAL)

                                            val patKey = RegistryKey.of(RegistryKeys.TRIM_PATTERN, patId)
                                            val matKey = RegistryKey.of(RegistryKeys.TRIM_MATERIAL, matId)

                                            val patEntry = patternReg.getEntry(patKey).orElse(null)
                                                ?: run {
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemTrimInvalidPattern",
                                                            "pattern" to patId.toString()
                                                        )
                                                    )
                                                    return@executes 0
                                                }
                                            val matEntry = materialReg.getEntry(matKey).orElse(null)
                                                ?: run {
                                                    p.sendMessage(
                                                        LangManager.msg(
                                                            "itemTrimInvalidMaterial",
                                                            "material" to matId.toString()
                                                        )
                                                    )
                                                    return@executes 0
                                                }

                                            val trim = ArmorTrim(matEntry, patEntry)
                                            s.set(DataComponentTypes.TRIM, trim)

                                            p.sendMessage(
                                                LangManager.msg(
                                                    "itemTrimApplied",
                                                    "pattern" to patId.toString(),
                                                    "material" to matId.toString()
                                                )
                                            )
                                            1
                                        }
                                )
                        )
                )

                // /itemeditor modeldata set/clear
                .then(
                    literal("modeldata")
                        .requires { has(it, "shadowessentials.item.modeldata") }
                        .then(
                            literal("set")
                                .then(
                                    argument("value", IntegerArgumentType.integer(0))
                                        .executes { ctx ->
                                            val p = requirePlayer(ctx.source) ?: return@executes 0
                                            if (!canUseHere(p)) return@executes 0

                                            val s = p.mainHandStack
                                            if (s.isEmpty) {
                                                p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                                return@executes 0
                                            }

                                            val value = getInteger(ctx, "value")
                                            s.set(
                                                DataComponentTypes.CUSTOM_MODEL_DATA,
                                                CustomModelDataComponent(value)
                                            )
                                            p.sendMessage(
                                                LangManager.msg(
                                                    "itemModelDataSet",
                                                    "value" to value.toString()
                                                )
                                            )
                                            1
                                        }
                                )
                        )
                        .then(
                            literal("clear")
                                .executes { ctx ->
                                    val p = requirePlayer(ctx.source) ?: return@executes 0
                                    if (!canUseHere(p)) return@executes 0

                                    val s = p.mainHandStack
                                    if (s.isEmpty) {
                                        p.sendMessage(LangManager.msg("itemNoItemInHand"))
                                        return@executes 0
                                    }

                                    s.remove(DataComponentTypes.CUSTOM_MODEL_DATA)
                                    p.sendMessage(LangManager.msg("itemModelDataCleared"))
                                    1
                                }
                        )
                )

            val rootNode = dispatcher.register(root)

            dispatcher.register(
                literal("ie").redirect(rootNode)
            )
        }
    }

    private fun parseSlot(s: String): AttributeModifierSlot? = when (s) {
        "any"      -> AttributeModifierSlot.ANY
        "mainhand" -> AttributeModifierSlot.MAINHAND
        "offhand"  -> AttributeModifierSlot.OFFHAND
        "head"     -> AttributeModifierSlot.HEAD
        "chest"    -> AttributeModifierSlot.CHEST
        "legs"     -> AttributeModifierSlot.LEGS
        "feet"     -> AttributeModifierSlot.FEET
        else -> null
    }

    private fun parseOperation(s: String): Operation? = when (s) {
        "add", "add_value", "addition"             -> Operation.ADD_VALUE
        "base_mult", "multiply_base"               -> Operation.ADD_MULTIPLIED_BASE
        "total_mult", "add_mult", "multiply_total" -> Operation.ADD_MULTIPLIED_TOTAL
        else -> null
    }
}