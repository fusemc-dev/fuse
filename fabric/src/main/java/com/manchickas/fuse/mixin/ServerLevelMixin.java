package com.manchickas.fuse.mixin;

import com.manchickas.fuse.lifecycle.ScriptLoader;
import com.manchickas.fuse.standard.entity.living.ScriptPlayer;
import com.manchickas.fuse.standard.event.JoinEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addPlayer", at = @At("TAIL"))
    public void onPlayerConnected(ServerPlayer player, CallbackInfo ci) {
        var wrapped = ScriptPlayer.wrap(player);
        var event = new JoinEvent(wrapped);
        ScriptLoader.instance()
                .dispatchBound(event);
    }
}
