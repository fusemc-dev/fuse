package com.manchickas.fuse.lifecycle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.manchickas.Charcoal;
import com.manchickas.fuse.ParseException;
import com.manchickas.fuse.disastrous.event.Event;
import com.manchickas.fuse.disastrous.event.EventCallback;
import com.manchickas.fuse.disastrous.event.EventListener;
import com.manchickas.fuse.disastrous.event.EventType;
import com.manchickas.fuse.disastrous.selector.EventSelector;
import com.manchickas.fuse.disastrous.selector.Parser;
import com.manchickas.fuse.standard.event.JoinEvent;
import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    private final @NotNull Map<EventType<?, ?>, Set<EventListener.Bound<?, ?>>> boundListeners;
    private final @NotNull Map<Identifier, Set<EventListener.Unbound>> unboundListeners;

    public ScriptLoader() {
        this.contexts = HashBiMap.create();
        this.boundListeners = new Reference2ReferenceOpenHashMap<>();
        this.unboundListeners = new Object2ReferenceOpenHashMap<>();
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
            var listener = ((EventSelector.Unbound) parsed)
                    .bind(Jet.expect(EventCallback.Unbound.TEMPLATE, callback));
            this.unboundListeners.computeIfAbsent(identifier, _ -> new ObjectOpenHashSet<>())
                    .add(listener);
        } catch (ParseException e) {
            var span = e.span();
            if (span instanceof Option.Some<SourceSpan<String>>(var wrapped)) {
                LOGGER.error("({}) An error occurred whilst parsing an event selector: {}\n{}",
                        wrapped, e.getMessage(), wrapped.format(Charcoal.red()));
                return;
            }
            LOGGER.error("An error occurred whilst parsing an event selector: {}", e.getMessage());
        }
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
                    event.acceptListener((EventListener.Bound<E, C>) listener);
                } catch (PolyglotException e) {
                    LOGGER.error("An error occurred whilst dispatching an event '{}' to one of its listeners. The faulty listen will be excluded from future dispatch.", type, e);
                    listeners.remove(listener);
                }
            }
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
}
