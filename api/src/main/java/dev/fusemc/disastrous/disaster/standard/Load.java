package dev.fusemc.disastrous.disaster.standard;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a [ProxyServer] being initiated or reloaded.
///
/// ---
///
/// ```typescript
/// type Load = (server: Server) => void;
/// ```
///
/// @since 0.1.0
public final class Load implements Disaster<Callback.Load> {

    private final @NotNull ProxyServer server;

    public Load(@NotNull ProxyServer server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public boolean dispatch(@NotNull Callback.Load callback) {
        callback.onLoad(this.server);
        return true;
    }

    @Override
    public @NotNull Type<Callback.Load> type() {
        return Disastrous.LOAD;
    }
}

