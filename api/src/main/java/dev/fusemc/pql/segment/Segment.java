package dev.fusemc.pql.segment;

import com.manchickas.optionated.Option;
import dev.fusemc.pql.PositionProvider;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface Segment {

    @NotNull Segment ROOT = new Segment() {

        @Override
        public @NotNull Option<Tag> resolve(@NotNull Tag parent) {
            return Option.some(parent);
        }

        @Override
        public @NotNull Option<Tag> update(@NotNull Tag parent, @NotNull Tag value) {
            return Option.some(value);
        }
    };

    static @NotNull Segment property(@NotNull String name) {
        Objects.requireNonNull(name);
        return new Property(name);
    }

    static @NotNull Segment subscript(@NotNull Segment segment,
                                      @NotNull PositionProvider provider) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(provider);
        return new Subscript(segment, provider);
    }

    @NotNull Option<Tag> resolve(@NotNull Tag parent);

    @NotNull <T extends Tag> Option<T> update(@NotNull T parent, @NotNull Tag value);
}
