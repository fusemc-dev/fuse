package dev.fusemc.standard.entity;

import com.manchickas.optionated.Option;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.ProxySlot;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.syringe.Syringe;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.SlotRanges;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.NoSuchElementException;
import java.util.Objects;

@Documented("Entity")
public class ProxyEntity<T extends Entity> implements ProxyObject {

    private static final @NotNull String TYPE = "type";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String INJECT = "inject";
    private static final @NotNull String SAMPLE = "sample";
    private static final @NotNull String SLOT = "slot";
    private static final @NotNull String IS_SHIFT_DOWN = "isShiftDown";
    private static final @NotNull String TO_STRING = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyEntity.TYPE,
            ProxyEntity.IS_OF,
            ProxyEntity.IS_IN,
            ProxyEntity.INJECT,
            ProxyEntity.SAMPLE,
            ProxyEntity.IS_SHIFT_DOWN,
            ProxyEntity.SLOT,
            ProxyEntity.TO_STRING,
    };

    protected final @NotNull T self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable inject;
    private final @NotNull ProxyExecutable sample;
    private final @NotNull ProxyExecutable slot;
    private final @NotNull ProxyExecutable isShiftDown;
    private final @NotNull ProxyExecutable toString;

    protected ProxyEntity(@NonNull T self) {
        this.self = Objects.requireNonNull(self);
        this.type = ProxyIdentifier.from(BuiltInRegistries.ENTITY_TYPE.getKey(this.self.getType()));
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var type = BuiltInRegistries.ENTITY_TYPE.get(identifier)
                        .map(Holder.Reference::value);
                if (type.isPresent()) {
                    var unwrapped = type.get();
                    return this.self.getType() == unwrapped;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key = TagKey.create(Registries.ENTITY_TYPE, identifier);
                return this.self.getType().is(key);
            }
            throw new UnsupportedOperationException();
        };
        this.inject = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var vaccine    = Syringe.REGISTRY.getValue(identifier);
                if (vaccine != null)
                    return vaccine.attemptInject(this.self, args[1]);
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.sample = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var vaccine    = Syringe.REGISTRY.getValue(identifier);
                if (vaccine != null) {
                    var option = vaccine.attemptSample(this.self);
                    if (option instanceof Option.Some<Value>(var result))
                        return result;
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.slot = (args) -> {
            if (args.length == 1) {
                var name  = Tau.lower(Template.STRING, args[0]);
                var range = SlotRanges.nameToIds(name);
                if (range != null) {
                    var slots = range.slots();
                    if (slots.size() == 1) {
                        var access = this.self.getSlot(slots.getFirst());
                        if (access != null)
                            return ProxySlot.from(this.self, access);
                        return Tau.undefined();
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.isShiftDown = (args) -> {
            if (args.length == 0)
                return this.self.isShiftKeyDown();
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return this.self.toString();
            throw new UnsupportedOperationException();
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @NotNull ProxyEntity<T> from(@NotNull T self) {
        Objects.requireNonNull(self);
        return switch (self) {
            case ServerPlayer player -> (ProxyEntity<T>) ProxyPlayer.from(player);
            default -> new ProxyEntity<>(self);
        };
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyEntity.TYPE      -> this.type;
            case ProxyEntity.IS_OF     -> this.isOf;
            case ProxyEntity.IS_IN     -> this.isIn;
            case ProxyEntity.INJECT    -> this.inject;
            case ProxyEntity.SAMPLE    -> this.sample;
            case ProxyEntity.SLOT -> this.slot;
            case ProxyEntity.IS_SHIFT_DOWN -> this.isShiftDown;
            case ProxyEntity.TO_STRING -> this.toString;
            default -> throw new NoSuchElementException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyEntity.KEYS.length)
                    return ProxyEntity.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyEntity.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyEntity.KEYS) {
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
}
