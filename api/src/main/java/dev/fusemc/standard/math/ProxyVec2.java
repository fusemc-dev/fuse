package dev.fusemc.standard.math;

import dev.fusemc.iota.Standardized;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.world.phys.Vec2;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

/// A two-dimensional-vector.
///
/// `ProxyVec2` represents a two-dimensional vector of two `doubles`. It is intended
/// as a way to expose two-dimensional vectors to scripts.
///
/// A `ProxyVec2` is parsed with the following [Template]:
///
/// ```
/// Vec2 | [double, double]
/// ```
///
/// @since `0.1.0`
/// @see ProxyVec3
@Documented("Vec2")
@Standardized("Vec2")
public final class ProxyVec2 implements ProxyObject, ProxyIterable {

    private static final @NotNull String X                   = "x";
    private static final @NotNull String Y                   = "y";
    private static final @NotNull String ADD                 = "add";
    private static final @NotNull String SUB                 = "sub";
    private static final @NotNull String SCALE               = "scale";
    private static final @NotNull String DISTANCE_TO         = "distanceTo";
    private static final @NotNull String SQUARED_DISTANCE_TO = "squaredDistanceTo";
    private static final @NotNull String TO_STRING           = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyVec2.X,
            ProxyVec2.Y,
            ProxyVec2.ADD,
            ProxyVec2.SUB,
            ProxyVec2.SCALE,
            ProxyVec2.DISTANCE_TO,
            ProxyVec2.SQUARED_DISTANCE_TO,
            ProxyVec2.TO_STRING,
    };

    public static final Template<ProxyVec2> TEMPLATE = Template.union(
            Template.reference(ProxyVec2.class),
            Template.tuple(
                    Template.DOUBLE.element(vec -> vec.x),
                    Template.DOUBLE.element(vec -> vec.y),
                    ProxyVec2::new
            )
    );
    private static final @NotNull ProxyExecutable TO_STRING_IMPL = args -> {
        if (args.length == 0)
            return "[object Vec2]";
        throw new UnsupportedOperationException();
    };

    public final double x;
    public final double y;

    private final @NotNull ProxyExecutable add;
    private final @NotNull ProxyExecutable sub;
    private final @NotNull ProxyExecutable scale;
    private final @NotNull ProxyExecutable distanceTo;
    private final @NotNull ProxyExecutable squaredDistanceTo;

    public ProxyVec2(double x, double y) {
        this.x = x;
        this.y = y;
        this.add = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec2.TEMPLATE, args[0]);
                return new ProxyVec2(this.x + other.x, this.y + other.y);
            }
            throw new UnsupportedOperationException();
        };
        this.sub = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec2.TEMPLATE, args[0]);
                return new ProxyVec2(this.x - other.x, this.y - other.y);
            }
            throw new UnsupportedOperationException();
        };
        this.scale = (args) -> {
            if (args.length == 1) {
                double factor = Tau.lower(Template.DOUBLE, args[0]);
                return new ProxyVec2(this.x * factor, this.y * factor);
            }
            throw new UnsupportedOperationException();
        };
        this.squaredDistanceTo = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec2.TEMPLATE, args[0]);
                return Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2);
            }
            throw new UnsupportedOperationException();
        };
        this.distanceTo = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec2.TEMPLATE, args[0]);
                return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
            }
            throw new UnsupportedOperationException();
        };
    }

    /// Constructs a `ScriptVec2` from the given [Vec2].
    ///
    /// @since `0.1.0`
    public static @NotNull ProxyVec2 fromVec2(@NotNull Vec2 vec) {
        Objects.requireNonNull(vec);
        return new ProxyVec2(vec.x, vec.y);
    }

    /// Converts the given `ScriptVec2` to a [Vec2].
    ///
    /// @since `0.1.0`
    public static @NotNull Vec2 toVec2(@NotNull ProxyVec2 vec) {
        Objects.requireNonNull(vec);
        return new Vec2((float) vec.x, (float) vec.y);
    }

    @Override
    public @NotNull Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyVec2.X                   -> this.x;
            case ProxyVec2.Y                   -> this.y;
            case ProxyVec2.ADD                 -> this.add;
            case ProxyVec2.SUB                 -> this.sub;
            case ProxyVec2.SCALE               -> this.scale;
            case ProxyVec2.DISTANCE_TO         -> this.distanceTo;
            case ProxyVec2.SQUARED_DISTANCE_TO -> this.squaredDistanceTo;
            case ProxyVec2.TO_STRING           -> ProxyVec2.TO_STRING_IMPL;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var entry : ProxyVec2.KEYS) {
            if (entry.equals(key))
                return true;
        }
        return false;
    }

    @Override
    public @NotNull ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyVec2.KEYS.length)
                    return ProxyVec2.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyVec2.KEYS.length;
            }
        };
    }

    @Override
    @HostAccess.Export
    public @NotNull ProxyIterator getIterator() {
        return new ProxyIterator() {

            private int position = 0;

            @Override
            public boolean hasNext() {
                return this.position < 2;
            }

            @Override
            public @NotNull Double getNext() throws NoSuchElementException {
                return switch (this.position++) {
                    case 0 -> ProxyVec2.this.x;
                    case 1 -> ProxyVec2.this.y;
                    default -> throw new NoSuchElementException();
                };
            }
        };
    }
}
