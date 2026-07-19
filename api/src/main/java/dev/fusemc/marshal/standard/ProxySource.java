package dev.fusemc.marshal.standard;

import dev.fusemc.ValueOps;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.standard.server.ProxyServer;
import dev.fusemc.standard.server.ProxyWorld;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import net.minecraft.commands.CommandSourceStack;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Documented("Source")
public final class ProxySource implements ProxyObject {

    private static final @NotNull String ENTITY       = "entity";
    private static final @NotNull String PLAYER       = "player";
    private static final @NotNull String WORLD        = "world";
    private static final @NotNull String SERVER       = "server";
    private static final @NotNull String POSITION     = "position";
    private static final @NotNull String SEND_MESSAGE = "sendMessage";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxySource.POSITION,
            ProxySource.ENTITY,
            ProxySource.PLAYER,
            ProxySource.WORLD,
            ProxySource.SERVER,
            ProxySource.SEND_MESSAGE,
    };

    private final @NotNull CommandSourceStack stack;
    private final @NotNull ProxyServer server;
    private final @NotNull ProxyWorld world;
    private final @NotNull ProxyVec3 position;
    private final @NotNull ProxyExecutable entity;
    private final @NotNull ProxyExecutable player;
    private final @NotNull ProxyExecutable sendMessage;

    private ProxySource(@NotNull CommandSourceStack stack) {
        this.stack    = Objects.requireNonNull(stack);
        this.server   = ProxyServer.from(this.stack.getServer());
        this.world    = ProxyWorld.from(this.stack.getLevel());
        this.position = ProxyVec3.from(this.stack.getPosition());
        this.entity = (args) -> {
            if (args.length == 0) {
                var entity = this.stack.getEntity();
                if (entity != null)
                    return ProxyEntity.from(entity);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.player = (args) -> {
            if (args.length == 0) {
                var player = this.stack.getPlayer();
                if (player != null)
                    return ProxyPlayer.from(player);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.sendMessage = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                this.stack.sendSystemMessage(component);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxySource from(@NotNull CommandSourceStack stack) {
        Objects.requireNonNull(stack);
        return new ProxySource(stack);
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxySource.SERVER       -> this.server;
            case ProxySource.WORLD        -> this.world;
            case ProxySource.POSITION     -> this.position;
            case ProxySource.ENTITY       -> this.entity;
            case ProxySource.PLAYER       -> this.player;
            case ProxySource.SEND_MESSAGE -> this.sendMessage;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxySource.KEYS.length)
                    return ProxySource.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxySource.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxySource.KEYS) {
            if (candidate.equals(key))
                return true;
        }
        return false;
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }
}
