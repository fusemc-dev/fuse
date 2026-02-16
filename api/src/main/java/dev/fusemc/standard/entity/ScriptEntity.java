package dev.fusemc.standard.entity;

import dev.fusemc.standard.ScriptWrapper;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public class ScriptEntity<E extends Entity> extends ScriptWrapper<E> {

    public ScriptEntity(@NonNull E wrapped) {
        super(wrapped);
    }
}
