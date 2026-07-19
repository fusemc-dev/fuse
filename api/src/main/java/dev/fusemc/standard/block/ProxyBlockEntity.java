package dev.fusemc.standard.block;

import dev.fusemc.ValueOps;
import dev.fusemc.pql.PQL;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class ProxyBlockEntity implements ProxyObject {

    private static final @NotNull String TYPE  = "type";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String SLOT   = "slot";
    private static final @NotNull String INSERT = "insert";
    private static final @NotNull String DROP   = "drop";
    private static final @NotNull String GET = "get";
    private static final @NotNull String SET = "set";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyBlockEntity.TYPE,
            ProxyBlockEntity.IS_OF,
            ProxyBlockEntity.IS_IN,
            ProxyBlockEntity.SLOT,
            ProxyBlockEntity.INSERT,
            ProxyBlockEntity.GET,
            ProxyBlockEntity.SET
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyBlockEntity.class);

    private final @NotNull BlockEntity self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable slot;
    private final @NotNull ProxyExecutable insert;
    private final @NotNull ProxyExecutable drop;
    private final @NotNull ProxyExecutable get;
    private final @NotNull ProxyExecutable set;

    private ProxyBlockEntity(@NotNull BlockEntity self) {
        this.self = Objects.requireNonNull(self);
        this.type = ProxyIdentifier.from(Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(self.getType())));
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var holder = BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(this.self.getType());
                return holder.is(identifier);
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var holder = BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(this.self.getType());
                return holder.is(TagKey.create(Registries.BLOCK_ENTITY_TYPE, identifier));
            }
            return false;
        };
        this.slot = (args) -> {
            if (args.length == 1) {
                var name  = Tau.lower(Template.STRING, args[0]);
                var range = SlotRanges.nameToIds(name);
                if (range != null) {
                    var slots = range.slots();
                    if (slots.size() == 1) {
                        if (this.self instanceof Container container) {
                            var access = container.getSlot(slots.getFirst());
                            if (access != null)
                                return ProxyItem.from(access.get());
                            return Tau.undefined();
                        }
                        return Tau.undefined();
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.insert = (args) -> {
            if (args.length == 2) {
                var name      = Tau.lower(Template.STRING, args[0]);
                var candidate = Tau.lower(ProxyItem.TEMPLATE, args[1]);
                var range     = SlotRanges.nameToIds(name);
                if (range != null) {
                    var slots = range.slots();
                    if (slots.size() == 1) {
                        if (this.self instanceof Container container) {
                            var access = container.getSlot(slots.getFirst());
                            if (access != null)
                                return access.set(candidate.unwrap());
                            return false;
                        }
                        return false;
                    }
                    return false;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.drop = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var position   = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                var world      = (ServerLevel) this.self.getLevel();
                assert world != null;
                if (this.self instanceof Container container) {
                    var key      = ResourceKey.create(Registries.LOOT_TABLE, identifier);
                    var table    = world.getServer()
                            .reloadableRegistries()
                            .getLootTable(key);
                    var entries  = table.getRandomItems(new LootParams.Builder(world)
                            .withParameter(LootContextParams.ORIGIN, position.toVec3())
                            .create(LootContextParamSets.COMMAND));
                    for (var entry : entries) {
                        for (var j = 0; j < container.getContainerSize() && !entry.isEmpty(); j++) {
                            if (container.canPlaceItem(j, entry)) {
                                var slot = container.getItem(j);
                                if (slot.isEmpty()) {
                                    container.setItem(j, entry);
                                    break;
                                }
                                if (ItemStack.isSameItemSameComponents(slot, entry)) {
                                    if (slot.getCount() < slot.getMaxStackSize()) {
                                        var delta = Math.min(slot.getMaxStackSize() - slot.getCount(), entry.getCount());
                                        slot.grow(delta);
                                        entry.shrink(delta);
                                    }
                                }
                            }
                        }
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.get = (args) -> {
            if (args.length == 1) {
                var path = PQL.parse(Tau.lower(Template.STRING, args[0]));
                var world = this.self.getLevel();
                if (world != null)
                    return path.resolve(this.self.saveWithFullMetadata(world.registryAccess()))
                            .map(tag -> NbtOps.INSTANCE.convertTo(ValueOps.INSTANCE, tag))
                            .unwrapOr(Tau::undefined);
                return Tau.undefined();
            }
            return Tau.undefined();
        };
        this.set = (args) -> {
            if (args.length == 2) {
                var path  = PQL.parse(Tau.lower(Template.STRING, args[0]));
                var value = ValueOps.INSTANCE.convertTo(NbtOps.INSTANCE, args[1]);
                var world = this.self.getLevel();
                if (world != null) {
                    var data = this.self.saveWithFullMetadata(world.registryAccess());
                    if (path.update(data, value)) {
                        try (var collector = new ProblemReporter.ScopedCollector(this.self.problemPath(), LOGGER)) {
                            this.self.loadWithComponents(TagValueInput.create(collector, world.registryAccess(), data));
                        }
                        return true;
                    }
                    return false;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyBlockEntity from(@NotNull BlockEntity self) {
        Objects.requireNonNull(self);
        return new ProxyBlockEntity(self);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyBlockEntity.TYPE   -> this.type;
            case ProxyBlockEntity.IS_OF  -> this.isOf;
            case ProxyBlockEntity.IS_IN  -> this.isIn;
            case ProxyBlockEntity.SLOT   -> this.slot;
            case ProxyBlockEntity.INSERT -> this.insert;
            case ProxyBlockEntity.DROP   -> this.drop;
            case ProxyBlockEntity.GET    -> this.get;
            case ProxyBlockEntity.SET    -> this.set;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public Object get(long index) {
                if (index >= 0 && index < ProxyBlockEntity.KEYS.length)
                    return ProxyBlockEntity.KEYS[(int) index];
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyBlockEntity.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyBlockEntity.KEYS)
            if (key.equals(candidate))
                return true;
        return false;
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }
}
