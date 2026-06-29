package dev.fusemc.marshal;

import dev.fusemc.marshal.path.Parser;
import dev.fusemc.marshal.path.CommandPath;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Marshal {

    private Marshal() {
    }

    public static @NotNull CommandPath parse(@NotNull String path) {
        Objects.requireNonNull(path);
        var parser = new Parser(path);
        return parser.parsePath();
    }
}
