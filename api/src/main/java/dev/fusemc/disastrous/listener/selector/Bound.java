package dev.fusemc.disastrous.listener.selector;


import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.disastrous.listener.Listener;
import dev.fusemc.tau.Tau;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Bound<E extends Disaster<T>, T extends Callback> implements Selector {

    private final @NotNull Disaster.Type<T> type;
    private final @NotNull Guard<? super E> @NotNull[] guards;

    public Bound(@NotNull Disaster.Type<T> type,
                 @NotNull Guard<? super E> @NotNull[] guards) {
        this.type   = Objects.requireNonNull(type);
        this.guards = Objects.requireNonNull(guards);
    }

    public @NotNull Listener<E, T> bind(@NotNull Value callback) {
        Objects.requireNonNull(callback);
        return new Listener<>(Tau.lower(this.type.template(), callback), this.guards);
    }

    public @NotNull Disaster.Type<T> type() {
        return this.type;
    }

    @Override
    public @NotNull String toString() {
        return String.format("%s[%s]", this.type, Arrays.stream(this.guards)
                .map(Object::toString)
                .collect(Collectors.joining(" | ")));
    }
}
