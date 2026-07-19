package dev.fusemc.standard.entity.living;

import dev.fusemc.ValueOps;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.SoundDef;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

@Documented("Player")
public final class ProxyPlayer extends ProxyLiving<ServerPlayer> {

    private static final @NotNull VarHandle SERVER;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(ServerPlayer.class, MethodHandles.lookup());
            SERVER = lookup.findVarHandle(ServerPlayer.class, "server", MinecraftServer.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final @NotNull String TITLE        = "title";
    private static final @NotNull String SUBTITLE     = "subtitle";
    private static final @NotNull String ACTIONBAR    = "actionbar";
    private static final @NotNull String GRANT        = "grantAdvancement";
    private static final @NotNull String REVOKE       = "revokeAdvancement";
    private static final @NotNull String SEND_MESSAGE = "sendMessage";
    private static final @NotNull String PLAY_SOUND   = "playSound";
    private static final @NotNull String OFFER        = "offer";
    private static final @NotNull String DROP         = "drop";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyPlayer.TITLE,
            ProxyPlayer.SUBTITLE,
            ProxyPlayer.ACTIONBAR,
            ProxyPlayer.GRANT,
            ProxyPlayer.REVOKE,
            ProxyPlayer.SEND_MESSAGE,
            ProxyPlayer.PLAY_SOUND,
            ProxyPlayer.OFFER,
            ProxyPlayer.DROP,
    };

    private final @NotNull ProxyExecutable title;
    private final @NotNull ProxyExecutable subtitle;
    private final @NotNull ProxyExecutable actionbar;
    private final @NotNull ProxyExecutable grantAdvancement;
    private final @NotNull ProxyExecutable revokeAdvancement;
    private final @NotNull ProxyExecutable sendMessage;
    private final @NotNull ProxyExecutable playSound;
    private final @NotNull ProxyExecutable offer;
    private final @NotNull ProxyExecutable drop;

    @SuppressWarnings("JavaLangInvokeHandleSignature")
    private ProxyPlayer(@NonNull ServerPlayer self) {
        super(Objects.requireNonNull(self));
        this.title = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                var packet    = new ClientboundSetTitleTextPacket(component);
                this.self.connection.send(packet);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.subtitle = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                var packet    = new ClientboundSetSubtitleTextPacket(component);
                this.self.connection.send(packet);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.actionbar = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                var packet    = new ClientboundSetActionBarTextPacket(component);
                this.self.connection.send(packet);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.grantAdvancement = (args) -> {
            if (args.length == 1 || args.length == 2) {
                var identifier  = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var advancement = ((MinecraftServer) ProxyPlayer.SERVER.get(this.self))
                        .getAdvancements()
                        .get(identifier);
                if (advancement != null) {
                    var advancements = this.self.getAdvancements();
                    var progress     = advancements.getOrStartProgress(advancement);
                    if (args.length == 2) {
                        var criterion = Tau.lower(Template.STRING, args[1]);
                        advancements.award(advancement, criterion);
                        return true;
                    }
                    for (var criterion : progress.getRemainingCriteria())
                        advancements.award(advancement, criterion);
                    return true;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.revokeAdvancement = (args) -> {
            if (args.length == 1 || args.length == 2) {
                var identifier  = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var advancement = ((MinecraftServer) ProxyPlayer.SERVER.get(this.self))
                        .getAdvancements()
                        .get(identifier);
                if (advancement != null) {
                    var advancements = this.self.getAdvancements();
                    var progress     = advancements.getOrStartProgress(advancement);
                    if (args.length == 2) {
                        var criterion = Tau.lower(Template.STRING, args[1]);
                        progress.revokeProgress(criterion);
                        return true;
                    }
                    for (var criterion : progress.getRemainingCriteria())
                        progress.revokeProgress(criterion);
                    return true;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.sendMessage = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                this.self.sendSystemMessage(component, false);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.playSound = (args) -> {
            if (args.length == 2) {
                var sound    = Tau.lower(SoundDef.TEMPLATE, args[0]);
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                var playback = sound.playback();
                this.self.connection.send(new ClientboundSoundPacket(
                        Holder.direct(sound.event()),
                        sound.category(),
                        position.x,
                        position.y,
                        position.z,
                        playback.volume(),
                        playback.pitch(),
                        this.self.getRandom().nextLong()
                ));
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.offer = (args) -> {
            if (args.length == 1) {
                var stack = Tau.lower(ProxyItem.TEMPLATE, args[0]);
                var inventory = this.self.getInventory();
                return inventory.add(stack.unwrap());
            }
            throw new UnsupportedOperationException();
        };
        this.drop = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key        = ResourceKey.create(Registries.LOOT_TABLE, identifier);
                var table      = ((MinecraftServer) ProxyPlayer.SERVER.get(this.self))
                        .reloadableRegistries()
                        .getLootTable(key);
                var entries  = table.getRandomItems(new LootParams.Builder(this.self.level())
                        .withParameter(LootContextParams.ORIGIN, this.self.position())
                        .create(LootContextParamSets.COMMAND));
                var inventory  = this.self.getInventory();
                var successful = 0;
                for (var entry : entries) {
                    if (inventory.add(entry))
                        successful++;
                }
                return successful;
            }
            throw new UnsupportedOperationException();
        };
    }

    public static @NotNull ProxyPlayer from(@NotNull ServerPlayer self) {
        Objects.requireNonNull(self);
        return new ProxyPlayer(self);
    }

    public static @NotNull ServerPlayer to(@NotNull ProxyPlayer player) {
        Objects.requireNonNull(player);
        return player.self;
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyPlayer.TITLE        -> this.title;
            case ProxyPlayer.SUBTITLE     -> this.subtitle;
            case ProxyPlayer.ACTIONBAR    -> this.actionbar;
            case ProxyPlayer.SEND_MESSAGE -> this.sendMessage;
            case ProxyPlayer.GRANT        -> this.grantAdvancement;
            case ProxyPlayer.REVOKE       -> this.revokeAdvancement;
            case ProxyPlayer.PLAY_SOUND   -> this.playSound;
            case ProxyPlayer.OFFER        -> this.offer;
            case ProxyPlayer.DROP -> this.drop;
            default -> super.getMember(key);
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyPlayer.KEYS) {
            if (candidate.equals(key))
                return true;
        }
        return super.hasMember(key);
    }

    @Override
    public ProxyArray getMemberKeys() {
        var parent = super.getMemberKeys();
        return new ProxyArray() {

            @Override
            public Object get(long index) {
                var length = ProxyPlayer.KEYS.length;
                if (index >= 0 && index < length)
                    return ProxyPlayer.KEYS[(int) index];
                return parent.get(index - length);
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyPlayer.KEYS.length + parent.getSize();
            }
        };
    }
}
