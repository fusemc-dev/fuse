package com.manchickas.fuse.disastrous.guard;

import com.manchickas.fuse.disastrous.event.Event;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public sealed interface Guard<P> {

    boolean satisfies(@NotNull P payload);

    non-sealed interface Bound<E extends Event<?, ?>> extends Guard<E> {

        @Override
        boolean satisfies(@NotNull E payload);
    }

    sealed interface Unbound extends Guard<Iterator<Value>> {

        record Literal(@NotNull String value) implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    var value = payload.next();
                    if (value.isString())
                        return value.asString()
                                .equals(this.value);
                }
                return false;
            }
        }

        record Number(double value) implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    var value = payload.next();
                    if (value.isNumber())
                        return Math.abs(value.asDouble() - this.value) < 1.0E-5;
                }
                return false;
            }
        }

        record Boolean(boolean value) implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    var value = payload.next();
                    if (value.isBoolean())
                        return value.asBoolean() == this.value;
                }
                return false;
            }
        }

        record Null() implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    var value = payload.next();
                    return com.manchickas.jet.Undefined.isNull(value);
                }
                return false;
            }
        }

        record Undefined() implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    var value = payload.next();
                    return com.manchickas.jet.Undefined.isUndefined(value);
                }
                return false;
            }
        }

        record Wildcard() implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                if (payload.hasNext()) {
                    payload.next();
                    return true;
                }
                return false;
            }
        }

        record Variadic() implements Unbound {

            @Override
            public boolean satisfies(@NotNull Iterator<Value> payload) {
                while (payload.hasNext())
                    payload.next();
                return true;
            }
        }
    }
}
