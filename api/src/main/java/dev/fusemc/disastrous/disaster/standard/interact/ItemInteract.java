package dev.fusemc.disastrous.disaster.standard.interact;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.disastrous.disaster.WithHand;
import dev.fusemc.disastrous.disaster.WithInteraction;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/// An occurrence of a [ProxyPlayer] interacting with a [ProxyItem].
///
/// ---
/// ```typescript
/// type ItemInteract = (player: Player, item: Item, hand: Hand)
///     => "success" | "fail" | "consume" | "pass";
/// ```
///
/// @since 0.1.0
public final class ItemInteract extends WithInteraction<Callback.ItemInteract>
        implements WithHand<Callback.ItemInteract> {

    private final @NotNull ProxyPlayer player;
    private final @NotNull ProxyItem   item;
    private final @NotNull ProxyHand   hand;

    public ItemInteract(@NotNull ProxyPlayer player,
                        @NotNull ProxyItem item,
                        @NotNull ProxyHand hand) {
        this.player = Objects.requireNonNull(player);
        this.item   = Objects.requireNonNull(item);
        this.hand   = Objects.requireNonNull(hand);
    }

    @Override
    public boolean dispatch(@NotNull Callback.ItemInteract callback) {
        return this.suggestResult(callback.onInteract(this.player, this.item, this.hand));
    }

    @Override
    public @NotNull Type<Callback.ItemInteract> type() {
        return Disastrous.ITEM_INTERACT;
    }

    @Override
    public @NonNull ProxyHand hand() {
        return this.hand;
    }
}
