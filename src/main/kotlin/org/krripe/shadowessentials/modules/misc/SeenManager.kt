package org.krripe.shadowessentials.modules.misc

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowessentials.core.lang.LangManager
import java.io.File
import kotlin.math.max

object SeenManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val seenFile = File("config/ShadowEssentials/data/seen.json")

    private data class SeenStore(
        val lastSeen: MutableMap<String, Long> = mutableMapOf(),
        val names: MutableMap<String, String> = mutableMapOf()
    )

    private var seen = SeenStore()

    fun init() {
        loadSeen()

        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
            val p = handler.player
            seen.lastSeen[p.uuidAsString] = System.currentTimeMillis()
            seen.names[p.name.string.lowercase()] = p.uuidAsString
            saveSeen()
        })

        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            val p = handler.player
            seen.lastSeen[p.uuidAsString] = System.currentTimeMillis()
            seen.names[p.name.string.lowercase()] = p.uuidAsString
            saveSeen()
        })
    }

    private fun loadSeen() {
        if (!seenFile.exists()) return
        runCatching {
            val type = object : TypeToken<SeenStore>() {}.type
            seen = gson.fromJson<SeenStore>(seenFile.readText(), type) ?: SeenStore()
        }
    }

    private fun saveSeen() {
        seenFile.parentFile.mkdirs()
        seenFile.writeText(gson.toJson(seen))
    }

    fun formatSeen(source: ServerPlayerEntity, targetName: String): Text {
        val online = source.server.playerManager.getPlayer(targetName)
        if (online != null) {
            return LangManager.msg("seenOnline", "player" to online.name.string)
        }

        val uuidStr = seen.names[targetName.lowercase()]
            ?: return LangManager.msg("seenNever", "player" to targetName)

        val ts = seen.lastSeen[uuidStr]
            ?: return LangManager.msg("seenNever", "player" to targetName)

        val sec = max(0L, (System.currentTimeMillis() - ts) / 1000L)
        val d = sec / 86400
        val h = (sec % 86400) / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        val whenTxt = if (d > 0) "${d}d ${h}h ${m}m" else "%02d:%02d:%02d".format(h, m, s)

        return LangManager.msg("seenResult", "player" to targetName, "time" to whenTxt)
    }
}