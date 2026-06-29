package dev.fusemc.disastrous;

import dev.fusemc.ValueOps;
import dev.fusemc.disastrous.disaster.WithInteraction;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.standard.server.ProxyServer;
import dev.fusemc.tau.Template;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public interface Callback {

    @FunctionalInterface
    interface Join extends Callback {

        @NotNull Template<Join> TEMPLATE = Template.functional(Join.class, Template.UNDEFINED);

        void onJoin(@NotNull ProxyPlayer player);
    }

    @FunctionalInterface
    interface Tick extends Callback {

        Template<Tick> TEMPLATE = Template.functional(Tick.class, Template.UNDEFINED);

        void onTick(@NotNull ProxyServer server);
    }

    @FunctionalInterface
    interface ItemInteract extends Callback {

        Template<ItemInteract> TEMPLATE = Template.functional(ItemInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyItem item,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface BlockInteract extends Callback {

        Template<BlockInteract> TEMPLATE = Template.functional(BlockInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyBlock block,
                                              @NotNull ProxyVec3 position,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface EntityInteract extends Callback {

        Template<EntityInteract> TEMPLATE = Template.functional(EntityInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyEntity<?> entity,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface Unbound extends Callback {

        @NotNull Template<Unbound> TEMPLATE = Template.functional(Unbound.class, Template.ANY);

        @NotNull Value onEvent(@NotNull Value @NotNull ... args);
    }
}
