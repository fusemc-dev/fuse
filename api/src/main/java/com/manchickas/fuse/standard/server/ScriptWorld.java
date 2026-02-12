package com.manchickas.fuse.standard.server;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.manchickas.fuse.standard.ScriptWrapper;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ScriptWorld extends ScriptWrapper<ServerLevel> {

    private static final Interner<ScriptWorld> INTERNER
            = Interners.newWeakInterner();

    private ScriptWorld(@NonNull ServerLevel wrapped) {
        super(wrapped);
    }

    public static ScriptWorld wrap(@Nullable ServerLevel world) {
        if (world != null)
            return INTERNER.intern(new ScriptWorld(world));
        return null;
    }
}
