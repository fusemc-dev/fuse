package com.manchickas.fuse.disastrous.selector;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.manchickas.fuse.ArrayBuilder;
import com.manchickas.fuse.ParseException;
import com.manchickas.fuse.disastrous.event.Event;
import com.manchickas.fuse.disastrous.event.EventCallback;
import com.manchickas.fuse.disastrous.event.EventType;
import com.manchickas.fuse.disastrous.guard.Guard;
import com.manchickas.fuse.disastrous.guard.GuardType;
import com.manchickas.fuse.disastrous.selector.lexer.Lexeme;
import com.manchickas.fuse.disastrous.selector.lexer.Lexer;
import com.manchickas.fuse.standard.util.ScriptIdentifier;
import com.manchickas.optionated.Option;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Parser {

    private final Lexer lexer;
    private final Deque<Lexeme> buffer;

    public Parser(@NotNull String source) {
        this.lexer = new Lexer(source);
        this.buffer = new ArrayDeque<>(16);
    }

    public @NotNull EventSelector<?, ?> parse() throws ParseException {
        if (this.isAtSeparator('#')) {
            this.read();
            var identifier = this.expectIdentifier();
            var parsed = ScriptIdentifier.parse(identifier.value(), Identifier::fromNamespaceAndPath);
            return this.parseUnboundGuards(parsed);
        }
        return this.parseBound();
    }

    private @NotNull EventSelector.Bound<?, ?> parseBound() throws ParseException {
        var identifier = this.expectIdentifier();
        var parsed = ScriptIdentifier.parse(identifier.value(), Identifier::fromNamespaceAndPath);
        var type = EventType.lookup(parsed);
        if (type instanceof Option.Some<EventType<?,?>>(var wrapped))
            return this.parseGuards(wrapped);
        throw new ParseException(
                "Encountered an unrecognized event type '%s'."
                        .formatted(identifier.value()),
                identifier.span()
        );
    }

    @SuppressWarnings("unchecked")
    private <E extends Event<E, C>, C extends EventCallback> EventSelector.Bound<E, C> parseGuards(@NotNull EventType<E, C> event) throws ParseException {
        if (this.isAtSeparator('[')) {
            this.read();
            if (this.isAtSeparator(']')) {
                this.read();
                return new EventSelector.Bound<E, C>(event, new Guard.Bound[0]);
            }
            var buffer = new ArrayBuilder<Guard.Bound<E>>(8);
            while (true) {
                var name = this.expectIdentifier();
                var type = event.guard(name.value());
                if (type instanceof Option.Some<GuardType<? super E, ?>>(var wrapped)) {
                    this.expectSeparator('(');
                    var guard = wrapped.parse(this);
                    buffer.append((Guard.Bound<E>) guard);
                    this.expectSeparator(')');
                    if (this.isAtSeparator(']')) {
                        this.read();
                        return new EventSelector.Bound<>(event, buffer.build(Guard.Bound[]::new));
                    }
                    this.expectSeparator('|');
                    continue;
                }
                throw new ParseException(
                        "Encountered an unrecognized guard '%s'."
                                .formatted(name.value()),
                        name.span()
                );
            }
        }
        return new EventSelector.Bound<E, C>(event, new Guard.Bound[0]);
    }

    private @NotNull EventSelector.Unbound parseUnboundGuards(@NotNull Identifier event) throws ParseException {
        var buffer = new ArrayBuilder<Guard.Unbound>(8);
        if (this.isAtSeparator('[')) {
            this.read();
            if (this.isAtSeparator(']')) {
                this.read();
                return new EventSelector.Unbound(event, new Guard.Unbound[0]);
            }
            while (true) {
                if (this.peek() instanceof Lexeme.Variadic) {
                    this.read();
                    buffer.append(new Guard.Unbound.Variadic());
                    this.expectSeparator(']');
                    return new EventSelector.Unbound(event, buffer.build(Guard.Unbound[]::new));
                }
                buffer.append(this.parseUnboundGuard());
                if (this.isAtSeparator(']')) {
                    this.read();
                    return new EventSelector.Unbound(event, buffer.build(Guard.Unbound[]::new));
                }
                this.expectSeparator(',');
            }
        }
        return new EventSelector.Unbound(event, new Guard.Unbound[0]);
    }

    private Guard.Unbound parseUnboundGuard() throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        return switch (lexeme) {
            case Lexeme.Literal(var value, var _) -> new Guard.Unbound.Literal(value);
            case Lexeme.Number(var value, var _)  -> new Guard.Unbound.Number(value);
            case Lexeme.Boolean(var value, var _) -> new Guard.Unbound.Boolean(value);
            case Lexeme.Null(var _)               -> new Guard.Unbound.Null();
            case Lexeme.Undefined(var _)          -> new Guard.Unbound.Undefined();
            case Lexeme.Wildcard(var _)           -> new Guard.Unbound.Wildcard();
            default -> throw new ParseException("Expected an unbound guard.", lexeme.span());
        };
    }

    public @NotNull Lexeme.Identifier expectIdentifier() throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        var span = lexeme.span();
        if (lexeme instanceof Lexeme.Identifier identifier)
            return identifier;
        throw new ParseException("Expected an identifier literal.", span);
    }

    public @NotNull Lexeme.Literal expectLiteral() throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        var span = lexeme.span();
        if (lexeme instanceof Lexeme.Literal literal)
            return literal;
        throw new ParseException("Expected a string literal.", span);
    }

    public boolean isAtSeparator(char value) throws ParseException {
        var lexeme = this.peek();
        if (lexeme instanceof Lexeme.Separator(var actual, var _))
            return actual == value;
        return false;
    }

    @CanIgnoreReturnValue
    public @NotNull Lexeme.Separator expectSeparator(char value) throws ParseException {
        var lexeme = this.ensureNonEOF(this.read());
        var span = lexeme.span();
        if (lexeme instanceof Lexeme.Separator(var actual, var _)) {
            if (actual == value)
                return (Lexeme.Separator) lexeme;
            throw new ParseException(
                    "Encountered a separator '%c' while '%c' was expected."
                            .formatted(actual, value),
                    span
            );
        }
        throw new ParseException(
                "Expected a separator '%c'."
                        .formatted(value),
                span
        );
    }

    public @NotNull Lexeme ensureNonEOF(@Nullable Lexeme lexeme) throws ParseException {
        if (lexeme == null)
            throw new ParseException("Encountered an unexpected EOF.");
        return lexeme;
    }

    public @Nullable Lexeme peek() throws ParseException {
        if (this.buffer.isEmpty()) {
            var lexeme = this.lexer.next();
            if (lexeme != null)
                this.buffer.addLast(lexeme);
            return lexeme;
        }
        return this.buffer.peek();
    }

    public @Nullable Lexeme read() throws ParseException {
        if (this.buffer.isEmpty())
            return this.lexer.next();
        return this.buffer.poll();
    }
}
