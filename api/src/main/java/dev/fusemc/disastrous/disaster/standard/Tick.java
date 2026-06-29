package dev.fusemc.disastrous.disaster.standard;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Tick implements Disaster<Callback.Tick> {

    private final @NotNull ProxyServer server;

    public Tick(@NotNull ProxyServer server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public boolean onDispatch(@NotNull Callback.Tick callback) {
        Objects.requireNonNull(callback);
        callback.onTick(this.server);
        return true;
    }

    @Override
    public @NotNull Type<Callback.Tick> type() {
        return Disastrous.TICK;
    }
}
