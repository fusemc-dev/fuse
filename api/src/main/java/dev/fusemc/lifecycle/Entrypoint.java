package dev.fusemc.lifecycle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.manchickas.crayon.Crayon;
import com.manchickas.optionated.Option;
import dev.fusemc.ArrayBuilder;
import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.disastrous.listener.Listener;
import dev.fusemc.disastrous.listener.selector.Bound;
import dev.fusemc.disastrous.listener.selector.Unbound;
import dev.fusemc.iota.Standard;
import dev.fusemc.lifecycle.io.CloseableFileSystem;
import dev.fusemc.lifecycle.io.SharedFileSystem;
import dev.fusemc.lifecycle.io.VirtualFileSystem;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.marshal.Command;
import dev.fusemc.marshal.Marshal;
import dev.fusemc.marshal.Suggester;
import dev.fusemc.marshal.path.CommandPath;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.math.ProxyVec2;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.*;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

@Documented("Entrypoint")
public final class Entrypoint implements ProxyObject, PreparableReloadListener {

    private static final @NotNull VarHandle ROOT;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(PathPackResources.class, MethodHandles.lookup());
            ROOT = lookup.findVarHandle(PathPackResources.class, "root", Path.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final @NotNull String ON           = "on";
    private static final @NotNull String ON_PROPERTY  = "onProperty";
    private static final @NotNull String ON_COMMAND   = "onCommand";
    private static final @NotNull String ON_SUGGESTER = "onSuggester";
    private static final @NotNull String LOG          = "log";
    private static final @NotNull String WARN         = "warn";
    private static final @NotNull String ERROR        = "error";
    private static final @NotNull String DISPATCH     = "dispatch";
    private static final @NotNull String SCHEDULE     = "schedule";

    private static final @NotNull String @NotNull[] KEYS = {
            Entrypoint.ON,
            Entrypoint.ON_PROPERTY,
            Entrypoint.ON_COMMAND,
            Entrypoint.ON_SUGGESTER,
            Entrypoint.LOG,
            Entrypoint.WARN,
            Entrypoint.ERROR,
            Entrypoint.DISPATCH,
            Entrypoint.SCHEDULE,
    };

    private static final FileToIdConverter CONVERTER = new FileToIdConverter("script", ".js");
    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);
    private static final Entrypoint INSTANCE = new Entrypoint();
    private static final HostAccess ACCESS = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowImplementationsAnnotatedBy(HostAccess.Implementable.class)
            .allowAccessInheritance(true)
            .allowArrayAccess(true)
            .allowMapAccess(true)
            .build();

    public static final Standard IOTA = Standard.builder()
            .define(ProxyIdentifier.class, ProxyIdentifier.TEMPLATE)
            .define(ProxyVec2.class, ProxyVec2.TEMPLATE)
            .define(ProxyVec3.class, ProxyVec3.TEMPLATE)
            .build();

    private final @NotNull BiMap<Identifier, Context> contexts;
    private final @NotNull Map<Type<?>, Set<Listener<?, ?>>> boundListeners;
    private final @NotNull Map<ProxyIdentifier, Set<Callback.Unbound>> unboundListeners;
    private final @NotNull Map<CommandPath, Command> commands;
    private final @NotNull Map<ProxyIdentifier, Suggester> suggesters;
    private final @NotNull Map<ProxyIdentifier, Property> properties;
    private final @NotNull Set<CloseableFileSystem> handles;
    private final @NotNull PriorityQueue<Scheduled> pending;

    private final @NotNull ProxyExecutable on;
    private final @NotNull ProxyExecutable onProperty;
    private final @NotNull ProxyExecutable onCommand;
    private final @NotNull ProxyExecutable onSuggester;
    private final @NotNull ProxyExecutable dispatch;
    private final @NotNull ProxyExecutable schedule;
    /// The reference tick count.
    ///
    /// ---
    ///
    /// We require some sort of reference tick in order to schedule
    /// and run tasks. Since we mostly care about relatively, it
    /// is **never reset** either.
    private long reference;

