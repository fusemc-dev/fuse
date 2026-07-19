package dev.fusemc.disastrous;

import dev.fusemc.disastrous.disaster.WithInteraction;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.standard.server.ProxyServer;
import dev.fusemc.standard.server.ProxyWorld;
import dev.fusemc.tau.Template;
import net.minecraft.world.InteractionResult;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public interface Callback {

    @FunctionalInterface
    interface Join extends Callback {

        @NotNull Template<Join> TEMPLATE = Template.functional(Join.class);

        void onJoin(@NotNull ProxyServer server, @NotNull ProxyPlayer player);
    }

    @FunctionalInterface
    interface Load extends Callback {

        @NotNull Template<Load> TEMPLATE = Template.functional(Load.class);

        void onLoad(@NotNull ProxyServer server);
    }

    @FunctionalInterface
    interface Unload extends Callback {

        @NotNull Template<Unload> TEMPLATE = Template.functional(Unload.class);

        void onUnload(@NotNull ProxyServer server);
    }

    @FunctionalInterface
    interface ServerTick extends Callback {

        @NotNull Template<ServerTick> TEMPLATE = Template.functional(ServerTick.class);

        void onTick(@NotNull ProxyServer server);
    }

    @FunctionalInterface
    interface WorldTick extends Callback {

        @NotNull Template<WorldTick> TEMPLATE = Template.functional(WorldTick.class);

        void onTick(@NotNull ProxyWorld server);
    }

    @FunctionalInterface
    interface EntityTick extends Callback {

        @NotNull Template<EntityTick> TEMPLATE = Template.functional(EntityTick.class);

        void onTick(@NotNull ProxyEntity<?> entity);
    }

    @FunctionalInterface
    interface ItemInteract extends Callback {

        @NotNull Template<ItemInteract> TEMPLATE = Template.functional(ItemInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyItem item,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface BlockInteract extends Callback {

        @NotNull Template<BlockInteract> TEMPLATE = Template.functional(BlockInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyBlock block,
                                              @NotNull ProxyVec3 position,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface EntityInteract extends Callback {

        @NotNull Template<EntityInteract> TEMPLATE = Template.functional(EntityInteract.class, WithInteraction.RESULT);

        @NotNull InteractionResult onInteract(@NotNull ProxyPlayer player,
                                              @NotNull ProxyEntity<?> entity,
                                              @NotNull ProxyVec3 position,
                                              @NotNull ProxyHand hand);
    }

    @FunctionalInterface
    interface Unbound extends Callback {

        @NotNull Template<Unbound> TEMPLATE = Template.functional(Unbound.class, Template.ANY);

        @NotNull Value onEvent(@NotNull Value @NotNull ... args);
    }
}
