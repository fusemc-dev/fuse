package dev.fusemc.standard;

import com.manchickas.optionated.Option;
import dev.fusemc.ValueOps;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.template.dictionary.record.Record;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// A definition of a particle.
///
/// ---
/// A **particle definition** groups together the information needed to produce a particle.
/// It is intended to be constructed from **JS**, to reduce the arity of a function.
///
/// A `ParticleDef` is parsed with the following [Template]:
///
/// ```typescript
/// type ParticleDef = {
///     type: Registered<"minecraft:particle_type">,
///     options?: {...},
///     display?: {
///         offset?: Vec3 | [number, number, number],
///         count?: integer,
///         speed?: number
///     }
/// }
/// ```
///
/// @since 0.1.0
@Documented("ParticleDefinition")
public record ParticleDef<T extends ParticleOptions>(@NotNull ParticleType<T> type,
                                                     @NotNull T options,
                                                     @NotNull Display display) {

    private static final @NotNull Template<ParticleType<?>> TYPE = ValueOps.registered(BuiltInRegistries.PARTICLE_TYPE).map(Holder::value, Holder::direct);
    public static final @NotNull Template<ParticleDef<?>> TEMPLATE = Template.dispatch(
            ParticleDef.TYPE.property("type", ParticleDef::type),
            (type) -> {
                if (type instanceof SimpleParticleType options)
                    return Option.some(Template.record(
                            Display.TEMPLATE
                                    .<ParticleDef<?>>property("display", ParticleDef::display)
                                    .optional(() -> Display.DEFAULT),
                            (display) -> new ParticleDef<>(options, options, display)
                    ));
                return Option.some(ParticleDef.template(type));
            }
    );

    public ParticleDef(@NotNull ParticleType<T> type,
                       @NotNull T options,
                       @NotNull Display display) {
        this.type    = Objects.requireNonNull(type);
        this.options = Objects.requireNonNull(options);
        this.display = Objects.requireNonNull(display);
    }

    private static <T extends ParticleOptions> @NotNull Record<ParticleDef<T>> template(@NotNull ParticleType<T> type) {
        Objects.requireNonNull(type);
        return Template.record(
                ValueOps.delegate(type.codec().codec(), Description.ELLIPSIS)
                        .property("options", ParticleDef::options),
                Display.TEMPLATE
                        .<ParticleDef<T>>property("display", ParticleDef::display)
                        .optional(() -> Display.DEFAULT),
                (options, display) -> new ParticleDef<>(type, options, display)
        );
    }

    public record Display(@NotNull ProxyVec3 offset, double speed, int count) {

        private static final Display DEFAULT = new Display(ProxyVec3.ZERO, 0.0d, 1);

        public static final @NotNull Template<Display> TEMPLATE = Template.record(
                ProxyVec3.TEMPLATE
                        .property("offset", Display::offset)
                        .optional(() -> ProxyVec3.ZERO),
                Template.DOUBLE
                        .property("speed", Display::speed)
                        .optional(() -> 0.0d),
                Template.INTEGER
                        .property("count", Display::count)
                        .optional(() -> 1),
                Display::new
        );
    }
}
