package com.manchickas.fuse.standard.event;

import com.manchickas.fuse.disastrous.event.Event;
import com.manchickas.fuse.disastrous.event.EventCallback;
import com.manchickas.fuse.standard.entity.living.ScriptPlayer;
import org.jetbrains.annotations.NotNull;

public interface PlayerEvent<E extends PlayerEvent<E, C>, C extends EventCallback> extends Event<E, C> {

    @NotNull ScriptPlayer player();
}
