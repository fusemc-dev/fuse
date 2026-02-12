package com.manchickas.fuse.disastrous.guard;

import com.manchickas.fuse.ParseException;
import com.manchickas.fuse.disastrous.event.Event;
import com.manchickas.fuse.disastrous.selector.Parser;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GuardType<E extends Event<?, ?>, G extends Guard<E>> {

    @NotNull G parse(@NotNull Parser parser) throws ParseException;
}
