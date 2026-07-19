package dev.fusemc.disastrous.disaster.standard.tick;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.disastrous.disaster.WithLifetime;
import dev.fusemc.standard.server.ProxyWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a tick on a [ProxyWorld].
///
/// ---
///
/// ```typescript
/// type WorldTick = (world: World) => void;
/// ```
///
/// @since 0.1.0
public final class WorldTick implements WithLifetime<Callback.WorldTick> {

    private final @NotNull ProxyWorld world;

    public WorldTick(@NotNull ProxyWorld world) {
        this.world = Objects.requireNonNull(world);
    }

    @Override
    public boolean dispatch(@NotNull Callback.WorldTick callback) {
        callback.onTick(this.world);
        return true;
    }

    @Override
    public @NotNull Type<Callback.WorldTick> type() {
        return Disastrous.WORLD_TICK;
    }

    @Override
    public int lifetime() {
        var world  = this.world.unwrap();
        var server = world.getServer();
        return server.getTickCount();
    }
}
