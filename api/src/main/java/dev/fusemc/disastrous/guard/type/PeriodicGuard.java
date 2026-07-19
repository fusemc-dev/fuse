package dev.fusemc.disastrous.guard.type;

import dev.fusemc.disastrous.disaster.WithLifetime;
import dev.fusemc.disastrous.guard.Guard;
import dev.fusemc.quelle.Diagnostic;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public record PeriodicGuard(int period) implements Guard<WithLifetime<?>> {

    public static final @NotNull Guard.Parser<PeriodicGuard> PARSER = (reader) -> {
        var position = reader.position();
        var period = reader.readInteger();
        if (period > 0)
            return new PeriodicGuard((int) period);
        throw new Diagnostic("The period must be a positive integer.", reader.range(position));
    };

    @Override
    public boolean satisfies(@NonNull WithLifetime<?> event) {
        return event.lifetime() % this.period == 0;
    }
}
