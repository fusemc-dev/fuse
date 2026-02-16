package dev.fusemc.standard.server;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import dev.fusemc.ParseException;
import dev.fusemc.standard.ScriptWrapper;
import dev.fusemc.standard.util.ScriptIdentifier;
import com.manchickas.jet.exception.TypeException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
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

    @HostAccess.Export
    public @Nullable ScriptWorld world(@NotNull Value world) throws TypeException, ParseException {
        return ScriptWorld.wrap(this.wrapped.getLevel(ResourceKey.create(
                Registries.DIMENSION,
                ScriptIdentifier.expectVanilla(world)
        )));
    }
}
