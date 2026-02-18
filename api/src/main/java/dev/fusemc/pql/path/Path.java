package dev.fusemc.pql.path;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.fusemc.ParseException;
import dev.fusemc.ValueOps;
import dev.fusemc.pql.Parser;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class Path {

    private static final Logger LOGGER = LoggerFactory.getLogger(Path.class);

    private final @NotNull Segment @NotNull[] segments;

    public Path(@NotNull Segment @NotNull[] segments) {
        this.segments = segments;
    }

    public void update(@NotNull Tag root, @NotNull Updater value) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(value);
        var result = root;
        for (var i = 0; i < this.segments.length - 1; i++) {
            var segment = this.segments[i];
            result = segment.resolve(result);
        }
        var last = this.segments[this.segments.length - 1];
        last.update(result, value);
    }

    public @NotNull Value traverse(@NotNull Tag root) {
        Objects.requireNonNull(root);
        var result = root;
        for (var segment : this.segments)
            result = segment.resolve(result);
        return NbtOps.INSTANCE.convertTo(
                ValueOps.instance(),
                result
        );
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
