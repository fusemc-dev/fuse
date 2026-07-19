package dev.fusemc.standard.server;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import dev.fusemc.ValueOps;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.ProxyRegistry;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.StreamSupport;

public final class ProxyServer implements ProxyObject {

    private static final @NotNull String WORLD    = "world";
    private static final @NotNull String WORLDS   = "worlds";
    private static final @NotNull String PLAYER   = "player";
    private static final @NotNull String PLAYERS  = "players";
    private static final @NotNull String GAMERULE = "gamerule";
    private static final @NotNull String REGISTRY = "registry";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyServer.WORLD,
            ProxyServer.WORLDS,
            ProxyServer.PLAYER,
            ProxyServer.PLAYERS,
            ProxyServer.GAMERULE,
            ProxyServer.REGISTRY,
    };
    private static final Interner<ProxyServer> INTERNER
            = Interners.newWeakInterner();

    private final @NotNull MinecraftServer self;
    private final @NotNull ProxyExecutable world;
    private final @NotNull ProxyExecutable worlds;
    private final @NotNull ProxyExecutable player;
    private final @NotNull ProxyExecutable players;
    private final @NotNull ProxyExecutable gamerule;
    private final @NotNull ProxyExecutable registry;

    private ProxyServer(@NonNull MinecraftServer self) {
        this.self = Objects.requireNonNull(self);
        this.world = (args) -> {
            if (args.length == 1) {
                var type  = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key   = ResourceKey.create(Registries.DIMENSION, type);
                var level = this.self.getLevel(key);
                if (level != null)
                    return ProxyWorld.from(level);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.worlds = (args) -> {
            if (args.length == 0)
                return StreamSupport.stream(this.self.getAllLevels().spliterator(), false)
                        .map(ProxyWorld::from)
                        .toArray(ProxyWorld[]::new);
            throw new UnsupportedOperationException();
        };
        this.player = (args) -> {
            if (args.length == 1) {
                var name = Tau.lower(Template.STRING, args[0]);
                var list = this.self.getPlayerList();
                for (var candidate : list.getPlayers()) {
                    var profile = candidate.getGameProfile();
                    if (profile.name().equals(name))
                        return ProxyPlayer.from(candidate);
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.players = (args) -> {
            if (args.length == 0)
                return this.self.getPlayerList().getPlayers().stream()
                        .map(ProxyPlayer::from)
                        .toArray(ProxyPlayer[]::new);
            throw new UnsupportedOperationException();
        };
        this.gamerule = (args) -> {
            if (args.length == 2) {
                var type  = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var rule  = BuiltInRegistries.GAME_RULE.getValue(type);
                if (rule != null)
                    return this.setRule(rule, args[1]);
                return Tau.undefined();
            }
            if (args.length == 1) {
                var type  = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var rule  = BuiltInRegistries.GAME_RULE.getValue(type);
                if (rule != null)
                    return this.self.getWorldData()
                            .getGameRules()
                            .get(rule);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.registry = (args) -> {
            if (args.length == 1) {
                var type   = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key    = ResourceKey.createRegistryKey(type);
                var registry = this.self.reloadableRegistries()
                        .lookup()
                        .lookup(key);
                return registry.map(ProxyRegistry::from)
                        .map(wrapped -> (Object) wrapped)
                        .orElseGet(Tau::undefined);
            }
            throw new UnsupportedOperationException();
        };
    }

    public static ProxyServer from(@Nullable MinecraftServer server) {
        if (server != null)
            return ProxyServer.INTERNER.intern(new ProxyServer(server));
        return null;
    }

    @ApiStatus.Internal
    private <T> @NotNull Value setRule(@NotNull GameRule<T> rule,
                                       @NotNull Value value) {
        this.self.getWorldData()
                .getGameRules()
                .set(rule, Tau.lower(ValueOps.delegate(rule.valueCodec(), Description.ELLIPSIS), value), this.self);
        return Tau.undefined();
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyServer.WORLD    -> this.world;
            case ProxyServer.WORLDS   -> this.worlds;
            case ProxyServer.PLAYER   -> this.player;
            case ProxyServer.PLAYERS  -> this.players;
            case ProxyServer.GAMERULE -> this.gamerule;
            case ProxyServer.REGISTRY -> this.registry;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyServer.KEYS.length)
                    return ProxyServer.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyServer.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyServer.KEYS) {
            if (candidate.equals(key))
                return true;
        }
        return false;
    }

    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }

    public @NotNull MinecraftServer unwrap() {
        return this.self;
    }
}
