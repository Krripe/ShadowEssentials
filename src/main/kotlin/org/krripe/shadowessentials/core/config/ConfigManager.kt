package org.krripe.shadowessentials.core.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File

data class TeleportSettings(
    val sound: String? = "minecraft:entity.enderman.teleport",
    val particles: String? = "minecraft:portal",
    val dingSound: String? = "minecraft:block.note_block.pling"
)

data class GamemodeSettings(
    val enabled: Boolean = true,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class SpawnSettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = false,
    val cooldownSeconds: Long = 0,
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 3
)

data class BackSettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = true,
    val cooldownSeconds: Long = 10,
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 3,
    val dailyBacksEnabled: Boolean = false,
    val dailyBacks: Int = 3,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class TpaSettings(
    val enabled: Boolean = true,
    val requestTimeoutSeconds: Int = 60,
    val cooldownEnabled: Boolean = true,
    val cooldownSeconds: Long = 5,
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 3,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class FlySettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = false,
    val cooldownSeconds: Long = 0,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class TempFlyDailyTier(
    val permission: String = "",
    val time: String = "0s"   // ex: "30m", "1h", "12h"
)

data class TempFlySettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = false,
    val cooldownSeconds: Long = 0,
    val stackMode: String = "ADD", // RESET / ADD / MAX
    val notifyOnExpire: Boolean = true,
    val maxTime: String = "7200s", // ex: "600s", "10m", "1h", "1d"
    val dailyEnabled: Boolean = true,
    val dailyResetHour: Int = 0,
    val dailyTiers: List<TempFlyDailyTier> = listOf(
        TempFlyDailyTier("shadowessentials.tempfly.daily.tier1", "1h"),
        TempFlyDailyTier("shadowessentials.tempfly.daily.tier2", "12h"),
        TempFlyDailyTier("shadowessentials.tempfly.daily.tier3", "24h")
    ),
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class HomeLimitTier(
    val permission: String = "",
    val max: Int = 0
)

data class HomeSettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = true,
    val cooldownSeconds: Long = 5,
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 3,
    val defaultMaxHomes: Int = 3,
    val perPermissionLimits: List<HomeLimitTier> = listOf(
        HomeLimitTier("shadowessentials.homelimit.5", max = 5),
        HomeLimitTier("shadowessentials.homelimit.8", max = 8),
        HomeLimitTier("shadowessentials.homelimit.10", max = 10)
    ),
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    ),
    val blacklistWorlds: List<String> = emptyList()
)

data class InvseeSettings(
    val enabled: Boolean = true,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    )
)

data class WarpSettings(
    val enabled: Boolean = true,
    val cooldownEnabled: Boolean = true,
    val cooldownSeconds: Long = 5,
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 3,
    val allowWorlds: List<String> = listOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    ),
    val blacklistWorlds: List<String> = emptyList()
)

data class ItemEditorConfig(
    val enabled: Boolean = true,
    val allowWorlds: List<String> = emptyList()
)

data class MiscConfig(
    val enchantEnabled: Boolean = true,
    val enchantAllowWorlds: List<String> = emptyList(),
    val speedEnabled: Boolean = true,
    val speedAllowWorlds: List<String> = emptyList(),
    val repairEnabled: Boolean = true,
    val repairAllowWorlds: List<String> = emptyList(),
    val healEnabled: Boolean = true,
    val healAllowWorlds: List<String> = emptyList(),
    val feedEnabled: Boolean = true,
    val feedAllowWorlds: List<String> = emptyList(),
    val clearEnabled: Boolean = true,
    val clearAllowWorlds: List<String> = emptyList(),
    val nearEnabled: Boolean = true,
    val nearAllowWorlds: List<String> = emptyList(),
    val seenEnabled: Boolean = true,
    val seenAllowWorlds: List<String> = emptyList()
)

data class BlocksConfig(
    val enderchestEnabled: Boolean = true,
    val enderchestAllowWorlds: List<String> = emptyList(),
    val anvilEnabled: Boolean = true,
    val anvilAllowWorlds: List<String> = emptyList(),
    val cartographyEnabled: Boolean = true,
    val cartographyAllowWorlds: List<String> = emptyList(),
    val stonecutterEnabled: Boolean = true,
    val stonecutterAllowWorlds: List<String> = emptyList(),
    val grindstoneEnabled: Boolean = true,
    val grindstoneAllowWorlds: List<String> = emptyList(),
    val loomEnabled: Boolean = true,
    val loomAllowWorlds: List<String> = emptyList(),
    val craftEnabled: Boolean = true,
    val craftAllowWorlds: List<String> = emptyList()
)

