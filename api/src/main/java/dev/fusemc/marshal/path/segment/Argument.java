package dev.fusemc.marshal.path.segment;

import com.manchickas.optionated.Option;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.fusemc.marshal.Resolver;
import dev.fusemc.marshal.Suggester;
import dev.fusemc.marshal.parameter.Parameter;
import dev.fusemc.standard.ProxyIdentifier;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Argument<T> implements Segment {

    private final @NotNull  String name;
    private final @NotNull  ArgumentType<?> type;
    private final @NotNull  Parameter<T> parameter;
    private final @Nullable ProxyIdentifier suggester;

    public Argument(@NotNull  String name,
                    @NotNull  ArgumentType<?> type,
                    @NotNull  Parameter<T> parameter,
                    @Nullable ProxyIdentifier suggester) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.parameter = Objects.requireNonNull(parameter);
        this.suggester = suggester;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull ArgumentBuilder<CommandSourceStack, ?> build(@NotNull Resolver resolver) {
        Objects.requireNonNull(resolver);
        var builder = RequiredArgumentBuilder.<CommandSourceStack, Object>argument(this.name, (ArgumentType<Object>) this.type);
        if (this.suggester != null) {
            var option = resolver.resolve(this.suggester);
            if (option instanceof Option.Some<Suggester>(var resolved))
                return builder.suggests(resolved);
        }
        return builder;
    }

    public @NotNull String name() {
        return this.name;
    }

    public @NotNull T resolve(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return this.parameter.resolve(context, this.name);
    }

    @Override
    public @NotNull String toString() {
        return String.format("%s: %s<%s>", this.name, this.type, this.suggester);
    }
}
