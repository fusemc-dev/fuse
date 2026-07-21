package dev.fusemc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.fusemc.disastrous.disaster.standard.Load;
import dev.fusemc.disastrous.disaster.standard.Unload;
import dev.fusemc.disastrous.disaster.standard.tick.ServerTick;
import dev.fusemc.disastrous.disaster.standard.tick.WorldTick;
import dev.fusemc.iota.Iota;
import dev.fusemc.lifecycle.Entrypoint;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.standard.server.ProxyServer;
import dev.fusemc.standard.server.ProxyWorld;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServerMixin.class);

    @Shadow @Final
    private ServerTickRateManager tickRateManager;

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"))
    void onTick(@NotNull BooleanSupplier booleanSupplier, @NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        var self   = (MinecraftServer) (Object) this;
        if (this.tickRateManager.runsNormally()) {
            script.dispatch(new ServerTick(ProxyServer.from(self)));
            script.tickPending(self);
        }
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    void onWorldTick(@NotNull BooleanSupplier booleanSupplier,
                     @NotNull CallbackInfo ci,
                     @Local ServerLevel world) {
        var script = Entrypoint.instance();
        if (this.tickRateManager.runsNormally())
            script.dispatch(new WorldTick(ProxyWorld.from(world)));
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
    void onShutdown(@NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        var self   = (MinecraftServer) (Object) this;
        script.dispatch(new Unload(ProxyServer.from(self)));
        try {
            var path   = self.getFile("./properties.iota");
            var properties = script.refresh();
            var serialized = Iota.marshal(
                    Entrypoint.IOTA,
                    Tau.raise(
                            Template.array(Property.TEMPLATE, Property[]::new),
                            properties
                    ),
                    4
            );
            Files.writeString(path, serialized, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to store properties.", e);
        }
    }

    @WrapOperation(method = "reloadResources", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    <U> CompletableFuture<U> onBeforeReload(@NotNull Supplier<U> supplier,
                                            @NotNull Executor executor,
                                            @NotNull Operation<CompletableFuture<U>> original) {
        return CompletableFuture.runAsync(() -> {
            var script = Entrypoint.instance();
            var self   = (MinecraftServer) (Object) this;
            script.dispatch(new Unload(ProxyServer.from(self)));
        }, executor).thenComposeAsync((_) -> original.call(supplier, executor), executor);
    }

    @Inject(method = "method_29440(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;reloadResources()V"))
    void onReload(@NotNull Collection<String> collection,
                  @NotNull MinecraftServer.ReloadableResources reloadableResources,
                  @NotNull CallbackInfo ci) {
        var script = Entrypoint.instance();
        var self   = (MinecraftServer) (Object) this;
        script.refreshTree(self);
        script.dispatch(new Load(ProxyServer.from(self)));
    }
}
