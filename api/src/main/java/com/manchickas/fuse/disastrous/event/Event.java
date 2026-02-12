package com.manchickas.fuse.disastrous.event;

import org.jetbrains.annotations.NotNull;

public interface Event<E extends Event<E, C>, C extends EventCallback> {

    default boolean acceptListener(EventListener.Bound<E, C> listener) {
        if (listener.satisfies(this.self())) {
            this.accept(listener.callback());
            return true;
        }
        return false;
    }

    void accept(@NotNull C callback);

    @NotNull EventType<E, C> type();

    @NotNull E self();
}
