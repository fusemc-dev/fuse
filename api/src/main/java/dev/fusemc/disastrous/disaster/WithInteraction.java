package dev.fusemc.disastrous.disaster;

import com.manchickas.optionated.Option;
import dev.fusemc.disastrous.Callback;
import dev.fusemc.tau.Template;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class WithInteraction<T extends Callback> implements Disaster<T> {

    public static final Template<InteractionResult> RESULT = Template.union(
            Template.literal("success").flatMap(
                    _ -> Option.some(InteractionResult.SUCCESS_SERVER),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.SUCCESS_SERVER)
                            .map(_ -> "success")
            ),
            Template.literal("pass").flatMap(
                    _ -> Option.some(InteractionResult.PASS),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.PASS)
                            .map(_ -> "pass")
            ),
            Template.literal("consume").flatMap(
                    _ -> Option.some(InteractionResult.CONSUME),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.CONSUME)
                            .map(_ -> "consume")
            ),
            Template.literal("fail").flatMap(
                    _ -> Option.some(InteractionResult.FAIL),
                    result -> Option.some(result)
                            .filter(i -> i == InteractionResult.FAIL)
                            .map(_ -> "fail")
            )
    );
    private static final Object2IntMap<InteractionResult> PRIORITY = Object2IntMap.ofEntries(
            Object2IntMap.entry(InteractionResult.PASS,           0),
            Object2IntMap.entry(InteractionResult.CONSUME,        1),
            Object2IntMap.entry(InteractionResult.SUCCESS_SERVER, 2),
            Object2IntMap.entry(InteractionResult.FAIL,           3)
    );

    protected @NotNull InteractionResult result;

    public WithInteraction() {
        this.result = InteractionResult.PASS;
    }

    protected boolean suggestResult(@NotNull InteractionResult result) {
        Objects.requireNonNull(result);
        var priority = WithInteraction.PRIORITY.getOrDefault(result, 0);
        if (priority > WithInteraction.PRIORITY.getOrDefault(this.result, 0))
            this.result = result;
        return result == InteractionResult.PASS;
    }

    public @NotNull InteractionResult result() {
        return this.result;
    }
}
