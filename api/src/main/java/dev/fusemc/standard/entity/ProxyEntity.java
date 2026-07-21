package dev.fusemc.standard.entity;

import com.manchickas.optionated.Option;
import com.mojang.brigadier.context.CommandContext;
import dev.fusemc.ValueOps;
import dev.fusemc.pql.PQL;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.entity.living.ProxyLiving;
import dev.fusemc.standard.entity.living.ProxyPlayer;
import dev.fusemc.standard.item.ProxyItem;
import dev.fusemc.standard.math.ProxyVec2;
import dev.fusemc.standard.math.ProxyVec3;
import dev.fusemc.standard.server.ProxyWorld;
import dev.fusemc.syringe.Syringe;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/// An entity in a world.
///
/// ---
/// `ProxyEntity` represents an entity in a [ProxyWorld]. It is intended
/// as a way to expose {@link Entity Entities} to scripts.
///
/// A `ProxyEntity` is constructed through an [EntityDef].
///
/// @since 0.1.0
/// @see EntityDef
@Documented("Entity")
public class ProxyEntity<T extends Entity> implements ProxyObject {

    public static final @NotNull Template<ProxyEntity<?>> TEMPLATE = Template.reference(ProxyEntity.class)
            .map(entity -> (ProxyEntity<?>) entity, entity -> entity);