data class ShadowEssentialsConfig(
    val language: String = "en_us",
    val gamemode: GamemodeSettings = GamemodeSettings(),
    val teleport: TeleportSettings = TeleportSettings(),
    val spawn: SpawnSettings = SpawnSettings(),
    val back: BackSettings = BackSettings(),
    val tpa: TpaSettings = TpaSettings(),
    val fly: FlySettings = FlySettings(),
    val tempfly: TempFlySettings = TempFlySettings(),
    val home: HomeSettings = HomeSettings(),
    val warp: WarpSettings = WarpSettings(),
    val invsee: InvseeSettings = InvseeSettings(),
    val itemEditor: ItemEditorConfig = ItemEditorConfig(),
    val misc: MiscConfig = MiscConfig(),
    val blocks: BlocksConfig = BlocksConfig()
)

object ConfigManager {

    private val configFile = File("config/ShadowEssentials/config.jsonc")

    @Volatile
    var config: ShadowEssentialsConfig = ShadowEssentialsConfig()
        private set

    fun load() {
        val defaultCfg = ShadowEssentialsConfig()
        if (!configFile.exists()) {
            config = defaultCfg
            val json = JsonConfigLoader.gson.toJson(defaultCfg)
            configFile.parentFile.mkdirs()
            configFile.writeText(addCommentsToConfig(json))
            return
        }

        val rawJson = JsoncReader.readJsonc(configFile)
        val defaultJson = JsonConfigLoader.gson
            .toJsonTree(defaultCfg)
            .asJsonObject

        val existingElement: JsonElement? = runCatching {
            JsonConfigLoader.gson.fromJson(rawJson, JsonElement::class.java)
        }.getOrNull()

        val existingJson = existingElement?.asJsonObject ?: JsonObject()
        val merged = mergeJsonObjects(existingJson, defaultJson)
        config = JsonConfigLoader.gson.fromJson(merged, ShadowEssentialsConfig::class.java)

        configFile.parentFile.mkdirs()
        val mergedJsonStr = JsonConfigLoader.gson.toJson(merged)
        configFile.writeText(addCommentsToConfig(mergedJsonStr))
    }

    fun save() {
        configFile.parentFile.mkdirs()
        val json = JsonConfigLoader.gson.toJson(config)
        configFile.writeText(addCommentsToConfig(json))
    }

    private fun mergeJsonObjects(existing: JsonObject, defaults: JsonObject): JsonObject {
        val result = JsonObject()

        for ((key, defVal) in defaults.entrySet()) {
            if (existing.has(key)) {
                val exVal = existing.get(key)
                val mergedVal = when {
                    defVal.isJsonObject && exVal.isJsonObject ->
                        mergeJsonObjects(exVal.asJsonObject, defVal.asJsonObject)

                    defVal.isJsonArray && exVal.isJsonArray ->
                        exVal

                    defVal.isJsonPrimitive && exVal.isJsonPrimitive ->
                        exVal

                    else ->
                        defVal
                }
                result.add(key, mergedVal)
            } else {
                result.add(key, defVal)
            }
        }

        return result
    }

