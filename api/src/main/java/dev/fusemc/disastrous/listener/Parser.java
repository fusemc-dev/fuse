package dev.fusemc.disastrous.listener;

import com.manchickas.optionated.Option;
import dev.fusemc.disastrous.Disastrous;
import dev.fusemc.disastrous.disaster.Disaster;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.disastrous.listener.selector.Selector;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import dev.fusemc.quelle.position.CharRange;
import dev.fusemc.standard.ProxyIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public final class Parser extends StringReader<String> {

    public Parser(@NotNull String source) {
        super(Objects.requireNonNull(source));
    }

    private static Diagnostic incomplete(@NotNull CharRange<String> range) {
        Objects.requireNonNull(range);
        return new Diagnostic("Encountered an incomplete selector expression.", range);
    }

    public @NotNull Selector parse() {
        var position = this.position();
        if (this.skipWhitespace()) {
            if (this.isAt('|')) {
                var selector = this.parseUnbound();
                if (this.skipWhitespace())
                    throw new Diagnostic("Encountered trailing data in a selector expression.", this.pointRange());
                return selector;
            }
            var selector = this.parseBound();
            if (this.skipWhitespace())
                throw new Diagnostic("Encountered trailing data in a selector expression.", this.pointRange());
            return selector;
        }
        throw Parser.incomplete(this.range(position));
    }

    public @NotNull Disaster.Type<?> parseType() {
        var position   = this.position();
        var identifier = ProxyIdentifier.readIdentifier(this);
        var option     = Disastrous.dispatch(identifier);
        if (option instanceof Option.Some<Disaster.Type<?>>(var type)) {
            assert type != null;
            return type;
        }
        throw new Diagnostic(String.format("Encountered an unrecognized disaster type '%s'.", identifier), this.range(position));
    }

    @SuppressWarnings("unchecked")
    private @NotNull Selector parseBound() {
        var stamp = this.position();
        var type  = this.parseType();
        if (this.skipWhitespace()) {
            if (this.isAt('[')) {
                this.read();
                var buffer = new Guard[16];
                var length = 0;
                if (this.skipWhitespace()) {
                    if (this.isAt(']')) {
                        this.read();
                        return Selector.bound(type, new Guard[0]);
                    }
                    while (this.skipWhitespace()) {
                        var position   = this.position();
                        var identifier = this.readIdentifier(c -> c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '-');
                        var option     = type.dispatch(identifier);
                        if (option instanceof Option.Some<Guard.Type<?>>(var gt)) {
                            assert gt != null;
                            if (this.skipWhitespace()) {
                                if (this.isAt('(')) {
                                    this.read();
                                    var guard = gt.parse(this);
                                    if (this.skipWhitespace()) {
                                        if (this.isAt(')')) {
                                            this.read();
                                            if (length >= buffer.length)
                                                buffer = Arrays.copyOf(buffer, buffer.length << 1);
                                            buffer[length++] = guard;
                                            if (this.skipWhitespace()) {
                                                if (this.isAt('|')) {
                                                    this.read();
                                                    continue;
                                                }
                                                if (this.isAt(']')) {
                                                    this.read();
                                                    return Selector.bound(type, Arrays.copyOf(buffer, length));
                                                }
                                                throw new Diagnostic("Expected either a termination ']' or a continuation '|' of the guard list.", this.pointRange());
                                            }
                                            throw Parser.incomplete(this.range(stamp));
                                        }
                                        throw new Diagnostic("Expected a termination of the argument list ')'.", this.pointRange());
                                    }
                                    throw Parser.incomplete(this.range(stamp));
                                }
                                throw new Diagnostic("Expected an argument list '('.", this.pointRange());
                            }
                            throw Parser.incomplete(this.range(stamp));
                        }
                        throw new Diagnostic(String.format("Unrecognized guard clause '%s'.", identifier), this.range(position));
                    }
                    throw Parser.incomplete(this.range(stamp));
                }
                throw Parser.incomplete(this.range(stamp));
            }
            throw new Diagnostic("Expected a guard list '['.", this.pointRange());
        }
        return Selector.bound(type, new Guard[0]);
    }

    private @NotNull Selector parseUnbound() {
        var stamp = this.position();
        if (this.skipWhitespace()) {
            if (this.isAt('|')) {
                this.read();
                if (this.skipWhitespace()) {
                    var identifier = ProxyIdentifier.readIdentifier(this);
                    if (this.skipWhitespace()) {
                        if (this.isAt('|')) {
                            this.read();
                            return Selector.unbound(identifier);
                        }
                        throw new Diagnostic("Expected a closing '|'.", this.pointRange());
                    }
                    throw Parser.incomplete(this.range(stamp));
                }
                throw Parser.incomplete(this.range(stamp));
            }
            throw new Diagnostic("Expected an opening '|'.", this.pointRange());
        }
        throw Parser.incomplete(this.range(stamp));
    }
}
