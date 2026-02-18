package dev.fusemc.pql;

import com.manchickas.quelle.SourceReader;
import dev.fusemc.ArrayBuilder;
import dev.fusemc.ParseException;
import dev.fusemc.pql.path.Path;
import dev.fusemc.pql.path.Segment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// entity.get(string) -> data at that string
// /data get entity path.to.data
public class Parser extends SourceReader<String> {

    public Parser(@NotNull String source) {
        super(source);
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAllowed(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || Parser.isDigit(c) || c == '_';
    }

    public @NotNull Path parse() throws ParseException {
        if (this.canRead()) {
            var buffer = new ArrayBuilder<Segment>(16);
            while (this.canRead()) {
                buffer.append(this.parseSegment());
                if (this.canRead()) {
                    if (this.isAt('/')) {
                        this.read();
                        continue;
                    }
                    throw new ParseException("Expected a path separator '/'.", this.characterSpan());
                }
                break;
            }
            return new Path(buffer.build(Segment[]::new));
        }
        throw new ParseException("Encountered an empty PQL expression.");
    }

    private @NotNull Segment parseSegment() throws ParseException {
        if (this.canRead()) {
            if (Parser.isAllowed(this.peek())) {
                var buffer = new StringBuilder();
                while (this.canRead()) {
                    var c = this.peek();
                    if (Parser.isAllowed(c)) {
                        buffer.appendCodePoint(this.read());
                        continue;
                    }
                    if (c == '[')
                        return this.parseSubscript(new Segment.Member(buffer.toString()));
                    if (c == '/')
                        break;
                    throw new ParseException(
                            "Encountered an invalid character '%c' in an identifier literal."
                                    .formatted(c),
                            this.characterSpan()
                    );
                }
                return new Segment.Member(buffer.toString());
            }
            throw new ParseException(
                    "Expected an identifier beginning with [A-Za-z0-9_].",
                    this.characterSpan()
            );
        }
        throw new ParseException(
                "Expected an identifier beginning with [A-Za-z0-9_].",
                this.inclusiveCharacterSpan()
        );
    }

    private @NotNull Segment parseSubscript(@NotNull Segment operand) throws ParseException {
        Objects.requireNonNull(operand);
        if (this.canRead()) {
            if (this.isAt('[')) {
                this.read();
                var index = this.parseInteger();
                if (this.canRead()) {
                    if (this.isAt(']')) {
                        this.read();
                        var segment = new Segment.Subscript(operand, index);
                        if (this.isAt('['))
                            return this.parseSubscript(segment);
                        return segment;
                    }
                    throw new ParseException(
                            "Expected a closing bracket ']' to terminate the subscript operator.",
                            this.characterSpan()
                    );
                }
                throw new ParseException(
                        "Expected a closing bracket ']' to terminate the subscript operator.",
                        this.inclusiveCharacterSpan()
                );
            }
            throw new ParseException(
                    "Expected an opening bracket '[' to begin the subscript operator.",
                    this.characterSpan()
            );
        }
        throw new ParseException(
                "Expected an opening bracket '[' to begin the subscript operator.",
                this.inclusiveCharacterSpan()
        );
    }

    private int parseInteger() throws ParseException {
        if (this.canRead()) {
            if (Parser.isDigit(this.peek())) {
                var buffer = new StringBuilder();
                var stamp = this.stamp();
                while (this.canRead()) {
                    var c = this.peek();
                    if (c >= '0' && c <= '9') {
                        buffer.appendCodePoint(this.read());
                        continue;
                    }
                    break;
                }
                try {
                    return Integer.parseInt(buffer.toString());
                } catch (NumberFormatException e) {
                    throw new ParseException(
                            "Encountered a malformed integer literal in a PQL expression.",
                            this.span(stamp)
                    );
                }
            }
            throw new ParseException(
                    "Expected an integer literal beginning with [0-9].",
                    this.characterSpan()
            );
        }
        throw new ParseException(
                "Expected an integer literal beginning with [0-9].",
                this.inclusiveCharacterSpan()
        );
    }
}
