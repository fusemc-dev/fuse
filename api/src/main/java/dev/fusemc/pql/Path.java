package dev.fusemc.pql;

import com.manchickas.optionated.Option;
import dev.fusemc.pql.segment.Segment;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public final class Path {

    private final @NotNull Segment @NotNull[] segments;

    public Path(@NotNull Segment @NotNull[] segments) {
        this.segments = Objects.requireNonNull(segments);
    }

    public @NotNull Option<Tag> resolve(@NotNull Tag root) {
        Objects.requireNonNull(root);
        for (var segment : this.segments) {
            var option = segment.resolve(root);
            if (option instanceof Option.Some(var resolved)) {
                root = Objects.requireNonNull(resolved);
                continue;
            }
            return Option.none();
        }
        return Option.some(root);
    }

    public boolean update(@NotNull Tag root, @NotNull Tag value) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(value);
        for (var i = 0; i < this.segments.length - 1; i++) {
            var option = this.segments[i].resolve(root);
            if (option instanceof Option.Some(var resolved)) {
                root = Objects.requireNonNull(resolved);
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.segments);
    }
}
