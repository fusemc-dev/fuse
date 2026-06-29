package dev.fusemc.disastrous.listener.selector;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.standard.ProxyIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface Selector permits Bound, Unbound {

    static @NotNull Selector unbound(@NotNull ProxyIdentifier identifier) {
        Objects.requireNonNull(identifier);
        return new Unbound(identifier);
    }

    static <E extends Disaster<T>, T extends Callback> @NotNull Selector bound(@NotNull Disaster.Type<T> type,
                                                                               @NotNull Guard<? super E> @NotNull[] guards) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(guards);
        return new Bound<>(type, guards);
    }
}
