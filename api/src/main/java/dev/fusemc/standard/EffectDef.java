package dev.fusemc.standard;

import dev.fusemc.ValueOps;
import dev.fusemc.tau.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

/// A definition of an effect.
///
/// ---
/// An **effect definition** groups together the information needed to produce a [MobEffectInstance].
/// It is intended to be constructed from **JS**, to reduce the arity of a function.
///
/// An `EffectDef` is parsed with the following [Template]:
///
/// ```typescript
/// type EffectDef = {
///     type: Registered<"minecraft:mob_effect">;
///     duration: integer;
///     amplifier?: integer;
///     display?: {
///         ambient?: boolean;
///         particle?: boolean;
///         icon?: boolean;
///     };
/// }
/// ```
///
/// @since 0.1.0
public record EffectDef(@NotNull Holder<MobEffect> type,
                        @NotNull Display display,
                        int amplifier,
                        int duration) {

    public static final @NotNull Template<EffectDef> TEMPLATE = Template.record(
            ValueOps.registered(BuiltInRegistries.MOB_EFFECT)
                    .property("type", EffectDef::type),
            Display.TEMPLATE
                    .property("display", EffectDef::display)
                    .optional(() -> Display.DEFAULT),
            Template.INTEGER.property("amplifier", EffectDef::amplifier)
                    .optional(() -> 0),
            Template.INTEGER.property("duration", EffectDef::duration),
            EffectDef::new
    );

    /// Create a [MobEffectInstance] according to this definition.
    ///
    /// ---
    /// @since 0.1.0
    public @NotNull MobEffectInstance create() {
        return new MobEffectInstance(this.type, this.duration, this.amplifier, this.display.ambient, this.display.particle, this.display.icon);
    }

    public record Display(boolean ambient, boolean icon, boolean particle) {

        private static final @NotNull Display DEFAULT = new Display(false, true, true);

        public static final @NotNull Template<Display> TEMPLATE = Template.record(
                Template.BOOLEAN
                        .property("ambient", Display::ambient)
                        .optional(() -> false),
                Template.BOOLEAN
                        .property("icon", Display::icon)
                        .optional(() -> true),
                Template.BOOLEAN
                        .property("particle", Display::particle)
                        .optional(() -> true),
                Display::new
        );
    }

}
