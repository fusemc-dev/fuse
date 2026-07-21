package dev.fusemc.standard.math;

import dev.fusemc.iota.Standardized;
import dev.fusemc.marshal.standard.ProxyContext;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Inspectable;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

/// A three-dimensional-vector.
///
/// ---
/// `ScriptVec3` represents a three-dimensional vector of three `doubles`. It is intended
/// as a way to expose three-dimensional vectors to scripts.
///
/// A `ScriptVec3` is parsed with the following [Template]:
///
/// ```typescript
/// type Vec3 = Vec3 | [number, number, number]
/// ```
///
/// @since 0.1.0
/// @see ProxyVec2
@Documented("Vec3")
@Standardized("vec3")
public final class ProxyVec3 implements ProxyObject, ProxyIterable, Inspectable {

    public static final @NotNull ProxyVec3 ZERO = new ProxyVec3(0, 0, 0);

    private static final @NotNull String X = "x";
    private static final @NotNull String Y = "y";
    private static final @NotNull String Z = "z";
    private static final @NotNull String ADD = "add";
    private static final @NotNull String SUB = "sub";
    private static final @NotNull String SCALE = "scale";
    private static final @NotNull String CENTER = "center";
    private static final @NotNull String DISTANCE_TO = "distanceTo";
    private static final @NotNull String SQUARED_DISTANCE_TO = "squaredDistanceTo";
    private static final @NotNull String MIN = "min";
    private static final @NotNull String MAX = "max";
    private static final @NotNull String TO_STRING = "toString";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyVec3.X,
            ProxyVec3.Y,
            ProxyVec3.Z,
            ProxyVec3.ADD,
            ProxyVec3.SUB,
            ProxyVec3.SCALE,
            ProxyVec3.CENTER,
            ProxyVec3.DISTANCE_TO,
            ProxyVec3.SQUARED_DISTANCE_TO,
            ProxyVec3.MIN,
            ProxyVec3.MAX,
            ProxyVec3.TO_STRING,
    };

    public static final @NotNull Template<ProxyVec3> TEMPLATE = Template.union(
            Template.tuple(
                    Template.DOUBLE.element(vec -> vec.x),
                    Template.DOUBLE.element(vec -> vec.y),
                    Template.DOUBLE.element(vec -> vec.z),
                    ProxyVec3::new
            ),
            Template.reference(ProxyVec3.class)
    );

    public final double x;
    public final double y;
    public final double z;
    private final @NotNull ProxyExecutable add;
    private final @NotNull ProxyExecutable sub;
    private final @NotNull ProxyExecutable scale;
    private final @NotNull ProxyExecutable center;
    private final @NotNull ProxyExecutable distanceTo;
    private final @NotNull ProxyExecutable squaredDistanceTo;
    private final @NotNull ProxyExecutable min;
    private final @NotNull ProxyExecutable max;
    private final @NotNull ProxyExecutable toString;

    public ProxyVec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.add = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return new ProxyVec3(this.x + other.x, this.y + other.y, this.z + other.z);
            }
            throw new UnsupportedOperationException();
        };
        this.sub = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return new ProxyVec3(this.x - other.x, this.y - other.y, this.z - other.z);
            }
            throw new UnsupportedOperationException();
        };
        this.scale = (args) -> {
            if (args.length == 1) {
                double factor = Tau.lower(Template.DOUBLE, args[0]);
                return new ProxyVec3(this.x * factor, this.y * factor, this.z * factor);
            }
            throw new UnsupportedOperationException();
        };
        this.center = (args) -> {
            if (args.length == 0)
                return new ProxyVec3(Math.floor(this.x) + 0.5, Math.floor(this.y) + 0.5, Math.floor(this.z) + 0.5);
            throw new UnsupportedOperationException();
        };
        this.distanceTo = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2));
            }
            throw new UnsupportedOperationException();
        };
        this.squaredDistanceTo = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2);
            }
            throw new UnsupportedOperationException();
        };
        this.min = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return new ProxyVec3(
                        Math.min(this.x, other.x),
                        Math.min(this.y, other.y),
                        Math.min(this.z, other.z)
                );
            }
            throw new UnsupportedOperationException();
        };
        this.max = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyVec3.TEMPLATE, args[0]);
                return new ProxyVec3(
                        Math.max(this.x, other.x),
                        Math.max(this.y, other.y),
                        Math.max(this.z, other.z)
                );
            }
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return this.toString();
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyVec3 from(@NotNull Vec3 vec) {
        Objects.requireNonNull(vec);
        return new ProxyVec3(vec.x, vec.y, vec.z);
    }

    public static @NotNull ProxyVec3 from(@NotNull BlockPos pos) {
        Objects.requireNonNull(pos);
        return new ProxyVec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public @NotNull BlockPos toBlockPos() {
        return new BlockPos((int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.floor(this.z));
    }

    public @NotNull Vec3 toVec3() {
        return new Vec3(this.x, this.y, this.z);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyVec3.X                   -> this.x;
            case ProxyVec3.Y                   -> this.y;
            case ProxyVec3.Z                   -> this.z;
            case ProxyVec3.ADD                 -> this.add;
            case ProxyVec3.SUB                 -> this.sub;
            case ProxyVec3.SCALE               -> this.scale;
            case ProxyVec3.CENTER              -> this.center;
            case ProxyVec3.DISTANCE_TO         -> this.distanceTo;
            case ProxyVec3.SQUARED_DISTANCE_TO -> this.squaredDistanceTo;
            case ProxyVec3.MIN                 -> this.min;
            case ProxyVec3.MAX                 -> this.max;
            case ProxyVec3.TO_STRING           -> this.toString;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyVec3.KEYS.length)
                    return ProxyVec3.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyVec3.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyVec3.KEYS) {
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
    @HostAccess.Export
    public @NotNull String toString() {
        return "[object Vec3]";
    }

    @Override
    public ProxyIterator getIterator() {
        return new ProxyIterator() {

            private int position = 0;

            @Override
            public Double getNext() throws NoSuchElementException {
                return switch (this.position++) {
                    case 0 -> ProxyVec3.this.x;
                    case 1 -> ProxyVec3.this.y;
                    case 2 -> ProxyVec3.this.z;
                    default -> throw new NoSuchElementException();
                };
            }

            @Override
            public boolean hasNext() {
                return this.position < 3;
            }
        };
    }

    @Override
    public @NotNull Description inspect() {
        return Description.concat(
                Description.reference(ProxyVec3.class),
                Description.delimiter(' '),
                Description.concat(
                        Description.delimiter('['),
                        Description.join(Description.delimiter(", "),
                                Description.numeric(this.x),
                                Description.numeric(this.y),
                                Description.numeric(this.z)),
                        Description.delimiter(']')
                )
        );
    }
}
