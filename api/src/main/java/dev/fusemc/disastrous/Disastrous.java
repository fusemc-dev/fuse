package dev.fusemc.disastrous;

import com.manchickas.optionated.Option;
import com.mojang.serialization.Lifecycle;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.disastrous.guard.type.HandGuard;
import dev.fusemc.disastrous.guard.type.PeriodicGuard;
import dev.fusemc.disastrous.listener.Parser;
import dev.fusemc.disastrous.listener.selector.Selector;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Template;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/// ![banner](./banner.png)
///
/// ---
///
/// Disastrous is an original take on an **event system**, designed around an event _owning the
/// signature of its [Callback]_ and _its dispatch_.
public final class Disastrous {

    public static final @NotNull Registry<Type<?>> REGISTRY = new MappedRegistry<>(
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("fuse", "disaster")),
            Lifecycle.stable()
    );

    public static final Type<Callback.Join> JOIN = Disastrous.register("join", Callback.Join.TEMPLATE);
    public static final Type<Callback.Load> LOAD = Disastrous.register("load", Callback.Load.TEMPLATE);
    public static final Type<Callback.Unload> UNLOAD = Disastrous.register("unload", Callback.Unload.TEMPLATE);
    public static final Type<Callback.ServerTick> SERVER_TICK = Disastrous.register("tick/server", Callback.ServerTick.TEMPLATE, name -> switch (name) {
        case "periodically" -> Option.some(PeriodicGuard.PARSER);
        default             -> Option.none();
    });
    public static final Type<Callback.WorldTick> WORLD_TICK = Disastrous.register("tick/world", Callback.WorldTick.TEMPLATE, name -> switch (name) {
        case "periodically" -> Option.some(PeriodicGuard.PARSER);
        default             -> Option.none();
    });
    public static final Type<Callback.EntityTick> ENTITY_TICK = Disastrous.register("tick/entity", Callback.EntityTick.TEMPLATE, name -> switch (name) {
        case "periodically" -> Option.some(PeriodicGuard.PARSER);
        default             -> Option.none();
    });
    public static final Type<Callback.ItemInteract> ITEM_INTERACT = Disastrous.register("interact/item", Callback.ItemInteract.TEMPLATE, name -> switch (name) {
        case "hand" -> Option.some(HandGuard.PARSER);
        default     -> Option.none();
    });
    public static final Type<Callback.BlockInteract> BLOCK_INTERACT = Disastrous.register("interact/block", Callback.BlockInteract.TEMPLATE, name -> switch (name) {
        case "hand" -> Option.some(HandGuard.PARSER);
        default     -> Option.none();
    });
    public static final Type<Callback.EntityInteract> ENTITY_INTERACT = Disastrous.register("interact/entity", Callback.EntityInteract.TEMPLATE, name -> switch (name) {
        case "hand" -> Option.some(HandGuard.PARSER);
        default     -> Option.none();
    });

    private Disastrous() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Selector parse(@NotNull String source) {
        Objects.requireNonNull(source);
        var parser = new Parser(source);
        return parser.parse();
    }

    public static @NotNull Option<Type<?>> dispatch(@NotNull ProxyIdentifier identifier) {
        return Option.fromNullable(Disastrous.REGISTRY.getValue(ProxyIdentifier.to(identifier)));
    }

    private static <T extends Callback> @NotNull Type<T> register(@NotNull String name,
                                                                  @NotNull Template<T> template) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(template);
        return Disastrous.register(name, template, (_) -> Option.none());
    }

    private static <T extends Callback> @NotNull Type<T> register(@NotNull String name,
                                                                  @NotNull Template<T> template,
                                                                  @NotNull Function<String, Option<Guard.Parser<?>>> dispatch) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(template);
        Objects.requireNonNull(dispatch);
        return Registry.register(Disastrous.REGISTRY, Identifier.withDefaultNamespace(name), new Type<>(template, dispatch));
    }
}
