package dev.fusemc.marshal;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.fusemc.marshal.standard.ProxyContext;
import dev.fusemc.marshal.standard.ProxySource;
import dev.fusemc.tau.Template;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Suggester extends SuggestionProvider<CommandSourceStack> {

    @NotNull Template<Suggester> TEMPLATE = Template.functional(Suggester.class, Template.array(Template.STRING, String[]::new));

    @NotNull String[] suggest(@NotNull ProxySource source, @NotNull ProxyContext context);

    // TODO: A prime place to implement async functions,
    //       but as of right now, due to fuse not having any
    //       notion of I/O or network requests, this seems
    //       redundant.
    @Override
    default @NotNull CompletableFuture<Suggestions> getSuggestions(@NotNull CommandContext<CommandSourceStack> context,
                                                                   @NotNull SuggestionsBuilder builder) {
        var source      = builder.getInput();
        var range       = StringRange.between(builder.getStart(), source.length());
        var suggestions = this.suggest(ProxySource.from(context.getSource()), ProxyContext.from(builder));
        return CompletableFuture.completedFuture(Suggestions.create(
                source,
                Arrays.stream(suggestions)
                        .map(suggestion -> new Suggestion(range, suggestion))
                        .toList()
        ));
    }
}
