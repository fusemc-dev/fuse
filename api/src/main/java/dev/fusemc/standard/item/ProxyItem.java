package dev.fusemc.standard.item;

import com.mojang.serialization.Codec;
import dev.fusemc.ValueOps;
import dev.fusemc.iota.Standardized;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

@Documented("Item")
@Standardized("Item")
public final class ProxyItem implements ProxyObject {

    private static final @NotNull String TYPE      = "type";
    private static final @NotNull String COUNT     = "count";
    private static final @NotNull String DECREMENT = "decrement";
    private static final @NotNull String INCREMENT = "increment";
    private static final @NotNull String IS_EMPTY  = "isEmpty";
    private static final @NotNull String GET       = "get";
    private static final @NotNull String SET       = "set";
    private static final @NotNull String IS_OF     = "isOf";
    private static final @NotNull String IS_IN     = "isIn";
    private static final @NotNull String TO_STRING = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyItem.TYPE,
            ProxyItem.COUNT,
            ProxyItem.DECREMENT,
            ProxyItem.INCREMENT,
            ProxyItem.IS_EMPTY,
            ProxyItem.GET,
            ProxyItem.SET,
            ProxyItem.IS_OF,
            ProxyItem.IS_IN,
            ProxyItem.TO_STRING,
    };

    public static final @NotNull Template<ProxyItem> TEMPLATE = Template.union(
            Template.record(
                    ValueOps.registered(BuiltInRegistries.ITEM).property("type", item -> item.self.getItemHolder()),
                    Template.INTEGER.property("count", item -> item.self.getCount()),
                    ValueOps.delegate(DataComponentPatch.CODEC, Description.ELLIPSIS)
                            .<ProxyItem>property("components", item -> item.self.getComponentsPatch())
                            .optional(() -> DataComponentPatch.EMPTY),
                    (type, count, components) -> new ProxyItem(new ItemStack(type, count, components))
            ),
            Template.reference(ProxyItem.class)
    );

    private final @NotNull ItemStack self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable decrement;
    private final @NotNull ProxyExecutable increment;
    private final @NotNull ProxyExecutable isEmpty;
    private final @NotNull ProxyExecutable get;
    private final @NotNull ProxyExecutable set;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable toString;

    private ProxyItem(@NotNull ItemStack self) {
        this.self = Objects.requireNonNull(self);
        this.type  = ProxyIdentifier.from(BuiltInRegistries.ITEM.getKey(self.getItem()));
        this.decrement = (args) -> {
            if (args.length == 0) {
                this.self.shrink(1);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.increment = (args) -> {
            if (args.length == 0) {
                this.self.grow(1);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.isEmpty = (args) -> {
            if (args.length == 0)
                return this.self.isEmpty();
            throw new UnsupportedOperationException();
        };
        this.get = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var component  = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
                if (component != null) {
                    @SuppressWarnings("unchecked")
                    var codec  = (Codec<Object>) component.codecOrThrow();
                    var value  = (Object) this.self.get(component);
                    return codec.encodeStart(ValueOps.INSTANCE, value)
                            .resultOrPartial()
                            .orElseGet(Tau::undefined);
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.set = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                @SuppressWarnings("unchecked")
                var component  = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
                if (component != null) {
                    var codec  = component.codecOrThrow();
                    var result = codec.parse(ValueOps.INSTANCE, args[1])
                            .resultOrPartial();
                    //noinspection OptionalIsPresent
                    if (result.isPresent()) {
                        var unwrapped = result.get();
                        this.self.set(component, unwrapped);
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.isOf = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var entry = BuiltInRegistries.ITEM.get(other);
                if (entry.isPresent()) {
                    var unwrapped = entry.get();
                    return this.self.is(unwrapped);
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key   = TagKey.create(Registries.ITEM, other);
                return this.self.is(key);
            }
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return this.self.toString();
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyItem from(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack);
        return new ProxyItem(stack);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyItem.TYPE      -> this.type;
            case ProxyItem.COUNT     -> this.self.getCount();
            case ProxyItem.DECREMENT -> this.decrement;
            case ProxyItem.INCREMENT -> this.increment;
            case ProxyItem.IS_EMPTY  -> this.isEmpty;
            case ProxyItem.GET       -> this.get;
            case ProxyItem.SET       -> this.set;
            case ProxyItem.IS_OF     -> this.isOf;
            case ProxyItem.IS_IN     -> this.isIn;
            case ProxyItem.TO_STRING -> this.toString;
            default -> throw new NoSuchElementException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyItem.KEYS.length)
                    return ProxyItem.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyItem.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyItem.KEYS) {
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

    public @NotNull ItemStack unwrap() {
        return this.self;
    }
}
