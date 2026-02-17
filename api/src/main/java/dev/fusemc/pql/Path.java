package dev.fusemc.pql;

import dev.fusemc.ParseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Path {

    private final @NotNull Segment @NotNull[] segments;

    public Path(@NotNull Segment @NotNull[] segments) {
        this.segments = segments;
    }

    static void main() {
        var nbt = new CompoundTag();
        var nested = new CompoundTag();
        nested.putString("bar", "baz");
        nbt.put("foo", nested);
        try {
            var path = Path.compile("foo/bar");
            System.out.println(path.traverse(nbt));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static @NotNull Path compile(@NotNull String path) throws ParseException {
        Objects.requireNonNull(path);
        return new Parser(path)
                .parse();
    }

    public void set(@NotNull Tag root, @NotNull Tag value) {
        var result = root;
        for (var i = 0; i < this.segments.length - 1; i++) {
            var segment = this.segments[i];
            result = segment.resolve(result);
        }
        var last = this.segments[this.segments.length - 1];
        last.set(result, value);
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
