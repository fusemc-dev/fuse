package dev.fusemc.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import dev.fusemc.disastrous.disaster.standard.BlockBreak;
import dev.fusemc.disastrous.disaster.standard.interact.BlockInteract;
import dev.fusemc.lifecycle.Entrypoint;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.disastrous.disaster.standard.interact.ItemInteract;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @WrapOperation(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    InteractionResult onItemInteract(@NotNull ItemStack instance,
                                     @NotNull Level level,
                                     @NotNull Player player,
                                     @NotNull InteractionHand interactionHand,
                                     @NotNull Operation<InteractionResult> original) {
        var script = Entrypoint.instance();
        var event  = script.dispatch(new ItemInteract(
                ProxyPlayer.from((ServerPlayer) player),
                ProxyItem.from(instance),
                ProxyHand.from(interactionHand)
        ));
        return event.resultOr(() -> original.call(instance, level, player, interactionHand));
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    InteractionResult onInteractOn(BlockState instance, Level level, Player player, BlockHitResult blockHitResult, Operation<InteractionResult> original, @Local(argsOnly = true) InteractionHand hand) {
        var script = Entrypoint.instance();
        var event  = script.dispatch(new BlockInteract(
                ProxyPlayer.from((ServerPlayer) player),
                ProxyBlock.from(instance),
                ProxyVec3.from(blockHitResult.getBlockPos()),
                ProxyHand.from(hand)
        ));
        return event.resultOr(() -> original.call(instance, level, player, blockHitResult));
    }

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    BlockState onBlockBreak(Block instance, Level level, BlockPos blockPos, BlockState blockState, Player player, Operation<BlockState> original, @Cancellable CallbackInfoReturnable<Boolean> ci) {
        var script = Entrypoint.instance();
        var event  = script.dispatch(new BlockBreak(
                ProxyPlayer.from((ServerPlayer) player),
                ProxyBlock.from(blockState),
                ProxyVec3.from(blockPos)
        ));
        if (event.isCancelled()) {
            ci.setReturnValue(false);
            return blockState;
        }
        return original.call(instance, level, blockPos, blockState, player);
    }
}
