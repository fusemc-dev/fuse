package dev.fusemc.pql.segment;

import com.manchickas.optionated.Option;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Property(@NotNull String name) implements Segment {

    @Override
    public @NotNull Option<Tag> resolve(@NotNull Tag parent) {
        Objects.requireNonNull(parent);
        if (parent instanceof CompoundTag compound)
            return Option.fromNullable(compound.get(this.name));
        return Option.none();
    }

    @Override
    public boolean update(@NotNull Tag parent, @NotNull Tag value) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(value);
        if (parent instanceof CompoundTag compound) {
            compound.put(this.name, value);
            return true;
        }
        return false;
    }
}
