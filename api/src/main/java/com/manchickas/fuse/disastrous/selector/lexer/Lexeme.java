package com.manchickas.fuse.disastrous.selector.lexer;

import com.manchickas.quelle.position.SourceSpan;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface Lexeme {

    @NotNull SourceSpan<String> span();

    record Identifier(@NotNull String value,
                      @NotNull SourceSpan<String> span) implements Lexeme {

        public Identifier {
            Objects.requireNonNull(value);
            Objects.requireNonNull(span);
        }
    }

    record Literal(@NotNull String value,
                   @NotNull SourceSpan<String> span) implements Lexeme {

        public Literal {
            Objects.requireNonNull(value);
            Objects.requireNonNull(span);
        }
    }

    record Number(double value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Number {
            Objects.requireNonNull(span);
        }
    }

    record Boolean(boolean value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Boolean {
            Objects.requireNonNull(span);
        }
    }

    record Undefined(@NotNull SourceSpan<String> span) implements Lexeme {

        public Undefined {
            Objects.requireNonNull(span);
        }
    }

    record Null(@NotNull SourceSpan<String> span) implements Lexeme {

        public Null {
            Objects.requireNonNull(span);
        }
    }

    record Separator(char value, @NotNull SourceSpan<String> span) implements Lexeme {

        public Separator {
            Objects.requireNonNull(span);
        }
    }

    record Wildcard(@NotNull SourceSpan<String> span) implements Lexeme {

        public Wildcard {
            Objects.requireNonNull(span);
        }
    }

    record Variadic(@NotNull SourceSpan<String> span) implements Lexeme {

        public Variadic {
            Objects.requireNonNull(span);
        }
    }
}
