package dev.fusemc.standard.entity;

import dev.fusemc.ValueOps;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/// A definition of an entity.
///
/// ---
/// An **entity definition** groups together the information needed to spawn an entity.
/// It is intended to be constructed from **JS**, to reduce the arity of a function.
///
/// An `EntityDef` is parsed with the following [Template]:
///
/// ```typescript
/// type EntityDef = {
///     type: Registered<"minecraft:entity_type">,
///     payload?: {
///         [_: string]: any;
///     }
/// } | Registered<"minecraft:entity_type">
/// ```
///
/// @since 0.1.0
@Documented("EntityDefinition")
public record EntityDef<T extends Entity>(@NotNull EntityType<T> type,
                                          @NotNull CompoundTag payload) {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(EntityDef.class);
    private static final @NotNull Template<EntityType<?>> TYPE = ValueOps.registered(BuiltInRegistries.ENTITY_TYPE)
            .map(Holder::value, Holder::direct);

    public static final @NotNull Template<EntityDef<?>> TEMPLATE = Template.union(
            Template.record(
                    EntityDef.TYPE.property("type", EntityDef::type),
                    ValueOps.delegate(CompoundTag.CODEC, Description.concat(Description.delimiter('{'), Description.ELLIPSIS, Description.delimiter('}')))
                            .<EntityDef<?>>property("payload", EntityDef::payload)
                            .optional(CompoundTag::new),
                    EntityDef::new
            ),
            EntityDef.TYPE.map((type) -> new EntityDef<>(type, new CompoundTag()), EntityDef::type)
    );

    public EntityDef(@NotNull EntityType<T> type,
                     @NotNull CompoundTag payload) {
        this.type = Objects.requireNonNull(type);
        this.payload = Objects.requireNonNull(payload);
    }

    /// Create an entity according to the definition.
    ///
    /// ---
    /// Creates an entity in the provided [ServerLevel] with and initiates
    /// it with the definition's payload.
    ///
    /// @since 0.1.0
    public @Nullable T create(@NotNull ServerLevel level) {
        var entity = this.type.create(level, EntitySpawnReason.COMMAND);
        if (entity != null) {
            try (var collector = new ProblemReporter.ScopedCollector(entity.problemPath(), EntityDef.LOGGER)) {
                entity.load(TagValueInput.create(collector, entity.registryAccess(), this.payload));
                return entity;
            }
        }
        return null;
    }
}
