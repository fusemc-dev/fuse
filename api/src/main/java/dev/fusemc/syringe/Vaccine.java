package dev.fusemc.syringe;

import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import com.manchickas.optionated.Option;
import net.minecraft.world.entity.Entity;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Vaccine<E extends Entity, T> {

    private final @NotNull Function<E, T> sampler;
    private final @NotNull BiConsumer<E, T> injector;
    private final @NotNull Template<T> template;
    private final @NotNull Class<E> type;

    private Vaccine(
            @NotNull Function<E, T> sampler,
            @NotNull BiConsumer<E, T> injector,
            @NotNull Template<T> template,
            @NotNull Class<E> type
    ) {
        this.sampler = Objects.requireNonNull(sampler);
        this.injector = Objects.requireNonNull(injector);
        this.template = Objects.requireNonNull(template);
        this.type = Objects.requireNonNull(type);
    }

    public static <E extends Entity, T> Vaccine.Builder<E, T> builder(@NotNull Class<E> type) {
        return new Vaccine.Builder<>(type);
    }

    public @NotNull Option<Value> attemptSample(@NotNull Entity entity) {
        Objects.requireNonNull(entity);
        if (this.type.isInstance(entity)) {
            var sample = this.sampler.apply(this.type.cast(entity));
            return this.template.serialize(sample);
        }
        return Option.none();
    }

    public boolean attemptInject(@NotNull Entity entity, @NotNull Value value) throws TypeException {
        Objects.requireNonNull(entity);
        Objects.requireNonNull(value);
        if (this.type.isInstance(entity)) {
            this.injector.accept(
                    this.type.cast(entity),
                    Jet.expect(this.template, value)
            );
            return true;
        }
        return false;
    }

    public static class Builder<E extends Entity, T> {

        private final @NotNull Class<E> type;
        private @Nullable Function<E, T> sampler;
        private @Nullable BiConsumer<E, T> injector;
        private @Nullable Template<T> template;

        private Builder(@NotNull Class<E> type) {
            this.type = Objects.requireNonNull(type);
            this.sampler = null;
            this.injector = null;
            this.template = null;
        }

        @Contract("_ -> this")
        public Builder<E, T> onSample(@NotNull Function<E, T> sampler) {
            this.sampler = Objects.requireNonNull(sampler);
            return this;
        }

        @Contract("_ -> this")
        public Builder<E, T> onInject(@NotNull BiConsumer<E, T> injector) {
            this.injector = Objects.requireNonNull(injector);
            return this;
        }

        @Contract("_ -> this")
        public Builder<E, T> withTemplate(@NotNull Template<T> template) {
            this.template = Objects.requireNonNull(template);
            return this;
        }

        public @NotNull Vaccine<E, T> build() {
            Objects.requireNonNull(this.sampler, "Attempted to build a vaccine without a sampler.");
            Objects.requireNonNull(this.injector, "Attempted to build a vaccine without an injector.");
            Objects.requireNonNull(this.template, "Attempted to build a vaccine without a template.");
            return new Vaccine<>(this.sampler, this.injector, this.template, this.type);
        }
    }
}
