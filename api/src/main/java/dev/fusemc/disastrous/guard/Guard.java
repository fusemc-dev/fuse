package dev.fusemc.disastrous.guard;

import dev.fusemc.disastrous.Disaster;
import dev.fusemc.quelle.Diagnostic;
import dev.fusemc.quelle.StringReader;
import org.jetbrains.annotations.NotNull;

public interface Guard<E extends Disaster<?>> {

    boolean satisfies(@NotNull E event);

    interface Parser<G extends Guard<?>> {

        @NotNull G parse(@NotNull StringReader<String> reader) throws Diagnostic;
    }
}
