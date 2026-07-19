package dev.fusemc.pql;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PQL {

    private PQL() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Path parse(@NotNull String path) {
        Objects.requireNonNull(path);
        return new Parser(path).parsePath();
    }
}
