package dev.fusemc.pql;

import com.manchickas.optionated.Option;
import com.oracle.truffle.js.nodes.access.LocalVarIncNode;
import dev.fusemc.pql.segment.Segment;
import net.minecraft.nbt.Tag;
import org.apache.commons.compress.harmony.unpack200.SegmentOptions;
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

    @SuppressWarnings("unchecked")
    public <T extends Tag> @NotNull Option<T> update(@NotNull T root, @NotNull Tag value) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(value);
        return this.updateFrom(root, value, 0)
                .map(self -> (T) self);
    }

    private @NotNull Option<Tag> updateFrom(@NotNull Tag root, @NotNull Tag value, int position) {
        if (position < this.segments.length) {
            var segment = this.segments[position];
            var option = segment.resolve(root);
            if (option instanceof Option.Some(var resolved)) {
                assert resolved != null;
                var updated = this.updateFrom(resolved, value, position + 1);
                if (updated instanceof Option.Some(var wrapped)) {
                    assert wrapped != null;
                    return segment.update(root, wrapped);
                }
                return Option.none();
            }
            return Option.none();
        }
        return Option.some(value);
    }

    public @NotNull Path parent() {
        return new Path(Arrays.copyOf(this.segments, this.segments.length - 1));
    }

    public int length() {
        return this.segments.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.segments);
    }
}
