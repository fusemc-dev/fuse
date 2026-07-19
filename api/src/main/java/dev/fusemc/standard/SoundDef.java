package dev.fusemc.standard;

import dev.fusemc.ValueOps;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// A definition of a sound occurrence.
///
/// ---
/// An **entity definition** groups together the information needed to spawn an entity.
/// It is intended to be constructed from **JS**, to reduce the arity of a function.
///
/// A `SoundDef` is parsed with the following [Template]:
///
/// ```typescript
/// type SoundDef = {
///     type: Registered<"minecraft:sound_event">,
///     category: "master" | ... | "ambient",
///     playback?: {
///         volume?: number,
///         pitch?: number
///     }
/// };
/// ```
///
/// @since 0.1.0
@Documented("SoundDefinition")
public record SoundDef(@NotNull SoundEvent event,
                       @NotNull SoundSource category,
                       @NotNull Playback playback) {

    public static final @NotNull Template<SoundDef> TEMPLATE = Template.record(
            ValueOps.registered(BuiltInRegistries.SOUND_EVENT)
                    .map(Holder::value, Holder::direct)
                    .property("type", sound -> sound.event),
            Template.enumerate(SoundSource.class, SoundSource::getName)
                    .property("category", sound -> sound.category),
            Playback.TEMPLATE.<SoundDef>property("playback", sound -> sound.playback)
                    .optional(() -> Playback.DEFAULT),
            SoundDef::new
    );

    public SoundDef(@NotNull SoundEvent event,
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
