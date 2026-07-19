package dev.fusemc.disastrous.disaster.standard.interact;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.Type;
import dev.fusemc.disastrous.disaster.WithHand;
import dev.fusemc.disastrous.disaster.WithInteraction;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/// An occurrence of a [ProxyPlayer] interacting with a [ProxyBlock].
///
/// ---
///
/// ```typescript
/// type BlockInteract = (player: Player, block: Block, position: Vec3, hand: Hand)
///     => "success" | "fail" | "consume" | "pass";
/// ```
/// @since 0.1.0
public final class BlockInteract extends WithInteraction<Callback.BlockInteract>
        implements WithHand<Callback.BlockInteract> {

    private final @NotNull ProxyPlayer player;
    private final @NotNull ProxyBlock  block;
    private final @NotNull ProxyVec3   position;
    private final @NotNull ProxyHand   hand;

    public BlockInteract(@NotNull ProxyPlayer player,
                         @NotNull ProxyBlock block,
                         @NotNull ProxyVec3 position,
                         @NotNull ProxyHand hand) {
        this.player   = Objects.requireNonNull(player);
        this.block    = Objects.requireNonNull(block);
        this.position = Objects.requireNonNull(position);
        this.hand     = Objects.requireNonNull(hand);
    }

    @Override
    public boolean dispatch(@NotNull Callback.BlockInteract callback) {
        return this.suggestResult(callback.onInteract(this.player, this.block, this.position, this.hand));
    }

    @Override
    public @NotNull Type<Callback.BlockInteract> type() {
        return Disastrous.BLOCK_INTERACT;
    }

    @Override
    public @NonNull ProxyHand hand() {
        return this.hand;
    }
}
