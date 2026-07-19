package dev.fusemc.standard.entity.living;

import dev.fusemc.standard.EffectDef;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.entity.ProxyAttribute;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Documented("Living")
public class ProxyLiving<T extends LivingEntity> extends ProxyEntity<T> {

    private static final @NotNull String EFFECT          = "effect";
    private static final @NotNull String CLEAR_EFFECT    = "clearEffect";
    private static final @NotNull String ATTRIBUTE       = "attribute";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyLiving.EFFECT,
            ProxyLiving.CLEAR_EFFECT,
            ProxyLiving.ATTRIBUTE,
    };

    private final @NotNull ProxyExecutable effect;
    private final @NotNull ProxyExecutable clearEffect;
    private final @NotNull ProxyExecutable attribute;

    protected ProxyLiving(@NotNull T self) {
        super(Objects.requireNonNull(self));
        this.effect = (args) -> {
            if (args.length == 1) {
                var definition = Tau.lower(EffectDef.TEMPLATE, args[0]);
                return this.self.addEffect(definition.create());
            }
            throw new UnsupportedOperationException();
        };
        this.clearEffect = (args) -> {
            if (args.length == 1) {
                var type = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                return BuiltInRegistries.MOB_EFFECT.get(type)
                        .map(this.self::removeEffect)
                        .orElse(false);
            }
            throw new UnsupportedOperationException();
        };
        this.attribute = (args) -> {
            if (args.length == 1) {
                var type     = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var attribute = BuiltInRegistries.ATTRIBUTE.get(type);
                if (attribute.isPresent()) {
                    var instance = this.self.getAttribute(attribute.get());
                    if (instance != null)
                        return ProxyAttribute.from(instance);
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
    }

    public static <T extends LivingEntity> ProxyLiving<T> from(@NotNull T self) {
        Objects.requireNonNull(self);
        return new ProxyLiving<>(self);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyLiving.EFFECT -> this.effect;
            case ProxyLiving.CLEAR_EFFECT -> this.clearEffect;
            case ProxyLiving.ATTRIBUTE -> this.attribute;
            default -> super.getMember(key);
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyLiving.KEYS) {
            if (candidate.equals(key))
                return true;
        }
        return super.hasMember(key);
    }

    @Override
    public ProxyArray getMemberKeys() {
        var parent = super.getMemberKeys();
        return new ProxyArray() {

            @Override
            public Object get(long index) {
                var length = ProxyLiving.KEYS.length;
                if (index >= 0 && index < length)
                    return ProxyLiving.KEYS[(int) index];
                return parent.get(index - length);
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyLiving.KEYS.length + parent.getSize();
            }
        };
    }
}
