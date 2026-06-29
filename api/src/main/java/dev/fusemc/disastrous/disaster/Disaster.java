package dev.fusemc.disastrous.disaster;

import com.manchickas.optionated.Option;
import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.tau.Template;
import org.jetbrains.annotations.NotNull;

/// A single occurrence of an event.
///
/// ---
///
/// @since `0.1.0`
public interface Disaster<T extends Callback> {

    /// Dispatch the disaster to the provided callback.
    ///
    /// ---
    /// Since a disaster owns its [Callback] signature, it is responsible for
    /// calling the function manually.
    ///
    /// ```java
    /// interface BirthCallback extends Callback {
    ///
    ///     void onBirth(@NotNull String name);
    /// }
    /// // ...
    /// @Override
    /// void onDispatch(@NotNull BirthCallback callback) {
    ///     callback.onBirth(this.name);
    /// }
    /// ```
    ///
    /// The `onDispatch` function is called whenever the disaster occurs for all
    /// registered listeners. This allows the disaster to accumulate the return values
    /// of its listeners:
    ///
    /// ```java
    /// @Override
    /// void onDispatch(@NotNull LoginCallback callback) {
    ///     this.shouldReject |= callback.onLogin(...);
    /// }
    /// ```
    ///
    /// @since `0.1.0`
    boolean onDispatch(@NotNull T callback);

    @NotNull Disaster.Type<T> type();

    interface Type<T extends Callback> {

        @NotNull Template<T> template();

        @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name);
    }
}
