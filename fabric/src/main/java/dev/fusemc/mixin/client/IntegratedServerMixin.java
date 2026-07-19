package dev.fusemc.mixin.client;

import dev.fusemc.disastrous.disaster.standard.Load;
import dev.fusemc.iota.Iota;
import dev.fusemc.lifecycle.Entrypoint;
import dev.fusemc.lifecycle.property.Property;
import dev.fusemc.standard.server.ProxyServer;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Unique
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    @Inject(method = "initServer", at = @At(value = "HEAD"))
    public void onInitProperties(CallbackInfoReturnable<Boolean> cir) {
        var self       = (MinecraftServer) (Object) this;
        var properties = self.getFile("./properties.iota");
        var script     = Entrypoint.instance();
        if (Files.exists(properties)) {
            try {
                var result = Iota.unmarshal(Entrypoint.IOTA, properties);
                script.rehydrate(Tau.lower(
                        Template.array(Property.TEMPLATE, Property[]::new),
                        result
                ));
                LOGGER.info("Successfully rehydrated properties from disk.");
            } catch (IOException e) {
                LOGGER.error("Failed to load properties.iota", e);
            }
            return;
        }
        LOGGER.info("Could not locate properties.iota on disk... Welcome to fuse!");
    }

    @Inject(method = "initServer", at = @At("TAIL"))
    public void onInitServer(CallbackInfoReturnable<Boolean> cir) {
        var self   = (MinecraftServer) (Object) this;
        var script = Entrypoint.instance();
        script.refreshTree(self);
        script.dispatch(new Load(ProxyServer.from(self)));
    }
}
