package dev.fusemc.standard.entity;

import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ProxyAttribute implements ProxyObject {

    private static final @NotNull String TYPE      = "type";
    private static final @NotNull String IS_OF     = "isOf";
    private static final @NotNull String IS_IN     = "isIn";
    private static final @NotNull String COMPUTE   = "compute";
    private static final @NotNull String BASE      = "base";
    private static final @NotNull String RESET     = "reset";
    private static final @NotNull String MODIFY    = "modify";
    private static final @NotNull String MODIFIERS = "modifiers";
    private static final @NotNull String HAS       = "has";
    private static final @NotNull String REMOVE    = "remove";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyAttribute.COMPUTE,
            ProxyAttribute.BASE,
            ProxyAttribute.RESET,
            ProxyAttribute.MODIFY,
            ProxyAttribute.MODIFIERS,
            ProxyAttribute.HAS,
            ProxyAttribute.REMOVE,
    };

    private final @NotNull AttributeInstance self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable compute;
    private final @NotNull ProxyExecutable base;
    private final @NotNull ProxyExecutable reset;
    private final @NotNull ProxyExecutable modify;
    private final @NotNull ProxyExecutable modifiers;
    private final @NotNull ProxyExecutable has;
    private final @NotNull ProxyExecutable remove;

    public ProxyAttribute(@NotNull AttributeInstance self) {
        this.self = Objects.requireNonNull(self);
        this.type = self.getAttribute()
                .unwrapKey()
                .map(ResourceKey::identifier)
                .map(ProxyIdentifier::from)
                .orElseThrow(AssertionError::new);
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                return this.self.getAttribute().is(identifier);
            }
            return false;
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                return this.self.getAttribute().is(TagKey.create(Registries.ATTRIBUTE, identifier));
            }
            return false;
        };
        this.compute = (args) -> {
            if (args.length == 0)
                return this.self.getValue();
            throw new UnsupportedOperationException();
        };
        this.base = (args) -> {
            if (args.length == 0)
                return this.self.getBaseValue();
            if (args.length == 1) {
                this.self.setBaseValue(Tau.lower(Template.DOUBLE, args[0]));
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.reset = (args) -> {
            if (args.length == 0) {
                var attribute = this.self.getAttribute().value();
                this.self.setBaseValue(attribute.getDefaultValue());
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.modify = (args) -> {
            if (args.length == 2) {
                var modifier  = Tau.lower(ModifierDef.TEMPLATE, args[1]);
                if (modifier.temporary()) {
                    self.addOrUpdateTransientModifier(modifier.create());
                    return true;
                }
                self.addOrReplacePermanentModifier(modifier.create());
                return true;
            }
            throw new UnsupportedOperationException();
        };
        this.modifiers = (args) -> {
            if (args.length == 0)
                return this.self.getModifiers().stream()
                        .map(AttributeModifier::id)
                        .map(ProxyIdentifier::from)
                        .toArray(ProxyIdentifier[]::new);
            throw new UnsupportedOperationException();
        };
        this.has = (args) -> {
            if (args.length == 1) {
                var type = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                return this.self.hasModifier(type);
            }
            throw new UnsupportedOperationException();
        };
        this.remove = (args) -> {
            if (args.length == 1) {
                var type = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                this.self.removeModifier(type);
                return true;
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyAttribute from(@NotNull AttributeInstance self) {
        Objects.requireNonNull(self);
        return new ProxyAttribute(self);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyAttribute.TYPE      -> this.type;
            case ProxyAttribute.IS_OF     -> this.isOf;
            case ProxyAttribute.IS_IN     -> this.isIn;
            case ProxyAttribute.COMPUTE   -> this.compute;
            case ProxyAttribute.BASE      -> this.base;
            case ProxyAttribute.RESET     -> this.reset;
            case ProxyAttribute.MODIFY    -> this.modify;
            case ProxyAttribute.MODIFIERS -> this.modifiers;
            case ProxyAttribute.HAS       -> this.has;
            case ProxyAttribute.REMOVE    -> this.remove;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyAttribute.KEYS.length)
                    return ProxyAttribute.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyAttribute.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyAttribute.KEYS) {
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
