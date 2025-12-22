package org.krripe.shadowessentials.core.command

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource

object Suggestions {

    val EMPTY: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { _, builder -> builder.buildFuture() }

    val onlinePlayers: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder ->
            val names = ctx.source.server.playerNames.asList()
            CommandSource.suggestMatching(names, builder)
        }

    fun fixed(values: Collection<String>): SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { _, builder ->
            CommandSource.suggestMatching(values, builder)
        }

    fun fixed(vararg values: String): SuggestionProvider<ServerCommandSource> =
        fixed(values.toList())

    fun dynamic(provider: (ServerCommandSource) -> Collection<String>): SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder ->
            val values = provider(ctx.source)
            CommandSource.suggestMatching(values, builder)
        }

    fun custom(block: (ctx: ServerCommandSource, builder: SuggestionsBuilder) -> Unit): SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder ->
            block(ctx.source, builder)
            builder.buildFuture()
        }
}