package dev.fusemc.disastrous.listener;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.disaster.Disaster;
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

    public boolean onEvent(@NotNull E event) {
        for (var guard : this.guards) {
            if (guard.satisfies(event))
                continue;
            return true;
        }
        return event.onDispatch(this.callback);
    }
}
