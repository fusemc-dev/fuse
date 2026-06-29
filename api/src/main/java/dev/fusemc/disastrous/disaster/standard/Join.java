package dev.fusemc.disastrous.disaster.standard;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Join implements Disaster<Callback.Join> {

    private final @NotNull ProxyPlayer player;

    public Join(@NotNull ProxyPlayer player) {
        this.player = Objects.requireNonNull(player);
    }

    @Override
    public boolean onDispatch(@NotNull Callback.Join callback) {
        callback.onJoin(this.player);
        return true;
    }

    @Override
    public @NotNull Disaster.Type<Callback.Join> type() {
        return Disastrous.JOIN;
    }
}
