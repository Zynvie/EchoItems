package cat.emir.echoitems.arguments

import cat.emir.echoitems.EchoItems
import cat.emir.echoitems.EchoItemsConfig
import cat.emir.echolib.extensions.toComponent
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture

class ItemArgument : CustomArgumentType.Converted<EchoItemsConfig.Item, String> {

    val ERROR_INVALID_ITEM = SimpleCommandExceptionType(
        MessageComponentSerializer.message().serialize("<red>Invalid item provided.</red>".toComponent())
    )

    override fun convert(nativeType: String): EchoItemsConfig.Item {
        return EchoItems.instance.itemManager.getItem(nativeType) ?: throw ERROR_INVALID_ITEM.create()
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun <S : Any> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        EchoItems.instance.config.typedConfig.items
            .map { it.key }
            .filter { it.startsWith(builder.remaining, true) }
            .forEach { builder.suggest(it) }

        return builder.buildFuture()
    }
}