package com.manchickas.fuse.disastrous.event;

import com.manchickas.fuse.disastrous.guard.Guard;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public sealed interface EventListener<P, C extends EventCallback> {

    boolean satisfies(@NotNull P payload);

    @NotNull C callback();

    record Bound<E extends Event<E, C>, C extends EventCallback>(
            @NotNull C callback,
            @NotNull Guard.Bound<? super E> @NotNull[] guards
    ) implements EventListener<E, C> {

        public Bound {
            Objects.requireNonNull(callback);
            Objects.requireNonNull(guards);
        }

        @Override
        public boolean satisfies(@NotNull E payload) {
            for (var guard : this.guards) {
                if (guard.satisfies(payload))
                    continue;
                return false;
            }
            return true;
        }
    }

    record Unbound(
            @NotNull EventCallback.Unbound callback,
            @NotNull Guard.Unbound @NotNull[] guards
    ) implements EventListener<Value[], EventCallback.Unbound> {

        @Override
        public boolean satisfies(@NotNull Value @NonNull[] payload) {
            var iterator = new Iterator<Value>() {

                int position = 0;

                @Override
                public Value next() {
                    if (this.hasNext())
                        return payload[this.position++];
                    throw new NoSuchElementException();
                }

                @Override
                public boolean hasNext() {
                    return this.position < payload.length;
                }
            };
            for (var guard : this.guards) {
                if (guard.satisfies(iterator))
                    continue;
                return false;
            }
            return !iterator.hasNext();
        }
    }
}