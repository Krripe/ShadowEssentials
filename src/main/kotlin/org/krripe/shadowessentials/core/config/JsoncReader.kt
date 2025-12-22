package org.krripe.shadowessentials.core.config

import java.io.File

object JsoncReader {

    fun readJsonc(file: File): String {
        val raw = file.readText()
        return stripComments(raw)
    }

    private fun stripComments(src: String): String {

        var s = src.replace(Regex("(?s)/\\*.*?\\*/"), "")
        val cleanedLines = s.lines().map { line ->
            var inString = false
            var escape = false
            val out = StringBuilder()

            var i = 0
            while (i < line.length) {
                val c = line[i]

                if (!inString && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                    break
                }

                out.append(c)

                if (c == '"' && !escape) {
                    inString = !inString
                }

                escape = (c == '\\') && !escape
                i++
            }
            out.toString()
        }

        s = cleanedLines.joinToString("\n")
        return s
    }
}