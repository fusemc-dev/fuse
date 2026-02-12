package com.manchickas.fuse.standard.entity;

import com.manchickas.fuse.standard.ScriptWrapper;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public class ScriptEntity<E extends Entity> extends ScriptWrapper<E> {

    public ScriptEntity(@NonNull E wrapped) {
        super(wrapped);
    }
}
