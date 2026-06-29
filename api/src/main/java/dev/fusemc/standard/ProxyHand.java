package dev.fusemc.standard;

import net.minecraft.world.InteractionHand;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class ProxyHand implements ProxyObject {

    private static final @NotNull ProxyHand MAIN = new ProxyHand(InteractionHand.MAIN_HAND);
    private static final @NotNull ProxyHand OFF  = new ProxyHand(InteractionHand.OFF_HAND);

    private static final @NotNull String IS_MAIN   = "isMain";
    private static final @NotNull String IS_OFF    = "isOff";
    private static final @NotNull String TO_STRING = "toString";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyHand.IS_MAIN,
            ProxyHand.IS_OFF,
            ProxyHand.TO_STRING,
    };

    private final @NotNull InteractionHand hand;
    private final @NotNull ProxyExecutable isMain;
    private final @NotNull ProxyExecutable isOff;
    private final @NotNull ProxyExecutable toString;

    private ProxyHand(@NotNull InteractionHand hand) {
        this.hand = Objects.requireNonNull(hand);
        this.isMain = (args) -> {
            if (args.length == 0)
                return this.hand == InteractionHand.MAIN_HAND;
            throw new UnsupportedOperationException();
        };
        this.isOff = (args) -> {
            if (args.length == 0)
                return this.hand == InteractionHand.OFF_HAND;
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return switch (this.hand) {
                    case MAIN_HAND -> "main";
                    case OFF_HAND  -> "off";
                };
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyHand from(@NotNull InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> ProxyHand.MAIN;
            case OFF_HAND  -> ProxyHand.OFF;
        };
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyHand.IS_MAIN   -> this.isMain;
            case ProxyHand.IS_OFF    -> this.isOff;
            case ProxyHand.TO_STRING -> this.toString;
            default -> throw new NoSuchElementException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyHand.KEYS.length)
                    return ProxyHand.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyHand.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyHand.KEYS) {
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

    public boolean matches(@NotNull InteractionHand hand) {
        return this.hand == hand;
    }
}
