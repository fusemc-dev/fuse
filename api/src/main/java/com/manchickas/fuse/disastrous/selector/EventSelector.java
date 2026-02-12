package com.manchickas.fuse.disastrous.selector;

import com.manchickas.fuse.disastrous.event.Event;
import com.manchickas.fuse.disastrous.event.EventCallback;
import com.manchickas.fuse.disastrous.event.EventListener;
import com.manchickas.fuse.disastrous.event.EventType;
import com.manchickas.fuse.disastrous.guard.Guard;
import com.manchickas.jet.exception.TypeException;
import net.minecraft.resources.Identifier;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public sealed interface EventSelector<L extends EventListener<?, C>, C extends EventCallback> {

    @NotNull L bind(@NotNull C callback);

    record Bound<E extends Event<E, C>, C extends EventCallback>(
            @NotNull EventType<E, C> type,
            @NotNull Guard.Bound<? super E> @NotNull[] guards
    ) implements EventSelector<EventListener.Bound<E, C>, C> {

        @Override
        public @NotNull EventListener.Bound<E, C> bind(@NotNull C callback) {
            return new EventListener.Bound<>(callback, this.guards);
        }

        public @NotNull EventListener.Bound<E, C> bind(@NotNull Value callback) throws TypeException {
            return new EventListener.Bound<>(this.type.expect(callback), this.guards);
        }

        @Override
        public @NotNull String toString() {
            return String.format("Bound[type=%s, guards=%s]", this.type, Arrays.toString(this.guards));
        }
    }

    record Unbound(
            @NotNull Identifier identifier,
            @NotNull Guard.Unbound @NotNull[] guards
    ) implements EventSelector<EventListener.Unbound, EventCallback.Unbound> {

        @Override
        public @NotNull EventListener.Unbound bind(EventCallback.@NotNull Unbound callback) {
            return new EventListener.Unbound(callback, this.guards);
        }

        @Override
        public @NotNull String toString() {
            return String.format("Unbound[identifier=%s, guards=%s]", this.identifier, Arrays.toString(this.guards));
        }
    }
}
