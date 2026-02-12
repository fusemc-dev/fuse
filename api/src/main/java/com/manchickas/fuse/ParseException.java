package com.manchickas.fuse;

import com.manchickas.optionated.Option;
import com.manchickas.quelle.position.SourceSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ParseException extends Exception {

    private final @Nullable SourceSpan<String> span;

    public ParseException(@NotNull String message) {
        this(Objects.requireNonNull(message), null);
    }

    public ParseException(@NotNull String message, @Nullable SourceSpan<String> span) {
        super(Objects.requireNonNull(message));
        this.span = span;
    }

    public @NotNull Option<SourceSpan<String>> span() {
        return Option.fromNullable(this.span);
    }
}
