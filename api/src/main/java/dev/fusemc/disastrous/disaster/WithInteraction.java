package dev.fusemc.disastrous.disaster;

import com.manchickas.optionated.Option;
import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.tau.Template;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/// Represents a [Disaster] around an interaction.
///
/// ---
///
/// Dispatching a `WithInteraction` accumulates an [InteractionResult] that corresponds to the
/// highest-priority result returned by a listener. The dispatch is terminated
/// once a non-pass result is suggested.
///
/// {@link Disaster Disasters} may use the `WithInteraction.RESULT` [Template] for their [Callback] signature:
///
/// ```typescript
/// type Result = "success" | "consume" | "fail" | "pass";
/// ```
///
/// @since 0.1.0
public abstract class WithInteraction<T extends Callback> implements Disaster<T> {

    public static final Template<InteractionResult> RESULT = Template.union(
            Template.literal("success").flatMap(
                    _ -> Option.some(InteractionResult.SUCCESS_SERVER),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.SUCCESS_SERVER)
                            .map(_ -> "success")
            ),
            Template.literal("pass").flatMap(
                    _ -> Option.some(InteractionResult.PASS),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.PASS)
                            .map(_ -> "pass")
            ),
            Template.literal("consume").flatMap(
                    _ -> Option.some(InteractionResult.CONSUME),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.CONSUME)
                            .map(_ -> "consume")
            ),
            Template.literal("fail").flatMap(
                    _ -> Option.some(InteractionResult.FAIL),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.FAIL)
                            .map(_ -> "fail")
            )
    );

    protected @NotNull InteractionResult result;

    public WithInteraction() {
        this.result = InteractionResult.PASS;
    }

    /// Suggest an [InteractionResult] as the result of the dispatch.
    ///
    /// ---
    ///
    /// Once the suggested result is not [InteractionResult#PASS], the function will return `false`.
    /// A [Disaster] should route the return of this function to its `dispatch` one:
    ///
    /// ```
    /// void dispatch(@NotNull InteractionCallback callback) {
    ///     var result = callback.onInteraction(this.foo);
    ///     return this.suggestResult(result);
    /// }
    /// ```
    ///
    /// @since 0.1.0
    protected boolean suggestResult(@NotNull InteractionResult result) {
        Objects.requireNonNull(result);
        if (result != InteractionResult.PASS) {
            this.result = result;
            return false;
        }
        return true;
    }

    /// Retrieve the suggested [InteractionResult], or compute the given [Supplier] if nothing over `"pass"`
    /// was suggested.
    ///
    /// ---
    ///
    /// @since 0.1.0
    public @NotNull InteractionResult resultOr(@NotNull Supplier<InteractionResult> supplier) {
        Objects.requireNonNull(supplier);
        if (this.result == InteractionResult.PASS)
            return supplier.get();
        return this.result;
    }

    /// Retrieve the final [InteractionResult].
    ///
    /// ---
    ///
    /// @since 0.1.0
    public @NotNull InteractionResult result() {
        return this.result;
    }
}
