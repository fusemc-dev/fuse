package dev.fusemc.disastrous.disaster.standard.tick;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Type;
import dev.fusemc.disastrous.disaster.WithLifetime;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a tick on a [ProxyServer].
///
/// ---
///
/// ```typescript
/// type ServerTick = (server: Server) => void;
/// ```
///
/// @since 0.1.0
public final class ServerTick implements WithLifetime<Callback.ServerTick> {

    private final @NotNull ProxyServer server;

    public ServerTick(@NotNull ProxyServer server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public boolean dispatch(@NotNull Callback.ServerTick callback) {
        callback.onTick(this.server);
        return true;
    }

    @Override
    public @NotNull Type<Callback.ServerTick> type() {
        return Disastrous.SERVER_TICK;
    }

    @Override
    public int lifetime() {
        var server = this.server.unwrap();
        return server.getTickCount();
    }
}
