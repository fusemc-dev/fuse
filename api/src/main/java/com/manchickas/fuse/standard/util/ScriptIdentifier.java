package com.manchickas.fuse.standard.util;

import com.manchickas.fuse.ParseException;
import com.manchickas.quelle.SourceReader;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.BiFunction;

public record ScriptIdentifier(
        @NotNull String namespace,
        @NotNull String path
) {

    public ScriptIdentifier {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
    }

    public static <T> @NotNull T parse(@NotNull String identifier,
                                       @NotNull BiFunction<@NotNull String, @NotNull String, @NotNull T> constructor) throws ParseException {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(constructor);
        var reader = new SourceReader<>(identifier);
        var stamp = reader.stamp();
        var buffer = new StringBuilder();
        while (reader.canRead()) {
            var c = reader.peek();
            if (c == ':') {
                var namespace = buffer.toString();
                reader.read();
                return ScriptIdentifier.parsePath(namespace, reader, constructor);
            }
            if (c == '/')
                break;
            if (ScriptIdentifier.isValid(c)) {
                buffer.appendCodePoint(reader.read());
                continue;
            }
            throw new ParseException(
                    "Encountered an invalid character '%c' in an identifier literal."
                            .formatted(c),
                    reader.characterSpan()
            );
        }
        reader.backtrack(stamp);
        return ScriptIdentifier.parsePath("minecraft", reader, constructor);
    }

    private static <T> @NotNull T parsePath(@NotNull String namespace,
                                            @NotNull SourceReader<String> reader,
                                            @NotNull BiFunction<@NotNull String, @NotNull String, @NotNull T> constructor) throws ParseException {
        if (reader.canRead()) {
            var buffer = new StringBuilder();
            var length = 0;
            while (reader.canRead()) {
                var c = reader.peek();
                if (c == '/') {
                    if (length > 0) {
                        buffer.appendCodePoint(reader.read());
                        length = 0;
                        continue;
                    }
                    throw new ParseException(
                            "Encountered an empty path segment in an identifier literal.",
                            reader.characterSpan()
                    );
                }
                if (ScriptIdentifier.isValid(c)) {
                    buffer.appendCodePoint(reader.read());
                    length++;
                    continue;
                }
                throw new ParseException(
                        "Encountered an invalid character '%c' in an identifier literal."
                                .formatted(c),
                        reader.characterSpan()
                );
            }
            if (length == 0)
                throw new ParseException(
                        "Encountered a trailing slash in an identifier literal.",
                        reader.inclusiveCharacterSpan()
                );
            return constructor.apply(namespace, buffer.toString());
        }
        throw new ParseException(
                "Encountered an empty path in an identifier literal.",
                reader.inclusiveCharacterSpan()
        );
    }

    public static boolean isValid(int c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-';
    }

    @Override
    public @NonNull String toString() {
        return this.namespace + ':' + this.path;
    }
}
