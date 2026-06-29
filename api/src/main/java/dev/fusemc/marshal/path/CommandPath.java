package dev.fusemc.marshal.path;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.fusemc.marshal.Command;
import dev.fusemc.marshal.Resolver;
import dev.fusemc.marshal.path.segment.Argument;
import dev.fusemc.marshal.path.segment.Segment;
import dev.fusemc.marshal.standard.ProxySource;
import dev.fusemc.tau.proxy.ObjectLike;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class CommandPath {

    private final @NotNull Segment @NotNull [] segments;

    CommandPath(@NotNull Segment @NotNull[] segments) {
        this.segments = Objects.requireNonNull(segments);
    }

    @SuppressWarnings("unchecked")
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build(@NotNull Command command,
                                                                     @NotNull Resolver resolver) {
        Objects.requireNonNull(command);
        var builder = this.segments[this.segments.length - 1].build(resolver)
                .executes(ctx -> {
                    try {
                        command.onCommand(ProxySource.from(ctx.getSource()), this.collectArguments(ctx));
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal(e.getMessage()));
                        return -1;
                    }
                    return 1;
                });
        if (this.segments.length > 1)
            for (var i = this.segments.length - 2; i >= 0; i--) {
                var segment = this.segments[i];
                builder = segment.build(resolver)
                        .then(builder);
            }
        return (LiteralArgumentBuilder<CommandSourceStack>) builder;
    }

    private @NotNull Value collectArguments(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Objects.requireNonNull(context);
        var buffer = ObjectLike.builder(this.segments.length);
        for (var segment : this.segments) {
            if (segment instanceof Argument<?> argument)
                buffer.append(argument.name(), Value.asValue(argument.resolve(context)));
        }
        return Value.asValue(buffer.build());
    }

    @Override
    public @NotNull String toString() {
        var buffer = new StringBuilder();
        for (var i = 0; i < this.segments.length; i++) {
            var segment = this.segments[i];
            if (i > 0)
                buffer.append('/');
            buffer.append(segment);
        }
        return buffer.toString();
    }
}
