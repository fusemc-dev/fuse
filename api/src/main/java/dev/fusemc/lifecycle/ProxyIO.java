package dev.fusemc.lifecycle;

import com.manchickas.optionated.Either;
import com.manchickas.optionated.Option;
import dev.fusemc.ArrayBuilder;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.resources.Identifier;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@Documented("IO")
public final class ProxyIO implements ProxyObject {

    /// A [Template] for a primitive array of **unsigned** bytes.
    ///
    /// ---
    ///
    /// The intention is that a script would want to specify some binary data as a series
    /// of **unsigned** bytes. Since a `byte` in Java is always **signed**, we have to accept
    /// those as an array of `int`s and convert accordingly.
    ///
    /// ```
    /// [255, 214, 187] -> [-1, -42, -69]
    /// ```
    private static final @NotNull Template<byte[]> BUFFER = Template.array(
            Template.INTEGER.flatMap(
                    i -> {
                        if (i >= 0 && i <= 255)
                            return Option.some((byte) (i & 0xFF));
                        return Option.none();
                    },
                    b -> Option.fromNullable(b)
                            .map(self -> self & 0xFF)
            ).alias(_ -> Description.concat(
                    Description.numeric(0),
                    Description.delimiter(".."),
                    Description.numeric(255))
            ),
            Byte[]::new
    ).map(bytes -> {
        var buffer = new byte[bytes.length];
        for (var i = 0; i < bytes.length; i++)
            buffer[i] = bytes[i];
        return buffer;
    }, bytes -> {
        var buffer = new Byte[bytes.length];
        for (var i = 0; i < bytes.length; i++)
            buffer[i] = bytes[i];
        return buffer;
    });

