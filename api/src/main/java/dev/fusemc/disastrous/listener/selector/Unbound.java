package dev.fusemc.disastrous.listener.selector;


import dev.fusemc.standard.ProxyIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Unbound implements Selector {

    private final @NotNull ProxyIdentifier identifier;

    public Unbound(@NotNull ProxyIdentifier identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    public @NotNull ProxyIdentifier identifier() {
        return this.identifier;
    }

    @Override
    public @NotNull String toString() {
        return String.format("|%s|", this.identifier);
    }
}
