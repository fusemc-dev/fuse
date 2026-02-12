package com.manchickas.fuse.standard.event;

import com.manchickas.fuse.disastrous.event.*;
import com.manchickas.fuse.standard.entity.living.ScriptPlayer;
import com.manchickas.fuse.standard.guard.PlayerNameGuard;
import com.manchickas.jet.Jet;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.HostAccess;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record JoinEvent(@NotNull ScriptPlayer player) implements Event<JoinEvent, JoinEvent.Callback>,
        PlayerEvent<JoinEvent, JoinEvent.Callback> {

    public static final EventType<JoinEvent, Callback> TYPE = new EventType<>(
            Jet.function(Callback.class, "(player: Player) => void"),
            (guard) -> switch (guard) {
                case "name" -> Option.some(PlayerNameGuard.TYPE);
                default -> Option.none();
            }
    );

    public JoinEvent {
        Objects.requireNonNull(player);
    }

    @Override
    public void accept(@NotNull JoinEvent.Callback callback) {
        callback.onJoin(this.player);
    }

    @Override
    public @NotNull EventType<JoinEvent, Callback> type() {
        return JoinEvent.TYPE;
    }

    @Override
    public @NotNull JoinEvent self() {
        return this;
    }

    @FunctionalInterface
    @HostAccess.Implementable
    public interface Callback extends EventCallback {

        @HostAccess.Export
        void onJoin(@NotNull ScriptPlayer player);
    }
}
