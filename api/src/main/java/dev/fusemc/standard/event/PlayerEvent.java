package dev.fusemc.standard.event;

import dev.fusemc.disastrous.event.Event;
import dev.fusemc.disastrous.event.EventCallback;
import dev.fusemc.standard.entity.living.ScriptPlayer;
import org.jetbrains.annotations.NotNull;

public interface PlayerEvent<E extends PlayerEvent<E, C>, C extends EventCallback> extends Event<E, C> {

    @NotNull ScriptPlayer player();
}
