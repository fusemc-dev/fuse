package dev.fusemc.disastrous.disaster.standard;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a [ProxyServer] being shutdown or reloaded.
///
/// ---
///
/// ```typescript
/// type Unload = (server: Server) => void;
/// ```
///
/// @since 0.1.0
public final class Unload implements Disaster<Callback.Unload> {

    private final @NotNull ProxyServer server;

    public Unload(@NotNull ProxyServer server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public boolean dispatch(@NotNull Callback.Unload callback) {
        callback.onUnload(this.server);
        return true;
    }

    @Override
    public @NotNull Type<Callback.Unload> type() {
        return Disastrous.UNLOAD;
    }
}
