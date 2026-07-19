package dev.fusemc.marshal.standard;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Documented("Context")
public final class ProxyContext implements ProxyObject {

    private static final @NotNull String SOURCE            = "source";
    private static final @NotNull String ARGUMENT          = "argument";
    private static final @NotNull String MATCHES           = "matches";
    private static final @NotNull String MATCHES_SENSITIVE = "matchesSensitive";
    private static final @NotNull String MATCHES_FRAGMENT  = "matchesFragment";
    private static final @NotNull String TO_STRING         = "toString";

    private static final @NotNull String @NotNull[] KEYS = {
            ProxyContext.SOURCE,
            ProxyContext.ARGUMENT,
            ProxyContext.MATCHES,
            ProxyContext.MATCHES_SENSITIVE,
            ProxyContext.MATCHES_FRAGMENT,
            ProxyContext.TO_STRING
    };

    private static final ProxyExecutable TO_STRING_IMPL = args -> {
        if (args.length == 0)
            return "[object Context]";
        throw new UnsupportedOperationException();
    };

    private final @NotNull String source;
    private final @NotNull String argument;
    private final @NotNull String lowerArgument;
    private final @NotNull ProxyExecutable matches;
    private final @NotNull ProxyExecutable matchesSensitive;
    private final @NotNull ProxyExecutable matchesFragment;

    public ProxyContext(@NotNull String source, int position) {
        this.source         = Objects.requireNonNull(source);
        this.argument       = this.source.substring(position);
        this.lowerArgument  = this.argument.toLowerCase();
        this.matches   = (args) -> {
            if (args.length == 1) {
                var candidate = Tau.lower(Template.STRING, args[0]).toLowerCase();
                return candidate.startsWith(this.lowerArgument);
            }
            throw new UnsupportedOperationException();
        };
        this.matchesSensitive = (args) -> {
            if (args.length == 1) {
                var candidate = Tau.lower(Template.STRING, args[0]);
                return candidate.startsWith(this.argument);
            }
            throw new UnsupportedOperationException();
        };
        this.matchesFragment = (args) -> {
            if (args.length == 2) {
                var candidate = Tau.lower(Template.STRING, args[0]);
                var pattern   = Tau.lowerPattern(args[1]);
                var fragments = pattern.split(candidate.toLowerCase());
                for (var fragment : fragments) {
                    if (fragment.startsWith(this.lowerArgument))
                        return true;
                }
                return false;
            }
            throw new UnsupportedOperationException();
        };
    }

    public static ProxyContext from(@NotNull SuggestionsBuilder builder) {
        Objects.requireNonNull(builder);
        return new ProxyContext(builder.getInput(), builder.getStart());
    }

    @Override
    public Object getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        return switch (key) {
            case ProxyContext.SOURCE    -> this.source;
            case ProxyContext.ARGUMENT -> this.argument;
            case ProxyContext.MATCHES   -> this.matches;
            case ProxyContext.MATCHES_SENSITIVE -> this.matchesSensitive;
            case ProxyContext.MATCHES_FRAGMENT  -> this.matchesFragment;
            case ProxyContext.TO_STRING -> ProxyContext.TO_STRING_IMPL;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public String get(long index) {
                if (index >= 0 && index < ProxyContext.KEYS.length)
                    return ProxyContext.KEYS[(int) index];
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, @NotNull Value value) {
                Objects.requireNonNull(value);
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ProxyContext.KEYS.length;
            }
        };
    }

    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        for (var candidate : ProxyContext.KEYS) {
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
}
