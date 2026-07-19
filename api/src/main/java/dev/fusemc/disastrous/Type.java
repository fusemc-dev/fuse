package dev.fusemc.disastrous;

import com.manchickas.optionated.Option;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.tau.Template;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class Type<T extends Callback> {

    private final @NotNull Template<T> template;
    private final @NotNull Function<String, Option<Guard.Parser<?>>> dispatch;

    public Type(@NotNull Template<T> template,
                @NotNull Function<String, Option<Guard.Parser<?>>> dispatch) {
        this.template = Objects.requireNonNull(template);
        this.dispatch = Objects.requireNonNull(dispatch);
    }

    public @NotNull Option<Guard.Parser<?>> dispatch(@NotNull String name) {
        Objects.requireNonNull(name);
        return this.dispatch.apply(name);
    }

    public @NotNull Template<T> template() {
        return this.template;
    }
}
