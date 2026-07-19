package dev.fusemc.disastrous.disaster.standard;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Type;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a [ProxyPlayer] connecting to a [ProxyServer].
///
/// ---
///
/// ```typescript
/// type Join = (server: Server, player: Player) => void;
/// ```
///
/// @since 0.1.0
public final class Join implements Disaster<Callback.Join> {

    private final @NotNull ProxyServer server;
    private final @NotNull ProxyPlayer player;

    public Join(@NotNull ProxyServer server,
                @NotNull ProxyPlayer player) {
        this.player = Objects.requireNonNull(player);
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public boolean dispatch(@NotNull Callback.Join callback) {
        callback.onJoin(this.server, this.player);
        return true;
    }

    @Override
    public @NotNull Type<Callback.Join> type() {
        return Disastrous.JOIN;
    }
}
