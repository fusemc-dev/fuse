package com.manchickas.fuse;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;

public final class ArrayBuilder<T> {

    private T[] buffer;
    private int length;

    @SuppressWarnings("unchecked")
    public ArrayBuilder(int capacity) {
        this.buffer = (T[]) new Object[capacity];
        this.length = 0;
    }

    public ArrayBuilder<T> append(T value) {
        if (this.length >= this.buffer.length)
            this.buffer = Arrays.copyOf(this.buffer, this.buffer.length * 2);
        this.buffer[this.length++] = value;
        return this;
    }

    public T @NotNull[] build(@NotNull IntFunction<T @NotNull[]> constructor) {
        var result = Objects.requireNonNull(constructor)
                .apply(this.length);
        System.arraycopy(this.buffer, 0, result, 0, this.length);
        return result;
    }
}
