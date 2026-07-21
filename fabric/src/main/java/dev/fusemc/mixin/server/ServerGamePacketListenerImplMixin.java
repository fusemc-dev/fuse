package dev.fusemc.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.fusemc.disastrous.disaster.standard.Swing;
import dev.fusemc.disastrous.disaster.standard.interact.EntityInteract;
import dev.fusemc.lifecycle.Entrypoint;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.math.ProxyVec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @WrapOperation(method = "handleAnimate", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    void onSwing(ServerPlayer player, InteractionHand hand, Operation<Void> original) {
        var script = Entrypoint.instance();
        var event  = script.dispatch(new Swing(
                ProxyPlayer.from(player),
                ProxyHand.from(hand)
        ));
        if (event.isCancelled())
            return;
        original.call(player, hand);
    }

    @Mixin(targets="net.minecraft.server.network.ServerGamePacketListenerImpl$1")
    public static class InteractionHandler {

        @WrapMethod(method = "method_33898")
        private static InteractionResult onInteraction(@NotNull Vec3 vec3,
                                                       @NotNull ServerPlayer player,
                                                       @NotNull Entity entity,
                                                       @NotNull InteractionHand hand,
                                                       @NotNull Operation<InteractionResult> original) {
            var script = Entrypoint.instance();
            var event = script.dispatch(new EntityInteract(
                    ProxyPlayer.from(player),
                    ProxyEntity.from(entity),
                    ProxyVec3.from(vec3),
                    ProxyHand.from(hand)
            ));
            return event.resultOr(() -> original.call(vec3, player, entity, hand));
        }
    }
}
