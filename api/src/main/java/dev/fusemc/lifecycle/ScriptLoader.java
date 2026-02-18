package dev.fusemc.lifecycle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.manchickas.Charcoal;
import dev.fusemc.ArrayBuilder;
import dev.fusemc.ParseException;
import dev.fusemc.disastrous.event.Event;
import dev.fusemc.disastrous.event.EventCallback;
import dev.fusemc.disastrous.event.EventListener;
import dev.fusemc.disastrous.event.EventType;
import dev.fusemc.disastrous.selector.EventSelector;
import dev.fusemc.disastrous.selector.Parser;
import dev.fusemc.standard.util.ScriptIdentifier;
import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import com.manchickas.optionated.Option;
import com.manchickas.quelle.position.SourceSpan;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class ScriptLoader implements PreparableReloadListener {

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

    private final @NotNull BiMap<Identifier, Context> contexts;
    private final @NotNull Map<EventType<?, ?>, Set<EventListener<?, ?>>> boundListeners;
    private final @NotNull Map<Identifier, Set<EventCallback.Unbound>> unboundListeners;
    private final @NotNull Queue<Task> pendingTasks;
    // TODO: Do we *need* an AtomicLong here?
    private final @NotNull AtomicLong tick;

    public ScriptLoader() {
        this.contexts = HashBiMap.create();
        this.boundListeners = new Reference2ReferenceOpenHashMap<>();
        this.unboundListeners = new Object2ReferenceOpenHashMap<>();
        this.pendingTasks = new PriorityQueue<>();
        this.tick = new AtomicLong();
    }

    public static @NotNull ScriptLoader instance() {
        return ScriptLoader.INSTANCE;
    }

    @HostAccess.Export
    public void on(@NotNull Value selector, @NotNull Value callback) throws TypeException {
        Objects.requireNonNull(selector);
        Objects.requireNonNull(callback);
        try {
            var parsed = new Parser(Jet.expect(Jet.STRING, selector))
                    .parse();
            if (parsed instanceof EventSelector.Bound<?, ?> bound) {
                var type = bound.type();
                var listener = bound.bind(callback);
                this.boundListeners.computeIfAbsent(type, _ -> new ObjectOpenHashSet<>())
                        .add(listener);
                return;
            }
            var identifier = ((EventSelector.Unbound) parsed).identifier();
            var listener = Jet.expect(EventCallback.Unbound.TEMPLATE, callback);
            this.unboundListeners.computeIfAbsent(identifier, _ -> new ObjectOpenHashSet<>())
                    .add(listener);
        } catch (ParseException e) {
            var span = e.span();
            if (span instanceof Option.Some<SourceSpan<String>>(var wrapped)) {
                LOGGER.error("{} An error occurred whilst parsing an event selector: {}\n{}",
                        wrapped, e.getMessage(), wrapped.format(Charcoal.red()));
                return;
            }
            LOGGER.error("An error occurred whilst parsing an event selector: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void schedule(@NotNull Value callback, long delay) throws TypeException {
        Objects.requireNonNull(callback);
        var task = new Task(
                Jet.expect(ScheduledCallback.TEMPLATE, callback),
                this.tick.get() + delay
        );
        this.pendingTasks.add(task);
    }

    @HostAccess.Export
    public @NotNull Value[] dispatch(@NotNull Value identifier, @NotNull Value... args) throws TypeException, ParseException {
        var type = ScriptIdentifier.expectVanilla(identifier);
        var listeners = this.unboundListeners.get(type);
        if (listeners != null) {
            var snapshot = ImmutableSet.copyOf(listeners);
            var buffer = new ArrayBuilder<Value>(listeners.size());
            for (var callback : snapshot) {
                try {
                    buffer.append(callback.onEvent(args));
                } catch (PolyglotException e) {
                    LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listener will be excluded from future dispatch.", type,  e);
                    listeners.remove(callback);
                }
            }
            return buffer.build(Value[]::new);
        }
        LOGGER.warn("An event of type '{}' has no listeners, yet a dispatch was attempted.", type);
        return new Value[0];
    }

    @SuppressWarnings("unchecked")
    public <E extends Event<E, C>, C extends EventCallback> @NotNull E dispatchBound(@NotNull E event) {
        Objects.requireNonNull(event);
        var type = event.type();
        var listeners = this.boundListeners.get(type);
        if (listeners != null) {
            var snapshot = ImmutableSet.copyOf(listeners);
            for (var listener : snapshot) {
                try {
                    event.acceptListener((EventListener<E, C>) listener);
                } catch (PolyglotException e) {
                    LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listener will be excluded from future dispatch.", type, e);
                    listeners.remove(listener);
                }
            }
        }
        return event;
    }

    public void tickScheduler() {
        var current = this.tick.getAndIncrement();
        for (Task task; (task = this.pendingTasks.peek()) != null && current >= task.target; this.pendingTasks.remove()) {
            try {
                task.callback.run();
            } catch (PolyglotException e) {
                LOGGER.error("An error occurred whilst executing a scheduled task.", e);
            }
        }
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
        var buffer = ImmutableMap.<Identifier, Source>builder();
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

    private void apply(@NotNull Map<Identifier, Source> prepared) {
        this.clear(true);
        for (var entry : prepared.entrySet()) {
            var identifier = entry.getKey();
            var source = entry.getValue();
            try {
                var ctx = Context.newBuilder("js")
                        .allowHostAccess(ScriptLoader.ACCESS)
                        .out(OutputStream.nullOutputStream())
                        .err(OutputStream.nullOutputStream())
                        .in(InputStream.nullInputStream())
                        .allowIO(IOAccess.NONE)
                        .build();
                var global = ctx.getBindings("js");
                global.putMember("script", this);
                ctx.eval(source);
                this.contexts.put(identifier, ctx);
            } catch (PolyglotException e) {
                LOGGER.error("An error occurred whilst loading script '{}'.", identifier, e);
            }
        }
        LOGGER.info("Loaded {} script(s).", this.contexts.size());
    }

    public void clear(boolean stageProperties) {
        var iterator = this.contexts.values()
                .iterator();
        while (iterator.hasNext()) {
            var ctx = iterator.next();
            ctx.close(true);
            iterator.remove();
        }
        this.boundListeners.clear();
        this.unboundListeners.clear();
        this.pendingTasks.clear();
    }

    private record Task(
            @NotNull ScheduledCallback callback,
            long target
    ) implements Comparable<Task> {

        public Task {
            Objects.requireNonNull(callback);
        }

        @Override
        public int compareTo(@NotNull Task other) {
            return Long.compare(this.target, other.target);
        }
    }

    @FunctionalInterface
    @HostAccess.Implementable
    private interface ScheduledCallback {

        Template<ScheduledCallback> TEMPLATE = Jet.function(ScheduledCallback.class, "() => void");

        @HostAccess.Export
        void run();
    }
}
