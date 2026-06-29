package dev.fusemc.marshal.path.segment;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import dev.fusemc.marshal.Resolver;
import dev.fusemc.marshal.parameter.Parameter;
import dev.fusemc.standard.ProxyIdentifier;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface Segment {

    static @NotNull Segment literal(@NotNull String literal) {
        Objects.requireNonNull(literal);
        return new Literal(literal);
    }

    static @NotNull Segment argument(@NotNull String name,
                                     @NotNull Parameter<?> parameter,
                                     @NotNull ArgumentType<?> type,
                                     @Nullable ProxyIdentifier suggester) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(parameter);
        Objects.requireNonNull(type);
        return new Argument<>(name, type, parameter, suggester);
    }

    @NotNull ArgumentBuilder<CommandSourceStack, ?> build(@NotNull Resolver resolver);
}
