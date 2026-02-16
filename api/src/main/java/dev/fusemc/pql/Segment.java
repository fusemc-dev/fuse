package dev.fusemc.pql;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public sealed interface Segment {

    @NotNull Tag resolve(@NotNull Tag tag);

    record Member(@NotNull String name) implements Segment {

        @Override
        public @NotNull Tag resolve(@NotNull Tag tag) {
            if (tag instanceof CompoundTag ct) {
                var member = ct.get(this.name);
                if (member != null)
                    return member;
                throw new RuntimeException("TODO: Member does not exist.");
            }
            throw new RuntimeException("TODO: Not a compound.");
        }

        @Override
        public @NonNull String toString() {
            return this.name;
        }
    }

    record Subscript(@NotNull Segment operand, int index) implements Segment {

        public Subscript {
            Objects.requireNonNull(operand);
            if (index < 0)
                throw new IllegalArgumentException("TODO: Index cannot be negative.");
        }

        @Override
        public @NotNull Tag resolve(@NotNull Tag tag) {
            var result = this.operand.resolve(tag);
            if (result instanceof CollectionTag ct) {
                if (this.index < ct.size())
                    return ct.get(this.index);
                throw new RuntimeException("TODO: Index out of bounds.");
            }
            throw new RuntimeException("TODO: Not a collection.");
        }

        @Override
        public @NonNull String toString() {
            return String.format("%s[%d]", this.operand, this.index);
        }
    }
}
