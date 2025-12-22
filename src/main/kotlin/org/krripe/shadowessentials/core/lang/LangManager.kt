package org.krripe.shadowessentials.core.lang

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.text.CustomColorParser
import org.krripe.shadowessentials.core.text.PlaceholderEngine
import org.krripe.shadowessentials.core.config.JsoncReader
import java.io.File

object LangManager {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val langDir = File("config/ShadowEssentials/lang")

    @Volatile
    private var currentCode: String = "en_us"

    @Volatile
    private var messages: Map<String, String> = emptyMap()

    fun init(defaultLang: String = "en_us") {
        currentCode = defaultLang.lowercase()
        ensureDefaultFiles()
        loadLanguage(currentCode)
    }

    fun reload() {
        ensureDefaultFiles()
        loadLanguage(currentCode)
    }

    fun setLanguage(code: String): Boolean {
        val file = File(langDir, "${code.lowercase()}.jsonc")
        if (!file.exists()) return false
        currentCode = code.lowercase()
        loadLanguage(currentCode)
        return true
    }

    fun currentLanguage(): String = currentCode

    fun availableLanguages(): List<String> {
        if (!langDir.exists()) return emptyList()
        return langDir.listFiles { f -> f.isFile && f.name.endsWith(".jsonc") }
            ?.map { it.nameWithoutExtension.lowercase() }
            ?.sorted()
            ?: emptyList()
    }

    fun msg(key: String, vararg placeholders: Pair<String, Any?>): Text {
        val raw = messages[key] ?: key
        val prefixValue = messages["prefix"] ?: ""
        val list = placeholders.toMutableList()
        if (list.none { it.first == "prefix" }) {
            list.add("prefix" to prefixValue)
        }
        val merged = list.toTypedArray()

        val withPh =
            if (merged.isNotEmpty())
                PlaceholderEngine.apply(raw, *merged)
            else
                raw

        return CustomColorParser.processAllFormats(withPh)
    }

    fun raw(key: String, vararg placeholders: Pair<String, Any?>): String {
        val raw = messages[key] ?: key

        val prefixValue = messages["prefix"] ?: ""

        val list = placeholders.toMutableList()
        if (list.none { it.first == "prefix" }) {
            list.add("prefix" to prefixValue)
        }
        val merged = list.toTypedArray()

        return if (merged.isNotEmpty())
            PlaceholderEngine.apply(raw, *merged)
        else
            raw
    }

    private fun writeLangFile(code: String, defaults: Map<String, String>) {
        if (!langDir.exists()) langDir.mkdirs()
        val file = File(langDir, "$code.jsonc")

        val existing: Map<String, String> = if (file.exists()) {
            val raw = JsoncReader.readJsonc(file)
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(raw, type) ?: emptyMap()
        } else {
            emptyMap()
        }

        val merged = linkedMapOf<String, String>()
        for ((k, v) in defaults) {
            merged[k] = existing[k] ?: v
        }

        file.writeText(gson.toJson(merged))
    }


