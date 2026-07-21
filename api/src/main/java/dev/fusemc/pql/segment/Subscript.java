package dev.fusemc.pql.segment;

import com.manchickas.optionated.Option;
import dev.fusemc.pql.PositionProvider;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record Subscript(@NotNull Segment operand,
                        @NotNull PositionProvider provider) implements Segment {

    @Override
    public @NotNull Option<Tag> resolve(@NotNull Tag parent) {
        Objects.requireNonNull(parent);
        return this.operand.resolve(parent)
                .flatMap(resolved -> {
                    if (resolved instanceof CollectionTag collection) {
                        var position = this.provider.compute(collection.size());
                        return Option.fromNullable(collection.get(position));
                    }
                    return Option.none();
                });
    }

    @Override
    public @NonNull Option<Tag> update(@NotNull Tag parent, @NotNull Tag value) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(value);
        if (parent instanceof CollectionTag collection) {
            var position = this.provider.compute(collection.size());
            if (collection.setTag(position, value))
                return Option.some(collection);
            return Option.none();
        }
        return Option.none();
    }
}
