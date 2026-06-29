package dev.fusemc.disastrous.disaster.standard.interact;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.disaster.WithHand;
import dev.fusemc.disastrous.disaster.WithInteraction;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class EntityInteract extends WithInteraction<Callback.EntityInteract>
        implements WithHand<Callback.EntityInteract> {

    private final @NotNull ProxyPlayer    player;
    private final @NotNull ProxyEntity<?> entity;
    private final @NotNull ProxyHand      hand;

    public EntityInteract(@NotNull ProxyPlayer player,
                          @NotNull ProxyEntity<?> entity,
                          @NotNull ProxyHand hand) {
        this.player = Objects.requireNonNull(player);
        this.entity = Objects.requireNonNull(entity);
        this.hand   = Objects.requireNonNull(hand);
    }

    @Override
    public boolean onDispatch(@NotNull Callback.EntityInteract callback) {
        return this.suggestResult(callback.onInteract(this.player, this.entity, this.hand));
    }

    @Override
    public @NotNull Type<Callback.EntityInteract> type() {
        return Disastrous.ENTITY_INTERACT;
    }

    @Override
    public @NonNull ProxyHand hand() {
        return this.hand;
    }
}
