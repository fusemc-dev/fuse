package dev.fusemc.standard;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof ScriptWrapper<?> other)
            return this.wrapped.equals(other.wrapped);
        return false;
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }
}
