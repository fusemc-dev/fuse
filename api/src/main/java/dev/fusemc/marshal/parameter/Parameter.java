package dev.fusemc.marshal.parameter;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.fusemc.ValueOps;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.standard.entity.ProxyEntity;
import dev.fusemc.standard.math.ProxyVec3;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.NbtOps;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public interface Parameter<T> {

    @NotNull Parameter<Double> NUMBER = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            var position = reader.position();
            if (reader.skipWhitespace()) {
                if (reader.isAt(')'))
                    return DoubleArgumentType.doubleArg();
                if (reader.readOnly("..")) {
                    var upper = reader.readFloating();
                    if (reader.skipWhitespace()) {
                        if (reader.isAt(')'))
                            return DoubleArgumentType.doubleArg(-Double.MAX_VALUE, upper);
                        throw new Diagnostic("Encountered trailing data in a number parameter.", reader.pointRange());
                    }
                    return DoubleArgumentType.doubleArg(-Double.MAX_VALUE, upper);
                }
                var lower = reader.readFloating();
                if (reader.skipWhitespace()) {
                    if (reader.readOnly("..")) {
                        if (reader.skipWhitespace()) {
                            if (reader.isAt(')'))
                                return DoubleArgumentType.doubleArg(lower);
                            var upper = reader.readFloating();
                            return DoubleArgumentType.doubleArg(lower, upper);
                        }
                        return DoubleArgumentType.doubleArg(lower);
                    }
                    throw new Diagnostic("Expected '..' to define a number range.", reader.range(position));
                }
                throw new Diagnostic("Encountered an incomplete number range.", reader.range(position));
            }
            return DoubleArgumentType.doubleArg();
        }

        @Override
        public @NonNull Double resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            return context.getArgument(name, Double.class);
        }
    };
    @NotNull Parameter<Long> INTEGER = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            var position = reader.position();
            if (reader.skipWhitespace()) {
                if (reader.isAt(')'))
                    return LongArgumentType.longArg();
                if (reader.readOnly("..")) {
                    var upper = reader.readInteger();
                    if (reader.skipWhitespace()) {
                        if (reader.isAt(')'))
                            return LongArgumentType.longArg(-Long.MAX_VALUE, upper);
                        throw new Diagnostic("Encountered trailing data in a number parameter.", reader.pointRange());
                    }
                    return LongArgumentType.longArg(-Long.MAX_VALUE, upper);
                }
                var lower = reader.readInteger();
                if (reader.skipWhitespace()) {
                    if (reader.readOnly("..")) {
                        if (reader.skipWhitespace()) {
                            if (reader.isAt(')'))
                                return LongArgumentType.longArg(lower);
                            var upper = reader.readInteger();
                            return LongArgumentType.longArg(lower, upper);
                        }
                        return LongArgumentType.longArg(lower);
                    }
                    throw new Diagnostic("Expected '..' to define a number range.", reader.range(position));
                }
                throw new Diagnostic("Encountered an incomplete number range.", reader.range(position));
            }
            return LongArgumentType.longArg();
        }

        @Override
        public @NonNull Long resolve(@NotNull CommandContext<CommandSourceStack> context,
                                     @NotNull String name) {
            return context.getArgument(name, Long.class);
        }
    };
    @NotNull Parameter<String> STRING = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            if (reader.skipWhitespace()) {
                if (reader.isAt(')'))
                    return StringArgumentType.string();
                var position = reader.position();
                var type     = reader.readIdentifier((c) -> c >= 'a' && c <= 'z');
                return switch (type) {
                    case "word"   -> StringArgumentType.word();
                    case "quoted" -> StringArgumentType.string();
                    case "greedy" -> StringArgumentType.greedyString();
                    default -> throw new Diagnostic(String.format("Encountered an unrecognized string type '%s'.", type), reader.range(position));
                };
            }
            return StringArgumentType.string();
        }

        @Override
        public @NonNull String resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            return context.getArgument(name, String.class);
        }
    };
    @NotNull Parameter<Boolean> BOOLEAN = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            return BoolArgumentType.bool();
        }

        @Override
        public @NonNull Boolean resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            return context.getArgument(name, Boolean.class);
        }
    };
    @NotNull Parameter<ProxyVec3> POSITION = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            if (reader.skipWhitespace()) {
                if (reader.isAt(')'))
                    return Vec3Argument.vec3();
                var position = reader.position();
                var type     = reader.readIdentifier((c) -> c >= 'a' && c <= 'z');
                return switch (type) {
                    case "aligned"   -> BlockPosArgument.blockPos();
                    case "freeform"  -> Vec3Argument.vec3();
                    default -> throw new Diagnostic(String.format("Encountered an unrecognized position type '%s'.", type), reader.range(position));
                };
            }
            return Vec3Argument.vec3();
        }

        @Override
        public @NonNull ProxyVec3 resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            var coordinates = context.getArgument(name, Coordinates.class);
            var source      = context.getSource();
            return ProxyVec3.from(coordinates.getPosition(source));
        }
    };
    @NotNull Parameter<ProxyIdentifier> IDENTIFIER = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            return IdentifierArgument.id();
        }

        @Override
        public @NonNull ProxyIdentifier resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            return ProxyIdentifier.from(IdentifierArgument.getId(context, name));
        }
    };
    @NotNull Parameter<ProxyEntity<?>[]> SELECTOR = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            return EntityArgument.entities();
        }

        @Override
        public @NonNull ProxyEntity<?> @NonNull [] resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) throws CommandSyntaxException {
            var selector = context.getArgument(name, EntitySelector.class);
            var source   = context.getSource();
            return selector.findEntities(source)
                    .stream()
                    .map(ProxyEntity::from)
                    .toArray(ProxyEntity<?>[]::new);
        }
    };
    @NotNull Parameter<Value> ANY = new Parameter<>() {

        @Override
        public @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader) {
            return NbtTagArgument.nbtTag();
        }

        @Override
        public @NonNull Value resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) {
            return NbtOps.INSTANCE.convertTo(ValueOps.INSTANCE, NbtTagArgument.getNbtTag(context, name));
        }
    };

    @NotNull ArgumentType<?> parse(@NotNull StringReader<String> reader);

    @NotNull T resolve(@NotNull CommandContext<CommandSourceStack> context, @NotNull String name) throws CommandSyntaxException;
}
