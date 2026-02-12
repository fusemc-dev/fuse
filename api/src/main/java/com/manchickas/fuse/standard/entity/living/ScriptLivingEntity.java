package com.manchickas.fuse.standard.entity.living;

import com.manchickas.fuse.standard.entity.ScriptEntity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.NonNull;

public class ScriptLivingEntity<E extends LivingEntity> extends ScriptEntity<E> {

    public ScriptLivingEntity(@NonNull E wrapped) {
        super(wrapped);
    }
}
