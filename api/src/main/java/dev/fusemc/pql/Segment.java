package dev.fusemc.pql;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public sealed interface Segment {

    @NotNull Tag resolve(@NotNull Tag tag) throws PathStructureException;
    void set(@NotNull Tag tag, @NotNull Tag value) throws PathStructureException;

    record Member(@NotNull String name) implements Segment {

        @Override
        public @NotNull Tag resolve(@NotNull Tag tag) {
            if (tag instanceof CompoundTag ct) {
                var member = ct.get(this.name);
                if (member != null)
                    return member;
                throw new PathStructureException("Member does not exist.");
            }
            throw new PathStructureException("Not a compound.");
        }

        @Override
        public void set(@NotNull Tag tag, @NotNull Tag value) {
            if (tag instanceof CompoundTag ct) {
                ct.put(this.name, value);
                return;
            }
            throw new PathStructureException("Not a compound.");
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
                throw new PathStructureException("Index cannot be negative.");
        }

        @Override
        public @NotNull Tag resolve(@NotNull Tag tag) {
            var result = this.operand.resolve(tag);
            if (result instanceof CollectionTag ct) {
                if (this.index < ct.size())
                    return ct.get(this.index);
                throw new PathStructureException("Index out of bounds.");
            }
            throw new PathStructureException("Not a collection.");
        }

        @Override
        public void set(@NotNull Tag tag, @NotNull Tag value) {
            var result = this.operand.resolve(tag);
            if (result instanceof CollectionTag ct) {
                if (this.index < ct.size()) {
                    ct.setTag(this.index, value);
                    return;
                }
                throw new PathStructureException("Index out of bounds.");
            }
            throw new PathStructureException("Not a collection.");
        }

        @Override
        public @NonNull String toString() {
            return String.format("%s[%d]", this.operand, this.index);
        }
    }
}
