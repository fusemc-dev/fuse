package dev.fusemc.standard;

import dev.fusemc.ValueOps;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.tau.Tau;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class ProxySlot implements ProxyObject {

    private static final @NotNull String SET = "set";
    private static final @NotNull String GET = "get";
    private static final @NotNull String MODIFY = "modify";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxySlot.SET,
            ProxySlot.GET,
            ProxySlot.MODIFY,
    };

    private final @NotNull Entity entity;
    private final @NotNull ReloadableServerRegistries.Holder registries;
    private final @NotNull SlotAccess slot;
    private final @NotNull ProxyExecutable get;
    private final @NotNull ProxyExecutable set;
    private final @NotNull ProxyExecutable modify;

    public ProxySlot(@NotNull Entity entity,
                     @NotNull ReloadableServerRegistries.Holder registries,
                     @NotNull SlotAccess slot) {
        this.entity     = Objects.requireNonNull(entity);
        this.registries = Objects.requireNonNull(registries);
        this.slot       = Objects.requireNonNull(slot);
        this.get        = (args) -> {
            if (args.length == 0)
                return ProxyItem.from(this.slot.get());
            throw new UnsupportedOperationException();
        };
        this.set        = (args) -> {
            if (args.length == 1)
                return this.slot.set(ProxyItem.to(Tau.lower(ProxyItem.TEMPLATE, args[0])));
            throw new UnsupportedOperationException();
        };
        this.modify     = (args) -> {
            if (args.length == 1) {
                var type     = ResourceKey.create(Registries.ITEM_MODIFIER, Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]));
                var modifier = this.registries.lookup()
                        .lookupOrThrow(Registries.ITEM_MODIFIER)
                        .get(type)
                        .map(Holder::value);
                if (modifier.isPresent()) {
                    var function = modifier.get();
                    var ctx = new LootContext.Builder(new LootParams.Builder((ServerLevel) this.entity.level())
                            .withParameter(LootContextParams.ORIGIN, this.entity.position())
                            .withParameter(LootContextParams.THIS_ENTITY, this.entity)
                            .create(LootContextParamSets.COMMAND))
                            .create(Optional.empty());
                    ctx.pushVisitedElement(LootContext.createVisitedEntry(function));
                    return slot.set(function.apply(this.slot.get().copy(), ctx));
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxySlot from(@NotNull Entity entity, SlotAccess slot) {
        Objects.requireNonNull(entity);
        Objects.requireNonNull(slot);
        return new ProxySlot(entity, Objects.requireNonNull(entity.level().getServer()).reloadableRegistries(), slot);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxySlot.SET    -> this.set;
            case ProxySlot.GET    -> this.get;
            case ProxySlot.MODIFY -> this.modify;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxySlot.KEYS.length)
                    return ProxySlot.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxySlot.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxySlot.KEYS) {
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
