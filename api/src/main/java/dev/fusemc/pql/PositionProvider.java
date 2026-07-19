package dev.fusemc.pql;

import org.jetbrains.annotations.NotNull;

public interface PositionProvider {

    int compute(int length);

    static @NotNull PositionProvider adaptive(int n) {
        if (n < 0)
            return PositionProvider.last(Math.abs(n));
        return PositionProvider.element(n);
    }

    static @NotNull PositionProvider element(int n) {
        return new PositionProvider() {

            @Override
            public int compute(int length) {
                return n;
            }

            @Override
            public String toString() {
                return "[" + n + "]";
            }
        };
    }

    static @NotNull PositionProvider last(int n) {
        return new PositionProvider() {

            @Override
            public int compute(int length) {
                return length - n;
            }

            @Override
            public String toString() {
                return "[-" + n + "]";
            }
        };
    }
}
