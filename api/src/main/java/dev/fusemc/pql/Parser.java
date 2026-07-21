package dev.fusemc.pql;

import dev.fusemc.ArrayBuilder;
import dev.fusemc.pql.segment.Segment;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import dev.fusemc.quelle.position.CharRange;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class Parser extends StringReader<String> {

    public Parser(@NonNull String source) {
        super(source);
    }

    private static @NotNull Diagnostic incomplete(@NotNull CharRange<String> range) {
        Objects.requireNonNull(range);
        return new Diagnostic("Encountered an incomplete PQL expression.", range);
    }

    public @NotNull Path parsePath() {
        var buffer   = new ArrayBuilder<Segment>(16);
        var position = this.position();
        if (this.skipWhitespace()) {
            if (this.readOnly('/'))
                buffer.append(Segment.ROOT);
            while (this.skipWhitespace()) {
                var segment = this.parseSegment();
                if (this.skipWhitespace()) {
                    if (this.readOnly('/')) {
                        buffer.append(segment);
                        continue;
                    }
                    throw new Diagnostic("Expected a '/' to separate segments in a PQL expression.", this.pointRange());
                }
                buffer.append(segment);
            }
            return new Path(buffer.build(Segment[]::new));
        }
        throw Parser.incomplete(this.range(position));
    }

    public @NotNull Segment parseSegment() {
        if (this.skipWhitespace()) {
            var buffer = new StringBuilder();
            while (this.canRead()) {
                var c = this.peek();
                if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
                    buffer.appendCodePoint(this.read());
                    continue;
                }
                break;
            }
            return this.parseSubscript(Segment.property(buffer.toString()));
        }
        throw Parser.incomplete(this.range(this.origin()));
    }

    private @NotNull Segment parseSubscript(@NotNull Segment segment) {
        if (this.skipWhitespace()) {
            if (this.isAt('/'))
                return segment;
            if (this.readOnly('[')) {
                if (this.skipWhitespace()) {
                    var provider = PositionProvider.adaptive((int) this.readInteger());
                    if (this.skipWhitespace()) {
                        if (this.readOnly(']'))
                            return Segment.subscript(segment, provider);
                        throw new Diagnostic("Expected a ']' to terminate a subscript clause.", this.pointRange());
                    }
                    throw Parser.incomplete(this.range(this.origin()));
                }
                throw Parser.incomplete(this.range(this.origin()));
            }
            throw new Diagnostic(String.format("Encountered an unexpected character '%c' in a PQL expression.", this.peek()), this.pointRange());
        }
        return segment;
    }
}
