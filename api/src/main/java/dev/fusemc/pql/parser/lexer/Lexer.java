package dev.fusemc.pql.parser.lexer;

import dev.fusemc.ParseException;
import com.manchickas.quelle.SourceReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Lexer extends SourceReader<String> {

    public Lexer(@NotNull String source) {
        super(source);
    }

    private static boolean isSeparator(int c) {
        return c == '/' || c == '[' || c == ']';
    }

    public @Nullable Lexeme next() throws ParseException {
        if (this.skipWhitespace()) {
            var c = this.peek();
            if (Lexer.isSeparator(c)) {
                var span = this.characterSpan();
                return new Lexeme.Separator((char) this.read(), span);
            }
            if (c >= '0' && c <= '9')
                return this.readInteger();
            return this.readIdentifier();
        }
        return null;
    }

    private @NotNull Lexeme.Integer readInteger() throws ParseException {
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
        var span = this.span(stamp);
        try {
            var result = Integer.parseInt(buffer.toString());
            return new Lexeme.Integer(result, span);
        } catch (NumberFormatException e) {
            throw new ParseException(
                    "Encountered a malformed integer literal in a PQL path.",
                    span
            );
        }
    }

    // TODO: Perhaps a more restrictive identifier format?
    private @NotNull Lexeme.Identifier readIdentifier() {
        var buffer = new StringBuilder();
        var stamp = this.stamp();
        while (this.canRead()) {
            var c = this.peek();
            if (Lexer.isSeparator(c) || Character.isWhitespace(c))
                break;
            buffer.appendCodePoint(this.read());
        }
        var span = this.span(stamp);
        return new Lexeme.Identifier(buffer.toString(), span);
    }
}
