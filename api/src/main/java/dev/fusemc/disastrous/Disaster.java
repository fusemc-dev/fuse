package dev.fusemc.disastrous;

import org.jetbrains.annotations.NotNull;

/// A single occurrence of a disaster.
///
/// ---
/// A disaster carries a payload that represents a specific occurrence of its disaster type.
///
/// @since 0.1.0
public interface Disaster<T extends Callback> {

    /// Dispatch the disaster to the provided callback.
    ///
    /// ---
    /// Since a disaster owns its [Callback] signature, it is responsible for
    /// calling the appropriate function manually. The returned `boolean` determines
    /// whether the dispatch should **continue**.
    ///
    /// ```java
    /// boolean dispatch(@NotNull NumericCallback callback) {
    ///     if (callback.onEvent(this.foo, this.bar) == 42)
    ///         return false;
    ///     return true;
    /// }
    /// ```
    ///
    /// @since 0.1.0
    boolean dispatch(@NotNull T callback);

    @NotNull Type<T> type();
}
