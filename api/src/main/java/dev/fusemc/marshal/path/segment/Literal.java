package dev.fusemc.marshal.path.segment;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.fusemc.marshal.Resolver;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Literal implements Segment {

    private final @NotNull String literal;

    public Literal(@NotNull String literal) {
        this.literal = Objects.requireNonNull(literal);
    }

    @Override
    public @NotNull ArgumentBuilder<CommandSourceStack, ?> build(@NotNull Resolver resolver) {
        Objects.requireNonNull(resolver);
        return LiteralArgumentBuilder.literal(this.literal);
    }

    @Override
    public @NotNull String toString() {
        return this.literal;
    }
}
