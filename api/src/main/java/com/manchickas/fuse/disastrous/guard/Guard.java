package com.manchickas.fuse.disastrous.guard;

import com.manchickas.fuse.disastrous.event.Event;
import org.jetbrains.annotations.NotNull;

public interface Guard<E extends Event<?, ?> > {

    boolean satisfies(@NotNull E payload);
}
