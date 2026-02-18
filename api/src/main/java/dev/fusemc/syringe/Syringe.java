package dev.fusemc.syringe;

import com.manchickas.jet.Jet;
import com.manchickas.optionated.Option;
import com.mojang.serialization.Lifecycle;
import dev.fusemc.standard.entity.living.ScriptPlayer;
import dev.fusemc.standard.util.ScriptVector;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

public abstract class Syringe {

    public static final @NotNull Registry<Vaccine<?, ?>> REGISTRY = new MappedRegistry<>(
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath("fuse", "vaccine")
            ),
            Lifecycle.stable()
    );
    private static final @NotNull VarHandle VISUAL_FIRE_HANDLE = Syringe.lookupHandle(Entity.class, "hasVisualFire", boolean.class);

    // Entity
    public static final @NotNull Vaccine<Entity, Integer> AIR = Syringe.register(
            "air", Entity.class,
            builder -> builder
                    .onSample(Entity::getAirSupply)
                    .onInject(Entity::setAirSupply)
                    .withTemplate(Jet.INTEGER)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Component> NAME = Syringe.register(
            "name", Entity.class,
            builder -> builder
                    .onSample(Entity::getName)
                    .onInject(Entity::setCustomName)
                    .withTemplate(ScriptPlayer.TEXT)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> NAME_VISIBLE = Syringe.register(
            "name_visible", Entity.class,
            builder -> builder
                    .onSample(Entity::isCustomNameVisible)
                    .onInject(Entity::setCustomNameVisible)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Double> FALL_DISTANCE = Syringe.register(
            "fall_distance", Entity.class,
            builder -> builder
                    .onSample(entity -> entity.fallDistance)
                    .onInject((entity, distance) -> entity.fallDistance = distance)
                    .withTemplate(Jet.DOUBLE)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Integer> FIRE_TICKS = Syringe.register(
            "fire", Entity.class,
            builder -> builder
                    .onSample(Entity::getRemainingFireTicks)
                    .onInject(Entity::setRemainingFireTicks)
                    .withTemplate(Jet.INTEGER)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> GLOWING = Syringe.register(
            "glowing", Entity.class,
            builder -> builder
                    .onSample(Entity::hasGlowingTag)
                    .onInject(Entity::setGlowingTag)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> HAS_VISUAL_FIRE = Syringe.register(
            "has_visual_fire", Entity.class,
            builder -> builder
                    .onSample(entity -> (boolean) Syringe.VISUAL_FIRE_HANDLE.get(entity))
                    .onInject(Syringe.VISUAL_FIRE_HANDLE::set)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> INVULNERABLE = Syringe.register(
            "invulnerable", Entity.class,
            builder -> builder
                    .onSample(Entity::isInvulnerable)
                    .onInject(Entity::setInvulnerable)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, ScriptVector> VELOCITY = Syringe.register(
            "velocity", Entity.class,
            builder -> builder
                    .onSample(entity -> ScriptVector.fromVec3(entity.getDeltaMovement()))
                    .onInject((entity, vector) -> entity.setDeltaMovement(vector.toVec3()))
                    .withTemplate(ScriptVector.TEMPLATE)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> NO_GRAVITY = Syringe.register(
            "gravity", Entity.class,
            builder -> builder
                    .onSample((entity) -> !entity.isNoGravity())
                    .onInject((entity, flag) -> entity.setNoGravity(!flag))
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> ON_GROUND = Syringe.register(
            "on_ground", Entity.class,
            builder -> builder
                    .onSample(Entity::onGround)
                    .onInject(Entity::setOnGround)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Integer> PORTAL_COOLDOWN = Syringe.register(
            "portal_cooldown", Entity.class,
            builder -> builder
                    .onSample(Entity::getPortalCooldown)
                    .onInject(Entity::setPortalCooldown)
                    .withTemplate(Jet.INTEGER)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, ScriptVector> POSITION = Syringe.register(
            "position", Entity.class,
            builder -> builder
                    .onSample(entity -> ScriptVector.fromVec3(entity.position()))
                    .onInject((entity, vector) -> entity.setPos(vector.toVec3()))
                    .withTemplate(ScriptVector.TEMPLATE)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Float[]> ROTATION = Syringe.register(
            "rotation", Entity.class,
            builder -> builder
                    .onSample(entity -> new Float[]{
                            entity.getXRot(),
                            entity.getYRot()
                    })
                    .onInject((entity, rotation) -> {
                        entity.setXRot(rotation[0]);
                        entity.setYRot(rotation[1]);
                    })
                    .withTemplate(Jet.tuple(
                            Jet.FLOAT.element(tuple -> tuple[0]),
                            Jet.FLOAT.element(tuple -> tuple[1]),
                            (yaw, pitch) -> new Float[]{ yaw, pitch }
                    ))
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Boolean> SILENT = Syringe.register(
            "silent", Entity.class,
            builder -> builder
                    .onSample(Entity::isSilent)
                    .onInject(Entity::setSilent)
                    .withTemplate(Jet.BOOLEAN)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, String[]> TAGS = Syringe.register(
            "tags", Entity.class,
            builder -> builder
                    .onSample((entity) -> entity.getTags()
                            .toArray(String[]::new))
                    .onInject((entity, tags) -> {
                        var present = entity.getTags();
                        present.clear();
                        Collections.addAll(present, tags);
                    })
                    .withTemplate(Jet.STRING.array(String[]::new))
                    .build()
    );
    public static final @NotNull Vaccine<Entity, Integer> TICKS_FROZEN = Syringe.register(
            "ticks_frozen", Entity.class,
            builder -> builder
                    .onSample(Entity::getTicksFrozen)
                    .onInject(Entity::setTicksFrozen)
                    .withTemplate(Jet.INTEGER)
                    .build()
    );
    public static final @NotNull Vaccine<Entity, String> UUID = Syringe.register(
            "uuid", Entity.class,
            builder -> builder
                    .onSample(Entity::getStringUUID)
                    .onInject((entity, uuid) -> entity.setUUID(java.util.UUID.fromString(uuid)))
                    .withTemplate(Jet.STRING)
                    .build()
    );

    // LivingEntity
    public static final @NotNull Vaccine<LivingEntity, Float> ABSORPTION = Syringe.register(
            "absorption", LivingEntity.class,
            builder -> builder
                    .onSample(LivingEntity::getAbsorptionAmount)
                    .onInject(LivingEntity::setAbsorptionAmount)
                    .withTemplate(Jet.FLOAT)
                    .build()
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(Syringe.class);

    private Syringe() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Option<Vaccine<?, ?>> attemptLookup(@NotNull Identifier name) {
        return Option.fromNullable(REGISTRY.getValue(name));
    }

    private static @NotNull <E extends Entity, T> Vaccine<E, T> register(@NotNull String identifier,
                                                                         @NotNull Class<E> type,
                                                                         @NotNull Function<Vaccine.Builder<E, T>, Vaccine<E, T>> builder) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(type);
        Objects.requireNonNull(builder);
        return Registry.register(
                Syringe.REGISTRY,
                Identifier.withDefaultNamespace(identifier),
                builder.apply(Vaccine.builder(type))
        );
    }

    private static @NotNull VarHandle lookupHandle(@NotNull Class<? extends Entity> clazz,
                                                   @NotNull String name,
                                                   @NotNull Class<?> type) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        try {
            var lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            return lookup.findVarHandle(
                    clazz, name, type
            );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
