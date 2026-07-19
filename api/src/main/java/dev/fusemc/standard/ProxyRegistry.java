package dev.fusemc.standard;

import dev.fusemc.tau.Tau;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class ProxyRegistry<T> implements ProxyObject, ProxyIterable {

    private static final @NotNull String TYPE = "type";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String CONTAINS = "contains";
    private static final @NotNull String ENTRIES  = "entries";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyRegistry.TYPE,
            ProxyRegistry.IS_OF,
            ProxyRegistry.CONTAINS,
            ProxyRegistry.ENTRIES,
    };

    private final @NotNull HolderLookup.RegistryLookup<T> self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable contains;
    private final @NotNull ProxyExecutable entries;

    @SuppressWarnings("unchecked")
    private ProxyRegistry(@NotNull HolderLookup.RegistryLookup<T> self) {
        this.self = Objects.requireNonNull(self);
        this.type = ProxyIdentifier.from(self.key().identifier());
        this.isOf = (args) -> {
            if (args.length == 1) {
                var type = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                return BuiltInRegistries.REGISTRY.get(type)
                        .filter(this.self::equals)
                        .isPresent();
            }
            throw new UnsupportedOperationException();
        };
        this.contains = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key        = ResourceKey.create((ResourceKey<? extends Registry<T>>) this.self.key(), identifier);
                return this.self.get(key).isPresent();
            }
            throw new UnsupportedOperationException();
        };
        this.entries = (args) -> {
            if (args.length == 0)
                return this.self.listElementIds()
                        .map(ResourceKey::identifier)
                        .map(ProxyIdentifier::from)
                        .toArray(ProxyIdentifier[]::new);
            throw new UnsupportedOperationException();
        };
    }

    public static <T> @NotNull ProxyRegistry<T> from(@NotNull HolderLookup.RegistryLookup<T> self) {
        Objects.requireNonNull(self);
        return new ProxyRegistry<>(self);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyRegistry.TYPE      -> this.type;
            case ProxyRegistry.IS_OF     -> this.isOf;
            case ProxyRegistry.CONTAINS  -> this.contains;
            case ProxyRegistry.ENTRIES   -> this.entries;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyRegistry.KEYS.length)
                    return ProxyRegistry.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyRegistry.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyRegistry.KEYS) {
            if (candidate.equals(key))
                return true;
        }
        return false;
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ProxyIterator getIterator() {
        return ProxyIterator.from(ProxyRegistry.this.self.listElementIds()
                .map(ResourceKey::identifier)
                .map(ProxyIdentifier::from)
                .iterator());
    }
}
