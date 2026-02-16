package dev.fusemc.pql;

import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Path {

    private @NotNull Segment @NotNull[] segments;

    public Path(@NotNull Segment @NotNull[] segments) {
        this.segments = segments;
    }

    public @NotNull Tag traverse(@NotNull Tag root) {
        Objects.requireNonNull(root);
        var result = root;
        for (var segment : this.segments)
            result = segment.resolve(result);
        return result;
    }

    @Override
    public @NotNull String toString() {
        var buffer = new StringBuilder();
        for (var i = 0; i < this.segments.length; i++) {
            var segment = this.segments[i];
            if (i > 0)
                buffer.append('/');
            buffer.append(segment);
        }
        return buffer.toString();
    }
}
