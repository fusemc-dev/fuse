package dev.fusemc.disastrous;

import com.manchickas.optionated.Option;
import com.mojang.serialization.Lifecycle;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.disastrous.guard.type.HandGuard;
import dev.fusemc.disastrous.listener.selector.Selector;
import dev.fusemc.disastrous.listener.Parser;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Template;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// ![banner](./banner.png)
/// # Disastrous
///
/// **Disastrous** is a component of [Fuse](https://fusemc.dev). Disastrous is
/// an original take on an **event system**, designed around an event _owning the
/// signature of its [Callback]_ and _its dispatch_.
public final class Disastrous {

    public static final @NotNull Registry<Disaster.Type<?>> REGISTRY = new MappedRegistry<>(
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("fuse", "disaster")),
            Lifecycle.stable()
    );

    public static final Disaster.Type<Callback.Join> JOIN = Disastrous.register("join", new Disaster.Type<>() {

        @Override
        public @NotNull Template<Callback.Join> template() {
            return Callback.Join.TEMPLATE;
        }

        @Override
        public @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name) {
            return Option.none();
        }
    });
    public static final Disaster.Type<Callback.Tick> TICK = Disastrous.register("tick", new Disaster.Type<>() {

        @Override
        public @NotNull Template<Callback.Tick> template() {
            return Callback.Tick.TEMPLATE;
        }

        @Override
        public @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name) {
            return Option.none();
        }
    });
    public static final Disaster.Type<Callback.ItemInteract> ITEM_INTERACT = Disastrous.register("interact/item", new Disaster.Type<>() {

        @Override
        public @NotNull Template<Callback.ItemInteract> template() {
            return Callback.ItemInteract.TEMPLATE;
        }

        @Override
        public @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name) {
            return switch (name) {
                case "hand" -> Option.some(HandGuard.TYPE);
                default     -> Option.none();
            };
        }
    });
    public static final Disaster.Type<Callback.BlockInteract> BLOCK_INTERACT = Disastrous.register("interact/block", new Disaster.Type<>() {

        @Override
        public @NotNull Template<Callback.BlockInteract> template() {
            return Callback.BlockInteract.TEMPLATE;
        }

        @Override
        public @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name) {
            return switch (name) {
                case "hand" -> Option.some(HandGuard.TYPE);
                default     -> Option.none();
            };
        }
    });
    public static final Disaster.Type<Callback.EntityInteract> ENTITY_INTERACT = Disastrous.register("interact/entity", new Disaster.Type<>() {

        @Override
        public @NotNull Template<Callback.EntityInteract> template() {
            return Callback.EntityInteract.TEMPLATE;
        }

        @Override
        public @NotNull Option<Guard.Type<?>> dispatch(@NotNull String name) {
            return switch (name) {
                case "hand" -> Option.some(HandGuard.TYPE);
                default     -> Option.none();
            };
        }
    });

    private Disastrous() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Selector parse(@NotNull String source) {
        Objects.requireNonNull(source);
        var parser = new Parser(source);
        return parser.parse();
    }

    public static @NotNull Option<Disaster.Type<?>> dispatch(@NotNull ProxyIdentifier identifier) {
        return Option.fromNullable(Disastrous.REGISTRY.getValue(ProxyIdentifier.to(identifier)));
    }

    private static <T extends Callback> @NotNull Disaster.Type<T> register(@NotNull String name, @NotNull Disaster.Type<T> type) {
        return Registry.register(Disastrous.REGISTRY, Identifier.withDefaultNamespace(name), type);
    }
}
