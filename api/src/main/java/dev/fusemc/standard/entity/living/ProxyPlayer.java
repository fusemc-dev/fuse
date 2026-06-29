package dev.fusemc.standard.entity.living;

import dev.fusemc.ValueOps;
import dev.fusemc.standard.ProxySound;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

@Documented("Player")
public final class ProxyPlayer extends ProxyEntity<ServerPlayer> {

    private static final @NotNull String MESSAGE = "sendMessage";
    private static final @NotNull String PLAY_SOUND = "playSound";
    private static final @NotNull String @NotNull[] KEYS = {
            ProxyPlayer.MESSAGE,
            ProxyPlayer.PLAY_SOUND,
    };

    private final @NotNull ProxyExecutable sendMessage;
    private final @NotNull ProxyExecutable playSound;

    private ProxyPlayer(@NonNull ServerPlayer self) {
        super(Objects.requireNonNull(self));
        this.sendMessage = (args) -> {
            if (args.length == 1) {
                var component = Tau.lower(ValueOps.COMPONENT, args[0]);
                this.self.sendSystemMessage(component, false);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.playSound = (args) -> {
            if (args.length == 3) {
                var sound   = Tau.lower(ProxySound.TEMPLATE, args[0]);
                var playback = sound.playback();
                this.self.playSound(sound.event(), playback.volume(), playback.pitch());
                return Tau.undefined();
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
            case ProxyPlayer.MESSAGE    -> this.sendMessage;
            case ProxyPlayer.PLAY_SOUND -> this.playSound;
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