    private fun addCommentsToConfig(json: String): String {
        val lines = json.lines()
        val out = mutableListOf<String>()

        var headerAdded = false

        for (line in lines) {
            val trimmed = line.trimStart()

            if (!headerAdded && trimmed.startsWith("\"language\"")) {
                out += "// ========================================"
                out += "//  ShadowEssentials - Main configuration"
                out += "// ========================================"
                out += "//"
                out += "//  Language: code for language file from"
                out += "//      config/ShadowEssentials/lang"
                out += "//              ex: \"en_us\""
                out += "//"
                headerAdded = true
            }

            when {
                trimmed.startsWith("\"gamemode\"") -> {
                    out += "  // Gamemode MODULE"
                    out += "  // Request-based Gamemode system: /gamemode, /gm, /gmc, /gms, /gma, /gmsp."
                    out += "  //  - enabled: turn the /gamemode commands on/off"
                    out += "  //  - allowWorlds: list of worlds where /gamemode is allowed to work"
                }

                trimmed.startsWith("\"teleport\"") -> {
                    out += "  // TELEPORT SETTINGS"
                    out += "  // Used by ALL teleports in the mod (spawn, back, homes, tpa, tempfly etc.)."
                    out += "  //  - sound:      sound played when a teleport happens"
                    out += "  //  - particles:  particle effect at the teleport location"
                    out += "  //  - dingSound:  short sound used for countdown ticks before teleport"
                }
                trimmed.startsWith("\"spawn\"") -> {
                    out += "  // SPAWN MODULE"
                    out += "  // Controls behaviour of the /spawn command."
                    out += "  //  - enabled:         turn the /spawn command on/off"
                    out += "  //  - cooldownEnabled: enable/disable global cooldown between uses"
                    out += "  //  - cooldownSeconds: how long a player must wait before reusing /spawn"
                    out += "  //  - delayEnabled:    enable/disable teleport delay (countdown) for /spawn"
                    out += "  //  - delaySeconds:    how many seconds the delay lasts before teleport"
                }
                trimmed.startsWith("\"back\"") -> {
                    out += "  // BACK MODULE"
                    out += "  // Controls /back, which teleports a player to their last saved location"
                    out += "  // (usually death or last teleport, depending on how you integrate it)."
                    out += "  //  - enabled:          turn the /back command on/off"
                    out += "  //  - cooldownEnabled:  enable/disable cooldown between /back uses"
                    out += "  //  - cooldownSeconds:  seconds between allowed /back uses"
                    out += "  //  - delayEnabled:     enable/disable teleport delay (countdown) for /back"
                    out += "  //  - delaySeconds:     delay in seconds before /back actually teleports"
                    out += "  //  - dailyBacksEnabled: if true, /back has a limited number of uses per day"
                    out += "  //  - dailyBacks:       how many /back uses each player gets per day"
                    out += "  //  - allowWorlds:      list of worlds where /back is allowed to work"
                }
                trimmed.startsWith("\"tpa\"") -> {
                    out += "  // TPA MODULE"
                    out += "  // Request-based teleport system: /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel."
                    out += "  // Players can send teleport requests to each other instead of instant teleports."
                    out += "  //  - enabled:              turn the whole TPA system on/off"
                    out += "  //  - requestTimeoutSeconds: how long a TPA request stays valid before expiring"
                    out += "  //  - cooldownEnabled:      enable/disable cooldown for sending new requests"
                    out += "  //  - cooldownSeconds:      seconds between allowed TPA requests from same player"
                    out += "  //  - delayEnabled:         enable/disable teleport delay after a request is accepted"
                    out += "  //  - delaySeconds:         countdown time before teleport after /tpaccept"
                    out += "  //  - allowWorlds:          list of worlds where TPA is allowed for both players"
                }
                trimmed.startsWith("\"fly\"") -> {
                    out += "  // FLY MODULE"
                    out += "  // Controls the permanent flight toggle using /fly."
                    out += "  //  - enabled:          turn /fly system on/off completely"
                    out += "  //  - cooldownEnabled:  enable/disable cooldown when toggling fly"
                    out += "  //  - cooldownSeconds:  how long a player must wait before toggling /fly again"
                    out += "  //  - allowWorlds:      list of worlds where /fly is allowed to be used"
                }
                trimmed.startsWith("\"tempfly\"") -> {
                    out += "  // TEMPFLY MODULE"
                    out += "  // Time-limited flight system using /tempfly (and aliases like /tf)."
                    out += "  // Players have a pool of flight time (seconds) which is consumed while flying."
                    out += "  //  - enabled:          turn the tempfly system on/off"
                    out += "  //  - cooldownEnabled:  apply a cooldown between toggling tempfly on/off"
                    out += "  //  - cooldownSeconds:  how long a player must wait before toggling tempfly again"
                    out += "  //  - stackMode:        how new time is added: \"RESET\", \"ADD\" or \"MAX\""
                    out += "  //                      RESET = replace time; ADD = add time; MAX = keep higher value"
                    out += "  //  - notifyOnExpire:   if true, player gets messages when flight is about to expire"
                    out += "  //  - maxTime:          maximum allowed stored flight time (e.g. \"7200s\", \"2h\")"
                    out += "  //  - dailyEnabled:     if true, players can claim daily flight based on permissions"
                    out += "  //  - dailyResetHour:   hour of the day (0–23) when daily rewards reset"
                    out += "  //  - dailyTiers:       list of { permission, time } objects for daily tempfly tiers"
                    out += "  //  - allowWorlds:      list of worlds where tempfly is allowed and counted"
                }
                trimmed.startsWith("\"home\"") -> {
                    out += "  // HOMES MODULE"
                    out += "  // Personal home system: /home, /sethome, /delhome, /homes (GUI)."
                    out += "  // Players can save named locations and teleport back to them."
                    out += "  //  - enabled:            turn the homes system on/off"
                    out += "  //  - cooldownEnabled:    enable/disable cooldown between /home teleports"
                    out += "  //  - cooldownSeconds:    seconds between allowed /home teleports"
                    out += "  //  - delayEnabled:       enable/disable teleport delay (countdown) for /home"
                    out += "  //  - delaySeconds:       delay in seconds before teleporting to a home"
                    out += "  //  - defaultMaxHomes:    default number of homes per player (without extra perms)"
                    out += "  //  - perPermissionLimits: map permission -> max homes (overrides default if higher)"
                    out += "  //  - allowWorlds:        worlds where players are allowed to SET homes"
                    out += "  //  - blacklistWorlds:    worlds where home commands cannot be used at all"
                }

                trimmed.startsWith("\"warp\"") -> {
                    out += "  // WARPS MODULE"
                    out += "  // Server warps: /warp, /setwarp, /delwarp, /warps (GUI)."
                    out += "  //  - enabled:          turn the warp system on/off"
                    out += "  //  - cooldownEnabled:  enable/disable cooldown between /warp teleports"
                    out += "  //  - cooldownSeconds:  seconds between allowed /warp uses"
                    out += "  //  - delayEnabled:     enable/disable teleport delay (countdown) for warps"
                    out += "  //  - delaySeconds:     delay in seconds before teleporting to a warp"
                    out += "  //  - allowWorlds:      worlds where players are allowed to SET warps"
                    out += "  //  - blacklistWorlds:  worlds where warp commands cannot be used at all"
                }

                trimmed.startsWith("\"invsee\"") -> {
                    out += "  // Invsee MODULE"
                    out += "  // Request-based Gamemode system: /invsee (read-only), /invseeedit (editable)."
                    out += "  //  - enabled: turn the /invsee commands on/off"
                    out += "  //  - allowWorlds: list of worlds where /invsee is allowed to work"
                }

                trimmed.startsWith("\"misc\"") -> {
                    out += "  // MISC MODULE"
                    out += "  // Collection of small utility commands: /heal, /feed, /repair, /clear, /seen, /near, /speed, etc."
                    out += "  // Use this section to globally control these simple QoL commands."
                    out += "  //  - enabled:       master switch for the misc commands"
                    out += "  //  - allowWorlds:   list of worlds where these commands are allowed"
                    out += "  //  - commands:      inside this block you can further enable/disable each"
                    out += "  //                   individual command (heal/feed/repair/clear/near/seen/speed)"
                }

                trimmed.startsWith("\"blocks\"") -> {
                    out += "  // BLOCKS MODULE"
                    out += "  // Virtual workstation commands: /anvil, /cartography, /stonecutter,"
                    out += "  // /grindstone, /loom, /craft and /enderchest (/ec)."
                    out += "  // These open the corresponding block GUI without needing the real block."
                    out += "  //  - enabled:       master switch for all virtual block commands"
                    out += "  //  - allowWorlds:   worlds where these virtual blocks can be opened"
                    out += "  //  - entries:       inside this block each workstation (anvil, loom, etc.)"
                    out += "  //                   can have its own enabled flag and world restrictions"
                }

                trimmed.startsWith("\"itemEditor\"") -> {
                    out += "  // ITEMEDITOR MODULE"
                    out += "  // Advanced item editing command: /itemeditor"
                    out += "  // Lets staff edit the item in hand: name, lore, amount, unbreakable,"
                    out += "  // durability, enchants, attributes, leather/potion colors, firework power,"
                    out += "  // trims, custom model data and more."
                    out += "  //  - enabled:       turn the /itemeditor system on/off"
                    out += "  //  - allowWorlds:   list of worlds where /itemeditor can be used"
                    out += "  //  - subCommands:   inside this block you can enable/disable individual"
                    out += "  //                   sub-commands (rename, lore, amount, enchant, attribute, etc.)"
                }
            }

            out += line
        }

        return out.joinToString("\n")
    }
}