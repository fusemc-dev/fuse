package dev.fusemc.pql.parser;

import dev.fusemc.ArrayBuilder;
import dev.fusemc.ParseException;
import dev.fusemc.pql.Path;
import dev.fusemc.pql.Segment;
import dev.fusemc.pql.parser.lexer.Lexeme;
import dev.fusemc.pql.parser.lexer.Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public final class Parser {

    private final Lexer lexer;
    private final Queue<Lexeme> buffer;

    public Parser(@NotNull String source) {
        this.lexer = new Lexer(source);
        this.buffer = new ArrayDeque<>(16);
    }

    public @NotNull Path parse() throws ParseException {
        var buffer = new ArrayBuilder<Segment>(16);
        if (this.isAtSeparator('/'))
            this.read();
        while (this.canRead()) {
            var segment = new Segment.Member(this.expectIdentifier().value());
            buffer.append(this.processSubscript(segment));
            if (this.canRead()) {
                this.expectSeparator('/');
                continue;
            }
            break;
        }
        return new Path(buffer.build(Segment[]::new));
    }

    private @NotNull Segment processSubscript(@NotNull Segment operand) throws ParseException {
        Objects.requireNonNull(operand);
        if (this.isAtSeparator('[')) {
            this.read();
            var index = this.expectInteger();
            this.expectSeparator(']');
            return this.processSubscript(new Segment.Subscript(operand, index.value()));
        }
        return operand;
    }

    private @NotNull Lexeme expectSeparator(char separator) throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        if (lexeme instanceof Lexeme.Separator(var actual, var _)) {
            if (actual == separator)
                return lexeme;
            throw new ParseException(
                    "Encountered a separator '%c' while '%c' was expected."
                            .formatted(actual, separator),
                    lexeme.span()
            );
        }
        throw new ParseException(
                "Expected a separator '%s'."
                        .formatted(separator),
                lexeme.span()
        );
    }

    public boolean isAtSeparator(char value) throws ParseException {
        var lexeme = this.peek();
        if (lexeme instanceof Lexeme.Separator(var actual, var _))
            return actual == value;
        return false;
    }

    private @NotNull Lexeme.Identifier expectIdentifier() throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        if (lexeme instanceof Lexeme.Identifier identifier)
            return identifier;
        throw new ParseException("Expected an identifier literal.", lexeme.span());
    }

    private @NotNull Lexeme.Integer expectInteger() throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        if (lexeme instanceof Lexeme.Integer integer)
            return integer;
        throw new ParseException("Expected an integer literal.", lexeme.span());
    }

    private @NotNull Lexeme ensureNonEOF(@Nullable Lexeme lexeme) throws ParseException {
        if (lexeme == null)
            throw new ParseException("Encountered an unexpected EOF.");
        return lexeme;
    }

    private @Nullable Lexeme read() throws ParseException {
        if (this.buffer.isEmpty())
            return this.lexer.next();
        return this.buffer.poll();
    }

    private @Nullable Lexeme peek() throws ParseException {
        if (this.buffer.isEmpty()) {
            var lexeme = this.lexer.next();
            if (lexeme != null) {
                this.buffer.add(lexeme);
                return lexeme;
            }
            return null;
        }
        return this.buffer.peek();
    }

    private boolean canRead() throws ParseException {
        if (this.buffer.isEmpty()) {
            var lexeme = this.lexer.next();
            if (lexeme != null) {
                this.buffer.add(lexeme);
                return true;
            }
            return false;
        }
        return true;
    }
}
