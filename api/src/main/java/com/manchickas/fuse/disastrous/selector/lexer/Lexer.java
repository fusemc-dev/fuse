package com.manchickas.fuse.disastrous.selector.lexer;

import com.manchickas.fuse.ParseException;
import com.manchickas.fuse.standard.util.ScriptIdentifier;
import com.manchickas.quelle.SourceReader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Lexer extends SourceReader<String> {

    public Lexer(@NonNull String source) {
        super(source);
    }

    private static boolean isSeparator(int c) {
        return c == '(' || c == ')' || c == '[' || c == ']' || c == ',' || c == '|' || c == '#';
    }

    public @Nullable Lexeme next() throws ParseException {
        if (this.skipWhitespace()) {
            var c = this.peek();
            if (Lexer.isSeparator(c)) {
                var span = this.characterSpan();
                return new Lexeme.Separator((char) this.read(), span);
            }
            if (c == '_') {
                var span = this.characterSpan();
                this.read();
                return new Lexeme.Wildcard(span);
            }
            if (c == '.' && this.peek(1) == '.' && this.peek(2) == '.') {
                var span = this.relativeSpan(3);
                this.read(3);
                return new Lexeme.Variadic(span);
            }
            if (c == '\'' || c == '"')
                return this.readString(this.read());
            if (c >= '0' && c <= '9'|| ((c == '+' || c == '-') && this.peek(1) >= '0' && this.peek(1) <= '9'))
                return this.readNumber();
            return this.readGeneric();
        }
        return null;
    }

    private Lexeme.Literal readString(int quote) throws ParseException {
        var buffer = new StringBuilder();
        var stamp = this.inclusiveStamp();
        while (this.canRead()) {
            var c = this.peek();
            if (c == quote) {
                this.read();
                return new Lexeme.Literal(buffer.toString(), this.span(stamp));
            }
            if (c == '\\') {
                var d = this.peek(1);
                if (d == quote) {
                    buffer.appendCodePoint(quote);
                    this.read(2);
                    continue;
                }
                throw new ParseException(
                        "Encountered an unrecognized escape sequence '\\%c'"
                                .formatted(d),
                        this.relativeSpan(2)
                );
            }
            buffer.appendCodePoint(this.read());
        }
        throw new ParseException(
                "Encountered an unterminated string literal.",
                this.span(stamp)
        );
    }

    private Lexeme.Number readNumber() throws ParseException {
        var buffer = new StringBuilder();
        var stamp = this.stamp();
        var allowsDecimal = true;
        if (this.isAt('+') || this.isAt('-'))
            buffer.appendCodePoint(this.read());
        while (this.canRead()) {
            var c = this.peek();
            if (c == '.') {
                var d = this.peek(1);
                if (d >= '0' && d <= '9') {
                    if (allowsDecimal) {
                        buffer.append('.');
                        allowsDecimal = false;
                        continue;
                    }
                    throw new ParseException(
                            "Encountered an out-of-place decimal point in a number literal.",
                            this.characterSpan()
                    );
                }
                break;
            }
            if (c >= '0' && c <= '9') {
                buffer.appendCodePoint(this.read());
                continue;
            }
            break;
        }
        var span = this.span(stamp);
        try {
            var value = Double.parseDouble(buffer.toString());
            return new Lexeme.Number(value, span);
        } catch (NumberFormatException e) {
            throw new ParseException("Encountered a malformed number literal.", span);
        }
    }

    private Lexeme readGeneric() throws ParseException {
        var buffer = new StringBuilder();
        var stamp = this.stamp();
        while (this.canRead()) {
            var c = this.peek();
            if (Lexer.isSeparator(c) || c == '\'' || c == '"' || Character.isWhitespace(c))
                break;
            if (ScriptIdentifier.isValid(c) || c >= 'A' && c <= 'Z' || c == ':' || c == '/') {
                buffer.appendCodePoint(this.read());
                continue;
            }
            throw new ParseException(
                    "Encountered an invalid character '%c' in an identifier literal."
                            .formatted(c),
                    this.characterSpan()
            );
        }
        var identifier = buffer.toString();
        var span = this.span(stamp);
        return switch (identifier) {
            case "true" -> new Lexeme.Boolean(true, span);
            case "false" -> new Lexeme.Boolean(false, span);
            case "undefined" -> new Lexeme.Undefined(span);
            case "null" -> new Lexeme.Null(span);
            default -> new Lexeme.Identifier(identifier, span);
        };
    }
}
