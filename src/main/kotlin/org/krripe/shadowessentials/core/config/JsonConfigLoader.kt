package org.krripe.shadowessentials.core.config

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object JsonConfigLoader {

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    inline fun <reified T> loadOrCreate(
        file: File,
        default: () -> T
    ): T {
        if (!file.exists()) {
            val value = default()
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(value))
            return value
        }

        val json = JsoncReader.readJsonc(file)

        return runCatching {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(json, type)
        }.getOrNull() ?: default().also {
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(it))
        }
    }

    fun <T> save(file: File, value: T) {
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(value))
    }
}