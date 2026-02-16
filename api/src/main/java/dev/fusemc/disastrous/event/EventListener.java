package dev.fusemc.disastrous.event;

import dev.fusemc.disastrous.guard.Guard;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record EventListener<E extends Event<E, C>, C extends EventCallback>(
        @NotNull C callback,
        @NotNull Guard<? super E> @NotNull[] guards
) {

    public EventListener {
        Objects.requireNonNull(callback);
        Objects.requireNonNull(guards);
    }

    public boolean satisfies(@NotNull E payload) {
        for (var guard : this.guards) {
            if (guard.satisfies(payload))
                continue;
            return false;
        }
        return true;
    }
}