package dev.fusemc.lifecycle.property;

import com.manchickas.optionated.Either;
import dev.fusemc.iota.Iota;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class Property implements ProxyObject, ProxyExecutable {

    private static final @NotNull String NAME      = "name";
    private static final @NotNull String GET       = "get";
    private static final @NotNull String SET       = "set";
    private static final @NotNull String TO_STRING = "toString";
    private static final @NotNull String @NotNull[] KEYS = {
            Property.NAME,
            Property.GET,
            Property.SET,
            Property.TO_STRING,
    };

    public static final Template<Property> TEMPLATE = Template.record(
            ProxyIdentifier.TEMPLATE.property("name", prop -> prop.identifier),
            Template.ANY.property("value", prop -> prop.value),
            Property::new
    );

    private final @NotNull ProxyIdentifier identifier;
    private final @NotNull ProxyExecutable get;
    private final @NotNull ProxyExecutable set;
    private final @NotNull ProxyExecutable toString;
    private @NotNull Value value;

    public Property(@NotNull ProxyIdentifier name,
                    @NotNull Value initial) {
        this.identifier = Objects.requireNonNull(name);
        this.value      = Objects.requireNonNull(initial);
        this.get        = (args) -> {
            if (args.length == 0)
                return this.value;
            throw new UnsupportedOperationException();
        };
        this.set        = (args) -> {
            if (args.length == 1) {
                var updater = Tau.lower(Template.sequence(Updater.TEMPLATE, Template.ANY), args[0]);
                return switch (updater) {
                    case Either.Left(var wrapped)  -> this.value = wrapped.update(this.value);
                    case Either.Right(var wrapped) -> this.value = wrapped;
                };
            }
            throw new UnsupportedOperationException();
        };
        this.toString   = (args) -> {
            if (args.length == 0)
                return this.value.toString();
            throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case Property.NAME      -> this.identifier;
            case Property.TO_STRING -> this.toString;
            default -> throw new NoSuchElementException();
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : Property.KEYS)
            if (candidate.equals(key))
                return true;
        return false;
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < Property.KEYS.length)
                    return Property.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return Property.KEYS.length;
            }
        };
    }

    @Override
    public @NotNull Object execute(@NotNull Value @NotNull... arguments) {
        Objects.requireNonNull(arguments);
        return switch (arguments.length) {
            case 0  -> this.get.execute(arguments);
            case 1  -> this.set.execute(arguments);
            default -> throw new UnsupportedOperationException();
        };
    }

    public @NotNull Value get() {
        return this.value;
    }

    public @NotNull Property rehydrate(@NotNull Property other) {
        Objects.requireNonNull(other);
        this.value = other.value;
        return this;
    }

    public @NotNull Property unbind() {
        return new Property(this.identifier, Iota.unbind(this.value));
    }

    public @NotNull ProxyIdentifier identifier() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.identifier, this.value);
    }

    interface Updater {

        @NotNull Template<Updater> TEMPLATE = Template.functional(Updater.class, Template.ANY);

        @NotNull Value update(@NotNull Value previous);
    }
}
