package dev.fusemc.disastrous.disaster;

import dev.fusemc.disastrous.Callback;
import dev.fusemc.disastrous.Disaster;
import dev.fusemc.standard.ProxyHand;
import org.jetbrains.annotations.NotNull;

public interface WithHand<T extends Callback> extends Disaster<T> {

    @NotNull ProxyHand hand();
}