    public Entrypoint() {
        this.contexts         = HashBiMap.create();
        this.boundListeners   = new Reference2ReferenceOpenHashMap<>();
        this.unboundListeners = new Object2ReferenceOpenHashMap<>();
        this.commands         = new Object2ObjectOpenHashMap<>();
        this.suggesters       = new Object2ObjectOpenHashMap<>();
        this.properties       = new Object2ObjectOpenHashMap<>();
        this.pending          = new PriorityQueue<>(Scheduled::compareTo);
        this.handles          = new ObjectArraySet<>();
        this.on = (args) -> {
            if (args.length == 2) {
                try {
                    var selector = Disastrous.parse(Tau.lower(Template.STRING, args[0]));
                    return switch (selector) {
                        case Bound<?, ?> bound -> {
                            var type     = bound.type();
                            var listener = bound.bind(args[1]);
                            this.boundListeners
                                    .computeIfAbsent(type, _ -> new ObjectOpenHashSet<>())
                                    .add(listener);
                            yield Tau.undefined();
                        }
                        case Unbound unbound -> {
                            var identifier = unbound.identifier();
                            var listener   = Tau.lower(Callback.Unbound.TEMPLATE, args[1]);
                            this.unboundListeners
                                    .computeIfAbsent(identifier, _ -> new ObjectOpenHashSet<>())
                                    .add(listener);
                            yield Tau.undefined();
                        }
                    };
                } catch (Diagnostic e) {
                    LOGGER.error(e.excerpt(Crayon.brightRed().underline()));
                    LOGGER.error("An error occurred whilst parsing an event selector: {}", e.getMessage());
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.onProperty = (args) -> {
            if (args.length == 2) {
                var name     = Tau.lower(ProxyIdentifier.TEMPLATE, args[0]);
                var property = new Property(name, args[1]);
                this.properties.put(name, property);
                return property;
            }
            throw new UnsupportedOperationException();
        };
        this.onCommand = (args) -> {
            if (args.length == 2) {
                try {
                    var path    = Marshal.parse(Tau.lower(Template.STRING, args[0]));
                    var command = Tau.lower(Command.TEMPLATE, args[1]);
                    this.commands.put(path, command);
                } catch (Diagnostic e) {
                    LOGGER.error(e.excerpt(Crayon.brightRed().underline()));
                    LOGGER.error("An error occurred whilst parsing a command path: {}", e.getMessage());
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.onSuggester = (args) -> {
            if (args.length == 2) {
                var name      = Tau.lower(ProxyIdentifier.TEMPLATE, args[0]);
                var suggester = Tau.lower(Suggester.TEMPLATE, args[1]);
                this.suggesters.put(name, suggester);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.dispatch = (args) -> {
            if (args.length > 0) {
                var identifier = Tau.lower(ProxyIdentifier.TEMPLATE, args[0]);
                var listeners  = this.unboundListeners.get(identifier);
                if (listeners != null) {
                    var snapshot = ImmutableSet.copyOf(listeners);
                    var buffer   = new ArrayBuilder<Value>(listeners.size());
                    var shifted  = new Value[args.length - 1];
                    System.arraycopy(args, 1, shifted, 0, shifted.length);
                    for (var callback : snapshot) {
                        try {
                            buffer.append(callback.onEvent(shifted));
                        } catch (Exception e) {
                            LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listener will be removed from future dispatch.", identifier,  e);
                            listeners.remove(callback);
                        }
                    }
                    return buffer.build(Value[]::new);
                }
                return new Value[0];
            }
            throw new UnsupportedOperationException();
        };
        this.schedule = (args) -> {
            if (args.length == 2) {
                var callback = Tau.lower(Template.FUNCTION, args[0]);
                var delay    = Tau.lower(Template.INTEGER, args[1]);
                var initiator = Context.getCurrent();
                if (initiator != null) {
                    this.pending.add(new Scheduled(Context.getCurrent(), callback, this.reference + delay));
                    return Tau.undefined();
                }
                throw new AssertionError();
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull Entrypoint instance() {
        return Entrypoint.INSTANCE;
    }

    /// Rehydrate the properties from the given `Property[]`.
    ///
    /// ---
    ///
    /// Attempts to rehydrate any registered properties present in the given `candidates`.
    /// If a candidate isn't registered, it is **dropped**.
    ///
    /// @since 0.1.0
    public void rehydrate(@NotNull Property @NotNull[] candidates) {
        Objects.requireNonNull(candidates);
        var successful = new AtomicInteger();
        for (var candidate : candidates) {
            var name = candidate.identifier();
            this.properties.compute(name, (_, property) -> {
                if (property != null) {
                    successful.incrementAndGet();
                    return property.assign(candidate);
                }
                LOGGER.warn("Dropping property '{}'...", name);
                return null;
            });
        }
        LOGGER.info("Successfully rehydrated {} property(ies).", successful);
    }

    public void tickPending(@NotNull MinecraftServer server) {
        var manager = server.tickRateManager();
        if (manager.runsNormally()) {
            this.reference++;
            this.pending.removeIf(scheduled -> {
                try {
                    return scheduled.attempt(this.reference);
                } catch (Exception e) {
                    var initiator  = scheduled.initiator();
                    var identifier = this.contexts.inverse().get(initiator);
                    if (identifier != null) {
                        var logger = LoggerFactory.getLogger(identifier.toString());
                        logger.error("An exception occurred within a scheduled callback.", e);
                        return true;
                    }
                    throw new AssertionError();
                }
            });
        }
    }

    public void refreshTree(@NotNull MinecraftServer server) {
        Objects.requireNonNull(server);
        var commands   = server.getCommands();
        var dispatcher = commands.getDispatcher();
        var list       = server.getPlayerList();
        for (var entry : this.commands.entrySet()) {
            var path    = entry.getKey();
            var command = entry.getValue();
            var node    = path.build(command, (name) -> {
                var suggester = this.suggesters.get(name);
                return Option.fromNullable(suggester);
            });
            dispatcher.register(node);
        }
        for (var player : list.getPlayers())
            commands.sendCommands(player);
        LOGGER.info("Registered {} command(s).", this.commands.size());
    }

    @SuppressWarnings("unchecked")
    public <T extends Disaster<?>> @NotNull T dispatch(@NotNull T disaster) {
        Objects.requireNonNull(disaster);
        var type      = disaster.type();
        var listeners = this.boundListeners.get(type);
        if (listeners != null) {
            var snapshot = ImmutableSet.copyOf(listeners);
            for (var listener : snapshot) {
                try {
                    if (((Listener<T, ?>) listener).onDisaster(disaster))
                        continue;
                    break;
                } catch (Exception e) {
                    LOGGER.error("An error occurred whilst dispatching a disaster '{}' to one of its listeners. The faulty listener will be removed from future dispatch.", type, e);
                    listeners.remove(listener);
                }
            }
            return disaster;
        }
        return disaster;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(@NotNull SharedState sharedState,
                                                   @NotNull Executor prepareExecutor,
                                                   @NotNull PreparationBarrier preparationBarrier,
                                                   @NotNull Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> this.prepare(sharedState.resourceManager()), prepareExecutor)
                .thenComposeAsync(preparationBarrier::wait)
                .thenAcceptAsync(this::apply, applyExecutor);
    }

    private @NotNull Map<Identifier, Script> prepare(@NotNull ResourceManager manager) {
        var candidates = Entrypoint.CONVERTER.listMatchingResources(manager);
        var buffer     = ImmutableMap.<Identifier, Script>builder();
        for (var candidate : candidates.entrySet()) {
            var identifier = Entrypoint.CONVERTER.fileToId(candidate.getKey());
            var resource   = candidate.getValue();
            try {
                var source = Source.newBuilder("js", resource.openAsReader(), identifier.toString())
                        .mimeType("application/javascript+module")
                        .cached(true)
                        .build();
                buffer.put(identifier, new Script(resource.source(), source));
            } catch (IOException e) {
                LOGGER.error("An error occurred whilst loading '{}'.", identifier, e);
            }
        }
        return buffer.buildKeepingLast();
    }

    private @NotNull Option<ZipFile> getRoot(@NotNull FilePackResources resources) {
        Objects.requireNonNull(resources);
        try {
            var field = FilePackResources.class.getDeclaredField("zipFileAccess");
            if (field.trySetAccessible()) {
                var type   = field.getType();
                var access = field.get(resources);
                var method = type.getMethod("getOrCreateZipFile");
                return Option.fromNullable((ZipFile) method.invoke(access));
            }
        } catch (ReflectiveOperationException ex) {
            return Option.none();
        }
        return Option.none();
    }

    private @NotNull java.nio.file.FileSystem fromZip(@NotNull ZipFile file) throws IOException {
        var uri = URI.create("jar:" + Path.of(file.getName()).normalize().toUri());
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException _) {
            return FileSystems.newFileSystem(uri, Map.of());
        }
    }

    private static @NotNull String cwd(@NotNull Identifier identifier) {
        var buffer   = new StringBuilder("data/");
        var path     = identifier.getPath();
        var segments = path.split("/");
        buffer.append(identifier.getNamespace());
        buffer.append("/script");
        for (var i = 0; i < segments.length - 1; i++) {
            var segment = segments[i];
            buffer.append('/');
            buffer.append(segment);
        }
        return buffer.toString();
    }

    private void apply(@NotNull Map<Identifier, Script> prepared) {
        Objects.requireNonNull(prepared);
        var staged   = this.refresh();
        var interner = new HashMap<java.nio.file.FileSystem, SharedFileSystem>();
        for (var entry : prepared.entrySet()) {
            var identifier  = entry.getKey();
            var script      = entry.getValue();
            var system      = this.bindFileSystem(script.pack, identifier, interner);
            var ctx         = Context.newBuilder("js")
                    .allowHostAccess(Entrypoint.ACCESS)
                    .out(OutputStream.nullOutputStream())
                    .err(OutputStream.nullOutputStream())
                    .in(InputStream.nullInputStream())
                    .option("js.console", "false")
                    .option("js.strict",  "true")
                    .allowIO(IOAccess.newBuilder()
                            .fileSystem(system)
                            .allowHostFileAccess(false)
                            .allowHostSocketAccess(false)
                            .build())
                    .build();
            var global = ctx.getBindings("js");
            global.putMember("script", this);
            global.putMember("io", new ProxyIO(identifier, system));
            global.putMember("vec2", Tau.constructor(ProxyVec2.TEMPLATE));
            global.putMember("vec3", Tau.constructor(ProxyVec3.TEMPLATE));
            global.putMember("identifier", Tau.constructor(ProxyIdentifier.TEMPLATE));
            try {
                ctx.eval(script.source);
                if (system instanceof CloseableFileSystem closeable)
                    this.handles.add(closeable);
                this.contexts.put(identifier, ctx);
            } catch (PolyglotException e) {
                LOGGER.error("An error occurred whilst loading script '{}'.", identifier, e);
                try {
                    if (system instanceof CloseableFileSystem closeable)
                        closeable.close(false);
                    ctx.close(true);
                } catch (IOException io) {
                    throw new IOError(io);
                }
            }
        }
        LOGGER.info("Loaded {} script(s).", this.contexts.size());
        LOGGER.info("Registered ({}/{}) disaster listener(s).", this.boundListeners.size(), this.unboundListeners.size());
        this.rehydrate(staged);
    }

    private @NotNull FileSystem bindFileSystem(@NotNull PackResources pack,
                                               @NotNull Identifier identifier,
                                               @NotNull Map<java.nio.file.FileSystem, SharedFileSystem> interner) {
        Objects.requireNonNull(pack);
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(interner);
        return switch (pack) {
            case FilePackResources resources -> {
                try {
                    var option = this.getRoot(resources);
                    if (option instanceof Option.Some(var wrapped)) {
                        assert wrapped != null;
                        var system   = this.fromZip(wrapped);
                        var delegate = FileSystem.newFileSystem(system);
                        var handle   = interner.computeIfAbsent(system, SharedFileSystem::new);
                        yield new VirtualFileSystem(
                                handle.acquire(),
                                delegate.parsePath("/"),
                                delegate.parsePath(Entrypoint.cwd(identifier))
                        );
                    }
                    yield FileSystem.newDenyIOFileSystem();
                } catch (IOException e) {
                    LOGGER.error("An error occurred whilst creating a ZIP filesystem for '{}'. All IO for that script will be restricted.", identifier, e);
                    yield FileSystem.newDenyIOFileSystem();
                }
            }
            case PathPackResources resources -> {
                var root = (Path) Entrypoint.ROOT.get(resources);
                var cwd  = root.resolve(Entrypoint.cwd(identifier));
                yield new VirtualFileSystem(FileSystem.newDefaultFileSystem(), root, cwd);
            }
            default -> {
                LOGGER.error("Could not identify the filesystem type for '{}'. All IO for that script will be restricted.", identifier);
                yield FileSystem.newDenyIOFileSystem();
            }
        };
    }

    public @NotNull Property @NotNull[] refresh() {
        var buffer = new ArrayBuilder<Property>(this.properties.size());
        for (var entry : this.properties.values())
            buffer.append(entry.unbound());
        for (var handle : this.handles) {
            try {
                handle.close(true);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        this.handles.clear();
        this.unboundListeners.clear();
        this.boundListeners.clear();
        this.properties.clear();
        this.commands.clear();
        this.pending.clear();
        this.contexts.forEach((_, ctx) -> ctx.close(true));
        this.contexts.clear();
        return buffer.build(Property[]::new);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case Entrypoint.ON           -> this.on;
            case Entrypoint.ON_PROPERTY  -> this.onProperty;
            case Entrypoint.ON_COMMAND   -> this.onCommand;
            case Entrypoint.ON_SUGGESTER -> this.onSuggester;
            case Entrypoint.DISPATCH     -> this.dispatch;
            case Entrypoint.SCHEDULE     -> this.schedule;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < Entrypoint.KEYS.length)
                    return Entrypoint.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return Entrypoint.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : Entrypoint.KEYS) {
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

    record Script(@NotNull PackResources pack, @NotNull Source source) {

    }
}
