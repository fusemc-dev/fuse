package dev.fusemc.lifecycle;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/// A scheduled task.
///
/// ---
///
/// A `Scheduled` represents a callback that shall be run at some future **reference tick**.
/// It is identified by the [Context] that initiated it.
///
/// @since 0.1.0
public final class Scheduled implements Comparable<Scheduled> {

    private final @NotNull Context initiator;
    private final @NotNull ProxyExecutable callback;
    private final long reference;

    public Scheduled(@NotNull Context initiator,
                     @NotNull ProxyExecutable callback,
                     long reference) {
        this.initiator = Objects.requireNonNull(initiator);
        this.callback  = Objects.requireNonNull(callback);
        this.reference = reference;
    }

    /// Attempt running the scheduled.
    ///
    /// ---
    ///
    /// Given a reference tick, if the associated one is due, the callback is run
    /// and `true` is returned. Otherwise, `false` is returned.
    ///
    /// @since 0.1.0
    public boolean attempt(long reference) {
        if (reference >= this.reference) {
            this.callback.execute();
            return true;
        }
        return false;
    }

    public @NotNull Context initiator() {
        return this.initiator;
    }

    @Override
    public int compareTo(@NonNull Scheduled o) {
        return Long.compare(this.reference, o.reference);
    }
}
