package dev.fusemc.pql.segment;

import com.manchickas.optionated.Option;
import dev.fusemc.pql.PositionProvider;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface Segment {

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

    boolean update(@NotNull Tag parent, @NotNull Tag value);
}
