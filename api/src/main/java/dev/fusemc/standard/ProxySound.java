package dev.fusemc.standard;

import dev.fusemc.ValueOps;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// player.playSound({
///     type: "item.flint_and_steel.use",
///     category: "player",
///     playback: {
///         volume: 5,
///         pitch: 2
///     }
/// });
public record ProxySound(@NotNull SoundEvent event,
                         @NotNull SoundSource category,
                         @NotNull Playback playback) {

    public static final @NotNull Template<ProxySound> TEMPLATE = Template.record(
            ValueOps.holder(BuiltInRegistries.SOUND_EVENT)
                    .map(Holder::value, Holder::direct)
                    .property("type", sound -> sound.event),
            Template.enumerate(SoundSource.class, SoundSource::getName)
                    .property("category", sound -> sound.category),
            Playback.TEMPLATE.<ProxySound>property("playback", sound -> sound.playback)
                    .optional(() -> Playback.DEFAULT),
            ProxySound::new
    );

    public ProxySound(@NotNull SoundEvent event,
                      @NotNull SoundSource category,
                      @NotNull Playback playback) {
        this.event    = Objects.requireNonNull(event);
        this.category = Objects.requireNonNull(category);
        this.playback = Objects.requireNonNull(playback);
    }

    public record Playback(float volume, float pitch) {

        private static final @NotNull Playback DEFAULT = new Playback(1.0f, 1.0f);

        public static final @NotNull Template<Playback> TEMPLATE = Template.record(
                Template.FLOAT.property("volume", Playback::volume)
                        .optional(() -> 1.0f),
                Template.FLOAT.property("pitch", Playback::pitch)
                        .optional(() -> 1.0f),
                Playback::new
        );
    }
}
