package dev.fusemc.disastrous.guard.type;

import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.disastrous.disaster.WithHand;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class HandGuard implements Guard<WithHand<?>> {

    private static final @NotNull HandGuard MAIN = new HandGuard(InteractionHand.MAIN_HAND);
    private static final @NotNull HandGuard OFF  = new HandGuard(InteractionHand.OFF_HAND);

    public static final @NotNull Guard.Type<HandGuard> TYPE = (reader) -> {
        var position   = reader.position();
        var identifier = reader.readIdentifier((c) -> c >= 'a' && c <= 'z');
        return switch (identifier) {
            case "main" -> HandGuard.MAIN;
            case "off"  -> HandGuard.OFF;
            default     -> throw new Diagnostic(String.format("Encountered an unrecognized hand '%s'.", identifier), reader.range(position));
        };
    };

    private final @NotNull InteractionHand hand;

    private HandGuard(@NotNull InteractionHand hand) {
        this.hand = Objects.requireNonNull(hand);
    }

    @Override
    public boolean satisfies(@NonNull WithHand<?> disaster) {
        var other = disaster.hand();
        return other.matches(this.hand);
    }
}
