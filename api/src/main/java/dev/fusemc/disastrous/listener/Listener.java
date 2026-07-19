package dev.fusemc.disastrous.listener;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.guard.Guard;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Listener<E extends Disaster<T>, T extends Callback> {

    private final @NotNull T callback;
    private final @NotNull Guard<? super E>[] guards;

    public Listener(@NotNull T callback,
                    @NotNull Guard<? super E>[] guards) {
        this.callback = Objects.requireNonNull(callback);
        this.guards   = Objects.requireNonNull(guards);
    }

    public boolean onDisaster(@NotNull E disaster) {
        for (var guard : this.guards) {
            if (guard.satisfies(disaster))
                continue;
            return true;
        }
        return disaster.dispatch(this.callback);
    }
}
