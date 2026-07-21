package dev.fusemc.standard;

import com.manchickas.optionated.Option;
import dev.fusemc.iota.Standardized;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import dev.fusemc.quelle.position.CharPosition;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Inspectable;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;

/// A namespaced name.
///
/// `ProxyIdentifier` represents a namespaced name in form of `namespace:path/...`. It is
/// intended as a **script-facing** version of [Identifier].
///
/// It appears iterable to scripts, allowing, among other, for destructuring:
///
/// ```js
/// const [namespace, path] = name;
/// ```
///
/// A `ProxyIdentifier` is parsed with the following [Template]:
///
/// ```
/// Identifier | /namespace:path/
/// ```
///
/// @since 0.1.0
@Documented("Identifier")
@Standardized("identifier")
public final class ProxyIdentifier implements ProxyObject, ProxyIterable, Inspectable {

    private static final @NotNull String NAMESPACE = "namespace";
    private static final @NotNull String PATH      = "path";
    private static final @NotNull String MATCHES   = "matches";
    private static final @NotNull String TO_STRING = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyIdentifier.NAMESPACE,
            ProxyIdentifier.PATH,
            ProxyIdentifier.MATCHES,
            ProxyIdentifier.TO_STRING,
    };

    public static final @NotNull Template<ProxyIdentifier> TEMPLATE = Template.union(
            Template.STRING.flatMap(
                    ProxyIdentifier::parse,
                    identifier -> Option.fromNullable(identifier)
                            .map(Object::toString)
            ).alias(_ -> Description.concat(
                    Description.delimiter('/'),
                    Description.concat(
                            Description.keyword("namespace"),
                            Description.delimiter(':'),
                            Description.keyword("path")
                    ),
                    Description.delimiter('/')
            )),
            Template.reference(ProxyIdentifier.class)
    );
    public static final @NotNull Template<Identifier> MAPPED_TEMPLATE = ProxyIdentifier.TEMPLATE.map(ProxyIdentifier::to, ProxyIdentifier::from);

    private final @NotNull String namespace;
    private final @NotNull String path;
    private final @NotNull ProxyExecutable matches;
    private final @NotNull ProxyExecutable toString;

    public ProxyIdentifier(@NotNull String namespace,
                           @NotNull String path) {
        this.namespace = Objects.requireNonNull(namespace);
        this.path      = Objects.requireNonNull(path);
        this.matches = (args) -> {
            if (args.length == 1) {
                var other = Tau.lower(ProxyIdentifier.TEMPLATE, args[0]);
                return this.equals(other);
            }
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return this.toString();
            throw new UnsupportedOperationException();
        };
    }

    /// Constructs a `ProxyIdentifier` from the given [Identifier].
    ///
    /// @since 0.1.0
    public static @NotNull ProxyIdentifier from(@NotNull Identifier identifier) {
        Objects.requireNonNull(identifier);
        return new ProxyIdentifier(
                identifier.getNamespace(),
                identifier.getPath()
        );
    }

    /// Converts the given `ProxyIdentifier` to an [Identifier].
    ///
    /// @since 0.1.0
    public static @NotNull Identifier to(@NotNull ProxyIdentifier identifier) {
        Objects.requireNonNull(identifier);
        return Identifier.fromNamespaceAndPath(
                identifier.namespace,
                identifier.path
        );
    }

    @ApiStatus.Internal
    private static boolean isPart(int point) {
        return point >= 'a' && point <= 'z' || point >= '0' && point <= '9' || point == '_' || point == '-' || point == '.';
    }

    private static @NotNull Option<ProxyIdentifier> parse(@NotNull String source) {
        Objects.requireNonNull(source);
        try {
            var reader     = new StringReader<>(source);
            var identifier = ProxyIdentifier.readIdentifier(reader);
            if (reader.canRead())
                return Option.none();
            return Option.some(identifier);
        } catch (Diagnostic _) {
            return Option.none();
        }
    }

    @ApiStatus.Internal
    public static @NotNull ProxyIdentifier readIdentifier(@NotNull StringReader<String> reader) {
        Objects.requireNonNull(reader);
        var position = reader.position();
        var buffer = new StringBuilder();
        while (reader.canRead()) {
            var c = reader.peek();
            if (c == ':') {
                reader.read();
                var namespace = buffer.toString();
                return ProxyIdentifier.readPath(namespace, reader, position);
            }
            if (c == '/')
                break;
            if (ProxyIdentifier.isPart(c)) {
                buffer.appendCodePoint(reader.read());
                continue;
            }
            break;
        }
        reader.align(position);
        return ProxyIdentifier.readPath("minecraft", reader, position);
    }

    @ApiStatus.Internal
    private static @NotNull ProxyIdentifier readPath(@NotNull String namespace,
                                                     @NotNull StringReader<String> reader,
                                                     @NotNull CharPosition<String> position) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(reader);
        var buffer = new StringBuilder();
        var length = 0;
        while (reader.canRead()) {
            var c = reader.peek();
            if (c == '/') {
                if (length > 0) {
                    buffer.appendCodePoint(reader.read());
                    length = 0;
                    continue;
                }
                throw new Diagnostic("Encountered an empty segment in an name literal.", reader.pointRange());
            }
            if (ProxyIdentifier.isPart(c)) {
                buffer.appendCodePoint(reader.read());
                length++;
                continue;
            }
            break;
        }
        if (length > 0)
            return new ProxyIdentifier(namespace, buffer.toString());
        throw new Diagnostic("Encountered a trailing slash in an name literal.", reader.range(position));
    }

    @Override
    public @NotNull Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyIdentifier.NAMESPACE -> this.namespace;
            case ProxyIdentifier.PATH      -> this.path;
            case ProxyIdentifier.MATCHES   -> this.matches;
            case ProxyIdentifier.TO_STRING -> this.toString;
            default -> throw new NoSuchElementException();
        };
    }

    @Override
    public @NotNull ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyIdentifier.KEYS.length)
                    return ProxyIdentifier.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyIdentifier.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyIdentifier.KEYS) {
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
    public @NotNull ProxyIterator getIterator() {
        return new ProxyIterator() {

            private int position = 0;

            @Override
            public boolean hasNext() {
                return this.position < 2;
            }

            @Override
            public @NotNull String getNext() throws NoSuchElementException {
                return switch (this.position++) {
                    case 0 -> ProxyIdentifier.this.namespace;
                    case 1 -> ProxyIdentifier.this.path;
                    default -> throw new NoSuchElementException();
                };
            }
        };
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj instanceof ProxyIdentifier other)
            return this.namespace.equals(other.namespace) && this.path.equals(other.path);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.path);
    }

    @Override
    public @NotNull String toString() {
        return String.format("%s:%s", namespace, path);
    }

    @Override
    public @NotNull Description inspect() {
        return Description.concat(
                Description.reference(ProxyIdentifier.class),
                Description.concat(
                        Description.delimiter('('),
                        Description.concat(
                                Description.delimiter(this.namespace),
                                Description.delimiter(':'),
                                Description.delimiter(this.path)
                        ),
                        Description.delimiter(')')
                )
        );
    }
}
