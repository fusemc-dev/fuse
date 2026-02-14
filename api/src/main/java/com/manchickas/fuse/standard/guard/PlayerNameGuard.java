package com.manchickas.fuse.standard.guard;

import com.manchickas.fuse.ParseException;
import com.manchickas.fuse.disastrous.guard.Guard;
import com.manchickas.fuse.disastrous.guard.GuardType;
import com.manchickas.fuse.standard.event.PlayerEvent;
import com.manchickas.quelle.SourceReader;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record PlayerNameGuard(
        @NotNull String name
) implements Guard<PlayerEvent<?, ?>> {

    public static final GuardType<PlayerEvent<?, ?>, PlayerNameGuard> TYPE = (parser) -> {
        var literal = parser.expectLiteral();
        var reader = new SourceReader<>(literal.value());
        var length = 0;
        while (reader.canRead()) {
            var c = reader.peek();
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
                reader.read();
                length++;
                continue;
            }
            throw new ParseException(
                    "Encountered an invalid character '%c' in a username literal."
                            .formatted(c),
                    reader.characterSpan()
            );
        }
        if (length >= 3 && length <= 16)
            return new PlayerNameGuard(literal.value());
        throw new ParseException(
                "Encountered a username literal with an invalid length (%d characters)."
                        .formatted(length),
                literal.span()
        );
    };

    public PlayerNameGuard {
        Objects.requireNonNull(name);
    }

    @Override
    public boolean satisfies(@NonNull PlayerEvent<?, ?> payload) {
        return payload.player()
                .unwrap()
                .getGameProfile()
                .name()
                .equals(this.name);
    }
}
