package dev.fusemc.pql.parser.lexer;

import com.manchickas.quelle.position.SourceSpan;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface Lexeme {

    @NotNull SourceSpan<String> span();

    record Identifier(@NotNull String value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Identifier {
            Objects.requireNonNull(value);
            Objects.requireNonNull(span);
        }
    }

    record Separator(char value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Separator {
            Objects.requireNonNull(span);
        }
    }

    record Integer(int value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Integer {
            Objects.requireNonNull(span);
        }
    }
}
