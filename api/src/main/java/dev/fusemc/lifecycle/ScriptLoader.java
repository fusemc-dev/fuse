package dev.fusemc.lifecycle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.manchickas.crayon.Crayon;
import com.manchickas.optionated.Option;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.fusemc.ArrayBuilder;
import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.listener.Listener;
import dev.fusemc.disastrous.listener.selector.Bound;
import dev.fusemc.disastrous.listener.selector.Unbound;
import dev.fusemc.iota.Standard;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.marshal.Command;
import dev.fusemc.marshal.Marshal;
import dev.fusemc.marshal.Suggester;
import dev.fusemc.marshal.path.CommandPath;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.math.ProxyVec2;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ScriptLoader implements ProxyObject, PreparableReloadListener {

    private static final @NotNull String ON           = "on";
    private static final @NotNull String ON_PROPERTY  = "onProperty";
    private static final @NotNull String ON_COMMAND   = "onCommand";
    private static final @NotNull String ON_SUGGESTER = "onSuggester";
    private static final @NotNull String DISPATCH     = "dispatch";

    private static final @NotNull String @NotNull[] KEYS = {
            ScriptLoader.ON,
            ScriptLoader.ON_PROPERTY,
            ScriptLoader.ON_COMMAND,
            ScriptLoader.ON_SUGGESTER,
            ScriptLoader.DISPATCH,
    };

    private static final FileToIdConverter CONVERTER = new FileToIdConverter("script", ".js");
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptLoader.class);
    private static final ScriptLoader INSTANCE = new ScriptLoader();
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
    private final @NotNull Map<Disaster.Type<?>, Set<Listener<?, ?>>> boundListeners;
    private final @NotNull Map<ProxyIdentifier, Set<Callback.Unbound>> unboundListeners;
    private final @NotNull Map<CommandPath, Command> commands;
    private final @NotNull Map<ProxyIdentifier, Suggester> suggesters;
    private final @NotNull Map<ProxyIdentifier, Property> properties;
    private final @NotNull ProxyExecutable on;
    private final @NotNull ProxyExecutable onProperty;
    private final @NotNull ProxyExecutable onCommand;
    private final @NotNull ProxyExecutable onSuggester;
    private final @NotNull ProxyExecutable dispatch;

    public ScriptLoader() {
        this.contexts         = HashBiMap.create();
        this.boundListeners   = new Reference2ReferenceOpenHashMap<>();
        this.unboundListeners = new Object2ReferenceOpenHashMap<>();
        this.commands         = new Object2ObjectOpenHashMap<>();
        this.suggesters       = new Object2ObjectOpenHashMap<>();
        this.properties       = new Object2ObjectOpenHashMap<>();
        this.on = (args) -> {
            if (args.length == 2) {
                try {
                    var selector = Disastrous.parse(Tau.lower(Template.STRING, args[0]));
                    if (selector instanceof Bound<?, ?> bound) {
                        var type     = bound.type();
                        var listener = bound.bind(args[1]);
                        this.boundListeners.computeIfAbsent(type, _ -> new ObjectOpenHashSet<>())
                                .add(listener);
                        return Tau.undefined();
                    }
                    var identifier = ((Unbound) selector).identifier();
                    var listener   = Tau.lower(Callback.Unbound.TEMPLATE, args[1]);
                    this.unboundListeners.computeIfAbsent(identifier, _ -> new ObjectOpenHashSet<>())
                            .add(listener);
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
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.TEMPLATE, args[0]);
                var listeners  = this.unboundListeners.get(identifier);
                if (listeners != null) {
                    var snapshot = ImmutableSet.copyOf(listeners);
                    var buffer   = new ArrayBuilder<Value>(listeners.size());
                    for (var callback : snapshot) {
                        try {
                            buffer.append(callback.onEvent(args));
                        } catch (PolyglotException e) {
                            LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listener will be excluded from future dispatch.", identifier,  e);
                            listeners.remove(callback);
                        }
                    }
                    return buffer.build(Value[]::new);
                }
                LOGGER.warn("An event of type '{}' has no listeners, yet a dispatch was attempted.", identifier);
                return new Value[0];
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ScriptLoader instance() {
        return ScriptLoader.INSTANCE;
    }

    public static @NotNull LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        var node = Commands.literal("fuse");
        return node.then(Commands.literal("property")
                .then(Commands.argument("name", IdentifierArgument.id())
                        .suggests((_, builder) -> {
                            var script = ScriptLoader.instance();
                            return SharedSuggestionProvider.suggestResource(script.properties.keySet().stream()
                                    .map(ProxyIdentifier::to)
                                    .toList(), builder);
                        })
                        .executes(ctx -> {
                            var name     = ProxyIdentifier.from(IdentifierArgument.getId(ctx, "name"));
                            var script   = ScriptLoader.instance();
                            var property = script.properties.get(name);
                            var source   = ctx.getSource();
                            if (property != null) {
                                var value = property.get();
                                source.sendSuccess(() -> Component.literal("Property '%s' has value '%s'".formatted(name, value)), false);
                                return 1;
                            }
                            source.sendFailure(Component.literal("Unknown property '%s'".formatted(name)));
                            return -1;
                        }))
                .then(Commands.literal("registries")
                        .executes(ctx -> {
                            var source = ctx.getSource();
                            source.getServer().reloadableRegistries().lookup()
                                    .listRegistryKeys()
                                    .forEach(System.out::println);
                            return 1;
                        })));
    }

    public void rehydrate(@NotNull Property @NotNull[] properties) {
        Objects.requireNonNull(properties);
        var n = 0;
        for (var candidate : properties) {
            var name = candidate.identifier();
            if (this.properties.containsKey(name)) {
                this.properties.compute(name, (_, property) -> {
                    assert property != null;
                    return property.rehydrate(candidate);
                });
                n++;
                continue;
            }
            LOGGER.warn("Dropping a property '{}'.", name);
        }
        LOGGER.info("Successfully rehydrated {} property(ies).", n);
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
        dispatcher.register(ScriptLoader.createCommand());
        if (list != null)
            for (var player : list.getPlayers())
                commands.sendCommands(player);
        LOGGER.info("Registered {} command(s).", this.commands.size());
    }

    @SuppressWarnings("unchecked")
    public <E extends Disaster<C>, C extends Callback> @NotNull E dispatch(@NotNull E event) {
        Objects.requireNonNull(event);
        var type      = event.type();
        var listeners = this.boundListeners.get(type);
        if (listeners != null) {
            var snapshot = ImmutableSet.copyOf(listeners);
            for (var listener : snapshot) {
                try {
                    if (((Listener<E, C>) listener).onEvent(event))
                        continue;
                    break;
                } catch (Exception e) {
                    LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listener will be excluded from future dispatch.", type, e);
                    listeners.remove(listener);
                }
            }
            return event;
        }
        return event;
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

    private @NotNull Map<Identifier, Source> prepare(@NotNull ResourceManager manager) {
        var resources = ScriptLoader.CONVERTER.listMatchingResources(manager);
        var buffer    = ImmutableMap.<Identifier, Source>builder();
        for (var entry : resources.entrySet()) {
            var identifier = ScriptLoader.CONVERTER.fileToId(entry.getKey());
            var resource = entry.getValue();
            try {
                var src = Source.newBuilder("js", resource.openAsReader(), identifier.toString())
                        .mimeType("text/javascript")
                        .cached(true)
                        .build();
                buffer.put(identifier, src);
            } catch (IOException e) {
                LOGGER.error("An error occurred whilst loading script '{}'.", identifier, e);
            }
        }
        return buffer.buildKeepingLast();
    }

    @SuppressWarnings("LoggingPlaceholderCountMatchesArgumentCount")
    private void apply(@NotNull Map<Identifier, Source> prepared) {
        Objects.requireNonNull(prepared);
        var staged = this.refresh();
        for (var entry : prepared.entrySet()) {
            var identifier  = entry.getKey();
            var source      = entry.getValue();
            var ctx         = Context.newBuilder("js")
                    .allowHostAccess(ScriptLoader.ACCESS)
                    .out(OutputStream.nullOutputStream())
                    .err(OutputStream.nullOutputStream())
                    .in(InputStream.nullInputStream())
                    .allowIO(IOAccess.NONE)
                    .build();
            try {
                var global = ctx.getBindings("js");
                global.putMember("script", this);
                ctx.eval(source);
                this.contexts.put(identifier, ctx);
            } catch (PolyglotException e) {
                LOGGER.error("An error occurred whilst loading script '{}'.", identifier, (e.isHostException() ? e.asHostException() : e));
                ctx.close(true);
            }
        }
        LOGGER.info("Loaded {} script(s).", this.contexts.size());
        LOGGER.info("Registered {} disaster listener(s).", this.boundListeners.size());
        this.rehydrate(staged);
    }

    public @NotNull Property @NotNull[] refresh() {
        var buffer = new ArrayBuilder<Property>(this.properties.size());
        for (var entry : this.properties.values())
            buffer.append(entry.unbind());
        this.unboundListeners.clear();
        this.boundListeners.clear();
        this.properties.clear();
        this.commands.clear();
        this.contexts.forEach((_, ctx) -> ctx.close(true));
        this.contexts.clear();
        return buffer.build(Property[]::new);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ScriptLoader.ON           -> this.on;
            case ScriptLoader.ON_PROPERTY  -> this.onProperty;
            case ScriptLoader.ON_COMMAND   -> this.onCommand;
            case ScriptLoader.ON_SUGGESTER -> this.onSuggester;
            case ScriptLoader.DISPATCH     -> this.dispatch;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ScriptLoader.KEYS.length)
                    return ScriptLoader.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ScriptLoader.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ScriptLoader.KEYS) {
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
}
