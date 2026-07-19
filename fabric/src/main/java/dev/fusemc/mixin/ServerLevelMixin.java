package dev.fusemc.mixin;

import dev.fusemc.disastrous.disaster.standard.tick.EntityTick;
import dev.fusemc.lifecycle.Entrypoint;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.disastrous.disaster.standard.Join;
import dev.fusemc.standard.server.ProxyServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow @Final
    private @NotNull MinecraftServer server;

    @Inject(method = "addPlayer", at = @At("TAIL"))
    public void onPlayerConnected(@NotNull ServerPlayer player, @NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        script.dispatch(new Join(
                ProxyServer.from(this.server),
                ProxyPlayer.from(player)
        ));
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    public void onTickNonPassenger(@NotNull Entity entity,
                                   @NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        script.dispatch(new EntityTick(ProxyEntity.from(entity)));
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    public void onTickPassenger(@NotNull Entity entity,
                                @NotNull Entity passenger,
                                @NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        script.dispatch(new EntityTick(ProxyEntity.from(passenger)));
    }
}
