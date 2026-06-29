package dev.fusemc.standard.server;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.manchickas.optionated.Either;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.ProxyParticle;
import dev.fusemc.standard.ProxySound;
import dev.fusemc.standard.block.ProxyBlock;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Predicate;

/*
    const entity = world.spawn("minecraft:zombie", [0.0, 0.0, 0.0]);
*/
public final class ProxyWorld implements ProxyObject {

    private static final @NotNull String TYPE  = "type";
    private static final @NotNull String SERVER = "server";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String BLOCK = "block";
    private static final @NotNull String SELECT = "select";
    private static final @NotNull String PARTICLE = "particle";
    private static final @NotNull String PLAY_SOUND = "playSound";
    private static final @NotNull String SPAWN = "spawn";
    private static final @NotNull String[] KEYS = {
            ProxyWorld.TYPE,
            ProxyWorld.SERVER,
            ProxyWorld.IS_OF,
            ProxyWorld.IS_IN,
            ProxyWorld.BLOCK,
            ProxyWorld.SELECT,
            ProxyWorld.PARTICLE,
            ProxyWorld.PLAY_SOUND,
            ProxyWorld.SPAWN,
    };

    private static final Interner<ProxyWorld> INTERNER
            = Interners.newWeakInterner();

    private final @NotNull ServerLevel self;
    private final @NotNull ProxyServer server;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable block;
    private final @NotNull ProxyExecutable select;
    private final @NotNull ProxyExecutable particle;
    private final @NotNull ProxyExecutable playSound;
    private final @NotNull ProxyExecutable spawn;

    public ProxyWorld(@NonNull ServerLevel self) {
        this.self   = Objects.requireNonNull(self);
        this.server = ProxyServer.from(self.getServer());
        this.type = ProxyIdentifier.from(self.dimension().identifier());
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var dimension  = this.self.registryAccess()
                        .lookupOrThrow(Registries.DIMENSION_TYPE)
                        .getValue(identifier);
                if (dimension != null)
                    return this.self.dimensionType() == dimension;
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var key        = TagKey.create(Registries.DIMENSION_TYPE, identifier);
                return this.self.dimensionTypeRegistration().is(key);
            }
            throw new UnsupportedOperationException();
        };
        this.block = (args) -> {
            if (args.length == 1) {
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[0]).toBlockPos();
                var state    = this.self.getBlockState(position);
                return ProxyBlock.from(state);
            }
            if (args.length == 2) {
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[0]).toBlockPos();
                var updater  = Tau.lower(Template.sequence(Updater.TEMPLATE, ProxyBlock.TEMPLATE), args[1]);
                return switch (updater) {
                    case Either.Left(var wrapped) -> this.self.setBlockAndUpdate(position, wrapped.update(ProxyBlock.from(this.self.getBlockState(position))).unwrap());
                    case Either.Right(var wrapped) -> this.self.setBlockAndUpdate(position, wrapped.unwrap());
                };
            }
            throw new UnsupportedOperationException();
        };
        this.playSound = (args) -> {
            if (args.length == 2) {
                var sound    = Tau.lower(ProxySound.TEMPLATE, args[0]);
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                var playback = sound.playback();
                this.self.playSound(null, position.x, position.y, position.z, sound.event(), sound.category(), playback.volume(), playback.pitch());
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.select = (args) -> {
            if (args.length == 1) {
                var selector = Tau.lower(Selector.TEMPLATE, args[0]);
                var entities = this.self.getEntities(EntityTypeTest.forClass(Entity.class), selector);
                return entities.stream()
                        .map(ProxyEntity::from)
                        .toArray(ProxyEntity[]::new);
            }
            throw new UnsupportedOperationException();
        };
        this.particle = (args) -> {
            if (args.length == 2) {
                var particle = Tau.lower(ProxyParticle.TEMPLATE, args[0]);
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                this.self.sendParticles(
                        particle.options,
                        position.x,
                        position.y,
                        position.z,
                        particle.count,
                        particle.offset.x,
                        particle.offset.y,
                        particle.offset.z,
                        particle.speed
                );
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.spawn = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var position   = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                var option     = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier);
                if (option.isPresent()) {
                    var type = option.get();
                    var entity = type.create(this.self, EntitySpawnReason.COMMAND);
                    if (entity != null) {
                        entity.snapTo(position.toVec3());
                        this.self.addFreshEntity(entity);
                        return ProxyEntity.from(entity);
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
    }

    public static ProxyWorld from(@NotNull ServerLevel world) {
        Objects.requireNonNull(world);
        return INTERNER.intern(new ProxyWorld(world));
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyWorld.TYPE     -> this.type;
            case ProxyWorld.SERVER   -> this.server;
            case ProxyWorld.IS_OF    -> this.isOf;
            case ProxyWorld.IS_IN    -> this.isIn;
            case ProxyWorld.BLOCK    -> this.block;
            case ProxyWorld.SELECT   -> this.select;
            case ProxyWorld.PARTICLE -> this.particle;
            case ProxyWorld.PLAY_SOUND -> this.playSound;
            case ProxyWorld.SPAWN    -> this.spawn;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public Object get(long index) {
                if (index >= 0 && index < ProxyWorld.KEYS.length)
                    return ProxyWorld.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyWorld.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyWorld.KEYS) {
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

    interface Selector extends Predicate<Entity> {

        Template<Selector> TEMPLATE = Template.functional(Selector.class, Template.BOOLEAN);

        boolean matches(@NotNull ProxyEntity<?> candidate);

        @Override
        default boolean test(@NotNull Entity entity) {
            Objects.requireNonNull(entity);
            return this.matches(ProxyEntity.from(entity));
        }
    }

    interface Updater {

        @NotNull Template<Updater> TEMPLATE = Template.functional(Updater.class, ProxyBlock.TEMPLATE);

        @NotNull ProxyBlock update(@NotNull ProxyBlock block);
    }
}
