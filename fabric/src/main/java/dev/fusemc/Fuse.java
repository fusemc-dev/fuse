package dev.fusemc;

import dev.fusemc.lifecycle.ScriptLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public final class Fuse implements ModInitializer {

    @Override
    public void onInitialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
                Identifier.fromNamespaceAndPath("fuse","script"),
                ScriptLoader.instance()
        );
    }
}