    private static final @NotNull String TYPE = "type";
    private static final @NotNull String IS_OF = "isOf";
    private static final @NotNull String IS_IN = "isIn";
    private static final @NotNull String INJECT = "inject";
    private static final @NotNull String SAMPLE = "sample";
    private static final @NotNull String GET = "get";
    private static final @NotNull String SET = "set";
    private static final @NotNull String WORLD = "world";
    private static final @NotNull String SLOT = "slot";
    private static final @NotNull String INSERT = "insert";
    private static final @NotNull String HAS_TAG = "hasTag";
    private static final @NotNull String APPEND_TAG = "appendTag";
    private static final @NotNull String REMOVE_TAG = "removeTag";
    private static final @NotNull String POSITION = "position";
    private static final @NotNull String IS_SNEAKING = "isSneaking";
    private static final @NotNull String MATCHES = "matches";
    private static final @NotNull String DAMAGE = "damage";
    private static final @NotNull String EXECUTE = "execute";
    private static final @NotNull String FUNCTION = "function";
    private static final @NotNull String REMOVE = "remove";
    private static final @NotNull String KILL = "kill";
    private static final @NotNull String TELEPORT = "teleport";
    private static final @NotNull String TO_STRING = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyEntity.TYPE,
            ProxyEntity.IS_OF,
            ProxyEntity.IS_IN,
            ProxyEntity.INJECT,
            ProxyEntity.SAMPLE,
            ProxyEntity.GET,
            ProxyEntity.SET,
            ProxyEntity.WORLD,
            ProxyEntity.SLOT,
            ProxyEntity.INSERT,
            ProxyEntity.HAS_TAG,
            ProxyEntity.APPEND_TAG,
            ProxyEntity.REMOVE_TAG,
            ProxyEntity.POSITION,
            ProxyEntity.IS_SNEAKING,
            ProxyEntity.MATCHES,
            ProxyEntity.DAMAGE,
            ProxyEntity.EXECUTE,
            ProxyEntity.FUNCTION,
            ProxyEntity.REMOVE,
            ProxyEntity.KILL,
            ProxyEntity.TELEPORT,
            ProxyEntity.TO_STRING,
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyEntity.class);

    protected final @NotNull T self;
    private final @NotNull ProxyIdentifier type;
    private final @NotNull ProxyExecutable isOf;
    private final @NotNull ProxyExecutable isIn;
    private final @NotNull ProxyExecutable inject;
    private final @NotNull ProxyExecutable sample;
    private final @NotNull ProxyExecutable get;
    private final @NotNull ProxyExecutable set;
    private final @NotNull ProxyExecutable world;
    private final @NotNull ProxyExecutable slot;
    private final @NotNull ProxyExecutable insert;
    private final @NotNull ProxyExecutable hasTag;
    private final @NotNull ProxyExecutable appendTag;
    private final @NotNull ProxyExecutable removeTag;
    private final @NotNull ProxyExecutable position;
    private final @NotNull ProxyExecutable teleport;
    private final @NotNull ProxyExecutable matches;
    private final @NotNull ProxyExecutable isSneaking;
    private final @NotNull ProxyExecutable execute;
    private final @NotNull ProxyExecutable function;
    private final @NotNull ProxyExecutable damage;
    private final @NotNull ProxyExecutable remove;
    private final @NotNull ProxyExecutable kill;
    private final @NotNull ProxyExecutable toString;

    protected ProxyEntity(@NonNull T self) {
        this.self = Objects.requireNonNull(self);
        this.type = ProxyIdentifier.from(BuiltInRegistries.ENTITY_TYPE.getKey(this.self.getType()));
        this.isOf = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var type = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(this.self.getType());
                return type.is(identifier);
            }
            throw new UnsupportedOperationException();
        };
        this.isIn = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var type = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(this.self.getType());
                return type.is(TagKey.create(Registries.ENTITY_TYPE, identifier));
            }
            throw new UnsupportedOperationException();
        };
        this.inject = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var vaccine    = Syringe.REGISTRY.getValue(identifier);
                if (vaccine != null)
                    return vaccine.attemptInject(this.self, args[1]);
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.sample = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var vaccine    = Syringe.REGISTRY.getValue(identifier);
                if (vaccine != null) {
                    var option = vaccine.attemptSample(this.self);
                    if (option instanceof Option.Some<Value>(var result))
                        return result;
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.get = (args) -> {
            if (args.length == 1) {
                var path = PQL.parse(Tau.lower(Template.STRING, args[0]));
                try (var collector = new ProblemReporter.ScopedCollector(this.self.problemPath(), LOGGER)) {
                    var output = TagValueOutput.createWithContext(collector, this.self.registryAccess());
                    if (this.self.save(output))
                        return path.resolve(output.buildResult())
                                .map(tag -> NbtOps.INSTANCE.convertTo(ValueOps.INSTANCE, tag))
                                .unwrapOr(Tau.undefined());
                    return Tau.undefined();
                }
            }
            throw new UnsupportedOperationException();
        };
        this.set = (args) -> {
            if (args.length == 2) {
                var path = PQL.parse(Tau.lower(Template.STRING, args[0]));
                var value = ValueOps.INSTANCE.convertTo(NbtOps.INSTANCE, args[1]);
                try (var collector = new ProblemReporter.ScopedCollector(this.self.problemPath(), LOGGER)) {
                    var output = TagValueOutput.createWithContext(collector, this.self.registryAccess());
                    if (this.self.save(output)) {
                        var buffer = output.buildResult();
                        if (path.update(buffer, value) instanceof Option.Some(var wrapped)) {
                            assert wrapped != null;
                            var input = TagValueInput.create(collector, this.self.registryAccess(), wrapped);
                            this.self.load(input);
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
            }
            throw new UnsupportedOperationException();
        };
        this.world = (args) -> {
            if (args.length == 0)
                return ProxyWorld.from((ServerLevel) this.self.level());
            throw new UnsupportedOperationException();
        };
        this.slot = (args) -> {
            if (args.length == 1) {
                var name  = Tau.lower(Template.STRING, args[0]);
                var range = SlotRanges.nameToIds(name);
                if (range != null) {
                    var slots = range.slots();
                    if (slots.size() == 1) {
                        var access = this.self.getSlot(slots.getFirst());
                        if (access != null)
                            return ProxyItem.from(access.get());
                        return Tau.undefined();
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.insert = (args) -> {
            if (args.length == 2) {
                var name      = Tau.lower(Template.STRING, args[0]);
                var candidate = Tau.lower(ProxyItem.TEMPLATE, args[1]);
                var range     = SlotRanges.nameToIds(name);
                if (range != null) {
                    var slots = range.slots();
                    if (slots.size() == 1) {
                        var access = this.self.getSlot(slots.getFirst());
                        if (access != null)
                            access.set(candidate.unwrap());
                        return Tau.undefined();
                    }
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.hasTag = (args) -> {
            if (args.length == 1) {
                var tag = Tau.lower(Template.STRING, args[0]);
                return this.self.getTags().contains(tag);
            }
            throw new UnsupportedOperationException();
        };
        this.appendTag = (args) -> {
            if (args.length == 1) {
                var tag = Tau.lower(Template.STRING, args[0]);
                return this.self.addTag(tag);
            }
            throw new UnsupportedOperationException();
        };
        this.matches = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var level      = (ServerLevel) this.self.level();
                var option     = level.getServer()
                        .reloadableRegistries()
                        .lookup()
                        .lookupOrThrow(Registries.PREDICATE)
                        .get(ResourceKey.create(Registries.PREDICATE, identifier))
                        .map(Holder::value);
                if (option.isPresent()) {
                    var predicate = option.get();
                    var context = new LootContext.Builder(new LootParams.Builder(level)
                            .withOptionalParameter(LootContextParams.THIS_ENTITY, this.self)
                            .withParameter(LootContextParams.ORIGIN, this.self.position())
                            .create(LootContextParamSets.COMMAND))
                            .create(Optional.empty());
                    return predicate.test(context);
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.damage = (args) -> {
            if (args.length == 2) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var amount     = Tau.lower(Template.FLOAT, args[1]);
                var registry = this.self.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
                var option = registry.get(identifier);
                if (option.isPresent()) {
                    var type = option.get();
                    return this.self.hurtServer((ServerLevel) this.self.level(), new DamageSource(type), amount);
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
        this.removeTag = (args) -> {
            if (args.length == 1) {
                var tag = Tau.lower(Template.STRING, args[0]);
                return this.self.removeTag(tag);
            }
            throw new UnsupportedOperationException();
        };
        this.position = (args) -> {
            if (args.length == 0)
                return ProxyVec3.from(this.self.position());
            throw new UnsupportedOperationException();
        };
        this.teleport = (args) -> {
            if (args.length == 3) {
                var world = Tau.lower(Template.union(
                        ProxyIdentifier.MAPPED_TEMPLATE.flatMap(identifier -> {
                            var key    = ResourceKey.create(Registries.DIMENSION, identifier);
                            var level  = this.self.level();
                            var server = level.getServer();
                            if (server != null)
                                return Option.fromNullable(server.getLevel(key))
                                        .map(ProxyWorld::from);
                            return Option.none();
                        }, level -> Option.fromNullable(level)
                                .map(ProxyWorld::unwrap)
                                .map(Level::dimension)
                                .map(ResourceKey::identifier)),
                        Template.reference(ProxyWorld.class)
                ), args[0]);
                var position = Tau.lower(ProxyVec3.TEMPLATE, args[1]);
                var rotation = Tau.lower(ProxyVec2.TEMPLATE, args[2]);
                return this.self.teleportTo(
                        world.unwrap(),
                        position.x,
                        position.y,
                        position.z,
                        Set.of(),
                        (float) rotation.x,
                        (float) rotation.y,
                        true
                );
            }
            throw new UnsupportedOperationException();
        };
        this.execute = (args) -> {
            if (args.length == 1) {
                var command    = Tau.lower(Template.STRING, args[0]);
                var world      = (ServerLevel) this.self.level();
                var server     = world.getServer();
                var commands   = server.getCommands();
                var dispatcher = commands.getDispatcher();
                // The idea isn't that the command is run BY the entity, but
                // rather AS the entity. We thus do not use the command source
                // of a potential player and always use elevated permissions.
                var results    = dispatcher.parse(command, new CommandSourceStack(
                        CommandSource.NULL,
                        this.self.position(),
                        this.self.getRotationVector(),
                        world,
                        LevelBasedPermissionSet.GAMEMASTER,
                        this.self.getPlainTextName(),
                        this.self.getDisplayName(),
                        server,
                        this.self
                ));
                commands.performCommand(results, command);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.function = (args) -> {
            if (args.length == 1) {
                var identifier = Tau.lower(ProxyIdentifier.MAPPED_TEMPLATE, args[0]);
                var level      = (ServerLevel) this.self.level();
                var server     = level.getServer();
                var functions  = server.getFunctions();
                var option     = functions.get(identifier);
                if (option.isPresent()) {
                    var function = option.get();
                    functions.execute(function, functions.getGameLoopSender());
                    return Tau.undefined();
                }
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.isSneaking = (args) -> {
            if (args.length == 0)
                return this.self.isShiftKeyDown();
            throw new UnsupportedOperationException();
        };
        this.remove = (args) -> {
            if (args.length == 0) {
                this.self.remove(Entity.RemovalReason.DISCARDED);
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.kill = (args) -> {
            if (args.length == 0) {
                this.self.kill((ServerLevel) this.self.level());
                return Tau.undefined();
            }
            throw new UnsupportedOperationException();
        };
        this.toString = (args) -> {
            if (args.length == 0)
                return this.self.toString();
            throw new UnsupportedOperationException();
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @NotNull ProxyEntity<T> from(@NotNull T self) {
        Objects.requireNonNull(self);
        return switch (self) {
            case ServerPlayer player -> (ProxyEntity<T>) ProxyPlayer.from(player);
            case LivingEntity living -> (ProxyEntity<T>) ProxyLiving.from(living);
            default -> new ProxyEntity<>(self);
        };
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyEntity.TYPE        -> this.type;
            case ProxyEntity.IS_OF       -> this.isOf;
            case ProxyEntity.IS_IN       -> this.isIn;
            case ProxyEntity.INJECT      -> this.inject;
            case ProxyEntity.SAMPLE      -> this.sample;
            case ProxyEntity.GET         -> this.get;
            case ProxyEntity.SET         -> this.set;
            case ProxyEntity.SLOT        -> this.slot;
            case ProxyEntity.INSERT      -> this.insert;
            case ProxyEntity.WORLD       -> this.world;
            case ProxyEntity.HAS_TAG     -> this.hasTag;
            case ProxyEntity.APPEND_TAG  -> this.appendTag;
            case ProxyEntity.REMOVE_TAG  -> this.removeTag;
            case ProxyEntity.POSITION    -> this.position;
            case ProxyEntity.IS_SNEAKING -> this.isSneaking;
            case ProxyEntity.MATCHES     -> this.matches;
            case ProxyEntity.REMOVE      -> this.remove;
            case ProxyEntity.KILL        -> this.kill;
            case ProxyEntity.DAMAGE      -> this.damage;
            case ProxyEntity.EXECUTE     -> this.execute;
            case ProxyEntity.FUNCTION    -> this.function;
            case ProxyEntity.TELEPORT    -> this.teleport;
            case ProxyEntity.TO_STRING   -> this.toString;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public @NotNull String get(long index) {
                if (index >= 0 && index < ProxyEntity.KEYS.length)
                    return ProxyEntity.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyEntity.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyEntity.KEYS) {
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

    public @NotNull T unwrap() {
        return this.self;
    }
}