    private fun ensureDefaultFiles() {
        if (!langDir.exists()) langDir.mkdirs()

        val defaultsEn = mapOf(
            "prefix"          to "&8[<gradient:#543383:#7B1DFF>SEssentials</gradient>&8]&r ",
            "noPermission"    to "%prefix% &cYou don't have permission for &e%perm%&c.",
            "reloadSuccess"   to "%prefix% &aConfig & language reloaded.",
            "commandDisabled"        to "%prefix% &cThis command is disabled on this server.",
            "commandWorldNotAllowed" to "%prefix% &cYou cannot use this command in this world.",

            "teleportFailed"  to "&cTeleport failed.",
            "tpDelayStart"    to "%prefix% &7Teleporting in &e%time% &7seconds...",
            "tpDelayTick"     to "&e%left% &7seconds left...",

            // GAMEMODE
            "gamemodeDisabled"      to "%prefix% &cGamemode commands are disabled on this server.",
            "gamemodeWorldNotAllowed" to "%prefix% &cYou cannot change gamemode in this world.",
            "gmInvalidTarget"       to "%prefix% &cThat player is not online.",
            "gmChangeSuccessSelf"   to "%prefix% &aYour gamemode was set to &e%mode%&a.",
            "gmChangeSuccessOther"  to "%prefix% &aSet gamemode of &e%target% &ato &e%mode%&a.",

            // SPAWN
            "spawnSet"        to "%prefix% &aSpawn set.",
            "spawnTeleport"   to "%prefix% &aTeleported to spawn.",
            "spawnDisabled"   to "%prefix% &cThe /spawn command is disabled.",
            "spawnOnCooldown" to "%prefix% &cYou must wait &e%time% &cseconds before using &e/spawn &cagain.",

            // BACK
            "backDisabled"      to "%prefix% &cThe /back command is disabled.",
            "backOnCooldown"    to "%prefix% &cYou must wait &e%time% &cseconds before using &e/back &cagain.",
            "backNoLocation"    to "%prefix% &cNo previous location saved.",
            "backTeleport"      to "%prefix% &aTeleported to your previous location.",
            "backWorldNotAllowed" to "%prefix% &cYou cannot use /back in this world.",
            "backDailyLimit"    to "&%prefix% cYou have reached the daily limit of &e%max% &c/back uses.",

            // TPA
            "tpaDisabled"         to "%prefix% &cTPA is disabled.",
            "tpaTargetOffline"    to "%prefix% &cThat player is not online.",
            "tpaSelf"             to "%prefix% &cYou cannot send a TPA request to yourself.",
            "tpaOnCooldown"       to "%prefix% &cYou must wait &e%time% &cseconds before sending another TPA.",
            "tpaRequestSent"      to "%prefix% &aTPA request sent to &e%target%&a. Expires in &e%time% &aseconds.",
            "tpaHereRequestSent"  to "%prefix% &aTPA-here request sent to &e%target%&a. Expires in &e%time% &aseconds.",
            "tpaRequestReceived"  to "%prefix% &e%player% &7wants to teleport to you. Use &a/tpaccept &7or &c/tpdeny&7.",
            "tpaHereRequestReceived" to "%prefix% &e%player% &7wants you to teleport to them. Use &a/tpaccept &7or &c/tpdeny&7.",
            "tpaNoRequest"        to "%prefix% &cYou don't have any pending TPA requests.",
            "tpaRequestExpired"   to "%prefix% &cYour TPA request has expired.",
            "tpaWorldNotAllowed"  to "%prefix% &cYou cannot use TPA in this world.",
            "tpaAcceptedSender"   to "%prefix% &aYour TPA request has been accepted.",
            "tpaAcceptedTarget"   to "%prefix% &aYou accepted TPA request from &e%player%&a.",
            "tpaDeniedSender"     to "%prefix% &cYour TPA request was denied by &e%player%&c.",
            "tpaDeniedTarget"     to "%prefix% &cYou denied TPA request from &e%player%&c.",
            "tpaTeleportSuccess"  to "%prefix% &aTeleported to &e%player%&a.",
            "tpaTeleportHereSuccess" to "%prefix% &aTeleported &e%player% &ato you.",
            "tpaCancelNoRequest"      to "%prefix% &cYou don't have any outgoing TPA request to cancel.",
            "tpaCancelSuccessSender"  to "%prefix% &aYour TPA request to &e%target% &ahas been cancelled.",
            "tpaCancelSuccessTarget"  to "%prefix% &e%player% &ccancelled the TPA request.",
            "tpallOnCooldown"        to "%prefix% &cYou must wait &e%time% &cseconds before using &e/tpall &cagain.",
            "tpallNoTargets"         to "%prefix% &cThere are no players to teleport.",
            "tpallTeleporting"       to "%prefix% &7Teleporting &e%count% &7players to you...",
            "tpallTeleportedTarget"  to "%prefix% &aYou have been teleported to &e%player%&a.",
            "tpallDone"              to "%prefix% &aTeleport finished.",
            "tpaRequestExpiredSender" to "&cYour teleport request to &e%player% &chas expired.",
            "tpaRequestExpiredTarget" to "&cThe teleport request from &e%player% &chas expired.",

            // FLY
            "flyNotAllowed"      to "%prefix% &cFly is disabled on this server.",
            "flyWorldNotAllowed" to "%prefix% &cYou cannot use fly in this world.",
            "flyCooldown"        to "%prefix% &cYou must wait &e%time% &cmore seconds before using &e/fly&c.",
            "flyEnabled"         to "%prefix% &aFlight enabled.",
            "flyDisabled"        to "%prefix% &cFlight disabled.",
            "flyEnabledOther"    to "%prefix% &aFlight enabled for &e%player%&a.",
            "flyDisabledOther"   to "%prefix% &cFlight disabled for &e%player%&c.",
            "flyToggledOther"    to "%prefix% &aToggled flight for &e%player%&a.",

            // TEMPFLY
            "tempflyEnabled"       to "%prefix% &aTempfly enabled.",
            "tempflyDisabled"      to "%prefix% &cTempfly disabled.",
            "tempflyBar"           to "%prefix% &eTempfly: &f%time%",
            "tempflyExpireSoon"    to "%prefix% &cYour tempfly will expire in &e%time%&c.",
            "tempflyDailyReceived"  to "%prefix% &aYou received &e%time% &aof daily flight.",
            "tempflyNotAllowed"     to "%prefix% &cTempfly is disabled on this server.",
            "tempflyWorldNotAllowed" to "%prefix% &cYou cannot use tempfly in this world.",
            "tempflyMaxDuration"    to "%prefix% &cYou cannot give more than &e%max% &cof flight.",
            "tempflyGiven"          to "%prefix% &aYou received &e%time% &aof flight.",
            "tempflyGivenOther"     to "%prefix% &aGave &e%time% &aof flight to &e%player%&a.",
            "tempflyExpired"        to "%prefix% &cYour flight time has expired.",
            "tempflyRemaining"      to "%prefix% &7You have &e%time% &7of flight remaining.",
            "tempflyNone"           to "%prefix% &cYou don't have temporary flight.",
            "tempflyCleared"        to "%prefix% &aYour temporary flight was cleared.",
            "tempflyClearedOther"   to "%prefix% &aCleared temporary flight for &e%player%&a.",
            "tempflyCooldown"       to "%prefix% &cYou must wait &e%time% &cmore seconds before toggling tempfly.",
            "tempflyInfo"           to "%prefix% &7You currently have &e%time% &7of flight available.",
            "tempflyNoTime"         to "%prefix% &cYou don't have any tempfly time.",
            "tempflyInvalidTime"    to "%prefix% &cInvalid time format. Use like &e10s&c, &e5m&c, &e1h&c, &e1d&c.",
            "tempflySet"            to "%prefix% &aYour tempfly time was set to &e%time%&a.",
            "tempflySetOther"       to "%prefix% &aSet tempfly time of &e%player% &ato &e%time%&a.",
            "tempflyExtracted"      to "%prefix% &aRemoved &e%time% &afrom your tempfly.",
            "tempflyExtractedOther" to "%prefix% &aRemoved tempfly time from &e%player%&a. New total: &e%time%&a.",
            "tempflyLeft"           to "%prefix% &7You currently have &e%time% &7of tempfly left.",

            // HOMES
            "homeDisabled"          to "%prefix% &cHomes are disabled on this server.",
            "homeOnCooldown"        to "%prefix% &cYou must wait &e%time% &cmore seconds before using &e/home&c.",
            "homeWorldNotAllowed"   to "%prefix% &cYou cannot set homes in this world.",
            "homeWorldBlacklisted"  to "%prefix% &cYou cannot use home commands in this world.",
            "homeTargetBlacklisted" to "%prefix% &cYou cannot teleport to that home because its world is blocked.",
            "homeLimitReached"      to "%prefix% &cYou reached the maximum of &e%limit% &chomes.",
            "homeNotFound"          to "%prefix% &cHome &e%home% &cdoes not exist.",
            "homeDeleted"           to "%prefix% &aHome &e%home% &ahas been deleted.",
            "homeListEmpty"         to "%prefix% &cYou don't have any homes.",
            "homeGuiTitle"          to "%prefix% &8Your Homes",
            "homeSet"               to "%prefix% &aHome &e%home% &ahas been set.",
            "homeTeleport"          to "%prefix% &aTeleported to home &e%home%&a.",
            "homeItemName"          to "&aHome &e%home%",
            "homeItemLoreWorld"     to "&7World: &f%world%",
            "homeItemLorePos"       to "&7Location: &f%x%&7, &f%y%&7, &f%z%",
            "homeItemLoreCreated"   to "&7Created: &f%date%",

            // INVSEE
            "invseeTitle"           to "%prefix% &8Inventory of &e%player%",
            "invseeDisabled"        to "%prefix% &cInvsee is disabled on this server.",
            "invseeWorldNotAllowed" to "%prefix% &cYou cannot use invsee in this world.",
            "invseeOfflineNotFound" to "%prefix% &cThat player has never joined this server.",
            "invseeOfflineNoData"   to "%prefix% &cNo saved inventory data found for &e%player%&c.",

            // ENCHANT
            "enchantNoItem"          to "%prefix% &cYou must hold an item in your main hand.",
            "enchantInvalid"         to "%prefix% &cUnknown enchantment &e%enchant%&c.",
            "enchantNotApplicable"   to "%prefix% &cThat enchantment cannot be applied to this item.",
            "enchantSet"             to "%prefix% &aAdded &e%enchant% &alevel &e%level% &ato your item.",

            // SPEED
            "speedWalk"              to "%prefix% &aWalk speed set to &e%value%x&a.",
            "speedFly"               to "%prefix% &aFly speed set to &e%value%x&a.",
            "speedReset"             to "%prefix% &aYour speed has been reset.",

            // REPAIR
            "repairNoItem"           to "%prefix% &cYou must hold a damageable item.",
            "repairDone"             to "%prefix% &aItem repaired.",
            "repairAllDone"          to "%prefix% &aRepaired &e%count% &aitems in your inventory.",

            // MISC
            "healSelf"               to "%prefix% &aYou have been healed.",
            "healOther"              to "%prefix% &aYou healed &e%player%&a.",
            "feedSelf"               to "%prefix% &aYou have been fed.",
            "feedOther"              to "%prefix% &aYou fed &e%player%&a.",
            "clearSelf"              to "%prefix% &aCleared &e%count% &aitems from your inventory.",
            "clearOther"             to "%prefix% &aCleared &e%count% &aitems from &e%player%&a's inventory.",
            "nearNone"               to "%prefix% &cNo players nearby.",
            "nearList"               to "%prefix% &7Nearby: &f%list%",
            "enderOpened"            to "%prefix% &aOpened your ender chest.",
            "seenOnline"             to "%prefix% &a%player% is currently online.",
            "seenNever"              to "%prefix% &cNever seen player &e%player%&c.",
            "seenResult"             to "%prefix% &e%player% &7was last seen &b%time% &7ago.",

            // ItemEditor
            "itemNoItemInHand"        to "%prefix% &cYou must hold an item in your main hand.",
            "itemRenamed"             to "%prefix% &aItem name set to &r%value%.",
            "itemNameCleared"         to "%prefix% &aItem name cleared.",
            "itemLoreIndexInvalid"    to "%prefix% &cInvalid lore line index.",
            "itemLoreAddedAt"         to "%prefix% &aAdded lore line at &e#index%&a.",
            "itemLoreRemovedAt"       to "%prefix% &aRemoved lore line &e%index%&a.",
            "itemLoreEdited"          to "%prefix% &aEdited lore line &e%index%&a.",
            "itemLoreCleared"         to "%prefix% &aCleared all lore.",
            "itemAmountSet"           to "%prefix% &aStack amount set to &e%value%&a.",
            "itemUnbreakableSet"      to "%prefix% &aUnbreakable set to &e%value%&a.",
            "itemNotDamageable"       to "%prefix% &cThis item does not have durability.",
            "itemDurabilitySet"       to "%prefix% &aRemaining durability set to &e%remaining%&a.",
            "itemEnchantNoItem"       to "%prefix% &cYou must hold an item to enchant.",
            "itemEnchantInvalid"      to "%prefix% &cUnknown enchantment &e%enchant%&c.",
            "itemEnchantNotApplicable" to "%prefix% &cThat enchantment cannot be applied to this item.",
            "itemEnchantSet"          to "%prefix% &aApplied enchantment &e%enchant% &alevel &e%level%&a.",
            "itemAttrInvalid"         to "%prefix% &cUnknown attribute &e%attribute%&c.",
            "itemAttrInvalidSlot"     to "%prefix% &cInvalid attribute slot &e%slot%&c.",
            "itemAttrInvalidOperation" to "%prefix% &cInvalid attribute operation &e%operation%&c.",
            "itemAttrAdded"           to "%prefix% &aAdded attribute &e%attribute% &a(&e%amount%&a, &e%slot%&a, &e%operation%&a).",
            "itemNotLeather"          to "%prefix% &cThis item cannot be dyed like leather armor.",
            "itemLeatherColorSet"     to "%prefix% &aLeather color set to &e%hex%&a.",
            "itemPotionColorSet"      to "%prefix% &aPotion color set to &e%hex%&a.",
            "itemNotFirework"         to "%prefix% &cYou must hold a firework rocket.",
            "itemFireworkPowerSet"    to "%prefix% &aFirework power set to &e%value%&a.",
            "itemNotArmor"            to "%prefix% &cYou must hold an armor item.",
            "itemTrimInvalidPattern"  to "%prefix% &cInvalid trim pattern &e%pattern%&c.",
            "itemTrimInvalidMaterial" to "%prefix% &cInvalid trim material &e%material%&c.",
            "itemTrimApplied"         to "%prefix% &aApplied armor trim &e%pattern% &awith material &e%material%&a.",
            "itemModelDataSet"        to "%prefix% &aCustom model data set to &e%value%&a.",
            "itemModelDataCleared"    to "%prefix% &aCustom model data cleared.",

            // Warps
            "warpDisabled"            to "%prefix% &cWarp module is disabled.",
            "warpWorldBlacklisted"    to "%prefix% &cYou cannot use warps in this world.",
            "warpWorldNotAllowed"     to "%prefix% &cYou cannot set warps in this world.",
            "warpSet"                 to "%prefix% &aWarp &e%warp% &ahas been set.",
            "warpDeleted"             to "%prefix% &cWarp &e%warp% &chas been deleted.",
            "warpNotFound"            to "%prefix% &cWarp &e%warp% &cwas not found.",
            "warpTargetBlacklisted"   to "%prefix% &cThat warp is in a blacklisted world.",
            "warpOnCooldown"          to "%prefix% &cYou must wait &e%time%s &cbefore using warps again.",
            "warpTeleport"            to "%prefix% &aTeleported to warp &e%warp%&a.",
            "warpNoPermissionForWarp" to "%prefix% &cYou don't have permission (&e%perm%&c) to use warp &e%warp%&c.",
            "warpPermissionSet"       to "%prefix% &aWarp &e%warp% &anow requires permission &e%perm%&a.",
            "warpPermissionCleared"   to "%prefix% &aWarp &e%warp% &ano longer requires a permission.",
            "warpsEmpty"              to "%prefix% &7There are no warps set.",
            "warpsList"               to "%prefix% &6Warps &8(&7%page%/%max%&8)&7: &f%list%",
            "warpEditorEnterPermission" to "&6Warp permission &8» &e%warp%",
            "warpEditorPermSet"       to "%prefix% &aPermission for warp &e%warp% &aset to &e%permission%&a.",
            "warpEditorPermCleared"   to "%prefix% &aPermission for warp &e%warp% &ahas been cleared.",
            "warpEditorEnterDisplayName" to "&6Warp name &8» &e%warp%",
            "warpEditorEnterLoreTitle" to "&6Warp lore &8» &e%warp%",
            "warpEditorIconNoItem" to "%prefix% &cYou must hold an item in your main hand to set the warp icon.",
            "warpEditorRelocated" to "%prefix% &aWarp &e%warp% &ahas been relocated to your current position.",
            "warpEditorNameSet" to "%prefix% &aDisplay name for warp &e%warp% &aset to &r%name%&a.",
            "warpEditorNameCleared" to "%prefix% &aDisplay name for warp &e%warp% &ahas been cleared.",
            "warpEditorLoreSet" to "%prefix% &aLore for warp &e%warp% &aset (&e%lines% &alines).",
            "warpEditorIconSet" to "%prefix% &aIcon for warp &e%warp% &aset to &e%icon%&a.",
            "warpEditorIconCleared" to "%prefix% &aIcon for warp &e%warp% &ahas been cleared.",

            // Anvil
            "anvilLinesTitle"            to "&6Lines &8(&e%count%&7/&e%max%&8)",
            "anvilLinesEmpty"            to "&7No lines added yet.",
            "anvilLinesEntrySelected"    to "&e» &f%text%",
            "anvilLinesEntryUnselected"  to "&8%index%. &7%text%",
            "anvilLClickSelectEdit"      to "&7Left-click: &eSelect / edit next line",
            "anvilRClickRemoveSelected"  to "&7Right-click: &cRemove selected line",

            "anvilDoneTitleDisabled"     to "&cDone",
            "anvilDoneTitleEnabled"      to "&aDone",
            "anvilDoneLore"              to "&7Click to finish editing.",

            "anvilSaveTitle"             to "&aSave line",
            "anvilAddTitle"              to "&aAdd line",
            "anvilTypeAbove"             to "&7Type the text in the anvil field above.",
            "anvilCurrentText"           to "&7Current: &f%text%",
            "anvilClickToSave"           to "&7Click to save this line.",
            "anvilBlankRemoves"          to "&7Blank text will &cremove &7this line.",
            "anvilClickToAdd"            to "&7Click to add this line.",

            )

        writeLangFile("en_us", defaultsEn)
    }

    private fun loadLanguage(code: String) {
        val base = loadFile("en_us")
        val override =
            if (code.lowercase() != "en_us") loadFile(code.lowercase()) else emptyMap()

        messages = base.toMutableMap().apply { putAll(override) }
    }

    private fun loadFile(code: String): Map<String, String> {
        val file = File(langDir, "$code.jsonc")
        if (!file.exists()) return emptyMap()

        val raw = JsoncReader.readJsonc(file)
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson<Map<String, String>>(raw, type) ?: emptyMap()
    }
}