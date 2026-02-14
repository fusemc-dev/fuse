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
import java.util.Objects;

public sealed interface EventSelector {

    record Bound<E extends Event<E, C>, C extends EventCallback>(
            @NotNull EventType<E, C> type,
            @NotNull Guard<? super E> @NotNull[] guards
    ) implements EventSelector {

        public Bound {
            Objects.requireNonNull(type);
            Objects.requireNonNull(guards);
        }

        public @NotNull EventListener<E, C> bind(@NotNull Value callback) throws TypeException {
            return new EventListener<>(this.type.expect(callback), this.guards);
        }

        @Override
        public @NotNull String toString() {
            return String.format("Bound[type=%s, guards=%s]", this.type, Arrays.toString(this.guards));
        }
    }

    record Unbound(@NotNull Identifier identifier) implements EventSelector {

        public Unbound {
            Objects.requireNonNull(identifier);
        }

        @Override
        public @NotNull String toString() {
            return String.format("Unbound[identifier=%s]", this.identifier);
        }
    }
}
