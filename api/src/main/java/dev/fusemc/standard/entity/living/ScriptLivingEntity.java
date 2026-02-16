package dev.fusemc.standard.entity.living;

import dev.fusemc.standard.entity.ScriptEntity;
import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class ScriptLivingEntity<E extends LivingEntity> extends ScriptEntity<E> {

    private static final @NotNull Template<Entity.RemovalReason> REMOVAL_REASON = Jet.union(
            Jet.literal("killed").map(
                    _ -> Entity.RemovalReason.KILLED,
                    _ -> "killed"
            ),
            Jet.literal("discarded").map(
                    _ -> Entity.RemovalReason.DISCARDED,
                    _ -> "discarded"
            ),
            Jet.literal("changed_dimension").map(
                    _ -> Entity.RemovalReason.CHANGED_DIMENSION,
                    _ -> "changed_dimensions"
            )
    );

    public ScriptLivingEntity(@NonNull E wrapped) {
        super(wrapped);
    }

    @HostAccess.Export
    public float health() {
        return this.wrapped.getHealth();
    }

    @HostAccess.Export
    public void remove(@NotNull Value reason) throws TypeException {
        this.wrapped.remove(Jet.expect(REMOVAL_REASON, reason));
    }
}
