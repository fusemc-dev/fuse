package dev.fusemc.standard.entity;

import dev.fusemc.tau.Documented;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Documented("Living")
public class ProxyLiving<T extends LivingEntity> extends ProxyEntity<T> {

    protected ProxyLiving(@NotNull T self) {
        super(Objects.requireNonNull(self));
    }
}
