package dev.fusemc.disastrous.guard;

import dev.fusemc.ParseException;
import dev.fusemc.disastrous.event.Event;
import dev.fusemc.disastrous.selector.Parser;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GuardType<E extends Event<?, ?>, G extends Guard<E>> {

    @NotNull G parse(@NotNull Parser parser) throws ParseException;
}
