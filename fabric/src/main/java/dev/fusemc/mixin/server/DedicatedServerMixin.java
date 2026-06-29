package dev.fusemc.mixin.server;

import dev.fusemc.iota.Iota;
import dev.fusemc.lifecycle.ScriptLoader;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.server.dedicated.DedicatedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(DedicatedServerMixin.class);

    @Inject(method = "initServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServerSettings;getProperties()Lnet/minecraft/server/dedicated/DedicatedServerProperties;"))
    public void onInitServer(CallbackInfoReturnable<Boolean> cir) {
        var self = (DedicatedServer) (Object) this;
        var properties = self.getFile("./properties.iota");
        var script = ScriptLoader.instance();
        if (Files.exists(properties)) {
            try {
                var result = Iota.unmarshal(ScriptLoader.IOTA, properties);
                script.rehydrate(Tau.lower(
                        Template.array(Property.TEMPLATE, Property[]::new),
                        result
                ));
                LOGGER.info("Successfully rehydrated properties from disk.");
            } catch (IOException e) {
                LOGGER.error("Failed to load properties.iota", e);
            }
            script.refreshTree(self);
            return;
        }
        LOGGER.info("Could not locate properties.iota on disk... Welcome to fuse!");
        script.refreshTree(self);
    }
}
