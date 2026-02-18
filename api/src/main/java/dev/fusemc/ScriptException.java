package dev.fusemc;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ScriptException extends Exception {

    public ScriptException(@NotNull String message) {
        super(Objects.requireNonNull(message));
    }
}
