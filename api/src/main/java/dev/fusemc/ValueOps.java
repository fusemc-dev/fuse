package dev.fusemc;

import com.google.common.collect.ImmutableMap;
import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapLike;
import net.minecraft.util.Util;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/// Defines an [DynamicOps] implementation for Polyglot [Value]s.
///
/// Since fuse relies on [Jet], a helper library used to validate Polyglot [Value]s,
/// the `ValueOps` implementation attempts to delegate as much work as possible
/// to [Jet], to reduce the code duplication.
public final class ValueOps implements DynamicOps<Value> {

    private static final @NotNull ValueOps INSTANCE = new ValueOps();
    private static final @NotNull Template<Value[]> ARRAY = Jet.ANY.array(Value[]::new);
    private static final @NotNull Template<Map<String, Value>> OBJECT = Jet.map(Jet.ANY);

    private ValueOps() {
        // Why don't scientists trust atoms?
        // Because they make up everything.
    }

    public static @NotNull ValueOps instance() {
        return ValueOps.INSTANCE;
    }

    @Override
    public <U> @NotNull U convertTo(@NotNull DynamicOps<U> ops,
                                    @NotNull Value value) {
        if (value.isNumber()) {
            if (value.fitsInByte())
                return ops.createByte(value.asByte());
            if (value.fitsInShort())
                return ops.createShort(value.asShort());
            if (value.fitsInInt())
                return ops.createInt(value.asInt());
            if (value.fitsInLong())
                return ops.createLong(value.asLong());
            if (value.fitsInFloat())
                return ops.createFloat(value.asFloat());
            return ops.createDouble(value.asDouble());
        }
        if (value.isBoolean())
            return ops.createBoolean(value.asBoolean());
        if (value.isString())
            return ops.createString(value.asString());
        if (value.hasArrayElements() || value.isHostObject() && value.asHostObject() instanceof Value[])
            return this.convertList(ops, value);
        if (value.hasMembers() && !value.hasArrayElements() || value.isHostObject() && value.asHostObject() instanceof Map<?, ?>)
            return this.convertMap(ops, value);
        return ops.empty();
    }

    @Override
    public @NotNull Value empty() {
        return Jet.undefined();
    }

    @Override
    public @NotNull DataResult<@NotNull Number> getNumberValue(@NotNull Value value) {
        Objects.requireNonNull(value);
        try {
            return DataResult.success(
                    Jet.expect(Jet.NUMBER, value),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public Value createNumeric(@NotNull Number number) {
        Objects.requireNonNull(number);
        return Value.asValue(number);
    }

    @Override
    public @NotNull DataResult<@NotNull String> getStringValue(@NotNull Value value) {
        Objects.requireNonNull(value);
        try {
            return DataResult.success(
                    Jet.expect(Jet.STRING, value),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public Value createString(@NotNull String s) {
        Objects.requireNonNull(s);
        return Value.asValue(s);
    }

    @Override
    public @NotNull DataResult<@NotNull Boolean> getBooleanValue(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            return DataResult.success(
                    Jet.expect(Jet.BOOLEAN, input),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull Value createBoolean(boolean value) {
        return Value.asValue(value);
    }

    @Override
    public @NotNull DataResult<@NotNull Value> mergeToList(@NotNull Value list,
                                         @NotNull Value entry) {
        Objects.requireNonNull(list);
        Objects.requireNonNull(entry);
        try {
            var array = Jet.expect(ValueOps.ARRAY, list);
            return DataResult.success(
                    Value.asValue(ArrayBuilder.withAppended(array, entry)),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull DataResult<@NotNull Value> mergeToMap(@NotNull Value input,
                                        @NotNull Value key,
                                        @NotNull Value value) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        try {
            var object = Jet.expect(ValueOps.OBJECT, input);
            var _key = Jet.expect(Jet.STRING, key);
            return DataResult.success(
                    Value.asValue(Util.copyAndPut(object, _key, value)),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull DataResult<@NotNull MapLike<@NotNull Value>> getMap(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            var object = Jet.expect(ValueOps.OBJECT, input);
            return DataResult.success(new MapLike<>() {

                @Override
                public @Nullable Value get(Value key) {
                    try {
                        return this.get(Jet.expect(Jet.STRING, key));
                    } catch (TypeException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public @Nullable Value get(String key) {
                    // It is very important to return 'null'
                    // if the key is missing.
                    return object.get(key);
                }

                @Override
                public @NotNull Stream<Pair<Value, Value>> entries() {
                    return object.entrySet()
                            .stream()
                            .map(e -> Pair.of(
                                    Value.asValue(e.getKey()),
                                    e.getValue()
                            ));
                }
            });
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull DataResult<@NotNull Stream<@NotNull Pair<@NotNull Value, @NotNull Value>>> getMapValues(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            var object = Jet.expect(ValueOps.OBJECT, input);
            return DataResult.success(object.entrySet()
                    .stream()
                    .map(e -> Pair.of(
                            Value.asValue(e.getKey()),
                            e.getValue()
                    )));
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull Value createMap(@NotNull Stream<Pair<Value, Value>> stream) {
        Objects.requireNonNull(stream);
        return Value.asValue(stream.collect(ImmutableMap.<Pair<Value, Value>, String, Value>toImmutableMap(
                entry -> {
                    try {
                        return Jet.expect(Jet.STRING, entry.getFirst());
                    } catch (TypeException e) {
                        throw new RuntimeException(e);
                    }
                },
                Pair::getSecond
        )));
    }

    @Override
    public @NotNull DataResult<@NotNull Stream<@NotNull Value>> getStream(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            var array = Jet.expect(ValueOps.ARRAY, input);
            return DataResult.success(Arrays.stream(array));
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull Value createList(@NotNull Stream<@NotNull Value> stream) {
        Objects.requireNonNull(stream);
        return Value.asValue(stream.toArray(Value[]::new));
    }

    @Override
    public @NotNull Value remove(@NotNull Value input,
            /* Love the inconsistency in the type here. */
                                 @NotNull String key) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(key);
        try {
            var object = Jet.expect(ValueOps.OBJECT, input);
            var builder = ImmutableMap.<String, Value>builderWithExpectedSize(object.size());
            for (var entry : object.entrySet()) {
                if (entry.getKey().equals(key))
                    continue;
                builder.put(
                        entry.getKey(),
                        entry.getValue()
                );
            }
            return Value.asValue(builder.build());
        } catch (TypeException e) {
            throw new RuntimeException(e);
        }
    }
}
