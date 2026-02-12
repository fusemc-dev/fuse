package com.manchickas.fuse.standard;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ScriptWrapper<T> {

    protected final @NotNull T wrapped;

    public ScriptWrapper(@NotNull T wrapped) {
        Objects.requireNonNull(wrapped);
        this.wrapped = wrapped;
    }

    public @NotNull T unwrap() {
        return this.wrapped;
    }
}
