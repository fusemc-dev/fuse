package com.manchickas.fuse.standard.server;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.manchickas.fuse.standard.ScriptWrapper;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ScriptServer extends ScriptWrapper<MinecraftServer> {

    private static final Interner<ScriptServer> INTERNER
            = Interners.newWeakInterner();

    private ScriptServer(@NonNull MinecraftServer wrapped) {
        super(wrapped);
    }

    public static ScriptServer wrap(@Nullable MinecraftServer server) {
        if (server != null)
            return ScriptServer.INTERNER.intern(new ScriptServer(server));
        return null;
    }
}
