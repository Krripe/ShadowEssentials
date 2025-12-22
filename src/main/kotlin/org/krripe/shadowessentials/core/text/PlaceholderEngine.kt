package org.krripe.shadowessentials.core.text

object PlaceholderEngine {

    fun apply(input: String, placeholders: Map<String, Any?>): String {
        var out = input
        placeholders.forEach { (key, value) ->
            val str = value?.toString() ?: ""
            out = out.replace("%$key%", str)
            out = out.replace("{$key}", str)
        }
        return out
    }

    fun apply(input: String, vararg placeholders: Pair<String, Any?>): String =
        apply(input, placeholders.toMap())
}
