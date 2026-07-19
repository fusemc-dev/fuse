package dev.fusemc.syringe;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.world.entity.Entity;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Vaccine<T, V> {

    private final @NotNull Function<T, V> sample;
    private final @NotNull BiConsumer<T, V> inject;
    private final @NotNull Template<V> template;
    private final @NotNull Class<T> type;

    private Vaccine(
            @NotNull Function<T, V> sampler,
            @NotNull BiConsumer<T, V> inject,
            @NotNull Template<V> template,
            @NotNull Class<T> type
    ) {
        this.sample = Objects.requireNonNull(sampler);
        this.inject = Objects.requireNonNull(inject);
        this.template = Objects.requireNonNull(template);
        this.type = Objects.requireNonNull(type);
    }

    public static <T, V> Vaccine.Builder<T, V> builder(@NotNull Class<T> type) {
        return new Vaccine.Builder<>(type);
    }

    public @NotNull Option<Value> attemptSample(@NotNull Entity entity) {
        Objects.requireNonNull(entity);
        if (this.type.isInstance(entity)) {
            var sample = this.sample.apply(this.type.cast(entity));
            return Option.some(Value.asValue(sample));
        }
        return Option.none();
    }

    public boolean attemptInject(@NotNull Entity entity, @NotNull Value value) {
        Objects.requireNonNull(entity);
        Objects.requireNonNull(value);
        if (this.type.isInstance(entity)) {
            this.inject.accept(
                    this.type.cast(entity),
                    Tau.lower(this.template, value)
            );
            return true;
        }
        return false;
    }

    public static class Builder<T, V> {

        private final @NotNull Class<T> type;
        private @Nullable Function<T, V> sampler;
        private @Nullable BiConsumer<T, V> injector;
        private @Nullable Template<V> template;

        private Builder(@NotNull Class<T> type) {
            this.type = Objects.requireNonNull(type);
            this.sampler = null;
            this.injector = null;
            this.template = null;
        }

        @Contract("_ -> this")
        public Builder<T, V> onSample(@NotNull Function<T, V> sampler) {
            this.sampler = Objects.requireNonNull(sampler);
            return this;
        }

        @Contract("_ -> this")
        public Builder<T, V> onInject(@NotNull BiConsumer<T, V> injector) {
            this.injector = Objects.requireNonNull(injector);
            return this;
        }

        @Contract("_ -> this")
        public Builder<T, V> withTemplate(@NotNull Template<V> template) {
            this.template = Objects.requireNonNull(template);
            return this;
        }

        public @NotNull Vaccine<T, V> build() {
            Objects.requireNonNull(this.sampler, "Attempted to build a vaccine without a sampler.");
            Objects.requireNonNull(this.injector, "Attempted to build a vaccine without an injector.");
            Objects.requireNonNull(this.template, "Attempted to build a vaccine without a template.");
            return new Vaccine<>(this.sampler, this.injector, this.template, this.type);
        }
    }
}
