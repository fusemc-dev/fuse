package dev.fusemc.disastrous.disaster;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.standard.server.ProxyServer;
import org.jetbrains.annotations.NotNull;

public interface WithLifetime<T extends Callback> extends Disaster<T> {

    int lifetime();
}
