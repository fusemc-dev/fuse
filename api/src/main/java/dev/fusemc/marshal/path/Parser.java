package dev.fusemc.marshal.path;

import dev.fusemc.ArrayBuilder;
import dev.fusemc.marshal.parameter.Parameter;
import dev.fusemc.marshal.path.segment.Segment;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import dev.fusemc.quelle.position.CharRange;
import dev.fusemc.standard.ProxyIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Parser extends StringReader<String> {

    public Parser(@NotNull String source) {
        super(source);
    }

    private static boolean isLiteralChar(int c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static Diagnostic incomplete(@NotNull CharRange<String> range) {
        Objects.requireNonNull(range);
        return new Diagnostic("Encountered an incomplete Marshal expression.", range);
    }

    public @NotNull CommandPath parsePath() {
        if (this.skipWhitespace()) {
            var builder = new ArrayBuilder<Segment>(16);
            var starred = this.parsePrefix();
            while (this.skipWhitespace()) {
                var segment = this.parseSegment();
                builder.append(segment);
                if (this.skipWhitespace()) {
                    if (this.isAt('/')) {
                        this.read();
                        continue;
                    }
                    throw new Diagnostic("Expected a continuation of a path.", this.pointRange());
                }
                return new CommandPath(builder.build(Segment[]::new), starred);
            }
            // TODO: Probably change it up a bit.
            return new CommandPath(builder.build(Segment[]::new), starred);
        }
        throw new Diagnostic("Expected a path.", this.pointRange());
    }

    public @NotNull Segment parseSegment() {
        if (this.skipWhitespace()) {
            var buffer = new StringBuilder();
            while (this.canRead()) {
                var c = this.peek();
                if (c == ':')
                    return this.parseArgument(buffer.toString());
                if (Parser.isLiteralChar(c)) {
                    buffer.appendCodePoint(this.read());
                    continue;
                }
                break;
            }
            return Segment.literal(buffer.toString());
        }
        throw Parser.incomplete(this.range(this.origin()));
    }

    public @NotNull Segment parseArgument(@NotNull String name) {
        if (this.skipWhitespace()) {
            if (this.readOnly(':')) {
                var parameter = this.parseParameter();
                var suggester = (ProxyIdentifier) null;
                if (this.skipWhitespace()) {
                    if (this.isAt('<')) {
                        suggester = this.parseSuggester();
                        this.skipWhitespace();
                    }
                    if (this.readOnly('(')) {
                        var type = parameter.parse(this);
                        if (this.skipWhitespace()) {
                            if (this.readOnly(')'))
                                return Segment.argument(name, parameter, type, suggester);
                            throw new Diagnostic("Expected a matching ')' to terminate the parameter constructor clause.", this.pointRange());
                        }
                        throw Parser.incomplete(this.range(this.origin()));
                    }
                    throw new Diagnostic("Expected a '(' to begin a parameter constructor clause.", this.pointRange());
                }
                throw Parser.incomplete(this.range(this.origin()));
            }
            throw new Diagnostic("Expected a ':' to separate the parameter type.", this.pointRange());
        }
        throw Parser.incomplete(this.range(this.origin()));
    }

    public @NotNull Parameter<?> parseParameter() {
        if (this.skipWhitespace()) {
            var position = this.position();
            var buffer   = new StringBuilder();
            while (this.canRead()) {
                var c = this.peek();
                if (Parser.isLiteralChar(c)) {
                    buffer.appendCodePoint(this.read());
                    continue;
                }
                break;
            }
            var candidate = buffer.toString();
            return switch (candidate) {
                case "any"        -> Parameter.ANY;
                case "number"     -> Parameter.NUMBER;
                case "integer"    -> Parameter.INTEGER;
                case "string"     -> Parameter.STRING;
                case "boolean"    -> Parameter.BOOLEAN;
                case "position"   -> Parameter.POSITION;
                case "selector"   -> Parameter.SELECTOR;
                case "identifier" -> Parameter.IDENTIFIER;
                default -> throw new Diagnostic(String.format("Encountered an unrecognized parameter type '%s'.", candidate), this.range(position));
            };
        }
        throw Parser.incomplete(this.range(this.origin()));
    }

    public @NotNull ProxyIdentifier parseSuggester() {
        if (this.skipWhitespace()) {
            if (this.readOnly('<')) {
                var identifier = ProxyIdentifier.readIdentifier(this);
                if (this.skipWhitespace()) {
                    if (this.readOnly('>'))
                        return identifier;
                    throw new Diagnostic("Expected a matching '>' to terminate the suggester clause.", this.pointRange());
                }
                throw Parser.incomplete(this.range(this.origin()));
            }
            throw new Diagnostic("Expected a '<' to begin a suggester clause.", this.pointRange());
        }
        throw Parser.incomplete(this.range(this.origin()));
    }

    public boolean parsePrefix() {
        if (this.readOnly('*')) {
            if (this.skipWhitespace()) {
                if (this.readOnly('/'))
                    return true;
                throw new Diagnostic("Expected a '/' to terminate the star prefix.", this.pointRange());
            }
            throw Parser.incomplete(this.range(this.origin()));
        }
        return false;
    }
}
