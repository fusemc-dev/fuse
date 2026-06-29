package dev.fusemc.mixin;

import dev.fusemc.iota.Iota;
import dev.fusemc.lifecycle.ScriptLoader;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServerMixin.class);

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
    void onShutdown(CallbackInfo ci) {
        var self   = (MinecraftServer) (Object) this;
        var path   = self.getFile("./properties.iota");
        var script = ScriptLoader.instance();
        try {
            var properties = script.refresh();
            var serialized = Iota.marshal(
                    ScriptLoader.IOTA,
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

    @Inject(method = "method_29440(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;reloadResources()V"))
    void onReload(@NotNull Collection<String> collection,
                  @NotNull MinecraftServer.ReloadableResources reloadableResources,
                  @NotNull CallbackInfo ci) {
        var self   = (MinecraftServer) (Object) this;
        var script = ScriptLoader.instance();
        script.refreshTree(self);
    }
}
