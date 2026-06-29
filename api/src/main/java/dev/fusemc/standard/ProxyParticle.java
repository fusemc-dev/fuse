package dev.fusemc.standard;

import com.manchickas.optionated.Option;
import dev.fusemc.ValueOps;
import dev.fusemc.standard.math.ProxyVec3;
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

public final class ProxyParticle<T extends ParticleOptions> {

    private static final @NotNull Template<ParticleType<?>> TYPE = ValueOps.holder(BuiltInRegistries.PARTICLE_TYPE).map(Holder::value, Holder::direct);
    public static final @NotNull Template<ProxyParticle<?>> TEMPLATE = Template.dispatch(
            ProxyParticle.TYPE.property("type", particle -> particle.type),
            (type) -> {
                if (type instanceof SimpleParticleType options)
                    return Option.some(Template.record(
                            ProxyVec3.TEMPLATE.<ProxyParticle<SimpleParticleType>>property("offset", particle -> particle.offset)
                                    .optional(() -> ProxyVec3.ZERO),
                            Template.DOUBLE.<ProxyParticle<SimpleParticleType>>property("speed", particle -> particle.speed)
                                    .optional(() -> 0.0d),
                            Template.INTEGER.<ProxyParticle<SimpleParticleType>>property("count", particle -> particle.count)
                                    .optional(() -> 1),
                            (offset, speed, count) -> new ProxyParticle<>(options, options, offset, speed, count)
                    ));
                return Option.some(ProxyParticle.template(type));
            }
    );

    public final @NotNull ParticleType<T> type;
    public final @NotNull T options;
    public final @NotNull ProxyVec3 offset;
    public final double speed;
    public final int count;

    public ProxyParticle(@NotNull ParticleType<T> type,
                         @NotNull T options,
                         @NotNull ProxyVec3 offset,
                         double speed,
                         int count) {
        this.type    = Objects.requireNonNull(type);
        this.options = Objects.requireNonNull(options);
        this.offset  = Objects.requireNonNull(offset);
        this.speed   = speed;
        this.count   = count;
    }

    private static <T extends ParticleOptions> @NotNull Record<ProxyParticle<T>> template(@NotNull ParticleType<T> type) {
        Objects.requireNonNull(type);
        return Template.record(
                ValueOps.delegate(type.codec().codec(), Description.ELLIPSIS)
                        .property("options", particle -> particle.options),
                ProxyVec3.TEMPLATE.<ProxyParticle<T>>property("offset", particle -> particle.offset)
                        .optional(() -> ProxyVec3.ZERO),
                Template.DOUBLE.<ProxyParticle<T>>property("speed", particle -> particle.speed)
                        .optional(() -> 0.0d),
                Template.INTEGER.<ProxyParticle<T>>property("count", particle -> particle.count)
                        .optional(() -> 1),
                (options, offset, speed, count) -> new ProxyParticle<>(type, options, offset, speed, count)
        );
    }
}
