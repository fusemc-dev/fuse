package dev.fusemc.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.fusemc.disastrous.disaster.standard.interact.BlockInteract;
import dev.fusemc.lifecycle.ScriptLoader;
import dev.fusemc.standard.ProxyHand;
import dev.fusemc.disastrous.disaster.standard.interact.ItemInteract;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @WrapOperation(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    InteractionResult onItemInteract(@NotNull ItemStack instance,
                                     @NotNull Level level,
                                     @NotNull Player player,
                                     @NotNull InteractionHand interactionHand,
                                     @NotNull Operation<InteractionResult> original) {
        var script = ScriptLoader.instance();
        var event  = script.dispatch(new ItemInteract(
                ProxyPlayer.from((ServerPlayer) player),
                ProxyItem.from(instance),
                ProxyHand.from(interactionHand)
        ));
        var result = event.result();
        if (result == InteractionResult.PASS)
            return original.call(instance, level, player, interactionHand);
        return result;
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    InteractionResult onInteractOn(BlockState instance, Level level, Player player, BlockHitResult blockHitResult, Operation<InteractionResult> original, @Local(argsOnly = true) InteractionHand hand) {
        var script = ScriptLoader.instance();
        var event  = script.dispatch(new BlockInteract(
                ProxyPlayer.from((ServerPlayer) player),
                ProxyBlock.from(instance),
                ProxyVec3.from(blockHitResult.getBlockPos()),
                ProxyHand.from(hand)
        ));
        var result = event.result();
        if (result == InteractionResult.PASS)
            return original.call(instance, level, player, blockHitResult);
        return result;
    }
}
