package dev.fusemc.disastrous.disaster.standard.tick;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.disastrous.disaster.WithLifetime;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.server.ProxyWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An occurrence of a tick on a [ProxyEntity].
///
/// ---
///
/// ```typescript
/// type EntityTick = (entity: Entity) => void;
/// ```
///
/// @since 0.1.0
public final class EntityTick implements WithLifetime<Callback.EntityTick> {

    private final @NotNull ProxyEntity<?> entity;

    public EntityTick(@NotNull ProxyEntity<?> entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    @Override
    public boolean dispatch(@NotNull Callback.EntityTick callback) {
        callback.onTick(this.entity);
        return true;
    }

    @Override
    public @NotNull Type<Callback.EntityTick> type() {
        return Disastrous.ENTITY_TICK;
    }

    @Override
    public int lifetime() {
        var entity = this.entity.unwrap();
        return entity.tickCount;
    }
}
