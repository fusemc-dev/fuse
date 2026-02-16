package dev.fusemc.disastrous.guard;

import dev.fusemc.disastrous.event.Event;
import org.jetbrains.annotations.NotNull;

public interface Guard<E extends Event<?, ?> > {

    boolean satisfies(@NotNull E payload);
}
