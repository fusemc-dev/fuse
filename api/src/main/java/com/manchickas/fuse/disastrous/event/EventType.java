package com.manchickas.fuse.disastrous.event;

import com.manchickas.fuse.disastrous.guard.GuardType;
import com.manchickas.fuse.standard.event.JoinEvent;
import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import com.manchickas.optionated.Option;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class EventType<E extends Event<E, C>, C extends EventCallback> {

    private static final ResourceKey<Registry<EventType<?, ?>>> REGISTRY_KEY
            = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("fuse","event_type"));
    public static final Registry<EventType<?, ?>> REGISTRY
            = new MappedRegistry<>(EventType.REGISTRY_KEY, Lifecycle.stable());

    static {
        Registry.register(REGISTRY, Identifier.withDefaultNamespace("join"), JoinEvent.TYPE);
    }

    private final @NotNull Template<C> template;
    private final @NotNull Function<@NotNull String, @NotNull Option<GuardType<? super E, ?>>> dispatch;

    public EventType(@NotNull Template<C> template,
                     @NotNull Function<String, Option<GuardType<? super E, ?>>> dispatch) {
        this.template = Objects.requireNonNull(template);
        this.dispatch = Objects.requireNonNull(dispatch);
    }

    public static @NotNull Option<EventType<?, ?>> lookup(@NotNull Identifier name) {
        return Option.fromNullable(EventType.REGISTRY.getValue(name));
    }

    public @NotNull Option<GuardType<? super E, ?>> guard(@NotNull String name) {
        Objects.requireNonNull(name);
        return this.dispatch.apply(name);
    }

    public @NotNull C expect(@NotNull Value value) throws TypeException {
        Objects.requireNonNull(value);
        return Jet.expect(this.template, value);
    }

    @Override
    public String toString() {
        return String.format("EventType[%s]", EventType.REGISTRY.getKey(this));
    }
}