    private static final @NotNull String LOG           = "log";
    private static final @NotNull String WARN          = "warn";
    private static final @NotNull String ERROR         = "error";
    private static final @NotNull String READ          = "read";
    private static final @NotNull String READ_STRING   = "readString";
    private static final @NotNull String WRITE         = "write";
    private static final @NotNull String WRITE_STRING  = "writeString";
    private static final @NotNull String APPEND        = "append";
    private static final @NotNull String APPEND_STRING = "appendString";
    private static final @NotNull String EXISTS        = "exists";
    private static final @NotNull String LS            = "ls";
    private static final @NotNull String RM            = "rm";
    private static final @NotNull String MKDIR         = "mkdir";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyIO.LOG,
            ProxyIO.WARN,
            ProxyIO.ERROR,
            ProxyIO.READ,
            ProxyIO.READ_STRING,
            ProxyIO.WRITE,
            ProxyIO.WRITE_STRING,
            ProxyIO.APPEND,
            ProxyIO.APPEND_STRING,
            ProxyIO.EXISTS,
            ProxyIO.LS,
            ProxyIO.RM,
            ProxyIO.MKDIR
    };
    private static final @NotNull Marker MARKER = MarkerFactory.getMarker("SCRIPT");
    private static final int CHUNK_SIZE = 1 << 13; // 8192 bytes

    private final @NotNull Identifier identifier;
    private final @NotNull FileSystem system;
    private final @NotNull Logger logger;

    private final @NotNull ProxyExecutable log;
    private final @NotNull ProxyExecutable warn;
    private final @NotNull ProxyExecutable error;
    private final @NotNull ProxyExecutable read;
    private final @NotNull ProxyExecutable readString;
    private final @NotNull ProxyExecutable write;
    private final @NotNull ProxyExecutable writeString;
    private final @NotNull ProxyExecutable append;
    private final @NotNull ProxyExecutable appendString;
    private final @NotNull ProxyExecutable exists;
    private final @NotNull ProxyExecutable ls;
    private final @NotNull ProxyExecutable rm;
    private final @NotNull ProxyExecutable mkdir;

    public ProxyIO(@NotNull Identifier identifier, @NotNull FileSystem system) {
        this.identifier = Objects.requireNonNull(identifier);
        this.system     = Objects.requireNonNull(system);
        this.logger     = LoggerFactory.getLogger(identifier.toString());
        this.log = (args) -> {
            if (args.length == 1) {
                var datum = Tau.lower(Template.sequence(Template.STRING, Template.ANY), args[0]);
                this.logger.info(ProxyIO.MARKER, switch (datum) {
                    case Either.Left(var wrapped)  -> wrapped;
                    case Either.Right(var wrapped) -> Tau.inspect(wrapped, Scope.hashScope())
                            .stringify(null);
                });
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.warn = (args) -> {
            if (args.length == 1) {
                var datum = Tau.lower(Template.sequence(Template.STRING, Template.ANY), args[0]);
                this.logger.warn(ProxyIO.MARKER, switch (datum) {
                    case Either.Left(var wrapped)  -> wrapped;
                    case Either.Right(var wrapped) -> Tau.inspect(wrapped, Scope.hashScope())
                            .stringify(null);
                });
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.error = (args) -> {
            if (args.length == 1) {
                var datum = Tau.lower(Template.sequence(Template.STRING, Template.ANY), args[0]);
                this.logger.error(ProxyIO.MARKER, switch (datum) {
                    case Either.Left(var wrapped)  -> wrapped;
                    case Either.Right(var wrapped) -> Tau.inspect(wrapped, Scope.hashScope())
                            .stringify(null);
                });
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.read   = (args) -> {
            if (args.length == 1) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.READ, StandardOpenOption.CREATE))) {
                    return Tau.raise(ProxyIO.BUFFER, ProxyIO.readBytes(channel));
                } catch (IOException e) {
                    this.logger.error("Could not read file '{}'.", path, e);
                    return Tau.undefined();
                }
            }
            throw new UnsupportedOperationException();
        };
        this.readString = (args) -> {
            if (args.length == 1) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.READ, StandardOpenOption.CREATE))) {
                    return new String(ProxyIO.readBytes(channel), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    this.logger.error("Could not read file '{}'.", path, e);
                    return Tau.undefined();
                }
            }
            throw new UnsupportedOperationException();
        };
        this.write = (args) -> {
            if (args.length == 2) {
                var path   = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                var buffer = ByteBuffer.wrap(Tau.lower(ProxyIO.BUFFER, args[1]));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    channel.write(buffer);
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not write to file '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.writeString = (args) -> {
            if (args.length == 2) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                var buffer = ByteBuffer.wrap(Tau.lower(Template.STRING, args[1]).getBytes(StandardCharsets.UTF_8));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    channel.write(buffer);
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not write to file '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.append = (args) -> {
            if (args.length == 2) {
                var path   = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                var buffer = ByteBuffer.wrap(Tau.lower(ProxyIO.BUFFER, args[1]));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                    channel.write(buffer);
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not append to file '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.appendString = (args) -> {
            if (args.length == 2) {
                var path   = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                var buffer = ByteBuffer.wrap(Tau.lower(Template.STRING, args[1]).getBytes(StandardCharsets.UTF_8));
                try (var channel = system.newByteChannel(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                    channel.write(buffer);
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not append to file '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.exists = (args) -> {
            if (args.length == 1) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                try {
                    try {
                        system.checkAccess(path, Set.of());
                        return true;
                    } catch (NoSuchFileException e) {
                        return false;
                    }
                } catch (IOException e) {
                    this.logger.error("Could not determine whether file '{}' exists.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.ls = (args) -> {
            if (args.length == 1) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                try (var stream = system.newDirectoryStream(path, (_) -> true)) {
                    var buffer = new ArrayBuilder<String>(16);
                    for (var entry : stream)
                        buffer.append(entry.toString());
                    return buffer.build(String[]::new);
                } catch (IOException e) {
                    this.logger.error("Could not list files in '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.rm = (args) -> {
            if (args.length == 1) {
                var path = this.system.parsePath(Tau.lower(Template.STRING, args[0]));
                try {
                    system.delete(path);
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not remove file '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.mkdir = (args) -> {
            if (args.length == 1) {
                var path = this.system.toAbsolutePath(this.system.parsePath(Tau.lower(Template.STRING, args[0])));
                try {
                    var root = path.getRoot();
                    for (var i = 1; i <= path.getNameCount(); i++) {
                        var subpath = root.resolve(path.subpath(0, i)).normalize();
                        try {
                            // Sadly, the Polyglot FileSystem does not expose
                            // a convenient .exists() check: we thus have
                            // to check the existence of a file through a try/catch
                            // construction.
                            system.checkAccess(subpath, Set.of());
                        } catch (NoSuchFileException e) {
                            system.createDirectory(subpath);
                        }
                    }
                    return true;
                } catch (IOException e) {
                    this.logger.error("Could not create directory '{}'.", path, e);
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
    }

    private static byte[] readBytes(@NotNull SeekableByteChannel channel) throws IOException {
        var bytes  = new byte[CHUNK_SIZE];
        var length = 0;
        for (var buffer = ByteBuffer.allocate(CHUNK_SIZE); channel.read(buffer) >= 0; buffer.clear()) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                if (length >= bytes.length)
                    bytes = Arrays.copyOf(bytes, bytes.length << 1);
                bytes[length++] = buffer.get();
            }
        }
        return Arrays.copyOf(bytes, length);
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case ProxyIO.LOG           -> this.log;
            case ProxyIO.WARN          -> this.warn;
            case ProxyIO.ERROR         -> this.error;
            case ProxyIO.READ          -> this.read;
            case ProxyIO.READ_STRING   -> this.readString;
            case ProxyIO.WRITE         -> this.write;
            case ProxyIO.WRITE_STRING  -> this.writeString;
            case ProxyIO.APPEND        -> this.append;
            case ProxyIO.APPEND_STRING -> this.appendString;
            case ProxyIO.EXISTS        -> this.exists;
            case ProxyIO.LS            -> this.ls;
            case ProxyIO.RM            -> this.rm;
            case ProxyIO.MKDIR         -> this.mkdir;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyIO.KEYS.length)
                    return ProxyIO.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyIO.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyIO.KEYS)
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
}
