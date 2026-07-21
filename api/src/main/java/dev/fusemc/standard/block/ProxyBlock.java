package dev.fusemc.standard.block;

import dev.fusemc.ValueOps;
import dev.fusemc.iota.Standardized;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Documented("Block")
@Standardized("Block")
public final class ProxyBlock implements ProxyObject {

    private static final @NotNull String TYPE  = "type";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String WITH  = "with";
    private static final @NotNull String GET   = "get";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyBlock.TYPE,
            ProxyBlock.IS_OF,
            ProxyBlock.IS_IN,
            ProxyBlock.WITH,
            ProxyBlock.GET,
    };

    public static final @NotNull Template<ProxyBlock> TEMPLATE = Template.union(
            ValueOps.delegate(BlockState.CODEC, Description.keyword("Block"))
                    .map(ProxyBlock::from, ProxyBlock::unwrap),
            Template.reference(ProxyBlock.class)
    );

    private final @NotNull BlockState self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable with;
    private final @NotNull ProxyExecutable get;

    private ProxyBlock(@NotNull BlockState self) {
        Objects.requireNonNull(self);
        this.self = Objects.requireNonNull(self);
        this.type = ProxyIdentifier.from(BuiltInRegistries.BLOCK.getKey(self.getBlock()));
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var holder = BuiltInRegistries.BLOCK.wrapAsHolder(this.self.getBlock());
                return holder.is(identifier);
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var holder = BuiltInRegistries.BLOCK.wrapAsHolder(this.self.getBlock());
                return holder.is(TagKey.create(Registries.BLOCK, identifier));
            }
            throw new UnsupportedOperationException();
        };
        this.with = (args) -> {
            if (args.length == 2) {
                var name     = Tau.lower(Template.STRING, args[0]);
                var property = this.self.getBlock()
                        .getStateDefinition()
                        .getProperty(name);
                if (property != null)
                    return this.with(property, args[1]);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.get = (args) -> {
            if (args.length == 1) {
                var name     = Tau.lower(Template.STRING, args[0]);
                var property = this.self.getBlock()
                        .getStateDefinition()
                        .getProperty(name);
                if (property != null)
                    return this.self.getValue(property);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyBlock from(@NotNull BlockState self) {
        Objects.requireNonNull(self);
        return new ProxyBlock(self);
    }

    private <T extends Comparable<T>> @NotNull ProxyBlock with(@NotNull Property<T> property,
                                                               @NotNull Value value) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(value);
        var result = Tau.lower(ValueOps.delegate(property.codec(), Description.ELLIPSIS), value);
        return ProxyBlock.from(this.self.setValue(property, result));
    }

    @Override
    public Object getMember(String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyBlock.TYPE  -> this.type;
            case ProxyBlock.IS_OF -> this.isOf;
            case ProxyBlock.IS_IN -> this.isIn;
            case ProxyBlock.WITH  -> this.with;
            case ProxyBlock.GET   -> this.get;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyBlock.KEYS.length)
                    return ProxyBlock.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyBlock.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyBlock.KEYS) {
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

    public @NotNull BlockState unwrap() {
        return this.self;
    }
}
