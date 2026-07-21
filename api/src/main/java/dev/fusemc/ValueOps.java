package dev.fusemc;

import com.manchickas.optionated.Option;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.TypeException;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.proxy.ObjectLike;
import dev.fusemc.tau.template.Mu;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class ValueOps implements DynamicOps<Value> {

    private static final @NotNull Template<Value[]> ARRAY = Template.array(Template.ANY, Value[]::new);
    private static final @NotNull Template<Map<String, Value>> OBJECT = Template.map(Template.STRING, Template.ANY);

    public static final @NotNull ValueOps INSTANCE = new ValueOps();
    public static final @NotNull Template<Component> COMPONENT = ValueOps.delegate(ComponentSerialization.CODEC, Description.keyword("Text"));

    private ValueOps() {
        // Why don't scientists trust atoms?
        // Because they make up everything.
    }

    public static @NotNull ValueOps instance() {
        return ValueOps.INSTANCE;
    }

    /// Constructs a [Template] for an entry of the provided [Registry].
    ///
    /// ---
    ///
    /// The returned [Template] will describe itself as follows, where `κ` denotes the [ResourceKey] of the provided [Registry]:
    ///
    /// ```
    /// Registered<"κ">
    /// ```
    ///
    /// @since 0.1.0
    public static <T> @NotNull Template<Holder<T>> registered(@NotNull Registry<T> registry) {
        Objects.requireNonNull(registry);
        return new Template<>() {

            @Override
            public @NonNull Option<Holder<T>> lower(@NonNull Value value) {
                return ProxyIdentifier.MAPPED_TEMPLATE.lower(value)
                        .flatMap(i -> Option.fromOptional(registry.get(i)))
                        .map(it -> it);
            }

            @Override
            public @NonNull Option<Value> raise(@Nullable Holder<T> value) {
                if (value != null)
                    return Option.fromOptional(value.unwrapKey())
                            .map(key -> key.identifier().toString())
                            .flatMap(Template.STRING::raise);
                return Option.none();
            }

            @Override
            public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
                return Description.attach(
                        Description.concat(
                                Description.reference("Registered"),
                                Description.concat(
                                        Description.delimiter('<'),
                                        Description.literal(registry.key()
                                                .identifier()
                                                .toString()),
                                        Description.delimiter('>')
                                )
                        ),
                        Domain.DESCRIBE
                );
            }
        };
    }

    public static <T> @NotNull Template<T> delegate(@NotNull Codec<T> codec,
                                                    @NotNull Description description) {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(description);
        return new Template<>() {

            @Override
            public @NotNull Option<T> lower(@NotNull Value value) {
                return Option.fromOptional(codec.parse(ValueOps.INSTANCE, value).resultOrPartial());
            }

            @Override
            public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
                return Option.fromOptional(codec.encodeStart(ValueOps.INSTANCE, value).resultOrPartial());
            }

            @Override
            public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
                return Description.attach(description, Domain.DESCRIBE);
            }
        };
    }

    @Override
    public <U> @NotNull U convertTo(@NotNull DynamicOps<U> ops,
                                    @NotNull Value value) {
        Objects.requireNonNull(ops);
        Objects.requireNonNull(value);
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
        if (value.hasArrayElements())
            return this.convertList(ops, value);
        if (value.hasMembers() && !value.hasArrayElements())
            return this.convertMap(ops, value);
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof byte[]
                    || host instanceof short[]
                    || host instanceof int[]
                    || host instanceof long[]
                    || host instanceof float[]
                    || host instanceof double[]
                    || host instanceof char[]
                    || host instanceof boolean[]
                    || host instanceof Object[]
                    || host instanceof Collection<?>)
                return this.convertList(ops, value);
            if (host instanceof Map<?,?>)
                return this.convertMap(ops, value);
            return ops.empty();
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof ProxyArray)
                return this.convertList(ops, value);
            if (proxy instanceof ProxyObject || proxy instanceof ProxyHashMap)
                return this.convertMap(ops, value);
            return ops.empty();
        }
        return ops.empty();
    }

    @Override
    public @NotNull Value empty() {
        return Tau.undefined();
    }

    @Override
    public @NotNull DataResult<@NotNull Number> getNumberValue(@NotNull Value value) {
        Objects.requireNonNull(value);
        try {
            return DataResult.success(
                    Tau.lower(Template.NUMBER, value),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public Value createNumeric(@NotNull Number number) {
        Objects.requireNonNull(number);
        return Tau.raise(Template.NUMBER, number);
    }

    @Override
    public @NotNull DataResult<@NotNull String> getStringValue(@NotNull Value value) {
        Objects.requireNonNull(value);
        try {
            return DataResult.success(
                    Tau.lower(Template.STRING, value),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public Value createString(@NotNull String s) {
        Objects.requireNonNull(s);
        return Tau.raise(Template.STRING, s);
    }

    @Override
    public @NotNull DataResult<@NotNull Boolean> getBooleanValue(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            return DataResult.success(
                    Tau.lower(Template.BOOLEAN, input),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull Value createBoolean(boolean value) {
        return Tau.raise(Template.BOOLEAN, value);
    }

    @Override
    public @NotNull DataResult<@NotNull Value> mergeToList(@NotNull Value value,
                                                           @NotNull Value entry) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(entry);
        try {
            if (Tau.isUndefined(value))
                return DataResult.success(
                        Value.asValue(new Value[]{ value }),
                        Lifecycle.stable()
                );
            var array = Tau.lower(ValueOps.ARRAY, value);
            return DataResult.success(
                    Value.asValue(ArrayBuilder.withAppended(array, entry)),
                    Lifecycle.stable()
            );
        } catch (TypeException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public @NotNull DataResult<@NotNull Value> mergeToMap(@NotNull Value map,
                                                          @NotNull Value key,
                                                          @NotNull Value value) {
        Objects.requireNonNull(map);
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        try {
            if (Tau.isUndefined(map))
                return DataResult.success(
                        Value.asValue(ObjectLike.builder()
                                .append(Tau.lower(Template.STRING, key), value)
                                .build()),
                        Lifecycle.stable()
                );
            var object = Tau.lower(ValueOps.OBJECT, map);
            var _key   = Tau.lower(Template.STRING, key);
            var buffer = ObjectLike.builder();
            for (var entry : object.entrySet()) {
                var k = entry.getKey();
                if (k.equals(_key))
                    continue;
                buffer.append(k, entry.getValue());
            }
            return DataResult.success(
                    Value.asValue(buffer.append(_key, value).build()),
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
            var object = Tau.lower(ValueOps.OBJECT, input);
            return DataResult.success(new MapLike<>() {

                @Override
                public @Nullable Value get(@NotNull Value key) {
                    Objects.requireNonNull(key);
                    try {
                        return this.get(Tau.lower(Template.STRING, key));
                    } catch (TypeException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public @Nullable Value get(@NotNull String key) {
                    Objects.requireNonNull(key);
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
            var object = Tau.lower(ValueOps.OBJECT, input);
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
        return Value.asValue(stream.collect(ObjectLike.toObject(
                entry -> Tau.lower(Template.STRING, entry.getFirst()),
                Pair::getSecond
        )));
    }

    @Override
    public @NotNull DataResult<@NotNull Stream<@NotNull Value>> getStream(@NotNull Value input) {
        Objects.requireNonNull(input);
        try {
            var array = Tau.lower(ValueOps.ARRAY, input);
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
        var object  = Tau.lower(ValueOps.OBJECT, input);
        var buffer  = ObjectLike.builder(object.size());
        for (var entry : object.entrySet()) {
            var k = entry.getKey();
            if (key.equals(k))
                continue;
            buffer.append(k, entry.getValue());
        }
        return Value.asValue(buffer.build());
    }
}
